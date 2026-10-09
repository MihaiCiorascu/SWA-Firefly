package com.wildfiresync.device.storage

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.Event
import java.time.Instant
import java.util.UUID

class EventRepository(private val database: LocalDatabase) {

    fun insert(event: Event) {
        database.connection.prepareStatement(
            """
            INSERT INTO events (event_id, record_id, record_type, operation, field, value, actor_id, created_at,
                                dot_device_id, dot_sequence, causal_context, supersedes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, event.eventId().toString())
            statement.setString(2, event.recordId().toString())
            statement.setString(3, event.recordType().name)
            statement.setString(4, event.operation().name)
            statement.setString(5, event.field())
            statement.setString(6, event.value())
            statement.setString(7, event.actorId())
            statement.setString(8, event.createdAt().toString())
            statement.setString(9, event.dot().node().value())
            statement.setLong(10, event.dot().seq())
            statement.setString(11, Json.write(event.causalContext()))
            statement.setString(12, event.supersedes()?.let { ids -> Json.write(ids.map { it.toString() }.sorted()) })
            statement.executeUpdate()
        }
    }

    fun forRecord(record: UUID): List<Event> =
        database.connection.prepareStatement(
            "SELECT * FROM events WHERE record_id = ? ORDER BY dot_device_id, dot_sequence",
        ).use { statement ->
            statement.setString(1, record.toString())
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            Event(
                                UUID.fromString(result.getString("event_id")),
                                UUID.fromString(result.getString("record_id")),
                                Event.RecordType.valueOf(result.getString("record_type")),
                                Event.Operation.valueOf(result.getString("operation")),
                                result.getString("field"),
                                result.getString("value"),
                                result.getString("actor_id"),
                                Instant.parse(result.getString("created_at")),
                                OpId(NodeId(result.getString("dot_device_id")), result.getLong("dot_sequence")),
                                Json.readCausalContext(result.getString("causal_context")),
                                result.getString("supersedes")?.let { Json.readUuids(it) },
                            ),
                        )
                    }
                }
            }
        }

}
