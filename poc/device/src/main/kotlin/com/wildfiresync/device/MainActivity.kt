package com.wildfiresync.device

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import com.wildfiresync.device.experiment.ExperimentScreen
import com.wildfiresync.device.experiment.ExperimentUi
import com.wildfiresync.device.experiment.createExperiment
import java.time.Instant
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var p2pManager: P2pManager
    private lateinit var experiment: ExperimentUi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localName = DeviceIdentity.get(applicationContext)
        p2pManager = P2pManager(applicationContext, localName)
        experiment = createExperiment(applicationContext, localName, p2pManager)

        setContent {
            MaterialTheme {
                TabletApp(localName = localName, p2pManager = p2pManager, experiment = experiment)
            }
        }
    }

    override fun onDestroy() {
        p2pManager.stop()
        experiment.close()
        super.onDestroy()
    }
}

private enum class Screen(val label: String) {
    REPORTS("Reports"),
    SERVER("Server"),
    P2P("P2P"),
    EXPERIMENT("Experiment"),
}

@Composable
private fun TabletApp(localName: String, p2pManager: P2pManager, experiment: ExperimentUi) {
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
                Screen.REPORTS -> ReportsScreen()
                Screen.SERVER -> ServerScreen()
                Screen.P2P -> P2pScreen(localName = localName, manager = p2pManager)
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
fun P2pScreen(localName: String, manager: P2pManager) {
    var started by remember { mutableStateOf(false) }
    var nextSeq by remember { mutableLongStateOf(1L) }
    var lastSent by remember { mutableStateOf("") }
    var lastReceived by remember { mutableStateOf("") }

    // Throwaway demo: a real consumer would hand the decoded operation to the merge
    // library and the local store instead of just showing it.
    LaunchedEffect(manager) {
        manager.onPayload = { peer, bytes ->
            lastReceived = try {
                "${OperationWire.decode(bytes)} from $peer"
            } catch (e: Exception) {
                "undecodable payload from $peer (${e.message})"
            }
        }
    }

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

        Button(
            onClick = {
                val operation = SetSeverity(
                    OpId(NodeId(localName), nextSeq),
                    ReportId("report-1"),
                    Random.nextInt(SetSeverity.MIN, SetSeverity.MAX + 1),
                    Instant.now(),
                )
                if (manager.send(OperationWire.encode(operation))) {
                    lastSent = "$operation"
                    nextSeq += 1
                }
            },
            enabled = manager.connectedPeers.isNotEmpty()
        ) {
            Text("Send severity op")
        }

        if (lastSent.isNotEmpty()) {
            Text("Last sent: $lastSent")
        }
        if (lastReceived.isNotEmpty()) {
            Text("Last received: $lastReceived")
        }
    }
}
