package com.wildfiresync.device.storage

import com.wildfiresync.core.Candidate
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.schema.CreateValues
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues
import java.util.UUID

/** field_conflicts, the candidate events of every Tier B field that is in conflict right now */
class ConflictRepository(private val database: LocalDatabase) {

    /** replaces the conflicts of this record's fields with the ones given */
    fun replace(record: RecordRef, conflicts: Map<Field, List<Candidate>>) {
        val keys = Field.entries.filter { it.recordType() == record.type() }.map { it.key() }
        database.db.execSQL(
            "DELETE FROM field_conflicts WHERE record_id = ? AND field IN (${keys.joinToString { "?" }})",
            arrayOf(record.id().toString(), *keys.toTypedArray()),
        )
        conflicts.forEach { (field, candidates) ->
            candidates.forEach { candidate ->
                database.db.execSQL(
                    "INSERT INTO field_conflicts (record_id, field, event_id) VALUES (?, ?, ?)",
                    arrayOf(record.id().toString(), field.key(), candidate.eventId().toString()),
                )
            }
        }
    }

    /** record id -> field -> its candidates, ordered like the merge core orders them */
    fun findAll(): Map<UUID, Map<Field, List<Candidate>>> =
        database.db.rawQuery(
            "SELECT c.field AS conflict_field, e.* FROM field_conflicts c JOIN events e ON e.event_id = c.event_id",
            arrayOf(),
        ).use { result ->
            val rows = buildList {
                while (result.moveToNext()) {
                    val event = result.toEvent()
                    // not e.field, a CREATE has none and can be a candidate for several fields
                    val field = Field.of(event.recordType(), result.getString(result.getColumnIndexOrThrow("conflict_field")))
                    add(event to field)
                }
            }
            rows.groupBy { (event, _) -> event.recordId() }.mapValues { (_, events) ->
                events.groupBy({ (_, field) -> field }, { (event, field) -> candidate(event, field) })
                    .mapValues { (_, candidates) -> candidates.sorted() }
            }
        }

    private fun candidate(event: Event, field: Field): Candidate {
        val value: FieldValue = if (event.operation() == Event.Operation.CREATE) {
            CreateValues.parse(event.recordType(), event.value()).getValue(field)
        } else {
            FieldValues.parse(field, event.value())
        }
        return Candidate(value, event.eventId(), event.dot(), event.causalContext(), event.actorId(), event.createdAt())
    }

}
