package com.wildfiresync.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ServerScreen(viewModel: ServerViewModel = viewModel()) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    // 10.0.2.2 is the computer running the emulator, so the docker-compose nats
    var url by rememberSaveable { mutableStateOf("nats://10.0.2.2:4222") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            url,
            { url = it },
            label = { Text("NATS url") },
            singleLine = true,
            enabled = !status.connected,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { if (status.connected) viewModel.disconnect() else viewModel.connect(url) },
                enabled = !busy,
            ) {
                Text(if (status.connected) "Disconnect" else "Connect")
            }
            OutlinedButton(onClick = { viewModel.resend() }, enabled = status.connected && !busy) {
                Text("Send everything again")
            }
        }

        Text("Status: " + if (status.connected) "connected to ${status.url}" else "not connected")

        if (status.connected) {
            // once connected, a change made here goes out by itself, so these keep climbing on their own
            Text("Sent ${status.sent} event(s), received ${status.received}")
            status.position?.let { Text("Position: ${it.latitude()}, ${it.longitude()}") }
            if (status.zones.isNotEmpty()) {
                Text("Listening to ${status.zones.size} zone(s): " + status.zones.sorted().joinToString(", "),
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        status.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
