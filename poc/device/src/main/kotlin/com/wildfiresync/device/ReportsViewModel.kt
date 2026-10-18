package com.wildfiresync.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ReportsUiState(val tablet: TabletState = TabletState(), val error: String? = null)

/** for the incidents and reports tabs, both go through the shared store */
class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TabletStore.get(application)

    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ReportsUiState> = combine(store.state, error, ::ReportsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState(store.state.value))

    fun createIncident(name: String) = write { createIncident(name) }

    fun createReport(incident: UUID, values: Map<Field, FieldValue> = emptyMap()) = write { createReport(incident, values) }

    fun setField(record: RecordRef, field: Field, value: FieldValue) = write { setField(record, field, value) }

    fun resolveConflict(record: RecordRef, field: Field, value: FieldValue, seen: Set<UUID>) =
        write { resolveConflict(record, field, value, seen) }

    /** typed in by hand, the merge core's parser says what is wrong with it */
    fun setField(record: RecordRef, field: Field, text: String) = write { setField(record, field, FieldValues.parse(field, text.trim())) }

    fun resolveConflict(record: RecordRef, field: Field, text: String, seen: Set<UUID>) =
        write { resolveConflict(record, field, FieldValues.parse(field, text.trim()), seen) }

    private fun write(block: ReportService.() -> Event) {
        viewModelScope.launch {
            error.value = try {
                store.write(block)
                null
            } catch (e: IllegalArgumentException) {
                e.message
            }
        }
    }

}
