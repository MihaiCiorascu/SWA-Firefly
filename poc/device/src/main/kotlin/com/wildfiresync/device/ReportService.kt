package com.wildfiresync.device

import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import com.wildfiresync.device.storage.ReportRepository

/**
 * EXAMPLE, work in progress: the API the rest of the tablet uses to work with reports, so callers never touch SQL.
 *
 * Only severity is supported, as the simplest example. Reports are saved locally first so they survive being
 * offline; nothing is synchronised with the server yet.
 *
 * TODO: store each change as an event (`events` table) with this tablet's next sequence number (`replica_state`)
 * TODO: compute a report's state by merging its events with MergeCore, instead of overwriting the stored value
 * TODO: keep track of which events still have to be sent, and send them when there is connectivity
 * TODO: support the other report fields (type, location, description, ...) and incidents
 */
class ReportService(private val reports: ReportRepository) {

    /** Saves [severity] as the severity of [report] on this tablet. */
    fun setSeverity(report: ReportId, severity: Int) {
        require(severity in SetSeverity.MIN..SetSeverity.MAX) {
            "severity must be between ${SetSeverity.MIN} and ${SetSeverity.MAX}, got $severity"
        }
        reports.saveSeverity(report, severity)
    }

    /** The severity this tablet knows for [report], or null if it has none. */
    fun severityOf(report: ReportId): Int? = reports.findSeverity(report)

}
