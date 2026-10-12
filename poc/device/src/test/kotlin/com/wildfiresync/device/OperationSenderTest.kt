package com.wildfiresync.device

import com.wildfiresync.core.EventMessage
import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import io.nats.client.support.NatsJetStreamConstants.MSG_ID_HDR
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationSenderTest {

    private val gson = operationGson

    private val report = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val event = event(Event.Operation.SET_FIELD, Field.REPORT_SEVERITY.key(), "4")

    @Test
    fun `publishes to the subject of the record`() {
        assertEquals("events.operation.report.$report", toMessage(event, gson).subject)
    }

    @Test
    fun `carries the dot as the deduplication header`() {
        assertEquals("tablet-a:7", toMessage(event, gson).headers.getFirst(MSG_ID_HDR))
    }

    @Test
    fun `payload is the json the server reads`() {
        assertEquals(
            """{"eventId":"00000000-0000-0000-0000-0000000000aa","recordId":"$report","recordType":"REPORT",""" +
                """"operation":"SET_FIELD","field":"severity","value":"4","actorId":"crew-tablet-a",""" +
                """"createdAt":"2026-09-27T10:15:00Z","nodeId":"tablet-a","seq":7,"context":{},"supersedes":[]}""",
            String(toMessage(event, gson).data, Charsets.UTF_8),
        )
    }

    @Test
    fun `payload round trips back to the same event`() {
        assertEquals(event, roundTrip(event))
    }

    @Test
    fun `a create with no field or value round trips`() {
        val create = event(Event.Operation.CREATE, null, null)

        assertEquals(create, roundTrip(create))
    }

    private fun roundTrip(event: Event): Event =
        gson.fromJson(String(toMessage(event, gson).data, Charsets.UTF_8), EventMessage::class.java).toEvent()

    private fun event(operation: Event.Operation, field: String?, value: String?) = Event(
        UUID.fromString("00000000-0000-0000-0000-0000000000aa"),
        report,
        Event.RecordType.REPORT,
        operation,
        field,
        value,
        "crew-tablet-a",
        Instant.parse("2026-09-27T10:15:00Z"),
        OpId(NodeId("tablet-a"), 7),
        CausalContext.EMPTY,
        emptySet(),
    )

}
