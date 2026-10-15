package com.wildfiresync.device

import android.content.Context
import com.wildfiresync.core.Candidate
import com.wildfiresync.core.NodeId
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import com.wildfiresync.device.storage.StoredIncident
import com.wildfiresync.device.storage.StoredReport
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** what the screens show, rebuilt after every change */
data class TabletState(
    val incidents: List<StoredIncident> = emptyList(),
    val reports: List<StoredReport> = emptyList(),
    val conflicts: Map<UUID, Map<Field, List<Candidate>>> = emptyMap(),
)

/**
 * the one database connection of the app, shared by the screens and p2p sync. everything runs on one thread, so
 * writes from the screens and from another tablet never interleave
 */
class TabletStore(open: () -> ReportService) {

    private val db = Dispatchers.IO.limitedParallelism(1)

    // opened on the db thread the first time it's used
    private val service by lazy(open)

    private val _state = MutableStateFlow(TabletState())
    val state: StateFlow<TabletState> = _state.asStateFlow()

    /** called on the db thread with every event written on this tablet, so p2p can pass it on */
    @Volatile
    var onWritten: ((Event) -> Unit)? = null

    init {
        CoroutineScope(SupervisorJob() + db).launch { refresh() }
    }

    suspend fun <T> read(block: ReportService.() -> T): T = withContext(db) { service.block() }

    /** a change made on this tablet */
    suspend fun write(block: ReportService.() -> Event): Event = withContext(db) {
        service.block().also {
            refresh()
            onWritten?.invoke(it)
        }
    }

    /** events from the server or another tablet */
    suspend fun receive(events: List<Event>): ReportService.Receipt = withContext(db) {
        service.receive(events).also { if (it.stored.isNotEmpty()) refresh() }
    }

    private fun refresh() {
        _state.value = TabletState(service.incidents(), service.reports(), service.conflicts())
    }

    companion object {

        @Volatile
        private var instance: TabletStore? = null

        fun get(context: Context): TabletStore = instance ?: synchronized(this) {
            instance ?: context.applicationContext.let { app ->
                TabletStore {
                    val database = LocalDatabase.open(app)
                    ReplicaStateRepository(database).enrol(NodeId(DeviceIdentity.get(app)))
                    ReportService(database)
                }
            }.also { instance = it }
        }

    }

}
