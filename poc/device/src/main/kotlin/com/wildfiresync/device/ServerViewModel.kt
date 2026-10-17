package com.wildfiresync.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.nats.client.Nats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ServerUiState(val sending: Boolean = false, val result: String? = null)

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TabletStore.get(application)

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    /**
     * sends every event the tablet holds, its own and the ones from other tablets. jetstream drops a dot it already
     * has and the server's merge is idempotent, so sending again after a cut off connection is safe
     */
    fun send(url: String) {
        _state.value = ServerUiState(sending = true)
        viewModelScope.launch {
            val result = try {
                val events = store.read { allEvents() }
                withContext(Dispatchers.IO) {
                    Nats.connect(url.trim()).use { connection ->
                        val sender = OperationSender(connection)
                        val duplicates = events.count { sender.send(it).isDuplicate }
                        "sent ${events.size} events, the broker already had $duplicates of them"
                    }
                }
            } catch (e: Exception) {
                "could not send: ${e.message}"
            }
            _state.value = ServerUiState(result = result)
        }
    }

}
