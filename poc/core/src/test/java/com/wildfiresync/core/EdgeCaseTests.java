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
 * </ul>
 */
class EdgeCaseTests {

	private static final RecordRef REPORT = RecordRef.report(new UUID(8, 1));

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

	private Resolution.Settled settledOn(SimulatedDevice device, Field field) {
		return assertInstanceOf(Resolution.Settled.class, device.resolution(REPORT, field).orElseThrow());
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

}
