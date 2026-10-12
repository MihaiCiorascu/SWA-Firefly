package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.OperationEvent
import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import io.nats.client.support.NatsJetStreamConstants.MSG_ID_HDR
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OperationSenderTest {

    private val gson = operationGson

    private val operation = SetSeverity(
        OpId(NodeId("tablet-a"), 7),
        ReportId("report-1"),
        4,
        Instant.parse("2026-09-27T10:15:00Z"),
    )

    @Test
    fun `publishes to the subject of the report`() {
        assertEquals("events.operation.report-1", toMessage(operation, gson).subject)
    }

    @Test
    fun `carries the operation id as the deduplication header`() {
        assertEquals("tablet-a:7", toMessage(operation, gson).headers.getFirst(MSG_ID_HDR))
    }

    @Test
    fun `payload is the json the server reads`() {
        assertEquals(
            """{"nodeId":"tablet-a","seq":7,"reportId":"report-1","severity":4,"recordedAt":"2026-09-27T10:15:00Z"}""",
            String(toMessage(operation, gson).data, Charsets.UTF_8),
        )
    }

    @Test
    fun `payload round trips back to the same operation`() {
        val payload = toMessage(operation, gson).data

        val event = gson.fromJson(String(payload, Charsets.UTF_8), OperationEvent::class.java)

        assertEquals(operation, event.toOperation())
    }

    @Test
    fun `rejects a report id that is not a subject token`() {
        val bad = SetSeverity(operation.id(), ReportId("report.1"), 4, operation.recordedAt())

        assertFailsWith<IllegalArgumentException> { toMessage(bad, gson) }
    }

}
