package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.ReportId
import com.wildfiresync.core.SetSeverity
import io.nats.client.Nats
import java.time.Instant

fun main(args: Array<String>) {
    val url = System.getenv("NATS_URL") ?: "nats://localhost:4222"
    val node = NodeId(args.getOrElse(0) { "tablet-a" })
    val report = ReportId(args.getOrElse(1) { "report-1" })
    val severity = args.getOrElse(2) { "5" }.toInt()
    val seq = args.getOrElse(3) { "1" }.toLong()

    Nats.connect(url).use { connection ->
        val sender = OperationSender(connection)
        val operation = SetSeverity(OpId(node, seq), report, severity, Instant.now())
        val ack = sender.send(operation)
        println("sent $operation to stream ${ack.stream} seq ${ack.seqno} duplicate=${ack.isDuplicate}")
    }
}
