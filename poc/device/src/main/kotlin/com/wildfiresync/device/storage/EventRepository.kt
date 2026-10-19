package com.wildfiresync.device.storage

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import java.time.Instant
import java.util.UUID

class EventRepository(private val database: LocalDatabase) {

    fun insert(event: Event) {
        database.db.insertOrThrow("events", null, values(event))
    }

    /** false when the event, or another one with its dot, is already stored */
    fun insertIfAbsent(event: Event): Boolean =
        database.db.insertWithOnConflict("events", null, values(event), SQLiteDatabase.CONFLICT_IGNORE) != -1L

    fun forRecord(record: UUID): List<Event> =
        query("SELECT * FROM events WHERE record_id = ? ORDER BY dot_device_id, dot_sequence", record.toString())

    fun all(): List<Event> = query("SELECT * FROM events ORDER BY dot_device_id, dot_sequence")

    /** the events the holder of this context has not seen */
    fun notCoveredBy(context: CausalContext): List<Event> = all().filterNot { context.covers(it.dot()) }

    /**
     * moves the counter of each of these nodes up for as long as the next sequence number is stored, so the context
     * only claims what this tablet really holds even when a sync left a gap
     */
    fun contiguous(context: CausalContext, nodes: Set<NodeId>): CausalContext {
        val counters = context.counters().toMutableMap()
        for (node in nodes) {
            var counter = counters[node] ?: 0L
            database.db.rawQuery(
                "SELECT dot_sequence FROM events WHERE dot_device_id = ? AND dot_sequence > ? ORDER BY dot_sequence",
                arrayOf(node.value(), counter.toString()),
            ).use {
                while (it.moveToNext() && it.getLong(0) == counter + 1) counter++
            }
            if (counter > 0) counters[node] = counter
        }
        return CausalContext(counters)
    }

    private fun query(sql: String, vararg args: String): List<Event> =
        database.db.rawQuery(sql, args).use { result -> buildList { while (result.moveToNext()) add(result.toEvent()) } }

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
        // the schema wants supersedes on resolutions only, core hands every other event an empty set
        put("supersedes", if (event.operation() == Event.Operation.RESOLVE_CONFLICT) Json.write(event.supersedes()) else null)
    }

}

/** reads an events row, the cursor has to select every column of events */
internal fun Cursor.toEvent(): Event {
    fun text(column: String): String? = getString(getColumnIndexOrThrow(column))

    return Event(
        UUID.fromString(text("event_id")),
        UUID.fromString(text("record_id")),
        Event.RecordType.valueOf(text("record_type")!!),
        Event.Operation.valueOf(text("operation")!!),
        text("field"),
        text("value"),
        text("actor_id"),
        Instant.parse(text("created_at")),
        OpId(NodeId(text("dot_device_id")), getLong(getColumnIndexOrThrow("dot_sequence"))),
        Json.readCausalContext(text("causal_context")!!),
        text("supersedes")?.let { Json.readUuids(it) },
    )
}
