package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.OperationEvent
import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import io.nats.client.support.NatsJetStreamConstants.MSG_ID_HDR
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OperationSenderTest {

    private val objectMapper = ObjectMapper()

    private val operation = SetSeverity(
        OpId(NodeId("tablet-a"), 7),
        ReportId("report-1"),
        4,
        Instant.parse("2026-09-27T10:15:00Z"),
    )

    @Test
    fun `publishes to the subject of the report`() {
        assertEquals("events.operation.report-1", toMessage(operation, objectMapper).subject)
    }

    @Test
    fun `carries the operation id as the deduplication header`() {
        assertEquals("tablet-a:7", toMessage(operation, objectMapper).headers.getFirst(MSG_ID_HDR))
    }

    @Test
    fun `payload round trips back to the same operation`() {
        val payload = toMessage(operation, objectMapper).data

        val event = objectMapper.readValue(payload, OperationEvent::class.java)

        assertEquals(operation, event.toOperation())
    }

    @Test
    fun `rejects a report id that is not a subject token`() {
        val bad = SetSeverity(operation.id(), ReportId("report.1"), 4, operation.recordedAt())

        assertFailsWith<IllegalArgumentException> { toMessage(bad, objectMapper) }
    }

}
