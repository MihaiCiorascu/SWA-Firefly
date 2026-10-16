package com.wildfiresync.device

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wildfiresync.device.experiment.ExperimentScreen
import com.wildfiresync.device.experiment.ExperimentUi
import com.wildfiresync.device.experiment.createExperiment

class MainActivity : ComponentActivity() {

    private lateinit var p2pManager: P2pManager
    private lateinit var peerSync: PeerSync
    private lateinit var experiment: ExperimentUi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localName = DeviceIdentity.get(applicationContext)
        p2pManager = P2pManager(applicationContext, localName)
        peerSync = PeerSync(
            TabletStore.get(applicationContext),
            object : PeerSync.Transport {
                override fun peers(): List<String> = p2pManager.connectedPeers.toList()
                override fun send(peer: String, bytes: ByteArray): Boolean = p2pManager.send(peer, bytes)
            },
            lifecycleScope,
        )
        p2pManager.onPeerConnected = peerSync::onConnected
        p2pManager.onPayload = { peer, bytes ->
            if (!peerSync.handle(peer, bytes)) {
                Log.w("MainActivity", "ignored a payload from $peer that is not a sync message")
            }
        }
        experiment = createExperiment(applicationContext, localName, p2pManager)

        setContent {
            MaterialTheme {
                TabletApp(localName = localName, p2pManager = p2pManager, peerSync = peerSync, experiment = experiment)
            }
        }
    }

    override fun onDestroy() {
        peerSync.close()
        p2pManager.stop()
        experiment.close()
        super.onDestroy()
    }
}

private enum class Screen(val label: String) {
    INCIDENTS("Incidents"),
    REPORTS("Reports"),
    SERVER("Server"),
    P2P("P2P"),
    EXPERIMENT("Experiment"),
}

@Composable
private fun TabletApp(localName: String, p2pManager: P2pManager, peerSync: PeerSync, experiment: ExperimentUi) {
    var screen by rememberSaveable { mutableStateOf(Screen.REPORTS) }

    Scaffold(
        topBar = {
            PrimaryTabRow(selectedTabIndex = screen.ordinal, modifier = Modifier.statusBarsPadding()) {
                Screen.entries.forEach {
                    Tab(selected = screen == it, onClick = { screen = it }, text = { Text(it.label) })
                }
            }
        },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                Screen.INCIDENTS -> IncidentsScreen()
                Screen.REPORTS -> ReportsScreen()
                Screen.SERVER -> ServerScreen()
                Screen.P2P -> P2pScreen(localName = localName, manager = p2pManager, sync = peerSync)
                Screen.EXPERIMENT -> ExperimentScreen(
                    manager = p2pManager,
                    runner = experiment.runner,
                    store = experiment.store,
                    settings = experiment.settings,
                    rows = experiment.rows,
                )
            }
        }
    }
}

/** The runtime permissions Nearby Connections needs, which differ by Android version. */
private fun requiredPermissions(): Array<String> {
    val permissions = mutableListOf<String>()

    // Needed on every version: below Android 12 for Bluetooth scanning, and on newer
    // ones Play Services' startDiscovery still fails with 8034 without coarse location.
    permissions += Manifest.permission.ACCESS_FINE_LOCATION
    permissions += Manifest.permission.ACCESS_COARSE_LOCATION

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ (API 31+)
        permissions += Manifest.permission.BLUETOOTH_ADVERTISE
        permissions += Manifest.permission.BLUETOOTH_CONNECT
        permissions += Manifest.permission.BLUETOOTH_SCAN
    }

    permissions += Manifest.permission.ACCESS_WIFI_STATE
    permissions += Manifest.permission.CHANGE_WIFI_STATE

    if (Build.VERSION.SDK_INT >= 33) {
        // Android 13+ (API 33+)
        permissions += Manifest.permission.NEARBY_WIFI_DEVICES
    }

    return permissions.toTypedArray()
}

@Composable
fun P2pScreen(localName: String, manager: P2pManager, sync: PeerSync, viewModel: ReportsViewModel = viewModel()) {
    var started by remember { mutableStateOf(false) }
    val summaries by sync.summaries.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { granted -> granted }) {
            manager.start()
            started = true
        } else {
            manager.status.value = "Permissions denied, cannot start P2P"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("This device: $localName", style = MaterialTheme.typography.titleMedium)
        Text("Status: ${manager.status.value}")

        Button(onClick = {
            if (!started) {
                permissionLauncher.launch(requiredPermissions())
            } else {
                manager.stop()
                started = false
            }
        }) {
            Text(if (started) "Stop P2P" else "Start P2P")
        }

        Text("Connected peers:", style = MaterialTheme.typography.titleSmall)
        if (manager.connectedPeers.isEmpty()) {
            Text("(none yet)")
        } else {
            manager.connectedPeers.forEach { peer -> Text("- $peer") }
        }

        // what came in and out, so both crew leaders see the sync happened
        Text("Sync:", style = MaterialTheme.typography.titleSmall)
        if (summaries.isEmpty()) {
            Text("(no sync yet, it starts by itself once a peer connects)")
        }
        summaries.values.forEach { summary ->
            Text(
                "${summary.peer}: sent ${summary.sent} events, received ${summary.received} new" +
                    " (${summary.duplicates} already held, ${summary.rejected} refused)",
            )
            summary.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        val open = state.tablet.conflicts.values.sumOf { it.size }
        if (open > 0) {
            Text(
                "$open field(s) in conflict, settle them on the Incidents and Reports tabs",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
