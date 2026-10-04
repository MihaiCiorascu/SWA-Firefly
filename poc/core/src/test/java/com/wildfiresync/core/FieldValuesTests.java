package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import com.wildfiresync.core.schema.FieldValues;
import com.wildfiresync.core.schema.FieldValues.IncidentStatus;
import com.wildfiresync.core.schema.FieldValues.Location;
import com.wildfiresync.core.schema.FieldValues.ReportType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The stored string and the typed value must mean the same thing on every node. */
class FieldValuesTests {

	private static void roundTrips(Field field, FieldValue value) {
		assertEquals(value, FieldValues.parse(field, FieldValues.format(value)));
	}

	@Test
	void everyKindSurvivesARoundTrip() {
		roundTrips(Field.REPORT_SEVERITY, new FieldValue.Num(7));
		roundTrips(Field.INCIDENT_NAME, new FieldValue.Text("Ridge fire"));
		roundTrips(Field.INCIDENT_STATUS, new FieldValue.Status(IncidentStatus.CONTAINED));
		roundTrips(Field.REPORT_TYPE, FieldValue.Types.of(ReportType.FIRE, ReportType.SMOKE));
		roundTrips(Field.REPORT_LOCATION, new FieldValue.Place(new Location(51.9244, 4.4777)));
		roundTrips(Field.REPORT_INCIDENT_ID, new FieldValue.Reference(UUID.fromString("00000000-0000-0000-0000-000000000001")));
	}

	@Test
	void reportTypesAreWrittenSortedSoBothDevicesProduceTheSameString() {
		assertEquals("SMOKE,FIRE", FieldValues.format(FieldValue.Types.of(ReportType.FIRE, ReportType.SMOKE)));
	}

	@Test
	void severityOutsideItsRangeIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> FieldValues.parse(Field.REPORT_SEVERITY, "0"));
		assertThrows(IllegalArgumentException.class, () -> FieldValues.parse(Field.REPORT_SEVERITY, "11"));
		assertThrows(IllegalArgumentException.class, () -> FieldValues.parse(Field.REPORT_SEVERITY, "high"));
	}

	@Test
	void unknownEnumValuesAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> FieldValues.parse(Field.INCIDENT_STATUS, "SMOULDERING"));
		assertThrows(IllegalArgumentException.class, () -> FieldValues.parse(Field.REPORT_TYPE, "FLOOD"));
		assertThrows(IllegalArgumentException.class, () -> FieldValues.parse(Field.REPORT_TYPE, ""));
	}

}
