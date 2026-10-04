package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wildfiresync.core.schema.Event.RecordType;
import com.wildfiresync.core.schema.Field;
import org.junit.jupiter.api.Test;

class FieldTests {

	@Test
	void looksUpAFieldByRecordTypeAndKey() {
		assertEquals(Field.REPORT_SEVERITY, Field.of(RecordType.REPORT, "severity"));
		assertEquals(Field.INCIDENT_NAME, Field.of(RecordType.INCIDENT, "name"));
	}

	@Test
	void rejectsAFieldThatBelongsToTheOtherRecordType() {
		assertThrows(IllegalArgumentException.class, () -> Field.of(RecordType.REPORT, "name"));
	}

	@Test
	void rejectsAnUnknownField() {
		assertThrows(IllegalArgumentException.class, () -> Field.of(RecordType.REPORT, "windDirection"));
	}

	@Test
	void onlyManualFieldsAreTierB() {
		for (Field field : Field.values()) {
			Field.Tier expected = field.policy() == Field.Policy.MANUAL ? Field.Tier.B : Field.Tier.A;
			assertEquals(expected, field.tier(), field.name());
		}
	}

	@Test
	void severityCarriesItsRange() {
		Field.Range range = Field.REPORT_SEVERITY.range().orElseThrow();
		assertEquals(1, range.min());
		assertEquals(10, range.max());
		assertTrue(Field.INCIDENT_NAME.range().isEmpty(), "only numeric fields have a range");
	}

}
