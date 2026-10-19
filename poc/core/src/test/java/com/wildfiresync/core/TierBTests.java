package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TierBTests {

	private static final RecordRef REPORT = RecordRef.report(new UUID(7, 1));

	private SimulatedDevice alpha;

	private SimulatedDevice bravo;

	@BeforeEach
	void twoDevicesOffline() {
		alpha = new SimulatedDevice("tablet-a", 1);
		bravo = new SimulatedDevice("tablet-b", 2);
	}

	private void reconnect() {
		List<Event> inFlight = new ArrayList<>(alpha.log());
		inFlight.addAll(bravo.log());
		alpha.receiveAll(inFlight);
		bravo.receiveAll(inFlight);
	}

	private Resolution.Conflicted conflictOn(SimulatedDevice device) {
		return assertInstanceOf(Resolution.Conflicted.class,
				device.resolution(REPORT, Field.REPORT_DESCRIPTION).orElseThrow());
	}

	private FieldValue settledOn(SimulatedDevice device) {
		return assertInstanceOf(Resolution.Settled.class,
				device.resolution(REPORT, Field.REPORT_DESCRIPTION).orElseThrow()).value();
	}

	@Test
	void concurrentDescriptionsBecomeAConflictEveryDeviceSeesIdentically() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));

		reconnect();

		assertEquals(2, conflictOn(alpha).candidates().size());
		assertEquals(conflictOn(bravo).candidates(), conflictOn(alpha).candidates());
		// Tier B converges too: the devices agree exactly on which values are in conflict.
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void aSingleValueNeedsNoResolving() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));

		reconnect();

		Resolution.Settled settled = assertInstanceOf(Resolution.Settled.class,
				bravo.resolution(REPORT, Field.REPORT_DESCRIPTION).orElseThrow());
		assertEquals(new FieldValue.Text("Fire crossed the ridge"), settled.value());
	}

	@Test
	void candidatesCarryWhoWroteThem() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));

		reconnect();

		List<String> actors = conflictOn(alpha).candidates().stream().map(Candidate::actorId).toList();
		assertEquals(List.of("crew-tablet-a", "crew-tablet-b"), actors);
	}


	@Test
	void aWriterWhoSawTheOtherValueSupersedesIt() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.receiveAll(alpha.log());
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge, wind turning"));

		reconnect();

		assertEquals(new FieldValue.Text("Fire crossed the ridge, wind turning"), settledOn(alpha));
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void aResolutionSupersedesTheConflictItSaw() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));
		reconnect();

		alpha.settle(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Ridge crossed, route B blocked"));
		reconnect();

		assertEquals(new FieldValue.Text("Ridge crossed, route B blocked"), settledOn(bravo));
	}

	@Test
	void concurrentResolutionsBecomeANewConflict() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));
		reconnect();

		alpha.settle(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Ridge crossed"));
		bravo.settle(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Route B blocked, use route C"));
		reconnect();

		List<FieldValue> values = conflictOn(alpha).candidates().stream().map(Candidate::value).toList();
		assertEquals(List.of(new FieldValue.Text("Ridge crossed"), new FieldValue.Text("Route B blocked, use route C")),
				values);
	}

	@Test
	void onlyATierBFieldCanBeResolved() {
		assertThrows(IllegalArgumentException.class,
				() -> alpha.settle(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(5)));
	}

	@Test
	void anEventCannotHaveSeenItself() {
		OpId dot = new OpId(new NodeId("tablet-c"), 2);
		assertThrows(IllegalArgumentException.class,
				() -> new Event(new UUID(3, 3), REPORT.id(), REPORT.type(), Event.Operation.SET_FIELD,
						Field.REPORT_DESCRIPTION.key(), "smoke", "crew-lead", Instant.EPOCH, dot,
						new CausalContext(Map.of(dot.node(), 2L)), Set.of()));
	}

	@Test
	void aCreateEventCarriesNoField() {
		assertThrows(IllegalArgumentException.class,
				() -> new Event(new UUID(3, 2), REPORT.id(), REPORT.type(), Event.Operation.CREATE, "severity", "5",
						"crew-lead", Instant.EPOCH, new OpId(new NodeId("tablet-c"), 2), CausalContext.EMPTY,
						Set.of()));
	}

}
