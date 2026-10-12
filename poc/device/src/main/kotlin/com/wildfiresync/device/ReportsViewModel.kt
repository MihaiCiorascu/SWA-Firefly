package com.wildfiresync.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wildfiresync.core.NodeId
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import com.wildfiresync.device.storage.StoredReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ReportsUiState(val reports: List<StoredReport> = emptyList(), val error: String? = null)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    // one thread for the db, keeps the writes in order
    private val db = Dispatchers.IO.limitedParallelism(1)

    private val database = lazy { LocalDatabase.open(application) }

    private val reports by lazy { ReportService(database.value) }

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        val node = NodeId(DeviceIdentity.get(application))
        onDb { ReplicaStateRepository(database.value).enrol(node) }
    }

    fun createReport(incident: String) = onDb {
        reports.createReport(UUID.fromString(incident.trim()))
    }

    fun setSeverity(report: UUID, severity: Int) = onDb {
        reports.setSeverity(report, severity)
    }

    private fun onDb(block: () -> Unit) {
        viewModelScope.launch(db) {
            try {
                block()
                _state.value = ReportsUiState(reports.reports())
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() {
        if (database.isInitialized()) database.value.close()
    }

}
