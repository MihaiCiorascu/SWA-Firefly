package com.wildfiresync.device.storage

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.Event
import java.time.Instant
import java.util.UUID

class EventRepository(private val database: LocalDatabase) {

    fun insert(event: Event) {
        database.db.execSQL(
            """
            INSERT INTO events (event_id, record_id, record_type, operation, field, value, actor_id, created_at,
                                dot_device_id, dot_sequence, causal_context, supersedes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                event.eventId().toString(),
                event.recordId().toString(),
                event.recordType().name,
                event.operation().name,
                event.field(),
                event.value(),
                event.actorId(),
                event.createdAt().toString(),
                event.dot().node().value(),
                event.dot().seq(),
                Json.write(event.causalContext()),
                event.supersedes()?.let { Json.write(it) },
            ),
        )
    }

    fun forRecord(record: UUID): List<Event> =
        database.db.rawQuery(
            "SELECT * FROM events WHERE record_id = ? ORDER BY dot_device_id, dot_sequence",
            arrayOf(record.toString()),
        ).use { result ->
            fun text(column: String): String? = result.getString(result.getColumnIndexOrThrow(column))

            buildList {
                while (result.moveToNext()) {
                    add(
                        Event(
                            UUID.fromString(text("event_id")),
                            UUID.fromString(text("record_id")),
                            Event.RecordType.valueOf(text("record_type")!!),
                            Event.Operation.valueOf(text("operation")!!),
                            text("field"),
                            text("value"),
                            text("actor_id"),
                            Instant.parse(text("created_at")),
                            OpId(NodeId(text("dot_device_id")), result.getLong(result.getColumnIndexOrThrow("dot_sequence"))),
                            Json.readCausalContext(text("causal_context")!!),
                            text("supersedes")?.let { Json.readUuids(it) },
                        ),
                    )
                }
            }
        }

}
