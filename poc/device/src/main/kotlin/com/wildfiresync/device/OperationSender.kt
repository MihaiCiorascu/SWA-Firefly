package com.wildfiresync.device

import com.wildfiresync.core.EventMessage
import com.wildfiresync.core.RecordRef
import com.wildfiresync.core.Subjects
import com.wildfiresync.core.schema.Event
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

    fun send(event: Event): PublishAck = jetStream.publish(toMessage(event, objectMapper))

}

internal fun toMessage(event: Event, objectMapper: ObjectMapper): Message = NatsMessage.builder()
    .subject(Subjects.forRecord(RecordRef(event.recordType(), event.recordId())))
    .headers(Headers().add(MSG_ID_HDR, event.dot().toString()))
    .data(objectMapper.writeValueAsBytes(EventMessage.of(event)))
    .build()
