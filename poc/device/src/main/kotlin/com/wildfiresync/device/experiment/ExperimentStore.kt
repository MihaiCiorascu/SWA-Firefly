package com.wildfiresync.device.experiment

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.wildfiresync.core.SetSeverity
import com.wildfiresync.device.storage.LocalDatabase
import java.util.UUID

/**
 * Where the experiment's events are kept. A separate database file with the shared schema, so test runs never mix
 * with the tablet's real reports, and the events survive the app being force-stopped.
 */
class ExperimentStore(private val database: LocalDatabase) {

    /** Stores the operations that are not held yet, in one transaction. Returns how many were new. */
    fun insertIfAbsent(operations: List<SetSeverity>): Int = database.transaction {
        operations.count { operation ->
            // the unique (dot_device_id, dot_sequence) constraint turns a redelivery into a no-op
            database.db.insertWithOnConflict("events", null, values(operation), SQLiteDatabase.CONFLICT_IGNORE) != -1L
        }
    }

    fun count(run: String): Int =
        database.db.rawQuery("SELECT count(*) FROM events WHERE dot_device_id = ?", arrayOf(author(run))).use {
            it.moveToFirst()
            it.getInt(0)
        }

    fun clear() {
        database.db.execSQL("DELETE FROM events")
    }

    private fun values(operation: SetSeverity) = ContentValues().apply {
        val node = operation.id().node().value()
        val seq = operation.id().seq()
        put("event_id", UUID.nameUUIDFromBytes("$node:$seq".toByteArray()).toString())
        put("record_id", operation.report().value())
        put("record_type", "REPORT")
        put("operation", "SET_FIELD")
        put("field", "severity")
        put("value", operation.severity().toString())
        put("actor_id", node)
        put("created_at", operation.recordedAt().toString())
        put("dot_device_id", node)
        put("dot_sequence", seq)
        // The wire format carries no causal context yet.
        put("causal_context", "{}")
    }

    companion object {

        /** The author of a run's events, so every run has dots of its own. */
        fun author(run: String) = "exp-$run"

    }

}
