package com.wildfiresync.experiments;

import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.schema.CausalContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/** The two designs for what two replicas exchange when they sync. */
public enum SyncStrategy {

	/** Each tablet sends its whole log, the receiver ignores what it already holds. */
	WHOLE_LOG {
		@Override
		public SyncResult sync(Replica a, Replica b) {
			long start = System.nanoTime();
			List<WireEvent> fromA = a.all();
			List<WireEvent> fromB = b.all();
			long duplicates = deliver(fromA, b) + deliver(fromB, a);
			long nanos = System.nanoTime() - start;
			return new SyncResult(bytes(fromA) + bytes(fromB), fromA.size() + fromB.size(), duplicates, 1, nanos,
					a.holdsSameEventsAs(b));
		}
	},

	/** Each tablet first sends its causal context, then only the events the other side does not cover. */
	CAUSAL_CONTEXT {
		@Override
		public SyncResult sync(Replica a, Replica b) {
			long start = System.nanoTime();
			CausalContext contextA = a.context();
			CausalContext contextB = b.context();
			List<WireEvent> fromA = a.notCoveredBy(contextB);
			List<WireEvent> fromB = b.notCoveredBy(contextA);
			long duplicates = deliver(fromA, b) + deliver(fromB, a);
			long nanos = System.nanoTime() - start;
			long bytes = bytes(contextA) + bytes(contextB) + bytes(fromA) + bytes(fromB);
			return new SyncResult(bytes, fromA.size() + fromB.size(), duplicates, 2, nanos, a.holdsSameEventsAs(b));
		}
	};

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public abstract SyncResult sync(Replica a, Replica b);

	private static long deliver(List<WireEvent> events, Replica receiver) {
		long duplicates = 0;
		for (WireEvent event : events) {
			if (!receiver.add(event)) {
				duplicates++;
			}
		}
		return duplicates;
	}

	private static long bytes(List<WireEvent> events) {
		long total = 0;
		for (WireEvent event : events) {
			total += event.wireBytes();
		}
		return total;
	}

	private static long bytes(CausalContext context) {
		Map<String, Long> counters = new HashMap<>();
		for (Map.Entry<NodeId, Long> entry : context.counters().entrySet()) {
			counters.put(entry.getKey().value(), entry.getValue());
		}
		return MAPPER.writeValueAsBytes(counters).length;
	}

}
