package com.wildfiresync.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.wildfiresync.core.Candidate
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val WHEN = DateTimeFormatter.ofPattern("d MMM HH:mm").withZone(ZoneId.systemDefault())

/** a field value the way a crew leader reads it */
fun show(value: FieldValue): String = when (value) {
    is FieldValue.Num -> value.value().toString()
    is FieldValue.Text -> value.value()
    is FieldValue.Status -> value.value().name
    is FieldValue.Types -> value.values().joinToString { it.name }
    is FieldValue.Place -> "${value.value().latitude()}, ${value.value().longitude()}"
    is FieldValue.Reference -> value.value().toString()
}

/**
 * the values two crews wrote without knowing about each other, with who wrote them and when. the crew leaders pick
 * one, or for text agree on new wording, and that settles it on every tablet
 */
@Composable
fun ConflictCard(
    field: Field,
    candidates: List<Candidate>,
    onKeep: (FieldValue, Set<UUID>) -> Unit,
    onAgree: (String, Set<UUID>) -> Unit,
) {
    // what the crew leaders were shown, the store refuses the resolution if it changed since
    val seen = candidates.map { it.eventId() }.toSet()
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${field.key()} is in conflict", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
            candidates.forEach { candidate ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(show(candidate.value()))
                        Text(
                            "by ${candidate.actorId()} at ${WHEN.format(candidate.createdAt())}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { onKeep(candidate.value(), seen) }) {
                        Text("Keep this")
                    }
                }
            }
            if (field.kind() == Field.Kind.TEXT) {
                var agreed by rememberSaveable(candidates) { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = agreed,
                        onValueChange = { agreed = it.take(MAX_TEXT_LENGTH) },
                        label = { Text("Agreed wording") },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onAgree(agreed, seen) }, enabled = agreed.isNotBlank()) {
                        Text("Resolve")
                    }
                }
            }
        }
    }
}
