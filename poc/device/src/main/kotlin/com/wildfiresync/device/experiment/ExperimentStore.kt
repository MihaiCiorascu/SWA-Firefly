package com.wildfiresync.device.experiment

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.wildfiresync.core.schema.Event
import com.wildfiresync.device.storage.Json
import com.wildfiresync.device.storage.LocalDatabase

/**
 * Where the experiment's events are kept. A separate database file with the shared schema, so test runs never mix
 * with the tablet's real reports, and the events survive the app being force-stopped.
 */
class ExperimentStore(private val database: LocalDatabase) {

    /** Stores the events that are not held yet, in one transaction. Returns how many were new. */
    fun insertIfAbsent(events: List<Event>): Int = database.transaction {
        events.count { event ->
            // the unique (dot_device_id, dot_sequence) constraint turns a redelivery into a no-op
            database.db.insertWithOnConflict("events", null, values(event), SQLiteDatabase.CONFLICT_IGNORE) != -1L
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

    private fun values(event: Event) = ContentValues().apply {
        put("event_id", event.eventId().toString())
        put("record_id", event.recordId().toString())
        put("record_type", event.recordType().name)
        put("operation", event.operation().name)
        put("field", event.field())
        put("value", event.value())
        put("actor_id", event.actorId())
        put("created_at", event.createdAt().toString())
        put("dot_device_id", event.dot().node().value())
        put("dot_sequence", event.dot().seq())
        put("causal_context", Json.write(event.causalContext()))
    }

    companion object {

        /** The author of a run's events, so every run has dots of its own. */
        fun author(run: String) = "exp-$run"

    }

}
