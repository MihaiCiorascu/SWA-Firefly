package com.wildfiresync.device

import com.wildfiresync.core.ReportId
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReportRepository
import java.nio.file.Path
import kotlin.system.exitProcess

// TODO the db has no tables until the SQL file from the database-schema MR has been run on it

/**
 * example CLI standing in for the tablet app
 *
 * device <report-id>             print the severity of the report
 * device <report-id> <severity>  save a new severity
 */
fun main(args: Array<String>) {
    if (args.size !in 1..2) {
        System.err.println("usage: device <report-id> [<severity>]")
        exitProcess(2)
    }

    val file = Path.of(System.getenv("WILDFIRESYNC_DB") ?: "device.db")

    LocalDatabase.open(file).use { database ->
        val reports = ReportService(ReportRepository(database))
        val report = ReportId(args[0])

        if (args.size == 2) {
            reports.setSeverity(report, args[1].toInt())
        }

        println("$report: severity ${reports.severityOf(report) ?: "unknown"}")
    }
}
