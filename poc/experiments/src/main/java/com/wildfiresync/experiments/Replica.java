package com.wildfiresync.experiments;

import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.OpId;
import com.wildfiresync.core.schema.CausalContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One tablet's copy of the event log. Events of one author are held as a prefix (sequence numbers 1 to n),
 * which is what lets a single counter per author describe everything the replica has seen.
 */
public final class Replica {

	private final Map<NodeId, List<WireEvent>> byAuthor = new TreeMap<>();

	/** Adds an event. False if it is already held. Throws if it would leave a gap in its author's prefix. */
	public boolean add(WireEvent event) {
		OpId dot = event.operation().id();
		List<WireEvent> log = byAuthor.computeIfAbsent(dot.node(), node -> new ArrayList<>());
		if (dot.seq() <= log.size()) {
			return false;
		}
		if (dot.seq() != log.size() + 1) {
			throw new IllegalStateException("gap before " + dot + ", the log holds " + log.size() + " events of this author");
		}
		log.add(event);
		return true;
	}

	public int size() {
		return byAuthor.values().stream().mapToInt(List::size).sum();
	}

	/** Highest sequence number held per author. */
	public CausalContext context() {
		Map<NodeId, Long> counters = new TreeMap<>();
		byAuthor.forEach((node, log) -> counters.put(node, (long) log.size()));
		return new CausalContext(counters);
	}

	public List<WireEvent> all() {
		List<WireEvent> events = new ArrayList<>();
		byAuthor.values().forEach(events::addAll);
		return events;
	}

	/** The events the other side has not seen, in the order they have to be delivered. */
	public List<WireEvent> notCoveredBy(CausalContext other) {
		List<WireEvent> events = new ArrayList<>();
		for (List<WireEvent> log : byAuthor.values()) {
			for (WireEvent event : log) {
				if (!other.covers(event.operation().id())) {
					events.add(event);
				}
			}
		}
		return events;
	}

	public boolean holdsSameEventsAs(Replica other) {
		return context().counters().equals(other.context().counters());
	}

}
