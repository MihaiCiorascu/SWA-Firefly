package com.wildfiresync.device

import com.wildfiresync.core.ReportId
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReportRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReportServiceTest {

    private val report = ReportId("report-1")

    private val database = LocalDatabase.inMemory().apply {
        // TODO once the database-schema MR is merged run its SQL file here instead of this hand-made reports table
        connection.createStatement().use {
            it.execute(
                """
                CREATE TABLE reports (
                    report_id TEXT    NOT NULL PRIMARY KEY,
                    severity  INTEGER CHECK (severity BETWEEN 1 AND 10)
                ) STRICT
                """,
            )
        }
    }

    private val reports = ReportService(ReportRepository(database))

    @AfterTest
    fun close() {
        database.close()
    }

    @Test
    fun `an unknown report has no severity`() {
        assertNull(reports.severityOf(report))
    }

    @Test
    fun `a saved severity can be read back`() {
        reports.setSeverity(report, 4)

        assertEquals(4, reports.severityOf(report))
    }

    @Test
    fun `saving again replaces the severity`() {
        reports.setSeverity(report, 4)
        reports.setSeverity(report, 7)

        assertEquals(7, reports.severityOf(report))
    }

    @Test
    fun `an invalid severity is rejected and not saved`() {
        assertFailsWith<IllegalArgumentException> { reports.setSeverity(report, 99) }

        assertNull(reports.severityOf(report))
    }

}
