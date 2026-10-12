package com.wildfiresync.experiments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wildfiresync.core.NodeId;
import com.wildfiresync.core.OpId;
import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SyncStrategyTest {

	private final LogGenerator generator = new LogGenerator();

	@Test
	void generatedTabletsHoldAboutTheRequestedNumberOfEvents() {
		LogGenerator.Pair pair = generator.generate(10, 1_000, 0.5, 1);

		assertTrue(Math.abs(pair.a().size() - 1_000) < 150, "A holds " + pair.a().size());
		assertTrue(Math.abs(pair.b().size() - 1_000) < 150, "B holds " + pair.b().size());
	}

	@Test
	void bothDesignsLeaveBothTabletsWithTheSameEvents() {
		for (SyncStrategy strategy : SyncStrategy.values()) {
			LogGenerator.Pair pair = generator.generate(10, 500, 0.9, 7);

			SyncResult result = strategy.sync(pair.a(), pair.b());

			assertTrue(result.converged(), strategy.name());
			assertEquals(pair.a().size(), pair.b().size(), strategy.name());
		}
	}

	@Test
	void bothDesignsEndWithTheUnionOfTheTwoLogs() {
		LogGenerator.Pair whole = generator.generate(10, 500, 0.5, 3);
		LogGenerator.Pair delta = generator.generate(10, 500, 0.5, 3);

		SyncStrategy.WHOLE_LOG.sync(whole.a(), whole.b());
		SyncStrategy.CAUSAL_CONTEXT.sync(delta.a(), delta.b());

		assertEquals(whole.a().context().counters(), delta.a().context().counters());
		assertEquals(whole.a().size(), delta.a().size());
	}

	@Test
	void sendingTheWholeLogDeliversEventsTheOtherTabletAlreadyHolds() {
		LogGenerator.Pair pair = generator.generate(10, 500, 0.9, 5);

		SyncResult result = SyncStrategy.WHOLE_LOG.sync(pair.a(), pair.b());

		assertTrue(result.duplicatesReceived() > 0);
	}

	@Test
	void theCausalContextDesignNeverSendsAnEventTheOtherTabletHolds() {
		LogGenerator.Pair pair = generator.generate(10, 500, 0.9, 5);

		SyncResult result = SyncStrategy.CAUSAL_CONTEXT.sync(pair.a(), pair.b());

		assertEquals(0, result.duplicatesReceived());
	}

	@Test
	void theCausalContextDesignSendsFewerBytesWhenMostEventsAreShared() {
		LogGenerator.Pair whole = generator.generate(10, 1_000, 0.99, 2);
		LogGenerator.Pair delta = generator.generate(10, 1_000, 0.99, 2);

		long wholeBytes = SyncStrategy.WHOLE_LOG.sync(whole.a(), whole.b()).bytesSent();
		long deltaBytes = SyncStrategy.CAUSAL_CONTEXT.sync(delta.a(), delta.b()).bytesSent();

		assertTrue(deltaBytes < wholeBytes, deltaBytes + " against " + wholeBytes);
	}

	@Test
	void withNothingSharedBothDesignsSendTheSameEvents() {
		LogGenerator.Pair whole = generator.generate(10, 500, 0.0, 4);
		LogGenerator.Pair delta = generator.generate(10, 500, 0.0, 4);

		SyncResult wholeResult = SyncStrategy.WHOLE_LOG.sync(whole.a(), whole.b());
		SyncResult deltaResult = SyncStrategy.CAUSAL_CONTEXT.sync(delta.a(), delta.b());

		assertEquals(wholeResult.eventsSent(), deltaResult.eventsSent());
		assertEquals(0, wholeResult.duplicatesReceived());
	}

	@Test
	void identicalTabletsStillConvergeAndTheCausalContextDesignSendsNoEvents() {
		LogGenerator.Pair pair = generator.generate(10, 500, 1.0, 6);

		SyncResult result = SyncStrategy.CAUSAL_CONTEXT.sync(pair.a(), pair.b());

		assertTrue(result.converged());
		assertEquals(0, result.eventsSent());
	}

	@Test
	void aReplicaRefusesAnEventThatWouldLeaveAGap() {
		Replica replica = new Replica();
		Event second = new Event(new UUID(1, 2), new UUID(2, 1), Event.RecordType.REPORT, Event.Operation.SET_FIELD,
				Field.REPORT_SEVERITY.key(), "4", "crew-000", Instant.parse("2026-10-01T06:00:02Z"),
				new OpId(new NodeId("crew-000"), 2), CausalContext.EMPTY, Set.of());

		assertThrows(IllegalStateException.class, () -> replica.add(new WireEvent(second, 100)));
		assertEquals(0, replica.size());
	}

}
