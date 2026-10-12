package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationWireTest {

    private val operation = SetSeverity(
        OpId(NodeId("tablet-a"), 7),
        ReportId("report-1"),
        4,
        Instant.parse("2026-09-27T10:15:00Z"),
    )

    @Test
    fun roundTripsAnOperation() {
        assertEquals(operation, OperationWire.decode(OperationWire.encode(operation)))
    }

    @Test
    fun usesTheSameJsonFieldsAsTheServerEvent() {
        val json = JSONObject(String(OperationWire.encode(operation), Charsets.UTF_8))

        assertEquals(5, json.length())
        assertEquals("tablet-a", json.getString("nodeId"))
        assertEquals(7L, json.getLong("seq"))
        assertEquals("report-1", json.getString("reportId"))
        assertEquals(4, json.getInt("severity"))
        assertEquals("2026-09-27T10:15:00Z", json.getString("recordedAt"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSeverityOutsideTheAllowedRange() {
        OperationWire.decode(
            """{"nodeId":"tablet-a","seq":1,"reportId":"report-1","severity":99,"recordedAt":"2026-09-27T10:15:00Z"}"""
                .toByteArray(Charsets.UTF_8),
        )
    }
}
