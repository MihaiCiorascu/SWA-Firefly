package com.wildfiresync.device

import com.wildfiresync.core.Subjects
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.MessageConsumer
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.api.DeliverPolicy

class ReportConsumer(
    private val connection: Connection,
    private val hexagons: Set<String>,
    private val onReport: (Message) -> Unit,
) {

    private var consumer: MessageConsumer? = null

    fun start() {
        val stream = connection.getStreamContext(REPORTS_STREAM)
        val config = ConsumerConfiguration.builder()
            .filterSubjects(filterSubjects(hexagons))
            .deliverPolicy(DeliverPolicy.LastPerSubject)
            .build()
        consumer = stream.createOrUpdateConsumer(config).consume { message ->
            onReport(message)
            message.ack()
        }
    }

    fun stop() {
        consumer?.stop()
        consumer = null
    }

    companion object {
        const val REPORTS_STREAM = "reports"
    }

}

internal fun filterSubjects(hexagons: Set<String>): List<String> =
    hexagons.map { Subjects.reportsIn(it) }
