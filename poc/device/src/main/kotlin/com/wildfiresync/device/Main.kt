package com.wildfiresync.device

import com.wildfiresync.core.ReportId
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReportRepository
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * EXAMPLE: a command-line stand-in for the tablet app, to try out the local storage.
 *
 *     device <report-id>              prints the severity this tablet knows for the report
 *     device <report-id> <severity>   saves a new severity for the report
 *
 * TODO: the database file has no tables until the schema from the database-schema MR is applied to it
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
