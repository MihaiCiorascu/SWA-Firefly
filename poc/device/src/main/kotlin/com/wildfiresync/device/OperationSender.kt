package com.wildfiresync.device

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
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
import java.time.Instant

class OperationSender(
    connection: Connection,
    private val gson: Gson = operationGson,
) {

    private val jetStream = connection.jetStream()

    fun send(event: Event): PublishAck = jetStream.publish(toMessage(event, gson))

}

internal fun toMessage(event: Event, gson: Gson): Message = NatsMessage.builder()
    .subject(Subjects.forRecord(RecordRef(event.recordType(), event.recordId())))
    .headers(Headers().add(MSG_ID_HDR, event.dot().toString()))
    .data(gson.toJson(EventMessage.of(event)).toByteArray(Charsets.UTF_8))
    .build()

internal val operationGson: Gson = GsonBuilder()
    .registerTypeAdapter(
        Instant::class.java,
        object : TypeAdapter<Instant>() {
            override fun write(out: JsonWriter, value: Instant) {
                out.value(value.toString())
            }

            override fun read(reader: JsonReader): Instant = Instant.parse(reader.nextString())
        }.nullSafe(),
    )
    .create()
