package com.wildfiresync.device

import com.wildfiresync.core.schema.Event
import org.json.JSONObject

/**
 * reads what the server publishes for a zone: one report and every event of it (core's EventBundle).
 *
 * the events inside are the same JSON this tablet sends, so each one is read with the decoder the
 * operation stream already uses
 */
object ReportWire {

    /** throws when the bytes are not a bundle. an empty list means the report is no longer in this zone */
    fun decode(bytes: ByteArray): List<Event> {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        val events = json.getJSONArray("events")
        return (0 until events.length()).map { OperationWire.fromJson(events.getJSONObject(it)) }
    }

}
