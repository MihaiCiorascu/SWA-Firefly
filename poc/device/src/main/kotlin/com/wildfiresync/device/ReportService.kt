package com.wildfiresync.device

import com.wildfiresync.core.schema.CreateValues
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues
import com.wildfiresync.device.storage.EventRepository
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import com.wildfiresync.device.storage.ReportRepository
import com.wildfiresync.device.storage.StoredReport
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

    fun createReport(incident: UUID): UUID = database.transaction {
        val report = UUID.randomUUID()
        // TODO agree with the server on what a CREATE value holds, for now only the immutable incidentId
        val values = CreateValues.format(
            Event.RecordType.REPORT,
            mapOf<Field, FieldValue>(Field.REPORT_INCIDENT_ID to FieldValue.Reference(incident)),
        )
        write(report, Event.Operation.CREATE, null, values)
        reports.insert(report, incident)
        report
    }

    fun setSeverity(report: UUID, severity: Int) {
        FieldValues.parse(Field.REPORT_SEVERITY, severity.toString())
        database.transaction {
            require(reports.exists(report)) { "unknown report $report" }
            write(report, Event.Operation.SET_FIELD, Field.REPORT_SEVERITY, severity.toString())
            reports.raiseSeverity(report, severity)
        }
    }

    /** null if this tablet doesn't know a severity for the report */
    fun severityOf(report: UUID): Int? = reports.findSeverity(report)

    /** every report this tablet knows about */
    fun reports(): List<StoredReport> = reports.findAll()

    /** the events behind a report, what will be sent to the server and other tablets */
    fun eventsOf(report: UUID): List<Event> = events.forRecord(report)

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

}
