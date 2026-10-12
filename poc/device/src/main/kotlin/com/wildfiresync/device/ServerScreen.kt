package com.wildfiresync.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ServerScreen(viewModel: ServerViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 10.0.2.2 is the computer running the emulator, so the docker-compose nats
    var url by rememberSaveable { mutableStateOf("nats://10.0.2.2:4222") }
    var report by rememberSaveable { mutableStateOf("00000000-0000-0000-0000-000000000001") }
    var severity by rememberSaveable { mutableStateOf("5") }
    var seq by rememberSaveable { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(url, { url = it }, label = { Text("NATS url") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(report, { report = it }, label = { Text("Report id") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            severity,
            { severity = it },
            label = { Text("Severity") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            seq,
            { seq = it },
            label = { Text("Seq") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = { viewModel.send(url, report, severity, seq) }, enabled = !state.sending) {
            Text(if (state.sending) "Sending..." else "Send severity op")
        }

        state.result?.let { Text(it) }
    }
}
