package com.wildfiresync.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** the Server tab. the link itself lives in ServerSync, this only drives it from the screen */
class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val sync = ServerSync(TabletStore.get(application), viewModelScope)

    val status: StateFlow<ServerSync.Status> = sync.status

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun connect(url: String) = whileBusy { sync.connect(url.trim()) }

    fun disconnect() = whileBusy { sync.disconnect() }

    fun resend() = whileBusy { sync.resend() }

    /** one at a time, so a second tap while a connection is opening does not start another */
    private fun whileBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            }
            finally {
                _busy.value = false
            }
        }
    }

    override fun onCleared() {
        // viewModelScope is already cancelled here, so the connection is closed on a scope of its own
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { sync.disconnect() }
    }

}
