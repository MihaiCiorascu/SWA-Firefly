package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Edge cases of the merge logic. Each test is one case and shows what the merge does with it.
 *
 * <ul>
 * <li>Equal severity written on two devices: both end with the same value, credited to the lower event id.</li>
 * <li>A resolution written by a device that saw only one of two conflicting values: the other value stays in conflict with the resolution.</li>
 * <li>Two creates of one record with different immutable values: the lower event id wins on every device.</li>
 * <li>A field write that arrives before the create of its record: the world is the same in both delivery orders.</li>
 * <li>A field from a newer app version: the merge refuses it and the world stays as it was.</li>
 * <li>A resolution when there is no conflict: it acts like a plain write.</li>
 * </ul>
 */
class EdgeCaseTests {

	private static final RecordRef REPORT = RecordRef.report(new UUID(8, 1));

	private SimulatedDevice alpha;

	private SimulatedDevice bravo;

	private SimulatedDevice charlie;

	@BeforeEach
	void devicesOffline() {
		alpha = new SimulatedDevice("tablet-a", 1);
		bravo = new SimulatedDevice("tablet-b", 2);
		charlie = new SimulatedDevice("tablet-c", 3);
	}

	private void reconnect() {
		List<Event> inFlight = new ArrayList<>(alpha.log());
		inFlight.addAll(bravo.log());
		inFlight.addAll(charlie.log());
		alpha.receiveAll(inFlight);
		bravo.receiveAll(inFlight);
		charlie.receiveAll(inFlight);
	}

	private Resolution.Settled settledOn(SimulatedDevice device, Field field) {
		return assertInstanceOf(Resolution.Settled.class, device.resolution(REPORT, field).orElseThrow());
	}

	private Resolution.Conflicted conflictOn(SimulatedDevice device, Field field) {
		return assertInstanceOf(Resolution.Conflicted.class, device.resolution(REPORT, field).orElseThrow());
	}

	@Test
	void equalSeverityOnTwoDevicesIsCreditedToTheLowerEventId() {
		Event first = alpha.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(5));
		Event second = bravo.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(5));

		reconnect();

		Resolution.Settled onAlpha = settledOn(alpha, Field.REPORT_SEVERITY);
		Resolution.Settled onBravo = settledOn(bravo, Field.REPORT_SEVERITY);
		UUID lower = first.eventId().compareTo(second.eventId()) <= 0 ? first.eventId() : second.eventId();
		assertEquals(new FieldValue.Num(5), onAlpha.value());
		assertEquals(onAlpha, onBravo);
		assertEquals(Optional.of(lower), onAlpha.source());
	}

	@Test
	void aResolutionThatSawOnlyOneOfTwoValuesLeavesTheOtherInConflict() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		bravo.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Evacuation route B blocked"));
		// charlie got alpha's value only, so its resolution covers that one and not bravo's
		charlie.receive(alpha.log().getFirst());
		charlie.settle(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Agreed wording"));

		reconnect();

		List<FieldValue> expected = List.of(new FieldValue.Text("Evacuation route B blocked"),
				new FieldValue.Text("Agreed wording"));
		for (SimulatedDevice device : List.of(alpha, bravo, charlie)) {
			List<FieldValue> values = conflictOn(device, Field.REPORT_DESCRIPTION).candidates().stream()
					.map(Candidate::value).toList();
			assertEquals(expected, values);
		}
		assertEquals(alpha.world(), bravo.world());
		assertEquals(alpha.world(), charlie.world());
	}

	@Test
	void twoCreatesWithDifferentImmutableValuesSettleOnTheLowerEventId() {
		UUID firstIncident = new UUID(8, 2);
		UUID secondIncident = new UUID(8, 3);
		Event first = alpha.create(REPORT, Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(firstIncident)));
		Event second = bravo.create(REPORT, Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(secondIncident)));

		reconnect();

		boolean alphaIsLower = first.eventId().compareTo(second.eventId()) < 0;
		FieldValue expected = new FieldValue.Reference(alphaIsLower ? firstIncident : secondIncident);
		assertEquals(expected, settledOn(alpha, Field.REPORT_INCIDENT_ID).value());
		assertEquals(expected, settledOn(bravo, Field.REPORT_INCIDENT_ID).value());
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void aFieldWriteThatArrivesBeforeItsCreateGivesTheSameWorldInBothOrders() {
		UUID incident = new UUID(8, 2);
		Event create = alpha.create(REPORT, Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(incident)));
		Event write = alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));

		bravo.receive(write);
		assertFalse(bravo.world().record(REPORT).orElseThrow().created());
		bravo.receive(create);

		charlie.receive(create);
		charlie.receive(write);

		assertTrue(bravo.world().record(REPORT).orElseThrow().created());
		assertEquals(charlie.world(), bravo.world());
	}

	@Test
	void aFieldFromANewerAppVersionIsRefusedAndLeavesTheWorldAlone() {
		alpha.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(5));
		World before = alpha.world();
		Event fromNewerVersion = new Event(new UUID(8, 9), REPORT.id(), REPORT.type(), Event.Operation.SET_FIELD,
				"windSpeed", "40", "crew-z", Instant.EPOCH, new OpId(new NodeId("tablet-z"), 1), CausalContext.EMPTY,
				Set.of());

		assertThrows(IllegalArgumentException.class, () -> MergeCore.validate(fromNewerVersion));
		assertThrows(IllegalArgumentException.class, () -> MergeCore.apply(before, fromNewerVersion));
		assertEquals(before, alpha.world());
	}

	@Test
	void aResolutionWithoutAConflictActsLikeAPlainWrite() {
		alpha.set(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge"));
		alpha.settle(REPORT, Field.REPORT_DESCRIPTION, new FieldValue.Text("Fire crossed the ridge, wind turning"));

		reconnect();

		Resolution.Settled onAlpha = settledOn(alpha, Field.REPORT_DESCRIPTION);
		assertEquals(new FieldValue.Text("Fire crossed the ridge, wind turning"), onAlpha.value());
		assertEquals(onAlpha, settledOn(bravo, Field.REPORT_DESCRIPTION));
	}

}
