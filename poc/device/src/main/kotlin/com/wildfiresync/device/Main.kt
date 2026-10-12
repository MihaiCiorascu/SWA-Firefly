package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.core.schema.Field
import com.wildfiresync.core.schema.FieldValues
import io.nats.client.Nats
import java.time.Instant
import java.util.UUID

fun main(args: Array<String>) {
    val url = System.getenv("NATS_URL") ?: "nats://localhost:4222"
    val node = NodeId(args.getOrElse(0) { "tablet-a" })
    val report = UUID.fromString(args.getOrElse(1) { "00000000-0000-0000-0000-000000000001" })
    val severity = FieldValues.parse(Field.REPORT_SEVERITY, args.getOrElse(2) { "5" })
    val seq = args.getOrElse(3) { "1" }.toLong()

    Nats.connect(url).use { connection ->
        val sender = OperationSender(connection)
        val event = Event(
            UUID.randomUUID(),
            report,
            Event.RecordType.REPORT,
            Event.Operation.SET_FIELD,
            Field.REPORT_SEVERITY.key(),
            FieldValues.format(severity),
            "crew-${node.value()}",
            Instant.now(),
            OpId(node, seq),
            CausalContext.EMPTY,
            emptySet(),
        )
        val ack = sender.send(event)
        println("sent $event to stream ${ack.stream} seq ${ack.seqno} duplicate=${ack.isDuplicate}")
    }
}
