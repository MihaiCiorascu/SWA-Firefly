package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues.ReportType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TierATests {

	private static final RecordRef REPORT = RecordRef.report(new UUID(9, 1));

	private static final RecordRef INCIDENT = RecordRef.incident(new UUID(9, 2));

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
		inFlight.addAll(new ArrayList<>(inFlight));
		alpha.receiveAll(inFlight);
		bravo.receiveAll(inFlight);
	}

	private FieldValue valueOn(SimulatedDevice device, RecordRef record, Field field) {
		return assertInstanceOf(Resolution.Settled.class, device.resolution(record, field).orElseThrow()).value();
	}

	@Test
	void severityKeepsTheHighestConcurrentValue() {
		alpha.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(4));
		bravo.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(7));

		reconnect();

		assertEquals(new FieldValue.Num(7), valueOn(alpha, REPORT, Field.REPORT_SEVERITY));
		assertEquals(new FieldValue.Num(7), valueOn(bravo, REPORT, Field.REPORT_SEVERITY));
	}

	@Test
	void severityCannotBeLoweredOnceRaised() {
		alpha.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(7));
		alpha.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(2));

		assertEquals(new FieldValue.Num(7), valueOn(alpha, REPORT, Field.REPORT_SEVERITY));
	}

	@Test
	void reportTypeKeepsLabelsAddedOnBothDevices() {
		alpha.set(REPORT, Field.REPORT_TYPE, FieldValue.Types.of(ReportType.SMOKE));
		bravo.set(REPORT, Field.REPORT_TYPE, FieldValue.Types.of(ReportType.ROAD_BLOCKED));

		reconnect();

		assertEquals(FieldValue.Types.of(ReportType.SMOKE, ReportType.ROAD_BLOCKED),
				valueOn(alpha, REPORT, Field.REPORT_TYPE));
		assertEquals(valueOn(alpha, REPORT, Field.REPORT_TYPE), valueOn(bravo, REPORT, Field.REPORT_TYPE));
	}

	@Test
	void incidentNameSettlesOnTheLargestEventIdAndBothDevicesAgree() {
		alpha.set(INCIDENT, Field.INCIDENT_NAME, new FieldValue.Text("Ridge fire"));
		bravo.set(INCIDENT, Field.INCIDENT_NAME, new FieldValue.Text("Ridge complex"));

		reconnect();

		// bravo event ids sort above alpha ones, so its wording wins. Deterministic, but arbitrary:
		// it has nothing to do with who wrote later or who knew more.
		assertEquals(new FieldValue.Text("Ridge complex"), valueOn(alpha, INCIDENT, Field.INCIDENT_NAME));
		assertEquals(valueOn(alpha, INCIDENT, Field.INCIDENT_NAME), valueOn(bravo, INCIDENT, Field.INCIDENT_NAME));
	}

	@Test
	void aRenameByAWriterWhoSawTheOldNameWinsWhateverTheEventIds() {
		bravo.set(INCIDENT, Field.INCIDENT_NAME, new FieldValue.Text("Ridge complex"));
		alpha.receiveAll(bravo.log());
		// alpha event ids sort below bravo ones, it still wins because it saw bravo's name
		alpha.set(INCIDENT, Field.INCIDENT_NAME, new FieldValue.Text("Ridge fire"));

		reconnect();

		assertEquals(new FieldValue.Text("Ridge fire"), valueOn(bravo, INCIDENT, Field.INCIDENT_NAME));
		assertEquals(valueOn(alpha, INCIDENT, Field.INCIDENT_NAME), valueOn(bravo, INCIDENT, Field.INCIDENT_NAME));
	}

	@Test
	void theCreateSetsTheImmutableIncidentId() {
		alpha.create(REPORT, Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(INCIDENT.id())));

		reconnect();

		assertEquals(new FieldValue.Reference(INCIDENT.id()), valueOn(bravo, REPORT, Field.REPORT_INCIDENT_ID));
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void anImmutableFieldCannotBeChangedAfterTheCreate() {
		alpha.create(REPORT, Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(INCIDENT.id())));

		assertThrows(IllegalArgumentException.class,
				() -> alpha.set(REPORT, Field.REPORT_INCIDENT_ID, new FieldValue.Reference(new UUID(9, 99))));
	}

	@Test
	void aValueGivenInTheCreateMergesLikeAnyOtherWrite() {
		alpha.create(REPORT, Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(INCIDENT.id()),
				Field.REPORT_SEVERITY, new FieldValue.Num(5)));
		bravo.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(8));

		reconnect();

		assertEquals(new FieldValue.Num(8), valueOn(alpha, REPORT, Field.REPORT_SEVERITY));
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void creatingTheSameRecordOnTwoDevicesIsHarmless() {
		Map<Field, FieldValue> values = Map.of(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(INCIDENT.id()));
		alpha.create(REPORT, values);
		bravo.create(REPORT, values);

		reconnect();

		assertTrue(alpha.world().record(REPORT).orElseThrow().created());
		assertEquals(alpha.world(), bravo.world());
	}

	@Test
	void editingDifferentFieldsNeverConflicts() {
		alpha.set(REPORT, Field.REPORT_SEVERITY, new FieldValue.Num(6));
		bravo.set(REPORT, Field.REPORT_TYPE, FieldValue.Types.of(ReportType.FIRE));

		reconnect();

		assertEquals(new FieldValue.Num(6), valueOn(bravo, REPORT, Field.REPORT_SEVERITY));
		assertEquals(FieldValue.Types.of(ReportType.FIRE), valueOn(alpha, REPORT, Field.REPORT_TYPE));
	}

}
