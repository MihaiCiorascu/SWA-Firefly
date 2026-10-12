package com.wildfiresync.device.experiment

import com.wildfiresync.core.SetSeverity
import com.wildfiresync.device.OperationWire
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The messages of the P2P transfer experiment. Both kinds travel as byte payloads. */
object RunProtocol {

    // Nearby Connections limits a byte payload to 32 KB.
    const val MAX_PAYLOAD_BYTES = 32 * 1024

    sealed interface Message {
        data class Start(val run: String, val total: Int, val batch: Int) : Message
        data class Events(val run: String, val operations: List<SetSeverity>) : Message
    }

    fun encodeStart(run: String, total: Int, batch: Int): ByteArray =
        JSONObject().put("type", "start").put("run", run).put("total", total).put("batch", batch)
            .toString().toByteArray(Charsets.UTF_8)

    fun encodeEvents(run: String, operations: List<SetSeverity>): ByteArray {
        val array = JSONArray()
        operations.forEach { array.put(JSONObject(String(OperationWire.encode(it), Charsets.UTF_8))) }
        val bytes = JSONObject().put("type", "events").put("run", run).put("ops", array)
            .toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_PAYLOAD_BYTES) {
            "${operations.size} events take ${bytes.size} bytes, a payload holds at most $MAX_PAYLOAD_BYTES"
        }
        return bytes
    }

    /** null when the bytes are not an experiment message, so another handler can take them. */
    fun decode(bytes: ByteArray): Message? {
        val json = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: JSONException) {
            return null
        }
        return when (json.optString("type")) {
            "start" -> Message.Start(json.getString("run"), json.getInt("total"), json.getInt("batch"))
            "events" -> {
                val operations = json.getJSONArray("ops")
                Message.Events(
                    json.getString("run"),
                    (0 until operations.length()).map {
                        OperationWire.decode(operations.getJSONObject(it).toString().toByteArray(Charsets.UTF_8))
                    },
                )
            }
            else -> null
        }
    }
}
