package com.wildfiresync.android

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var p2pManager: P2pManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localName = DeviceIdentity.get(applicationContext)
        p2pManager = P2pManager(applicationContext, localName)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    P2pScreen(localName = localName, manager = p2pManager)
                }
            }
        }
    }

    override fun onDestroy() {
        p2pManager.stop()
        super.onDestroy()
    }
}

/** The runtime permissions Nearby Connections needs, which differ by Android version. */
private fun requiredPermissions(): Array<String> {
    val permissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ (API 31+)
        permissions += Manifest.permission.BLUETOOTH_ADVERTISE
        permissions += Manifest.permission.BLUETOOTH_CONNECT
        permissions += Manifest.permission.BLUETOOTH_SCAN
    } else {
        // Below Android 12, Bluetooth scanning needed location instead.
        permissions += Manifest.permission.ACCESS_FINE_LOCATION
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
            onClick = { manager.sendTestMessage() },
            enabled = manager.connectedPeers.isNotEmpty()
        ) {
            Text("Send test message")
        }

        if (manager.lastReceivedMessage.value.isNotEmpty()) {
            Text("Last received: ${manager.lastReceivedMessage.value}")
        }
    }
}
