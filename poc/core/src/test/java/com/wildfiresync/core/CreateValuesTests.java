package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wildfiresync.core.schema.CausalContext;
import com.wildfiresync.core.schema.CreateValues;
import com.wildfiresync.core.schema.Event;
import com.wildfiresync.core.schema.Event.RecordType;
import com.wildfiresync.core.schema.Field;
import com.wildfiresync.core.schema.FieldValue;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateValuesTests {

	private static final UUID INCIDENT = UUID.fromString("00000000-0000-0000-0000-000000000009");

	private static final FieldValue.Reference INCIDENT_REF = new FieldValue.Reference(INCIDENT);

	private static void rejected(RecordType type, String json) {
		assertThrows(IllegalArgumentException.class, () -> CreateValues.parse(type, json), json);
	}

	@Test
	void writesTheKeysSortedSoEveryDeviceWritesTheSameString() {
		String json = CreateValues.format(RecordType.REPORT,
				Map.of(Field.REPORT_SEVERITY, new FieldValue.Num(7), Field.REPORT_INCIDENT_ID, INCIDENT_REF));

		assertEquals("{\"incidentId\":\"" + INCIDENT + "\",\"severity\":\"7\"}", json);
	}

	@Test
	void initialValuesRoundTrip() {
		Map<Field, FieldValue> values = Map.of(Field.REPORT_INCIDENT_ID, INCIDENT_REF,
				Field.REPORT_DESCRIPTION, new FieldValue.Text("smoke \"north\",\nnear the road"));

		assertEquals(values, CreateValues.parse(RecordType.REPORT, CreateValues.format(RecordType.REPORT, values)));
	}

	@Test
	void mutableFieldsAreOptional() {
		assertEquals(Map.of(Field.REPORT_INCIDENT_ID, INCIDENT_REF),
				CreateValues.parse(RecordType.REPORT, "{\"incidentId\":\"" + INCIDENT + "\"}"));
		assertTrue(CreateValues.parse(RecordType.INCIDENT, "{}").isEmpty(), "an incident has no immutable fields");
	}

	@Test
	void everyImmutableFieldIsRequired() {
		rejected(RecordType.REPORT, "{}");
		rejected(RecordType.REPORT, "{\"severity\":\"7\"}");
		assertThrows(IllegalArgumentException.class,
				() -> CreateValues.format(RecordType.REPORT, Map.of(Field.REPORT_SEVERITY, new FieldValue.Num(7))));
	}

	@Test
	void rejectsValuesTheSchemaCannotHold() {
		String incident = "\"incidentId\":\"" + INCIDENT + "\"";
		rejected(RecordType.REPORT, "{" + incident + ",\"severity\":\"11\"}");
		rejected(RecordType.REPORT, "{" + incident + ",\"severity\":7}");
		rejected(RecordType.REPORT, "{" + incident + ",\"windDirection\":\"N\"}");
		rejected(RecordType.REPORT, "{" + incident + ",\"name\":\"belongs to an incident\"}");
		rejected(RecordType.REPORT, "{" + incident + "," + incident + "}");
		rejected(RecordType.REPORT, "{\"incidentId\":\"not a uuid\"}");
	}

	@Test
	void rejectsAnythingButOneJsonObject() {
		rejected(RecordType.INCIDENT, "");
		rejected(RecordType.INCIDENT, "[]");
		rejected(RecordType.INCIDENT, "{} {}");
		rejected(RecordType.INCIDENT, "{\"name\":\"Ridge fire\"");
		rejected(RecordType.INCIDENT, "{'name':'Ridge fire'}");
	}

	@Test
	void theMergeRejectsACreateWithoutItsImmutableFields() {
		Event create = new Event(new UUID(8, 1), new UUID(8, 2), RecordType.REPORT, Event.Operation.CREATE, null, "{}",
				"crew-1", Instant.EPOCH, new OpId(new NodeId("tablet-a"), 1), CausalContext.EMPTY, Set.of());

		assertThrows(IllegalArgumentException.class, () -> MergeCore.validate(create));
		assertThrows(IllegalArgumentException.class, () -> MergeCore.lift(create));
	}

}
