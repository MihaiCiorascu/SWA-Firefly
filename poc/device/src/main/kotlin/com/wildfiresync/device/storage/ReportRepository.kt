package com.wildfiresync.device.storage

import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues
import com.wildfiresync.core.schema.FieldValues.Location
import com.wildfiresync.core.schema.FieldValues.ReportType
import java.util.UUID

/** the reports projection, written from the merge core's settled values only */
class ReportRepository(private val database: LocalDatabase) {

    /** a field missing from values is unknown or in conflict, its column goes back to NULL */
    fun upsert(report: UUID, values: Map<Field, FieldValue>) {
        val location = (values[Field.REPORT_LOCATION] as FieldValue.Place?)?.value()
        database.db.execSQL(
            """
            INSERT INTO reports (report_id, incident_id, report_types, latitude, longitude, severity, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (report_id) DO UPDATE SET
                incident_id = excluded.incident_id, report_types = excluded.report_types,
                latitude = excluded.latitude, longitude = excluded.longitude,
                severity = excluded.severity, description = excluded.description
            """.trimIndent(),
            arrayOf<Any?>(
                report.toString(),
                (values[Field.REPORT_INCIDENT_ID] as FieldValue.Reference?)?.value()?.toString(),
                values[Field.REPORT_TYPE]?.let(FieldValues::format),
                location?.latitude(),
                location?.longitude(),
                (values[Field.REPORT_SEVERITY] as FieldValue.Num?)?.value(),
                (values[Field.REPORT_DESCRIPTION] as FieldValue.Text?)?.value(),
            ),
        )
    }

    fun exists(report: UUID): Boolean =
        database.db.rawQuery("SELECT 1 FROM reports WHERE report_id = ?", arrayOf(report.toString())).use {
            it.moveToFirst()
        }

    fun findAll(): List<StoredReport> =
        database.db.rawQuery(
            "SELECT report_id, incident_id, report_types, latitude, longitude, severity, description FROM reports ORDER BY report_id",
            null,
        ).use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        StoredReport(
                            UUID.fromString(it.getString(0)),
                            it.getString(1)?.let(UUID::fromString),
                            it.getString(2)?.let { types -> (FieldValues.parse(Field.REPORT_TYPE, types) as FieldValue.Types).values() }
                                ?: emptySet(),
                            if (it.isNull(3)) null else Location(it.getDouble(3), it.getDouble(4)),
                            if (it.isNull(5)) null else it.getInt(5),
                            it.getString(6),
                        ),
                    )
                }
            }
        }

}

/** null means unknown (no event yet) or in conflict */
data class StoredReport(
    val report: UUID,
    val incident: UUID?,
    val types: Set<ReportType>,
    val location: Location?,
    val severity: Int?,
    val description: String?,
) {

    /** the CREATE always carries the incident, so a report without one only got edits so far */
    val created: Boolean get() = incident != null

}
