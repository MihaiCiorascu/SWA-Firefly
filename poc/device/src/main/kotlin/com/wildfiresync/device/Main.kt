package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.ReportId
import com.wildfiresync.device.storage.LocalDatabase
import com.wildfiresync.device.storage.ReplicaStateRepository
import java.nio.file.Path
import java.util.UUID
import kotlin.system.exitProcess

/**
 * example CLI standing in for the tablet app
 *
 * device new <incident-id>       create a report in the incident and print its id
 * device <report-id>             print the severity of the report
 * device <report-id> <severity>  save a new severity
 */
fun main(args: Array<String>) {
    if (args.size !in 1..2 || args.contentEquals(arrayOf("new"))) {
        System.err.println("usage: device new <incident-id> | device <report-id> [<severity>]")
        exitProcess(2)
    }

    val file = Path.of(System.getenv("WILDFIRESYNC_DB") ?: "device.db")
    val node = NodeId(System.getenv("WILDFIRESYNC_NODE") ?: "tablet-a")

    LocalDatabase.open(file).use { database ->
        // TODO real enrolment, for now the first run of the CLI enrols the tablet
        ReplicaStateRepository(database).enrol(node)
        val reports = ReportService(database)

        if (args[0] == "new") {
            println(reports.createReport(UUID.fromString(args[1])))
            return
        }

        val report = ReportId(args[0])
        if (args.size == 2) {
            reports.setSeverity(report, args[1].toInt())
        }

        println("$report: severity ${reports.severityOf(report) ?: "unknown"}")
    }
}
