package com.wildfiresync.device

import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import com.wildfiresync.device.storage.ReportRepository

// TODO store changes as events (events table) with the next seq from replica_state
// TODO merge events with MergeCore instead of overwriting the value
// TODO track what still has to be sent and send it once there's connectivity
// TODO rest of the report fields + incidents

/**
 * example API for the rest of the tablet so nothing else touches SQL. only severity for now.
 * saved locally first so it survives being offline, no syncing with the server yet
 */
class ReportService(private val reports: ReportRepository) {

    fun setSeverity(report: ReportId, severity: Int) {
        require(severity in SetSeverity.MIN..SetSeverity.MAX) {
            "severity must be between ${SetSeverity.MIN} and ${SetSeverity.MAX}, got $severity"
        }
        reports.saveSeverity(report, severity)
    }

    /** null if this tablet doesn't know a severity for the report */
    fun severityOf(report: ReportId): Int? = reports.findSeverity(report)

}
