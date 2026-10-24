package com.wildfiresync.server.service;

import java.util.List;
import java.util.Optional;

import com.wildfiresync.core.EventBundle;
import com.wildfiresync.core.MergeCore;
import com.wildfiresync.core.RecordRef;
import com.wildfiresync.core.World;
import com.wildfiresync.core.Zone;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.FieldValues;
import com.wildfiresync.server.nats.ReportPublisher;
import com.wildfiresync.server.store.Database;
import com.wildfiresync.server.store.EventStore;
import com.wildfiresync.server.store.Projections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Merges an event into this replica and passes the record on to the tablets in its zone.
 *
 * <p>The same two steps the tablet does: store the event, then rebuild the record from every event of
 * it with the merge library. Storing it is what makes the result independent of the order events
 * arrive in, and rebuilding from the whole list is what makes applying one twice harmless.
 *
 * <p>Publishing happens after the transaction commits, never inside it, so a broker that is slow or
 * away cannot hold a database transaction open. If it fails the event is already stored and the
 * consumer asks for the message again; the second attempt stores nothing and publishes the same
 * record, so the retry is safe.
 */
@Service
public class MergeService {

	private static final Logger log = LoggerFactory.getLogger(MergeService.class);

	private final Database database;

	private final EventStore events;

	private final Projections projections;

	private final ReportPublisher publisher;

	public MergeService(Database database, EventStore events, Projections projections, ReportPublisher publisher) {
		this.database = database;
		this.events = events;
		this.projections = projections;
		this.publisher = publisher;
	}

	public void apply(Event event) {
		RecordRef record = new RecordRef(event.recordType(), event.recordId());
		Merged merged = database.inTransaction(connection -> {
			boolean report = record.type() == Event.RecordType.REPORT;
			Optional<FieldValues.Location> before = report ? projections.locationOf(connection, record.id())
					: Optional.empty();
			boolean stored = events.insertIfAbsent(connection, event);
			List<Event> all = events.forRecord(connection, record.id());
			// an event already held changed nothing, so the rows and the location stand as they are
			Optional<FieldValues.Location> after = stored
					? projections.write(connection, record, MergeCore.applyAll(World.EMPTY, all)) : before;
			return new Merged(all, before, after, stored);
		});

		if (merged.stored()) {
			log.info("merged {} {} into {} {}, now {} event(s)", event.dot(), event.operation(), record.type(),
					record.id(), merged.events().size());
		}
		else {
			log.debug("{} was already held, republishing {} {}", event.dot(), record.type(), record.id());
		}
		broadcast(record, merged);
	}

	/**
	 * Sends the record to the tablets subscribed to its zone. Only reports are published: an incident is
	 * not somewhere on the map, so it has no zone to be published under.
	 */
	private void broadcast(RecordRef record, Merged merged) {
		if (record.type() != Event.RecordType.REPORT) {
			return;
		}
		Optional<String> was = merged.before().map(Zone::of);
		Optional<String> now = merged.after().map(Zone::of);
		if (was.isPresent() && !was.equals(now)) {
			// the report moved out of that zone, so leave nothing stale behind on the old subject
			publisher.withdraw(was.get(), record.id());
		}
		if (now.isPresent()) {
			publisher.publish(now.get(), EventBundle.of(record, merged.events()));
		}
		else {
			// MANUAL means two crews can disagree about where a report is, and then it belongs to no zone
			log.info("report {} has no settled location, so there is no zone to publish it to", record.id());
		}
	}

	private record Merged(List<Event> events, Optional<FieldValues.Location> before,
			Optional<FieldValues.Location> after, boolean stored) {
	}

}
