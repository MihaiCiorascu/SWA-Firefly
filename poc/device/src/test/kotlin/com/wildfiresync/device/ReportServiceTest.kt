package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValue
import com.wildfiresync.core.schema.FieldValues.IncidentStatus
import com.wildfiresync.core.schema.FieldValues.Location
import com.wildfiresync.core.schema.FieldValues.ReportType
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import com.wildfiresync.device.storage.StoredIncident
import com.wildfiresync.device.storage.StoredReport
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReportServiceTest {

    private val node = NodeId("tablet-a")

    private val incident = UUID.fromString("2f1c6a43-0d5e-4c4b-9a3e-6c1f0b8e7d21")

    private val clock = Clock.fixed(Instant.parse("2026-10-09T10:15:00Z"), ZoneOffset.UTC)

    private val databases = mutableListOf<LocalDatabase>()

    private val reports = tablet(node)

    private fun tablet(node: NodeId): ReportService {
        val database = LocalDatabase.inMemory(RuntimeEnvironment.getApplication()).also { databases += it }
        ReplicaStateRepository(database).enrol(node)
        return ReportService(database, clock)
    }

    @AfterTest
    fun close() {
        databases.forEach { it.close() }
    }

    private fun ReportService.report(id: UUID): StoredReport = reports().single { it.report == id }

    private fun ReportService.incident(id: UUID): StoredIncident = incidents().single { it.incident == id }

    private fun ReportService.newReport(values: Map<Field, FieldValue> = emptyMap()): UUID =
        createReport(incident, values).recordId()

    private fun ReportService.set(report: UUID, field: Field, value: FieldValue) =
        setField(RecordRef.report(report), field, value)

    /** both tablets end up holding every event the other one has */
    private fun exchange(a: ReportService, b: ReportService) {
        val forB = a.missingFor(b.context())
        val forA = b.missingFor(a.context())
        b.receive(forB)
        a.receive(forA)
    }

    @Test
    fun `a new report has only its incident`() {
        val report = reports.report(reports.newReport())

        assertEquals(StoredReport(report.report, incident, emptySet(), null, null, null), report)
        assertTrue(report.created)
    }

    @Test
    fun `a create carries the initial values`() {
        val report = reports.newReport(
            mapOf(
                Field.REPORT_SEVERITY to FieldValue.Num(6),
                Field.REPORT_TYPE to FieldValue.Types.of(ReportType.FIRE),
                Field.REPORT_DESCRIPTION to FieldValue.Text("Fire crossed the ridge"),
            ),
        )

        assertEquals(1, reports.eventsOf(report).size)
        assertEquals(StoredReport(report, incident, setOf(ReportType.FIRE), null, 6, "Fire crossed the ridge"), reports.report(report))
    }

    @Test
    fun `every report field can be set and read back`() {
        val report = reports.newReport()

        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(4))
        reports.set(report, Field.REPORT_TYPE, FieldValue.Types.of(ReportType.SMOKE))
        reports.set(report, Field.REPORT_LOCATION, FieldValue.Place(Location(51.99, 4.37)))
        reports.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Smoke over the dunes"))

        assertEquals(
            StoredReport(report, incident, setOf(ReportType.SMOKE), Location(51.99, 4.37), 4, "Smoke over the dunes"),
            reports.report(report),
        )
    }

    @Test
    fun `severity only goes up and report types only grow`() {
        val report = reports.newReport()

        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(7))
        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(4))
        reports.set(report, Field.REPORT_TYPE, FieldValue.Types.of(ReportType.FIRE))
        reports.set(report, Field.REPORT_TYPE, FieldValue.Types.of(ReportType.ROAD_BLOCKED))

        assertEquals(7, reports.report(report).severity)
        assertEquals(setOf(ReportType.FIRE, ReportType.ROAD_BLOCKED), reports.report(report).types)
    }

    @Test
    fun `an incident is created active and can be renamed and closed`() {
        val incident = reports.createIncident("Ridge fire").recordId()

        reports.setField(RecordRef.incident(incident), Field.INCIDENT_NAME, FieldValue.Text("Dune fire"))
        reports.setField(RecordRef.incident(incident), Field.INCIDENT_STATUS, FieldValue.Status(IncidentStatus.CLOSED))

        assertEquals(StoredIncident(incident, "Dune fire", IncidentStatus.CLOSED), reports.incident(incident))
    }

    @Test
    fun `an invalid value is rejected and not saved`() {
        val report = reports.newReport()

        assertFailsWith<IllegalArgumentException> { reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(99)) }
        assertFailsWith<IllegalArgumentException> { reports.set(report, Field.REPORT_INCIDENT_ID, FieldValue.Reference(incident)) }
        assertFailsWith<IllegalArgumentException> { reports.set(report, Field.INCIDENT_NAME, FieldValue.Text("x")) }
        assertFailsWith<IllegalArgumentException> {
            reports.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("x".repeat(MAX_TEXT_LENGTH + 1)))
        }

        assertNull(reports.report(report).severity)
        assertEquals(1, reports.eventsOf(report).size)
        assertEquals(OpId(node, 2), ReplicaStateRepository(databases.first()).find()!!.nextDot)
    }

    @Test
    fun `an unknown record is rejected`() {
        assertFailsWith<IllegalArgumentException> { reports.set(UUID.randomUUID(), Field.REPORT_SEVERITY, FieldValue.Num(4)) }
    }

    @Test
    fun `every change is stored as an event with the next dot and the context of this tablet`() {
        val report = reports.newReport()
        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(4))

        val (create, set) = reports.eventsOf(report)

        assertEquals(Event.Operation.CREATE, create.operation())
        assertNull(create.field())
        assertEquals("""{"incidentId":"$incident"}""", create.value())
        assertEquals(OpId(node, 1), create.dot())
        assertEquals(emptyMap(), create.causalContext().counters())
        assertEquals(emptySet(), create.supersedes())

        assertEquals(Event.Operation.SET_FIELD, set.operation())
        assertEquals("severity", set.field())
        assertEquals("4", set.value())
        assertEquals(OpId(node, 2), set.dot())
        assertEquals(mapOf(node to 1L), set.causalContext().counters())
        assertEquals(Instant.parse("2026-10-09T10:15:00Z"), set.createdAt())
    }

    @Test
    fun `two tablets that sync end with the same reports`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport()
        exchange(reports, bravo)

        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(4))
        bravo.set(report, Field.REPORT_SEVERITY, FieldValue.Num(8))
        bravo.set(report, Field.REPORT_TYPE, FieldValue.Types.of(ReportType.SMOKE))
        exchange(reports, bravo)

        assertEquals(reports.reports(), bravo.reports())
        assertEquals(8, bravo.report(report).severity)
        assertEquals(reports.allEvents().toSet(), bravo.allEvents().toSet())
    }

    @Test
    fun `concurrent descriptions become a conflict on both tablets and a resolution settles it`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport()
        exchange(reports, bravo)
        reports.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Fire crossed the ridge"))
        bravo.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Evacuation route B blocked"))

        exchange(reports, bravo)

        assertNull(reports.report(report).description)
        val candidates = reports.conflicts().getValue(report).getValue(Field.REPORT_DESCRIPTION)
        assertEquals(listOf("tablet-a", "tablet-b"), candidates.map { it.actorId() }.sorted())
        assertEquals(reports.conflicts(), bravo.conflicts())

        val resolution = bravo.resolveConflict(RecordRef.report(report), Field.REPORT_DESCRIPTION, FieldValue.Text("Ridge crossed, route B blocked"))
        exchange(reports, bravo)

        assertEquals(candidates.map { it.eventId() }.toSet(), resolution.supersedes())
        assertEquals("Ridge crossed, route B blocked", reports.report(report).description)
        assertEquals(emptyMap(), reports.conflicts())
        assertEquals(reports.reports(), bravo.reports())
    }

    @Test
    fun `a value from the create that the other writer never saw is a candidate`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport(mapOf(Field.REPORT_DESCRIPTION to FieldValue.Text("Smoke seen")))
        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(4))
        // bravo only gets the severity, so its context cannot cover the create
        bravo.receive(listOf(reports.eventsOf(report).last()))
        bravo.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Fire seen"))

        exchange(reports, bravo)

        val candidates = bravo.conflicts().getValue(report).getValue(Field.REPORT_DESCRIPTION)
        assertEquals(setOf(FieldValue.Text("Smoke seen"), FieldValue.Text("Fire seen")), candidates.map { it.value() }.toSet())
        assertEquals(reports.conflicts(), bravo.conflicts())
    }

    @Test
    fun `editing a field again replaces the value instead of conflicting`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport(mapOf(Field.REPORT_DESCRIPTION to FieldValue.Text("Smoke seen")))
        exchange(reports, bravo)
        bravo.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Fire seen"))
        bravo.set(report, Field.REPORT_DESCRIPTION, FieldValue.Text("Fire seen, spreading"))

        exchange(reports, bravo)

        assertEquals("Fire seen, spreading", reports.report(report).description)
        assertEquals(emptyMap(), reports.conflicts())
    }

    @Test
    fun `a field that is not in conflict cannot be resolved`() {
        val report = reports.newReport()

        assertFailsWith<IllegalArgumentException> {
            reports.resolveConflict(RecordRef.report(report), Field.REPORT_DESCRIPTION, FieldValue.Text("x"))
        }
    }

    @Test
    fun `receiving counts duplicates and drops what the merge core refuses`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport()
        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(5))
        val events = reports.allEvents()
        val invalid = Event(
            UUID.randomUUID(), report, Event.RecordType.REPORT, Event.Operation.SET_FIELD, "severity", "99", "tablet-c",
            Instant.EPOCH, OpId(NodeId("tablet-c"), 1), CausalContext.EMPTY, emptySet(),
        )

        val first = bravo.receive(events + invalid)
        val second = bravo.receive(events)

        assertEquals(ReportService.Receipt(events, 0, 1), first)
        assertEquals(ReportService.Receipt(emptyList(), 2, 0), second)
        assertEquals(5, bravo.report(report).severity)
        assertFalse(bravo.allEvents().any { it.dot().node() == NodeId("tablet-c") })
    }

    @Test
    fun `an edit that arrives before its create still shows up`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport()
        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(5))
        val (create, set) = reports.eventsOf(report)

        bravo.receive(listOf(set))
        assertEquals(StoredReport(report, null, emptySet(), null, 5, null), bravo.report(report))
        assertFalse(bravo.report(report).created)

        bravo.receive(listOf(create))
        assertEquals(reports.report(report), bravo.report(report))
    }

    @Test
    fun `the context only counts what is held without a gap`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport()
        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(5))
        reports.set(report, Field.REPORT_SEVERITY, FieldValue.Num(6))
        val (first, second, third) = reports.eventsOf(report)

        bravo.receive(listOf(first, third))
        assertEquals(mapOf(node to 1L), bravo.context().counters().filterKeys { it == node })
        assertEquals(listOf(third), bravo.missingFor(CausalContext(mapOf(node to 1L))).filter { it.dot().node() == node })

        bravo.receive(listOf(second))
        assertEquals(3L, bravo.context().counters()[node])
        assertEquals(emptyList(), reports.missingFor(bravo.context()))
    }

    @Test
    fun `the next event of a tablet carries what it received in its context`() {
        val bravo = tablet(NodeId("tablet-b"))
        val report = reports.newReport()
        bravo.receive(reports.allEvents())

        val edit = bravo.set(report, Field.REPORT_SEVERITY, FieldValue.Num(3))

        assertEquals(mapOf(node to 1L), edit.causalContext().counters())
        assertEquals(OpId(NodeId("tablet-b"), 1), edit.dot())
    }

}
