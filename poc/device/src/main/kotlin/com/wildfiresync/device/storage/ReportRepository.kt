package com.wildfiresync.device.storage

import com.wildfiresync.core.ReportId

// TODO reports should be derived from the events table, store events and rebuild it instead of writing to it directly

/**
 * example, SQL for the severity column of the reports table (database-schema MR)
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

    /** null if the report is unknown or has no severity yet */
    fun findSeverity(report: ReportId): Int? =
        database.connection.prepareStatement("SELECT severity FROM reports WHERE report_id = ?").use { statement ->
            statement.setString(1, report.value())
            statement.executeQuery().use { result ->
                if (result.next()) result.getInt(1).takeUnless { result.wasNull() } else null
            }
        }

}
