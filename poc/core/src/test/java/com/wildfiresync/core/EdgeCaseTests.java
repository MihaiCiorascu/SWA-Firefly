package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Edge cases of the merge logic. Each test is one case and shows what the merge does with it.
 *
 * <ul>
 * <li>Equal severity written on two devices: both end with the same value, credited to the lower event id.</li>
 * <li>A resolution written by a device that saw only one of two conflicting values: the other value stays in conflict with the resolution.</li>
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

}
