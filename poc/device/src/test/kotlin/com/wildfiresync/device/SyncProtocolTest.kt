package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.device.experiment.RunProtocol
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncProtocolTest {

    private fun description(seq: Long, text: String) = Event(
        UUID.nameUUIDFromBytes("tablet-a:$seq".toByteArray()),
        UUID.fromString("00000000-0000-4000-8000-0000000000e1"),
        Event.RecordType.REPORT,
        Event.Operation.SET_FIELD,
        Field.REPORT_DESCRIPTION.key(),
        text,
        "tablet-a",
        Instant.parse("2026-10-18T08:00:00Z"),
        OpId(NodeId("tablet-a"), seq),
        CausalContext(mapOf(NodeId("tablet-a") to seq - 1)),
        emptySet(),
    )

    @Test
    fun `a hello carries the causal context`() {
        val context = CausalContext(mapOf(NodeId("tablet-a") to 4L, NodeId("tablet-b") to 9L))

        assertEquals(SyncProtocol.Message.Hello(context), SyncProtocol.decode(SyncProtocol.encodeHello(context)))
    }

    @Test
    fun `many events are split into payloads that fit and arrive in order`() {
        val events = (1L..400L).map { description(it, "ünïcode wörds ".repeat(10 + (it % 50).toInt())) }

        val payloads = SyncProtocol.encodeEvents(events)

        assertTrue(payloads.size > 1)
        payloads.forEach { assertTrue(it.size <= RunProtocol.MAX_PAYLOAD_BYTES, "${it.size} bytes") }
        val decoded = payloads.flatMap { assertIs<SyncProtocol.Message.Events>(SyncProtocol.decode(it)).events }
        assertEquals(events, decoded)
    }

    @Test
    fun `the longest text a tablet accepts fits a payload`() {
        val payloads = SyncProtocol.encodeEvents(listOf(description(1, "\"\\\u0001".repeat(MAX_TEXT_LENGTH / 3))))

        assertEquals(1, payloads.size)
        assertTrue(payloads.single().size <= RunProtocol.MAX_PAYLOAD_BYTES)
    }

    @Test
    fun `an entry that is not an event is counted, the rest still arrive`() {
        val good = OperationWire.toJson(description(1, "Smoke seen")).toString()
        val bytes = """{"type":"sync-events","ops":[$good,{"eventId":"nope"}]}""".toByteArray()

        val message = assertIs<SyncProtocol.Message.Events>(SyncProtocol.decode(bytes))

        assertEquals(listOf(description(1, "Smoke seen")), message.events)
        assertEquals(1, message.undecodable)
    }

    @Test
    fun `payloads of other features are left to their own handler`() {
        assertNull(SyncProtocol.decode(RunProtocol.encodeStart("run", 10, 5)))
        assertNull(SyncProtocol.decode(OperationWire.encode(description(1, "Smoke seen"))))
        assertNull(SyncProtocol.decode("not json".toByteArray()))
        assertNull(RunProtocol.decode(SyncProtocol.encodeHello(CausalContext.EMPTY)))
    }

}
