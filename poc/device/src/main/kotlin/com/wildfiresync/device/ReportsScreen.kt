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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wildfiresync.core.schema.Field
import com.wildfiresync.device.storage.StoredReport
import java.util.UUID
import kotlin.math.roundToInt

private val SEVERITY = Field.REPORT_SEVERITY.range().orElseThrow()

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // TODO pick the incident from a list once there are incidents on the tablet
    var incident by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = incident,
            onValueChange = { incident = it },
            label = { Text("Incident id") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { viewModel.createReport(incident) }) {
            Text("New report")
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (state.reports.isEmpty()) {
            Text("(no reports yet)")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.reports, key = { it.report }) { report ->
                ReportCard(report, onSave = { viewModel.setSeverity(report.report, it) })
            }
        }
    }
}

@Composable
private fun ReportCard(report: StoredReport, onSave: (Int) -> Unit) {
    var picked by remember(report.severity) {
        mutableFloatStateOf((report.severity ?: SEVERITY.min()).toFloat())
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(report.report.toString(), style = MaterialTheme.typography.bodySmall)
            Text("severity ${report.severity ?: "unknown"}", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = picked,
                    onValueChange = { picked = it },
                    valueRange = SEVERITY.min().toFloat()..SEVERITY.max().toFloat(),
                    steps = SEVERITY.max() - SEVERITY.min() - 1,
                    modifier = Modifier.weight(1f),
                )
                Text("${picked.roundToInt()}", modifier = Modifier.padding(horizontal = 8.dp))
                TextButton(onClick = { onSave(picked.roundToInt()) }) {
                    Text("Save")
                }
            }
        }
    }
}
