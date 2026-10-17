package com.wildfiresync.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wildfiresync.core.Candidate
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues.ReportType
import com.wildfiresync.device.storage.StoredIncident
import com.wildfiresync.device.storage.StoredReport
import java.util.UUID
import kotlin.math.roundToInt

private val SEVERITY = Field.REPORT_SEVERITY.range().orElseThrow()

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val incidents = state.tablet.incidents

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NewReport(incidents, onCreate = viewModel::createReport)

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (state.tablet.reports.isEmpty()) {
            Text("(no reports yet)")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.tablet.reports, key = { it.report }) { report ->
                ReportCard(
                    report,
                    incidents.firstOrNull { it.incident == report.incident },
                    state.tablet.conflicts[report.report].orEmpty(),
                    viewModel,
                )
            }
        }
    }
}

@Composable
private fun NewReport(incidents: List<StoredIncident>, onCreate: (UUID, Map<Field, FieldValue>) -> Unit) {
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf("") }
    val incident = incidents.firstOrNull { it.incident.toString() == picked }

    if (incidents.isEmpty()) {
        Text("Create an incident on the Incidents tab first, every report belongs to one.")
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { open = true }) {
                Text(incident?.let { "Incident: ${it.name ?: it.incident}" } ?: "Pick an incident")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                incidents.forEach {
                    DropdownMenuItem(
                        text = { Text(it.name ?: it.incident.toString()) },
                        onClick = {
                            picked = it.incident.toString()
                            open = false
                        },
                    )
                }
            }
        }
        Button(
            onClick = {
                val values = if (description.isBlank()) emptyMap()
                else mapOf<Field, FieldValue>(Field.REPORT_DESCRIPTION to FieldValue.Text(description.trim()))
                onCreate(incident!!.incident, values)
                description = ""
            },
            enabled = incident != null,
        ) {
            Text("New report")
        }
    }
    OutlinedTextField(
        value = description,
        onValueChange = { description = it.take(MAX_TEXT_LENGTH) },
        label = { Text("Description (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReportCard(
    report: StoredReport,
    incident: StoredIncident?,
    conflicts: Map<Field, List<Candidate>>,
    viewModel: ReportsViewModel,
) {
    val record = RecordRef.report(report.report)
    var editing by rememberSaveable { mutableStateOf(false) }

    fun shown(field: Field, value: Any?): String = when {
        field in conflicts -> "in conflict"
        value == null -> "unknown"
        else -> value.toString()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when {
                            !report.created -> "(not created yet, only edits have arrived)"
                            else -> incident?.name ?: report.incident.toString()
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(report.report.toString(), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { editing = !editing }) {
                    Text(if (editing) "Done" else "Edit")
                }
            }
            Text("types: ${if (report.types.isEmpty()) "none" else report.types.joinToString { it.name }}")
            Text("location: ${shown(Field.REPORT_LOCATION, report.location?.let { "${it.latitude()}, ${it.longitude()}" })}")
            Text("severity: ${shown(Field.REPORT_SEVERITY, report.severity)}")
            Text("description: ${shown(Field.REPORT_DESCRIPTION, report.description)}")

            if (editing) {
                ReportEditor(report, record, viewModel)
            }
            conflicts.forEach { (field, candidates) ->
                ConflictCard(
                    field,
                    candidates,
                    onKeep = { viewModel.resolveConflict(record, field, it) },
                    onAgree = { viewModel.resolveConflict(record, field, it) },
                )
            }
        }
    }
}

@Composable
private fun ReportEditor(report: StoredReport, record: RecordRef, viewModel: ReportsViewModel) {
    var severity by remember(report.severity) { mutableFloatStateOf((report.severity ?: SEVERITY.min()).toFloat()) }
    var latitude by rememberSaveable(report.location) { mutableStateOf(report.location?.latitude()?.toString().orEmpty()) }
    var longitude by rememberSaveable(report.location) { mutableStateOf(report.location?.longitude()?.toString().orEmpty()) }
    var description by rememberSaveable(report.description) { mutableStateOf(report.description.orEmpty()) }

    // types only ever get added, the merge keeps every type any crew gave
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Add type")
        ReportType.entries.forEach { type ->
            FilterChip(
                selected = type in report.types,
                onClick = { if (type !in report.types) viewModel.setField(record, Field.REPORT_TYPE, FieldValue.Types.of(type)) },
                label = { Text(type.name) },
            )
        }
    }

    // severity only goes up, a lower value is stored but the merge keeps the highest
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Severity")
        Slider(
            value = severity,
            onValueChange = { severity = it },
            valueRange = SEVERITY.min().toFloat()..SEVERITY.max().toFloat(),
            steps = SEVERITY.max() - SEVERITY.min() - 1,
            modifier = Modifier.weight(1f),
        )
        Text("${severity.roundToInt()}", modifier = Modifier.padding(horizontal = 8.dp))
        TextButton(onClick = { viewModel.setField(record, Field.REPORT_SEVERITY, FieldValue.Num(severity.roundToInt())) }) {
            Text("Save")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text("Latitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text("Longitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { viewModel.setField(record, Field.REPORT_LOCATION, "$latitude,$longitude") },
            enabled = latitude.isNotBlank() && longitude.isNotBlank(),
        ) {
            Text("Save")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(MAX_TEXT_LENGTH) },
            label = { Text("Description") },
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { viewModel.setField(record, Field.REPORT_DESCRIPTION, FieldValue.Text(description.trim())) },
            enabled = description.isNotBlank() && description.trim() != report.description,
        ) {
            Text("Save")
        }
    }
}
