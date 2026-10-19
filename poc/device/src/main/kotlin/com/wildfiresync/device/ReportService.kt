package com.wildfiresync.device

import com.wildfiresync.core.Candidate
import com.wildfiresync.core.MergeCore
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.Resolution
import com.wildfiresync.core.World
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.CreateValues
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues
import com.wildfiresync.device.storage.ConflictRepository
import com.wildfiresync.device.storage.EventRepository
import com.wildfiresync.device.storage.IncidentRepository
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import com.wildfiresync.device.storage.ReportRepository
import com.wildfiresync.device.storage.StoredIncident
import com.wildfiresync.device.storage.StoredReport
import java.time.Clock
import java.util.UUID

// a description this long still fits one nearby byte payload (32 KB) with room to spare
const val MAX_TEXT_LENGTH = 2000

/**
 * API for the rest of the tablet so nothing else touches SQL. every change is saved as an event first, and the
 * incidents, reports and field_conflicts tables are rebuilt from the record's events with the merge core in the same
 * transaction, so it survives being offline. events from the server or another tablet go through receive and end up
 * the same way
 */
class ReportService(private val database: LocalDatabase, private val clock: Clock = Clock.systemUTC()) {

    private val replica = ReplicaStateRepository(database)
    private val events = EventRepository(database)
    private val reports = ReportRepository(database)
    private val incidents = IncidentRepository(database)
    private val conflicts = ConflictRepository(database)

    data class Receipt(val stored: List<Event>, val duplicates: Int, val rejected: Int)

    /** returns the CREATE, its record id is the new incident */
    fun createIncident(name: String): Event = create(
        RecordRef.incident(UUID.randomUUID()),
        mapOf(
            Field.INCIDENT_NAME to FieldValue.Text(name.trim()),
            Field.INCIDENT_STATUS to FieldValue.Status(FieldValues.IncidentStatus.ACTIVE),
        ),
    )

    /** returns the CREATE, its record id is the new report */
    fun createReport(incident: UUID, values: Map<Field, FieldValue> = emptyMap()): Event =
        create(RecordRef.report(UUID.randomUUID()), values + (Field.REPORT_INCIDENT_ID to FieldValue.Reference(incident)))

    fun setField(record: RecordRef, field: Field, value: FieldValue): Event = database.transaction {
        require(field.recordType() == record.type()) { "$field is not a field of ${record.type()}" }
        require(knows(record)) { "unknown $record" }
        write(record, Event.Operation.SET_FIELD, field.key(), FieldValues.format(checked(value)))
    }

    /**
     * settles the field with the value the crew leaders agreed on. the causal context does the settling, it covers
     * every candidate this tablet holds without a gap. supersedes only names the candidates that were shown
     */
    fun resolveConflict(record: RecordRef, field: Field, value: FieldValue): Event = database.transaction {
        val candidates = conflicts.findAll()[record.id()]?.get(field)
        require(!candidates.isNullOrEmpty()) { "$field of $record is not in conflict" }
        write(
            record,
            Event.Operation.RESOLVE_CONFLICT,
            field.key(),
            FieldValues.format(checked(value)),
            candidates.map { it.eventId() }.toSet(),
        )
    }

    /** stores the events it does not hold yet. an event the merge core refuses is counted and dropped */
    fun receive(incoming: List<Event>): Receipt = database.transaction {
        val stored = mutableListOf<Event>()
        var duplicates = 0
        var rejected = 0
        for (event in incoming) {
            val valid = try {
                MergeCore.validate(event)
                true
            } catch (e: RuntimeException) {
                false
            }
            when {
                !valid -> rejected++
                events.insertIfAbsent(event) -> stored += event
                else -> duplicates++
            }
        }
        stored.map { it.recordId() }.distinct().forEach(::project)
        if (stored.isNotEmpty()) {
            replica.observe(events.contiguous(context(), stored.map { it.dot().node() }.toSet()))
        }
        Receipt(stored, duplicates, rejected)
    }

    /** what a peer with this causal context is missing */
    fun missingFor(peer: CausalContext): List<Event> = events.notCoveredBy(peer)

    /** what this tablet has seen, as node -> highest sequence it holds without a gap */
    fun context(): CausalContext = checkNotNull(replica.find()) { "tablet is not enrolled" }.causalContext

    fun allEvents(): List<Event> = events.all()

    fun incidents(): List<StoredIncident> = incidents.findAll()

    fun reports(): List<StoredReport> = reports.findAll()

    /** record id -> field -> the candidates a crew leader has to choose between */
    fun conflicts(): Map<UUID, Map<Field, List<Candidate>>> = conflicts.findAll()

    /** the events behind a record, what will be sent to the server and other tablets */
    fun eventsOf(record: UUID): List<Event> = events.forRecord(record)

    private fun create(record: RecordRef, values: Map<Field, FieldValue>): Event = database.transaction {
        values.values.forEach(::checked)
        write(record, Event.Operation.CREATE, null, CreateValues.format(record.type(), values))
    }

    private fun knows(record: RecordRef): Boolean = when (record.type()) {
        Event.RecordType.REPORT -> reports.exists(record.id())
        Event.RecordType.INCIDENT -> incidents.exists(record.id())
    }

    private fun checked(value: FieldValue): FieldValue {
        if (value is FieldValue.Text) {
            require(value.value().length <= MAX_TEXT_LENGTH) { "text is longer than $MAX_TEXT_LENGTH characters" }
        }
        return value
    }

    private fun write(
        record: RecordRef,
        operation: Event.Operation,
        field: String?,
        value: String,
        supersedes: Set<UUID> = emptySet(),
    ): Event {
        val state = checkNotNull(replica.find()) { "tablet is not enrolled" }
        val event = Event(
            UUID.randomUUID(),
            record.id(),
            record.type(),
            operation,
            field,
            value,
            state.node.value(), // TODO the person using the tablet once there are logins
            clock.instant(),
            state.nextDot,
            state.causalContext,
            supersedes,
        )
        MergeCore.validate(event)
        events.insert(event)
        replica.advancePast(event.dot())
        project(record.id())
        return event
    }

    /** rebuilds the projection rows of a record from all of its events */
    private fun project(record: UUID) {
        val world = MergeCore.applyAll(World.EMPTY, events.forRecord(record))
        world.records().forEach { (ref, state) ->
            val resolved = state.fields().mapValues { (field, merged) -> MergeCore.resolve(field, merged) }
            val settled = resolved.mapNotNull { (field, it) -> (it as? Resolution.Settled)?.let { field to it.value() } }.toMap()
            when (ref.type()) {
                Event.RecordType.REPORT -> reports.upsert(ref.id(), settled)
                Event.RecordType.INCIDENT -> incidents.upsert(ref.id(), settled)
            }
            conflicts.replace(
                ref,
                resolved.mapNotNull { (field, it) -> (it as? Resolution.Conflicted)?.let { field to it.candidates() } }.toMap(),
            )
        }
    }

}
