package com.wildfiresync.device.storage

import java.util.UUID

class ReportRepository(private val database: LocalDatabase) {

    fun insert(report: UUID, incident: UUID) {
        database.db.execSQL(
            "INSERT INTO reports (report_id, incident_id) VALUES (?, ?)",
            arrayOf(report.toString(), incident.toString()),
        )
    }

    fun exists(report: UUID): Boolean =
        database.db.rawQuery("SELECT 1 FROM reports WHERE report_id = ?", arrayOf(report.toString())).use {
            it.moveToFirst()
        }

    fun raiseSeverity(report: UUID, severity: Int) {
        database.db.execSQL(
            "UPDATE reports SET severity = max(coalesce(severity, ?), ?) WHERE report_id = ?",
            arrayOf<Any>(severity, severity, report.toString()),
        )
    }

    /** null if the report is unknown or has no severity yet */
    fun findSeverity(report: UUID): Int? =
        database.db.rawQuery("SELECT severity FROM reports WHERE report_id = ?", arrayOf(report.toString())).use {
            if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) else null
        }

    fun findAll(): List<StoredReport> =
        database.db.rawQuery("SELECT report_id, incident_id, severity FROM reports ORDER BY report_id", null).use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        StoredReport(
                            UUID.fromString(it.getString(0)),
                            it.getString(1)?.let(UUID::fromString),
                            if (it.isNull(2)) null else it.getInt(2),
                        ),
                    )
                }
            }
        }

}

data class StoredReport(val report: UUID, val incident: UUID?, val severity: Int?)
