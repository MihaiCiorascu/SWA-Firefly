package com.wildfiresync.device

import com.wildfiresync.core.EventMessage
import com.wildfiresync.core.MergeCore
import com.wildfiresync.core.schema.Event
import java.time.Instant
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

// Same field names and ISO-8601 timestamp as the JSON the device sends to the server
// (core's EventMessage), so a payload read here matches what the server consumes.
object OperationWire {

    fun encode(event: Event): ByteArray {
        val message = EventMessage.of(event)
        return JSONObject()
            .put("eventId", message.eventId().toString())
            .put("recordId", message.recordId().toString())
            .put("recordType", message.recordType().name)
            .put("operation", message.operation().name)
            .putOpt("field", message.field())
            .putOpt("value", message.value())
            .put("actorId", message.actorId())
            .put("createdAt", message.createdAt().toString())
            .put("nodeId", message.nodeId())
            .put("seq", message.seq())
            .put("context", JSONObject(message.context()))
            .put("supersedes", JSONArray(message.supersedes().map { it.toString() }.sorted()))
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): Event {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        val context = json.getJSONObject("context")
        val supersedes = json.getJSONArray("supersedes")
        val event = EventMessage(
            UUID.fromString(json.getString("eventId")),
            UUID.fromString(json.getString("recordId")),
            Event.RecordType.valueOf(json.getString("recordType")),
            Event.Operation.valueOf(json.getString("operation")),
            json.nullableString("field"),
            json.nullableString("value"),
            json.getString("actorId"),
            Instant.parse(json.getString("createdAt")),
            json.getString("nodeId"),
            json.getLong("seq"),
            context.keys().asSequence().associateWith { context.getLong(it) },
            (0 until supersedes.length()).map { UUID.fromString(supersedes.getString(it)) }.toSet(),
        ).toEvent()
        MergeCore.validate(event)
        return event
    }

    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)

}
