package com.wildfiresync.device.storage

import com.wildfiresync.core.ReportId

/**
 * EXAMPLE, work in progress: the SQL for reports, kept out of the rest of the device code.
 *
 * Only reads and writes the `severity` column of the `reports` table from the database-schema MR.
 *
 * TODO: in the real design `reports` is derived from the `events` table; changes should be stored as events and
 *  this table rebuilt from them, instead of being written to directly as done here
 */
class ReportRepository(private val database: LocalDatabase) {

    fun saveSeverity(report: ReportId, severity: Int) {
        database.connection.prepareStatement(
            """
            INSERT INTO reports (report_id, severity) VALUES (?, ?)
            ON CONFLICT (report_id) DO UPDATE SET severity = excluded.severity
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, report.value())
            statement.setInt(2, severity)
            statement.executeUpdate()
        }
    }

    /** The stored severity of [report], or null if the report is unknown or has no severity yet. */
    fun findSeverity(report: ReportId): Int? =
        database.connection.prepareStatement("SELECT severity FROM reports WHERE report_id = ?").use { statement ->
            statement.setString(1, report.value())
            statement.executeQuery().use { result ->
                if (result.next()) result.getInt(1).takeUnless { result.wasNull() } else null
            }
        }

}
