package com.wildfiresync.device.storage

import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues.IncidentStatus
import java.util.UUID

/** the incidents projection, written from the merge core's settled values only */
class IncidentRepository(private val database: LocalDatabase) {

    /** a field missing from values is unknown or in conflict, its column goes back to NULL */
    fun upsert(incident: UUID, values: Map<Field, FieldValue>) {
        database.db.execSQL(
            """
            INSERT INTO incidents (incident_id, name, status) VALUES (?, ?, ?)
            ON CONFLICT (incident_id) DO UPDATE SET name = excluded.name, status = excluded.status
            """.trimIndent(),
            arrayOf<Any?>(
                incident.toString(),
                (values[Field.INCIDENT_NAME] as FieldValue.Text?)?.value(),
                (values[Field.INCIDENT_STATUS] as FieldValue.Status?)?.value()?.name,
            ),
        )
    }

    fun exists(incident: UUID): Boolean =
        database.db.rawQuery("SELECT 1 FROM incidents WHERE incident_id = ?", arrayOf(incident.toString())).use {
            it.moveToFirst()
        }

    fun findAll(): List<StoredIncident> =
        database.db.rawQuery("SELECT incident_id, name, status FROM incidents ORDER BY name, incident_id", null).use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        StoredIncident(
                            UUID.fromString(it.getString(0)),
                            it.getString(1),
                            it.getString(2)?.let(IncidentStatus::valueOf),
                        ),
                    )
                }
            }
        }

}

/** null means unknown (no event yet) or in conflict */
data class StoredIncident(val incident: UUID, val name: String?, val status: IncidentStatus?)
