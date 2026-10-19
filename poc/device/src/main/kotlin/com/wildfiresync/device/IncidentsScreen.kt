package com.wildfiresync.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wildfiresync.core.Candidate
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues.IncidentStatus
import com.wildfiresync.device.storage.StoredIncident

@Composable
fun IncidentsScreen(viewModel: ReportsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_TEXT_LENGTH) },
                label = { Text("Incident name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    viewModel.createIncident(name)
                    name = ""
                },
                enabled = name.isNotBlank(),
            ) {
                Text("New incident")
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (state.tablet.incidents.isEmpty()) {
            Text("(no incidents yet)")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.tablet.incidents, key = { it.incident }) { incident ->
                IncidentCard(incident, state.tablet.conflicts[incident.incident].orEmpty(), viewModel)
            }
        }
    }
}

@Composable
private fun IncidentCard(incident: StoredIncident, conflicts: Map<Field, List<Candidate>>, viewModel: ReportsViewModel) {
    val record = RecordRef.incident(incident.incident)
    var name by rememberSaveable(incident.name) { mutableStateOf(incident.name.orEmpty()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(incident.name ?: "(no name yet)", style = MaterialTheme.typography.titleMedium)
            Text(incident.incident.toString(), style = MaterialTheme.typography.bodySmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_TEXT_LENGTH) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { viewModel.setField(record, Field.INCIDENT_NAME, FieldValue.Text(name.trim())) },
                    enabled = name.isNotBlank() && name.trim() != incident.name,
                ) {
                    Text("Save")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status")
                IncidentStatus.entries.forEach { status ->
                    FilterChip(
                        selected = incident.status == status,
                        onClick = { if (incident.status != status) viewModel.setField(record, Field.INCIDENT_STATUS, FieldValue.Status(status)) },
                        label = { Text(status.name) },
                    )
                }
            }

            conflicts.forEach { (field, candidates) ->
                ConflictCard(
                    field,
                    candidates,
                    onKeep = { value, seen -> viewModel.resolveConflict(record, field, value, seen) },
                    onAgree = { text, seen -> viewModel.resolveConflict(record, field, text, seen) },
                )
            }
        }
    }
}
