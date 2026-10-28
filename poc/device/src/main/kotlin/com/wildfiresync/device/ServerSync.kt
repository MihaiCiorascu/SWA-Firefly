package com.wildfiresync.device

import android.util.Log
import com.wildfiresync.core.Zone
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.FieldValues.Location
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.Nats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ServerSync"

/**
 * keeps this tablet and the server in step through the broker.
 *
 * out: every event the tablet holds goes to the operation stream, the ones written while it had no
 * signal included. the broker drops a dot it has already stored, so connecting again costs nothing.
 *
 * in: the server publishes each report it has merged to the subject of its zone, as the whole event
 * list of that record rather than a merged row, so this tablet merges it with its own events and the
 * two end up agreeing. the tablet subscribes to the zone it stands in and the zones of the reports it
 * holds, each with the ring around it, so a report just over a boundary still arrives and one it holds
 * keeps arriving after the tablet has moved away.
 *
 * everything but the broker calls runs in the scope, which for the app is the main thread, so the
 * fields below are only ever touched from there.
 */
class ServerSync(
    private val store: TabletStore,
    private val scope: CoroutineScope,
    private val open: (String) -> Connection = { url -> Nats.connect(url) },
    private val position: () -> Location = DevicePosition::current,
) {

    /** what the Server tab shows */
    data class Status(
        val url: String? = null,
        val connected: Boolean = false,
        val sent: Int = 0,
        val received: Int = 0,
        val position: Location? = null,
        val zones: Set<String> = emptySet(),
        val error: String? = null,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private var connection: Connection? = null

    private var sender: OperationSender? = null

    private var consumer: ReportConsumer? = null

    private var hooks: List<AutoCloseable> = emptyList()

    suspend fun connect(url: String) {
        disconnect()
        _status.value = Status(url = url)
        val opened = try {
            withContext(Dispatchers.IO) { open(url) }
        }
        catch (e: Exception) {
            _status.update { it.copy(error = "could not reach $url: ${e.message ?: e}") }
            return
        }
        connection = opened
        sender = OperationSender(opened)
        _status.update { it.copy(connected = true) }

        publish(store.read { allEvents() })
        resubscribe()
        hooks = listOf(
            store.onWritten { event -> scope.launch { publish(listOf(event)) } },
            // events from a peer go on to the server too, so a tablet with signal relays for one without.
            // some of these came from the server itself; the broker drops those by their dot.
            store.onReceived { events -> scope.launch { publish(events); resubscribe() } },
        )
    }

    suspend fun disconnect() {
        hooks.forEach(AutoCloseable::close)
        hooks = emptyList()
        val stopping = consumer
        val closing = connection
        consumer = null
        connection = null
        sender = null
        withContext(Dispatchers.IO) {
            try {
                stopping?.stop()
                closing?.close()
            }
            catch (e: Exception) {
                Log.w(TAG, "while closing the connection", e)
            }
        }
        _status.update { it.copy(connected = false, zones = emptySet()) }
    }

    /**
     * sends everything the tablet holds again, which is the Server tab's catch-up button. the broker
     * drops the dots it already has, so this only costs the ones it is missing
     */
    suspend fun resend() {
        publish(store.read { allEvents() })
    }

    private suspend fun publish(events: List<Event>) {
        val sending = sender ?: return
        if (events.isEmpty()) return
        try {
            val stored = withContext(Dispatchers.IO) { events.count { !sending.send(it).isDuplicate } }
            _status.update { it.copy(sent = it.sent + stored, error = null) }
        }
        catch (e: Exception) {
            Log.w(TAG, "could not send ${events.size} event(s)", e)
            _status.update { it.copy(error = "could not send: ${e.message ?: e}") }
        }
    }

    /** points the subscription at the zones around this tablet and the reports it holds, when they have changed */
    private suspend fun resubscribe() {
        val subscribing = connection ?: return
        val here = position()
        val zones = zonesFor(here, store.read { reports() }.mapNotNull { it.location })
        if (zones == _status.value.zones && consumer != null) return

        val stopping = consumer
        consumer = null
        try {
            consumer = withContext(Dispatchers.IO) {
                stopping?.stop()
                ReportConsumer(subscribing, zones) { message -> onReport(message) }.also { it.start() }
            }
            _status.update { it.copy(position = here, zones = zones, error = null) }
        }
        catch (e: Exception) {
            // the stream is made by the server, so this is what a tablet sees when the server is not up yet
            Log.w(TAG, "could not subscribe to ${zones.size} zone(s)", e)
            _status.update { it.copy(zones = emptySet(), error = "no reports stream yet: ${e.message ?: e}") }
        }
    }

    /** called on a broker thread, so the merge is handed to the scope */
    private fun onReport(message: Message) {
        val events = try {
            ReportWire.decode(message.data)
        }
        catch (e: Exception) {
            Log.w(TAG, "ignoring a payload on ${message.subject} that is not a report bundle", e)
            return
        }
        if (events.isEmpty()) {
            // the report moved out of this zone, and this tablet cannot un-see what it already holds
            return
        }
        scope.launch {
            val receipt = store.receive(events)
            _status.update { it.copy(received = it.received + receipt.stored.size) }
        }
    }

}

internal fun zonesFor(position: Location, reports: List<Location>): Set<String> =
    (listOf(position) + reports).flatMapTo(mutableSetOf()) { Zone.around(it) }
