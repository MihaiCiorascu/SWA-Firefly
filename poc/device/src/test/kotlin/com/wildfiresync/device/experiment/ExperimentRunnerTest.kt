package com.wildfiresync.device.experiment

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.device.OperationWire
import com.wildfiresync.device.storage.LocalDatabase
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ExperimentRunnerTest {

    private val context = RuntimeEnvironment.getApplication()

    private val senderRows = mutableListOf<ExperimentRunner.Row>()
    private val receiverRows = mutableListOf<ExperimentRunner.Row>()

    private val receiverStore = ExperimentStore(LocalDatabase.inMemory(context))

    private val receiver = ExperimentRunner(receiverStore, { null }, log = { receiverRows += it })

    /** Delivers payloads straight to the receiver, until `deliverLimit` of them have gone through. */
    private inner class Link : ExperimentRunner.Transport {
        var deliverLimit = Int.MAX_VALUE
        var delivered = 0
        val ids = mutableListOf<Long>()
        val reached = mutableListOf<Boolean>()

        override fun send(bytes: ByteArray): Long {
            val id = (ids.size + 1).toLong()
            ids += id
            val arrives = delivered < deliverLimit
            reached += arrives
            if (arrives) {
                delivered++
                receiver.handle(bytes)
            }
            return id
        }

        /** What the link would report back to the sender for the payloads queued since `from`. */
        fun report(sender: ExperimentRunner, from: Int = 0) {
            for (i in from until ids.size) {
                sender.onTransferUpdate(ids[i], reached[i])
            }
        }
    }

    private val link = Link()

    private val sender = ExperimentRunner(
        ExperimentStore(LocalDatabase.inMemory(context)), link, log = { senderRows += it },
    )

    @Test
    fun `a run is stored completely and both sides log a complete row`() {
        assertTrue(sender.startRun(total = 250, batch = 10))
        link.report(sender)

        val run = receiverRows.single()
        assertTrue(run.complete)
        assertEquals(250, run.stored)
        assertEquals(0, run.lost)
        assertEquals(0, run.duplicates)
        assertEquals(25, run.payloads)
        assertEquals(250, receiverStore.count(run.run))

        val sent = senderRows.single()
        assertTrue(sent.complete)
        assertEquals(26, sent.payloads)
    }

    @Test
    fun `a run that is cut off is logged as incomplete and sending it again fills the gap`() {
        link.deliverLimit = 11
        sender.startRun(total = 250, batch = 10)
        link.report(sender)

        assertFalse(senderRows.single().complete)
        receiver.logPartial()
        val cutOff = receiverRows.single()
        assertFalse(cutOff.complete)
        assertEquals(100, cutOff.stored)
        assertEquals(150, cutOff.lost)

        link.deliverLimit = Int.MAX_VALUE
        val before = link.ids.size
        sender.resend()
        link.report(sender, from = before)

        val retry = receiverRows.last()
        assertTrue(retry.complete)
        assertEquals(1, retry.attempt)
        assertEquals(250, retry.stored)
        assertEquals(0, retry.lost)
        assertEquals(100, retry.duplicates)
        assertEquals(250, receiverStore.count(retry.run))
    }

    @Test
    fun `an event that arrives twice is counted but stored once`() {
        val events = (1L..5L).map { severity("exp-x", it, 3) }

        assertEquals(5, receiverStore.insertIfAbsent(events))
        assertEquals(0, receiverStore.insertIfAbsent(events))
        assertEquals(5, receiverStore.count("x"))
    }

    @Test
    fun `payloads for the largest batch stay under the size limit`() {
        val events = (1L..50L).map { severity("exp-0123456789ab", it, 10) }

        val bytes = RunProtocol.encodeEvents("0123456789ab", events)

        assertTrue(bytes.size < RunProtocol.MAX_PAYLOAD_BYTES, "${bytes.size} bytes")
        assertFailsWith<IllegalArgumentException> {
            RunProtocol.encodeEvents("0123456789ab", events + events + events + events + events + events + events + events + events + events)
        }
    }

    @Test
    fun `payloads of other features are left to their own handler`() {
        val single = OperationWire.encode(severity("crew-1", 1, 4))

        assertFalse(receiver.handle(single))
        assertFalse(receiver.handle("not json".toByteArray()))
    }

    private fun severity(node: String, seq: Long, severity: Int) = Event(
        UUID.nameUUIDFromBytes("$node:$seq".toByteArray()),
        UUID.fromString("00000000-0000-4000-8000-0000000000e1"),
        Event.RecordType.REPORT,
        Event.Operation.SET_FIELD,
        Field.REPORT_SEVERITY.key(),
        severity.toString(),
        node,
        Instant.parse("2026-10-01T06:00:00Z"),
        OpId(NodeId(node), seq),
        CausalContext.EMPTY,
        emptySet(),
    )

}
