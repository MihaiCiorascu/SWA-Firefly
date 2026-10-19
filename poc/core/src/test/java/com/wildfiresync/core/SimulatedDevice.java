package com.wildfiresync.core;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.CreateValues;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class SimulatedDevice {

	private final NodeId id;

	private final String actor;

	private final long idPrefix;

	private final Instant clock;

	private final Map<OpId, Event> log = new LinkedHashMap<>();

	private long counter;

	private World world = World.EMPTY;

	SimulatedDevice(String id, long idPrefix, Instant clock) {
		this.id = new NodeId(id);
		this.actor = "crew-" + id;
		this.idPrefix = idPrefix;
		this.clock = clock;
	}

	SimulatedDevice(String id, long idPrefix) {
		this(id, idPrefix, Instant.parse("2026-10-04T09:00:00Z"));
	}

	World world() {
		return world;
	}

	List<Event> log() {
		return new ArrayList<>(log.values());
	}

	Event create(RecordRef record, Map<Field, FieldValue> values) {
		return author(record, Event.Operation.CREATE, null, CreateValues.format(record.type(), values));
	}

	Event set(RecordRef record, Field field, FieldValue value) {
		return author(record, Event.Operation.SET_FIELD, field, FieldValues.format(value));
	}

	Event resolve(RecordRef record, Field field, FieldValue value, Set<UUID> supersedes) {
		return author(record, Event.Operation.RESOLVE_CONFLICT, field, FieldValues.format(value), supersedes);
	}

	private Event author(RecordRef record, Event.Operation operation, Field field, String value) {
		return author(record, operation, field, value, Set.of());
	}

	private Event author(RecordRef record, Event.Operation operation, Field field, String value,
			Set<UUID> supersedes) {
		counter++;
		Event event = new Event(new UUID(idPrefix, counter), record.id(), record.type(), operation,
				field == null ? null : field.key(), value, actor, clock, new OpId(id, counter),
				CausalContext.EMPTY, supersedes);
		receive(event);
		return event;
	}

	void receive(Event event) {
		if (log.putIfAbsent(event.dot(), event) != null) {
			return;
		}
		world = MergeCore.apply(world, event);
	}

	void receiveAll(List<Event> events) {
		events.forEach(this::receive);
	}

	Optional<Resolution> resolution(RecordRef record, Field field) {
		return world.record(record).flatMap(state -> state.field(field))
				.map(state -> MergeCore.resolve(field, state));
	}

	World rebuildFromLog() {
		return MergeCore.applyAll(World.EMPTY, log.values());
	}

}
