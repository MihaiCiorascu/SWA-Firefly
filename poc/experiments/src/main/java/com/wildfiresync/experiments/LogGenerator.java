package com.wildfiresync.experiments;

import com.wildfiresync.core.EventMessage;
import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.OpId;
import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

/**
 * Builds two tablets whose logs overlap by a given share.
 *
 * The union of both logs holds about 2n / (1 + overlap) events, spread over the authors with random weights.
 * Both tablets hold the same prefix of every author (the shared part). The rest of an author's events, its tail,
 * is held by one tablet only: tablet A for authors with an even index, tablet B for the others. Each tablet therefore
 * ends up with about n events, and each has events the other lacks unless the overlap is 100 percent.
 */
public final class LogGenerator {

	private static final Instant START = Instant.parse("2026-10-01T06:00:00Z");

	private static final Field.Range SEVERITY = Field.REPORT_SEVERITY.range().orElseThrow();

	private final ObjectMapper mapper = new ObjectMapper();

	public record Pair(Replica a, Replica b) {
	}

	public Pair generate(int authors, int eventsPerTablet, double overlap, long seed) {
		if (authors < 2) {
			throw new IllegalArgumentException("at least two authors are needed so both tablets have news for the other");
		}
		Random random = new Random(seed);
		long union = Math.round(2.0 * eventsPerTablet / (1.0 + overlap));

		double[] weights = new double[authors];
		double total = 0;
		for (int i = 0; i < authors; i++) {
			weights[i] = 0.5 + random.nextDouble();
			total += weights[i];
		}

		Replica a = new Replica();
		Replica b = new Replica();
		for (int author = 0; author < authors; author++) {
			NodeId node = new NodeId(String.format("crew-%03d", author));
			int length = (int) Math.max(1, Math.round(union * weights[author] / total));
			int shared = (int) Math.round(overlap * length);
			Replica holdsAll = author % 2 == 0 ? a : b;
			Replica holdsShared = author % 2 == 0 ? b : a;
			for (int seq = 1; seq <= length; seq++) {
				WireEvent event = event(node, seq, random);
				holdsAll.add(event);
				if (seq <= shared) {
					holdsShared.add(event);
				}
			}
		}
		return new Pair(a, b);
	}

	private WireEvent event(NodeId node, int seq, Random random) {
		Event event = new Event(
				UUID.nameUUIDFromBytes((node.value() + ":" + seq).getBytes(StandardCharsets.UTF_8)),
				UUID.nameUUIDFromBytes(("report-" + random.nextInt(50)).getBytes(StandardCharsets.UTF_8)),
				Event.RecordType.REPORT,
				Event.Operation.SET_FIELD,
				Field.REPORT_SEVERITY.key(),
				Integer.toString(SEVERITY.min() + random.nextInt(SEVERITY.max() - SEVERITY.min() + 1)),
				node.value(),
				START.plusSeconds(seq),
				new OpId(node, seq),
				CausalContext.EMPTY,
				Set.of());
		return new WireEvent(event, mapper.writeValueAsBytes(EventMessage.of(event)).length);
	}

}
