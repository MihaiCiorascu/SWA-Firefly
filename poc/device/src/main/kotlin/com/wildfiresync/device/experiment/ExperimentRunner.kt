package com.wildfiresync.device.experiment

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import java.time.Instant

// The schema requires report ids to be lowercase UUIDs.
private const val REPORT = "00000000-0000-4000-8000-0000000000e1"

private val BASE = Instant.parse("2026-10-01T06:00:00Z")

/**
 * Experiment 1 of the report: how well events travel over the P2P link. One device sends a run of generated
 * operations in payloads of a chosen size, the other stores them and logs how long that took. Sending the run again
 * after an interruption shows whether anything was lost or delivered twice.
 *
 * Times: the sender measures from its first payload until every payload reported success. The receiver measures from
 * the arrival of the run's start message until the last event is stored.
 */
class ExperimentRunner(
    private val store: ExperimentStore,
    private val transport: Transport,
    private val connectMillis: () -> Long? = { null },
    private val note: () -> String = { "" },
    private val clock: () -> Long = System::nanoTime,
    private val log: (Row) -> Unit = {},
) {

    fun interface Transport {
        /** Queues the bytes for every connected peer. Returns the payload id, or null when nobody is connected. */
        fun send(bytes: ByteArray): Long?
    }

    data class Row(
        val role: String,
        val run: String,
        val attempt: Int,
        val total: Int,
        val batch: Int,
        val note: String,
        val connectMillis: Long?,
        val transferMillis: Long,
        val payloads: Int,
        val eventsReceived: Int,
        val duplicates: Int,
        val stored: Int,
        val lost: Int,
        val complete: Boolean,
    ) {

        fun csv(device: String) = listOf(
            Instant.now(), device, role, run, attempt, total, batch, note.replace(',', ';'), connectMillis ?: "",
            transferMillis, payloads, eventsReceived, duplicates, stored, lost, complete,
        ).joinToString(",")

        companion object {

            const val CSV_HEADER = "time,device,role,run,attempt,events,batch,note,connect_ms,transfer_ms,payloads," +
                "events_received,duplicates,stored,lost,complete"

        }

    }

    private class Outgoing(val run: String, val total: Int, val batch: Int) {
        var attempt = 0
        var startNanos = 0L
        val pending = HashSet<Long>()
        var payloads = 0
        var sealed = false
        var failed = false
        var logged = false
    }

    private class Incoming(val run: String, val total: Int, val batch: Int, val attempt: Int, val startNanos: Long) {
        var payloads = 0
        var received = 0
        var duplicates = 0
        var logged = false
    }

    private var outgoing: Outgoing? = null

    private var incoming: Incoming? = null

    private val attempts = HashMap<String, Int>()

    /** Sender: starts a new run. False if nobody was connected when a payload was queued. */
    fun startRun(total: Int, batch: Int): Boolean {
        val run = System.currentTimeMillis().toString(36)
        return send(Outgoing(run, total, batch))
    }

    /** Sender: sends the whole last run again, as after a dropped connection. */
    fun resend(): Boolean {
        val last = outgoing ?: return false
        last.attempt++
        return send(last)
    }

    private fun send(run: Outgoing): Boolean {
        outgoing = run
        run.pending.clear()
        run.payloads = 0
        run.sealed = false
        run.failed = false
        run.logged = false
        run.startNanos = clock()

        if (!queue(run, RunProtocol.encodeStart(run.run, run.total, run.batch))) return false
        var seq = 1L
        while (seq <= run.total) {
            val last = minOf(run.total.toLong(), seq + run.batch - 1)
            val operations = (seq..last).map { operation(run.run, it) }
            if (!queue(run, RunProtocol.encodeEvents(run.run, operations))) return false
            seq = last + 1
        }
        run.sealed = true
        return true
    }

    private fun queue(run: Outgoing, bytes: ByteArray): Boolean {
        val id = transport.send(bytes) ?: return false
        run.pending += id
        run.payloads++
        return true
    }

    /** Sender: the link reported how one of our payloads ended. */
    fun onTransferUpdate(payloadId: Long, success: Boolean) {
        val run = outgoing ?: return
        if (!run.pending.remove(payloadId) || run.logged) return
        if (!success) run.failed = true
        if (run.failed || (run.sealed && run.pending.isEmpty())) {
            run.logged = true
            log(
                Row(
                    "sender", run.run, run.attempt, run.total, run.batch, note(), connectMillis(),
                    millisSince(run.startNanos), run.payloads, 0, 0, 0, 0, !run.failed,
                ),
            )
        }
    }

    /** Receiver: handles a payload. False if it is not part of the experiment. */
    fun handle(bytes: ByteArray): Boolean {
        when (val message = RunProtocol.decode(bytes) ?: return false) {
            is RunProtocol.Message.Start -> {
                val attempt = (attempts[message.run] ?: -1) + 1
                attempts[message.run] = attempt
                incoming = Incoming(message.run, message.total, message.batch, attempt, clock())
            }
            is RunProtocol.Message.Events -> {
                val stored = store.insertIfAbsent(message.operations)
                val run = incoming?.takeIf { it.run == message.run } ?: return true
                run.payloads++
                run.received += message.operations.size
                run.duplicates += message.operations.size - stored
                if (!run.logged && store.count(run.run) >= run.total) {
                    run.logged = true
                    log(row(run, complete = true))
                }
            }
        }
        return true
    }

    /** Receiver: logs where the last run stands, for a run that was cut off. */
    fun logPartial() {
        val run = incoming?.takeIf { !it.logged } ?: return
        run.logged = true
        log(row(run, complete = false))
    }

    private fun row(run: Incoming, complete: Boolean): Row {
        val stored = store.count(run.run)
        return Row(
            "receiver", run.run, run.attempt, run.total, run.batch, note(), connectMillis(),
            millisSince(run.startNanos), run.payloads, run.received, run.duplicates, stored, run.total - stored, complete,
        )
    }

    private fun millisSince(startNanos: Long) = (clock() - startNanos) / 1_000_000

    private fun operation(run: String, seq: Long) = SetSeverity(
        OpId(NodeId(ExperimentStore.author(run)), seq),
        ReportId(REPORT),
        SetSeverity.MIN + (seq % 10).toInt(),
        BASE.plusSeconds(seq),
    )

}
