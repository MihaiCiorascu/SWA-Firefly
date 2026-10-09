package com.wildfiresync.device.storage

import java.util.UUID

class ReportRepository(private val database: LocalDatabase) {

    fun insert(report: UUID, incident: UUID) {
        database.connection.prepareStatement("INSERT INTO reports (report_id, incident_id) VALUES (?, ?)").use {
            it.setString(1, report.toString())
            it.setString(2, incident.toString())
            it.executeUpdate()
        }
    }

    fun exists(report: UUID): Boolean =
        database.connection.prepareStatement("SELECT 1 FROM reports WHERE report_id = ?").use { statement ->
            statement.setString(1, report.toString())
            statement.executeQuery().use { it.next() }
        }

    fun raiseSeverity(report: UUID, severity: Int) {
        database.connection.prepareStatement(
            "UPDATE reports SET severity = max(coalesce(severity, ?), ?) WHERE report_id = ?",
        ).use { statement ->
            statement.setInt(1, severity)
            statement.setInt(2, severity)
            statement.setString(3, report.toString())
            statement.executeUpdate()
        }
    }

    /** null if the report is unknown or has no severity yet */
    fun findSeverity(report: UUID): Int? =
        database.connection.prepareStatement("SELECT severity FROM reports WHERE report_id = ?").use { statement ->
            statement.setString(1, report.toString())
            statement.executeQuery().use { result ->
                if (result.next()) result.getInt(1).takeUnless { result.wasNull() } else null
            }
        }

    fun findAll(): List<StoredReport> =
        database.connection.prepareStatement("SELECT report_id, incident_id, severity FROM reports ORDER BY report_id")
            .use { statement ->
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) {
                            add(
                                StoredReport(
                                    UUID.fromString(result.getString(1)),
                                    result.getString(2)?.let(UUID::fromString),
                                    result.getInt(3).takeUnless { result.wasNull() },
                                ),
                            )
                        }
                    }
                }
            }

}

data class StoredReport(val report: UUID, val incident: UUID?, val severity: Int?)
