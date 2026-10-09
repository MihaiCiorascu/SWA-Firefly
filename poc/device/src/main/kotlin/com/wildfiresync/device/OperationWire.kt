package com.wildfiresync.device

import com.wildfiresync.core.OperationEvent
import com.wildfiresync.core.SetSeverity
import java.time.Instant
import org.json.JSONObject

// Same field names and ISO-8601 timestamp as the JSON the device sends to the server
// (core's OperationEvent), so a payload read here matches what the server consumes.
object OperationWire {

    fun encode(operation: SetSeverity): ByteArray {
        val event = OperationEvent.of(operation)
        return JSONObject()
            .put("nodeId", event.nodeId())
            .put("seq", event.seq())
            .put("reportId", event.reportId())
            .put("severity", event.severity())
            .put("recordedAt", event.recordedAt().toString())
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): SetSeverity {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        return OperationEvent(
            json.getString("nodeId"),
            json.getLong("seq"),
            json.getString("reportId"),
            json.getInt("severity"),
            Instant.parse(json.getString("recordedAt")),
        ).toOperation()
    }
}
