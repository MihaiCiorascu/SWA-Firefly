package com.wildfiresync.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValues
import io.nats.client.Nats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class ServerUiState(val sending: Boolean = false, val result: String? = null)

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val node = NodeId(DeviceIdentity.get(application))

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    // Rares' NATS sender, used to be its own cli entry point
    fun send(url: String, report: String, severity: String, seq: String) {
        _state.value = ServerUiState(sending = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val event = Event(
                    UUID.randomUUID(),
                    UUID.fromString(report.trim()),
                    Event.RecordType.REPORT,
                    Event.Operation.SET_FIELD,
                    Field.REPORT_SEVERITY.key(),
                    FieldValues.format(FieldValues.parse(Field.REPORT_SEVERITY, severity.trim())),
                    node.value(),
                    Instant.now(),
                    OpId(node, seq.trim().toLong()),
                    CausalContext.EMPTY,
                    emptySet(),
                )
                Nats.connect(url.trim()).use { connection ->
                    val ack = OperationSender(connection).send(event)
                    "sent $event to stream ${ack.stream} seq ${ack.seqno} duplicate=${ack.isDuplicate}"
                }
            } catch (e: Exception) {
                "could not send: ${e.message}"
            }
            _state.value = ServerUiState(result = result)
        }
    }

}
