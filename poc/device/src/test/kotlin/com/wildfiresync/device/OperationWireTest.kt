package com.wildfiresync.device

import com.wildfiresync.core.EventMessage
import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import java.time.Instant
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationWireTest {

    private val event = severity("4")

    @Test
    fun roundTripsAnEvent() {
        assertEquals(event, OperationWire.decode(OperationWire.encode(event)))
    }

    @Test
    fun roundTripsACreateWithItsIncidentId() {
        val create = create("""{"incidentId":"00000000-0000-0000-0000-000000000009"}""")

        assertEquals(create, OperationWire.decode(OperationWire.encode(create)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsACreateWithoutItsIncidentId() {
        OperationWire.decode(OperationWire.encode(create("{}")))
    }

    @Test
    fun usesTheSameJsonFieldsAsTheServerEvent() {
        val json = JSONObject(String(OperationWire.encode(event), Charsets.UTF_8))

        assertEquals(EventMessage::class.java.recordComponents.map { it.name }.toSet(), json.keys().asSequence().toSet())
        assertEquals("tablet-a", json.getString("nodeId"))
        assertEquals(7L, json.getLong("seq"))
        assertEquals("severity", json.getString("field"))
        assertEquals("4", json.getString("value"))
        assertEquals("2026-09-27T10:15:00Z", json.getString("createdAt"))
        assertEquals(6L, json.getJSONObject("context").getLong("tablet-a"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSeverityOutsideTheAllowedRange() {
        OperationWire.decode(OperationWire.encode(severity("99")))
    }

    private fun create(value: String) = Event(
        UUID.fromString("00000000-0000-0000-0000-0000000000ab"),
        event.recordId(),
        Event.RecordType.REPORT,
        Event.Operation.CREATE,
        null,
        value,
        "crew-tablet-a",
        Instant.parse("2026-09-27T10:14:00Z"),
        OpId(NodeId("tablet-a"), 6),
        CausalContext.EMPTY,
        emptySet(),
    )

    private fun severity(value: String) = Event(
        UUID.fromString("00000000-0000-0000-0000-0000000000aa"),
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        Event.RecordType.REPORT,
        Event.Operation.SET_FIELD,
        Field.REPORT_SEVERITY.key(),
        value,
        "crew-tablet-a",
        Instant.parse("2026-09-27T10:15:00Z"),
        OpId(NodeId("tablet-a"), 7),
        CausalContext(mapOf(NodeId("tablet-a") to 6L)),
        emptySet(),
    )

}
