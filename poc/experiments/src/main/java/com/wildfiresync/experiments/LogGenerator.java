package com.wildfiresync.experiments;

import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.OpId;
import com.wildfiresync.core.OperationEvent;
import com.wildfiresync.core.ReportId;
import com.wildfiresync.core.SetSeverity;
import java.time.Instant;
import java.util.Random;
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
		SetSeverity operation = new SetSeverity(
				new OpId(node, seq),
				new ReportId("report-" + random.nextInt(50)),
				SetSeverity.MIN + random.nextInt(SetSeverity.MAX - SetSeverity.MIN + 1),
				START.plusSeconds(seq));
		return new WireEvent(operation, mapper.writeValueAsBytes(OperationEvent.of(operation)).length);
	}

}
