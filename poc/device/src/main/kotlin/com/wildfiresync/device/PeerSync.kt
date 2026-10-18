package com.wildfiresync.device

import android.util.Log
import com.wildfiresync.core.schema.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PeerSync"

/**
 * syncs this tablet with the tablets it is connected to. on connect each side sends its causal context and gets back
 * every event it does not cover, then both merge what came in. after that, an event written here or newly received
 * from another tablet is passed on right away, so a third tablet gets it too. the transport is only called from the
 * scope, which for the app is the main thread, where nearby's callbacks run
 */
class PeerSync(private val store: TabletStore, private val transport: Transport, private val scope: CoroutineScope) {

    interface Transport {
        fun peers(): List<String>

        /** false when the peer is not connected anymore */
        fun send(peer: String, bytes: ByteArray): Boolean
    }

    /** what went in and out with one peer since it connected */
    data class Summary(
        val peer: String,
        val sent: Int = 0,
        val received: Int = 0,
        val duplicates: Int = 0,
        val rejected: Int = 0,
        val error: String? = null,
    )

    private val _summaries = MutableStateFlow<Map<String, Summary>>(emptyMap())
    val summaries: StateFlow<Map<String, Summary>> = _summaries.asStateFlow()

    private val push: (Event) -> Unit = { event -> scope.launch { pass(listOf(event), from = null) } }

    init {
        store.onWritten = push
    }

    fun onConnected(peer: String) {
        _summaries.update { it + (peer to Summary(peer)) }
        scope.launch {
            transport.send(peer, SyncProtocol.encodeHello(store.read { context() }))
        }
    }

    /** false when the bytes are not a sync message */
    fun handle(peer: String, bytes: ByteArray): Boolean {
        val message = SyncProtocol.decode(bytes) ?: return false
        scope.launch {
            when (message) {
                is SyncProtocol.Message.Hello -> send(peer, store.read { missingFor(message.context) })
                is SyncProtocol.Message.Events -> {
                    val receipt = store.receive(message.events)
                    summary(peer) {
                        it.copy(
                            received = it.received + receipt.stored.size,
                            duplicates = it.duplicates + receipt.duplicates,
                            rejected = it.rejected + receipt.rejected + message.undecodable,
                        )
                    }
                    pass(receipt.stored, from = peer)
                }
            }
        }
        return true
    }

    fun close() {
        // a newer activity may already have hooked up its own
        if (store.onWritten === push) store.onWritten = null
    }

    private fun pass(events: List<Event>, from: String?) {
        if (events.isEmpty()) return
        transport.peers().filter { it != from }.forEach { send(it, events) }
    }

    private fun send(peer: String, events: List<Event>) {
        if (events.isEmpty()) return
        try {
            val sent = SyncProtocol.encodeEvents(events).all { transport.send(peer, it) }
            if (sent) summary(peer) { it.copy(sent = it.sent + events.size) }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "could not send to $peer", e)
            summary(peer) { it.copy(error = e.message) }
        }
    }

    private fun summary(peer: String, change: (Summary) -> Summary) {
        _summaries.update { it + (peer to change(it[peer] ?: Summary(peer))) }
    }

}
