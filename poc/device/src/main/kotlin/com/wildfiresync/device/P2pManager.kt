package com.wildfiresync.device

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.nio.charset.StandardCharsets

private const val TAG = "P2pManager"

// Any fixed string works, it just has to match on both devices so they recognize
// each other as speaking the same "service". Not related to the report's Device
// Identity, this identifies the app/protocol, not the tablet.
private const val SERVICE_ID = "com.wildfiresync.android.SERVICE_ID"

// Symmetric: every device both advertises and discovers, matching "both crew
// leaders switch on P2P mode" rather than a fixed host/client split.
private val STRATEGY = Strategy.P2P_CLUSTER

/**
 * Wraps the Nearby Connections API for this proof of concept: discover, connect,
 * and exchange one test payload. No persistence and no merge logic yet, both are
 * a separate follow-up task once this is proven on real hardware.
 */
class P2pManager(private val context: Context, private val localName: String) {

    private val connectionsClient = Nearby.getConnectionsClient(context)

    // Plain Compose state, observed directly by the UI. No ViewModel needed for
    // a single-screen PoC.
    val status = mutableStateOf("Idle")
    val connectedPeers = mutableStateListOf<String>()

    // Called with the sender's advertised name and the raw bytes of every received payload.
    // The transport does not interpret them, whoever plugs in (merge, storage) decodes.
    var onPayload: ((peerName: String, bytes: ByteArray) -> Unit)? = null

    // Lets the P2P experiment take its own payloads before they reach onPayload.
    // Returns true when it consumed the payload.
    var experimentHandler: ((peerName: String, bytes: ByteArray) -> Boolean)? = null

    // Called when a payload we sent has ended: true for success, false for failure or cancellation.
    var onTransferEnded: ((payloadId: Long, success: Boolean) -> Unit)? = null

    // Milliseconds from start() until the first connection came up, null until then.
    private val connectMillisState = mutableStateOf<Long?>(null)
    val connectMillis: Long? get() = connectMillisState.value
    private var startedAtNanos = 0L

    // True between start() and stop(), so a lost connection can restart the search.
    private var running = false

    // endpointId -> advertised name, so the UI can show a name instead of a raw id.
    private val endpointNames = mutableMapOf<String, String>()

    // endpointIds we've actually finished connecting to, this is what sendPayload
    // needs, not every endpoint we've merely seen or requested a connection to.
    private val connectedEndpointIds = mutableSetOf<String>()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if (info.endpointName == localName) {
                connectionsClient.rejectConnection(endpointId)
                return
            }
            endpointNames[endpointId] = info.endpointName
            // Auto-accept for this milestone. A production version would show
            // info.authenticationDigits on both screens and require a human to
            // confirm they match before accepting, closer to what R5 means by
            // "trust each other on request."
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpointIds.add(endpointId)
                if (connectMillisState.value == null) {
                    connectMillisState.value = (System.nanoTime() - startedAtNanos) / 1_000_000
                }
                val name = endpointNames[endpointId] ?: endpointId
                if (!connectedPeers.contains(name)) {
                    connectedPeers.add(name)
                }
                status.value = "Connected to $name"
                // Stop actively searching once connected, saves battery and radio use.
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()
            } else {
                status.value = "Connection failed"
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpointIds.remove(endpointId)
            val name = endpointNames.remove(endpointId) ?: endpointId
            connectedPeers.remove(name)
            status.value = "Disconnected from $name"
            if (running && connectedEndpointIds.isEmpty()) {
                startSearching()
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // We advertise and discover at the same time, so we can find ourselves.
            if (info.endpointName == localName) {
                return
            }
            endpointNames[endpointId] = info.endpointName
            status.value = "Found ${info.endpointName}, connecting..."
            connectionsClient.requestConnection(localName, endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            status.value = "Lost an endpoint"
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val peer = endpointNames[endpointId] ?: endpointId
                if (experimentHandler?.invoke(peer, bytes) == true) {
                    return
                }
                onPayload?.invoke(peer, bytes)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> onTransferEnded?.invoke(update.payloadId, true)
                PayloadTransferUpdate.Status.FAILURE,
                PayloadTransferUpdate.Status.CANCELED -> onTransferEnded?.invoke(update.payloadId, false)
                else -> Unit
            }
        }
    }

    /**
     * True once Bluetooth and WiFi are both actually on. As of the Nearby
     * Connections changes rolling out in late 2026, the API no longer turns
     * these radios on for you, so we check first instead of silently failing.
     */
    private fun radiosReady(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothOn = bluetoothManager?.adapter?.isEnabled == true

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiOn = wifiManager?.isWifiEnabled == true

        return bluetoothOn && wifiOn
    }

    fun start() {
        if (!radiosReady()) {
            status.value = "Turn on Bluetooth and WiFi, then tap Start P2P again"
            return
        }

        startedAtNanos = System.nanoTime()
        connectMillisState.value = null
        running = true
        startSearching()
    }

    private fun startSearching() {
        status.value = "Searching..."

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            localName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnFailureListener { e ->
            Log.e(TAG, "startAdvertising failed", e)
            status.value = "Advertising failed: ${e.message}"
        }

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnFailureListener { e ->
            Log.e(TAG, "startDiscovery failed", e)
            status.value = "Discovery failed: ${e.message}"
        }
    }

    fun stop() {
        running = false
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointIds.clear()
        connectedPeers.clear()
        endpointNames.clear()
        status.value = "Idle"
    }

    /** Sends the bytes to every connected peer. Returns false when nobody is connected. */
    fun send(bytes: ByteArray): Boolean {
        if (connectedEndpointIds.isEmpty()) {
            return false
        }
        connectionsClient.sendPayload(connectedEndpointIds.toList(), Payload.fromBytes(bytes))
        return true
    }

    /** Like send, but returns the payload id so the caller can follow it in onTransferEnded. Null when nobody is connected. */
    fun sendTracked(bytes: ByteArray): Long? {
        if (connectedEndpointIds.isEmpty()) {
            return null
        }
        val payload = Payload.fromBytes(bytes)
        connectionsClient.sendPayload(connectedEndpointIds.toList(), payload)
        return payload.id
    }
}
