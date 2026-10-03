package com.wildfiresync.device

import com.wildfiresync.core.OperationEvent
import com.wildfiresync.core.SetSeverity
import com.wildfiresync.core.Subjects
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.api.PublishAck
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import io.nats.client.support.NatsJetStreamConstants.MSG_ID_HDR
import tools.jackson.databind.ObjectMapper

class OperationSender(
    connection: Connection,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {

    private val jetStream = connection.jetStream()

    fun send(operation: SetSeverity): PublishAck = jetStream.publish(toMessage(operation, objectMapper))

}

internal fun toMessage(operation: SetSeverity, objectMapper: ObjectMapper): Message = NatsMessage.builder()
    .subject(Subjects.forReport(operation.report()))
    .headers(Headers().add(MSG_ID_HDR, operation.id().toString()))
    .data(objectMapper.writeValueAsBytes(OperationEvent.of(operation)))
    .build()
