package com.wildfiresync.device

import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.device.storage.EventRepository
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import com.wildfiresync.device.storage.ReportRepository
import java.time.Clock
import java.util.UUID

// TODO apply events received from the server / other tablets, not only the ones written here
// TODO track what still has to be sent and send it once there's connectivity
// TODO rest of the report fields + incidents

/**
 * API for the rest of the tablet so nothing else touches SQL. every change is saved as an event first and the reports
 * table is updated from it in the same transaction, so it survives being offline
 */
class ReportService(private val database: LocalDatabase, private val clock: Clock = Clock.systemUTC()) {

    private val replica = ReplicaStateRepository(database)
    private val events = EventRepository(database)
    private val reports = ReportRepository(database)

    fun createReport(incident: UUID): ReportId = database.transaction {
        val report = UUID.randomUUID()
        // TODO agree with the server on what a CREATE value holds, for now only the immutable incidentId
        write(report, Event.Operation.CREATE, null, """{"${Field.REPORT_INCIDENT_ID.key()}":"$incident"}""")
        reports.insert(report, incident)
        ReportId(report.toString())
    }

    fun setSeverity(report: ReportId, severity: Int) {
        require(severity in SetSeverity.MIN..SetSeverity.MAX) {
            "severity must be between ${SetSeverity.MIN} and ${SetSeverity.MAX}, got $severity"
        }
        val record = report.uuid()
        database.transaction {
            require(reports.exists(record)) { "unknown report $report" }
            write(record, Event.Operation.SET_FIELD, Field.REPORT_SEVERITY, severity.toString())
            reports.raiseSeverity(record, severity)
        }
    }

    /** null if this tablet doesn't know a severity for the report */
    fun severityOf(report: ReportId): Int? = reports.findSeverity(report.uuid())

    /** the events behind a report, what will be sent to the server and other tablets */
    fun eventsOf(report: ReportId): List<Event> = events.forRecord(report.uuid())

    private fun write(record: UUID, operation: Event.Operation, field: Field?, value: String) {
        val state = checkNotNull(replica.find()) { "tablet is not enrolled" }
        val event = Event(
            UUID.randomUUID(),
            record,
            Event.RecordType.REPORT,
            operation,
            field?.key(),
            value,
            state.node.value(), // TODO the person using the tablet once there are logins
            clock.instant(),
            state.nextDot,
            state.causalContext,
            null,
        )
        events.insert(event)
        replica.advancePast(event.dot())
    }

    private fun ReportId.uuid(): UUID = UUID.fromString(value())

}
