package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.ReportId
import com.wildfiresync.core.schema.Event
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReportServiceTest {

    private val node = NodeId("tablet-a")

    private val incident = UUID.fromString("2f1c6a43-0d5e-4c4b-9a3e-6c1f0b8e7d21")

    private val database = LocalDatabase.inMemory().apply { ReplicaStateRepository(this).enrol(node) }

    private val reports = ReportService(database, Clock.fixed(Instant.parse("2026-10-09T10:15:00Z"), ZoneOffset.UTC))

    @AfterTest
    fun close() {
        database.close()
    }

    @Test
    fun `a new report has no severity`() {
        assertNull(reports.severityOf(reports.createReport(incident)))
    }

    @Test
    fun `a saved severity can be read back`() {
        val report = reports.createReport(incident)

        reports.setSeverity(report, 4)

        assertEquals(4, reports.severityOf(report))
    }

    @Test
    fun `severity only goes up`() {
        val report = reports.createReport(incident)

        reports.setSeverity(report, 7)
        reports.setSeverity(report, 4)

        assertEquals(7, reports.severityOf(report))
    }

    @Test
    fun `an invalid severity is rejected and not saved`() {
        val report = reports.createReport(incident)

        assertFailsWith<IllegalArgumentException> { reports.setSeverity(report, 99) }

        assertNull(reports.severityOf(report))
        assertEquals(1, reports.eventsOf(report).size)
    }

    @Test
    fun `an unknown report is rejected`() {
        assertFailsWith<IllegalArgumentException> { reports.setSeverity(ReportId(UUID.randomUUID().toString()), 4) }
    }

    @Test
    fun `every change is stored as an event with the next dot of this tablet`() {
        val report = reports.createReport(incident)
        reports.setSeverity(report, 4)

        val (create, set) = reports.eventsOf(report)

        assertEquals(Event.Operation.CREATE, create.operation())
        assertNull(create.field())
        assertEquals(OpId(node, 1), create.dot())
        assertEquals(emptyMap(), create.causalContext().counters())

        assertEquals(Event.Operation.SET_FIELD, set.operation())
        assertEquals("severity", set.field())
        assertEquals("4", set.value())
        assertEquals(OpId(node, 2), set.dot())
        assertEquals(mapOf(node to 1L), set.causalContext().counters())
        assertEquals(Instant.parse("2026-10-09T10:15:00Z"), set.createdAt())
    }

    @Test
    fun `dots keep counting across reports`() {
        reports.createReport(incident)
        val second = reports.createReport(incident)

        assertEquals(OpId(node, 2), reports.eventsOf(second).single().dot())
    }

    @Test
    fun `lists the reports with their severity`() {
        val first = reports.createReport(incident)
        val second = reports.createReport(incident)
        reports.setSeverity(second, 3)

        val listed = reports.reports().associate { it.report.toString() to it.severity }

        assertEquals(mapOf(first.value() to null, second.value() to 3), listed)
    }

}
