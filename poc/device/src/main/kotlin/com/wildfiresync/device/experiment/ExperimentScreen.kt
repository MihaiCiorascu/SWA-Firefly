package com.wildfiresync.device.experiment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wildfiresync.device.P2pManager

/** What the person running the experiment types in. Read by the runner when it logs a row. */
class ExperimentSettings {

    var note by mutableStateOf("")

}

/**
 * Screen for Experiment 1. Start P2P on the P2P tab of both devices first. Then one device taps Send run and the
 * other just waits, each logs its own row to p2p-experiment.csv in the app's files folder.
 */
@Composable
fun ExperimentScreen(
    manager: P2pManager,
    runner: ExperimentRunner,
    store: ExperimentStore,
    settings: ExperimentSettings,
    rows: List<String>,
) {
    var events by remember { mutableStateOf(1_000) }
    var batch by remember { mutableStateOf(10) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Status: ${manager.status.value}", style = MaterialTheme.typography.titleSmall)
        Text("Time to connect: ${manager.connectMillis?.let { "$it ms" } ?: "not connected yet"}")

        Text("Events in the run")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 1_000, 10_000).forEach {
                FilterChip(selected = events == it, onClick = { events = it }, label = { Text("$it") })
            }
        }

        Text("Events per payload")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 10, 50).forEach {
                FilterChip(selected = batch == it, onClick = { batch = it }, label = { Text("$it") })
            }
        }

        OutlinedTextField(
            value = settings.note,
            onValueChange = { settings.note = it },
            label = { Text("Condition (distance, interruption)") },
            singleLine = true,
        )

        Button(
            enabled = manager.connectedPeers.isNotEmpty(),
            onClick = { message = if (runner.startRun(events, batch)) "Run sent" else "Link lost while sending" },
        ) {
            Text("Send run")
        }
        OutlinedButton(
            enabled = manager.connectedPeers.isNotEmpty(),
            onClick = { message = if (runner.resend()) "Last run sent again" else "Nothing to send again" },
        ) {
            Text("Send last run again")
        }
        OutlinedButton(onClick = { runner.logPartial() }) {
            Text("Log where this device stands (receiver)")
        }
        OutlinedButton(onClick = { store.clear(); message = "Stored events cleared" }) {
            Text("Clear stored events")
        }

        if (message.isNotEmpty()) {
            Text(message)
        }

        Text("Last rows", style = MaterialTheme.typography.titleSmall)
        if (rows.isEmpty()) {
            Text("(none yet)")
        } else {
            rows.takeLast(6).reversed().forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
