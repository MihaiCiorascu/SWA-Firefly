package com.wildfiresync.device

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.schema.CausalContext
import com.wildfiresync.core.schema.Event
import com.wildfiresync.device.experiment.RunProtocol
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** the messages of a p2p sync, as byte payloads. the type names stay clear of the experiment's */
object SyncProtocol {

    sealed interface Message {
        /** the sender's causal context, the answer is every event it does not cover */
        data class Hello(val context: CausalContext) : Message

        /** undecodable counts the entries that were not events at all */
        data class Events(val events: List<Event>, val undecodable: Int) : Message
    }

    private const val HELLO = "sync-hello"
    private const val EVENTS = "sync-events"

    fun encodeHello(context: CausalContext): ByteArray {
        val counters = JSONObject()
        context.counters().forEach { (node, counter) -> counters.put(node.value(), counter) }
        return JSONObject().put("type", HELLO).put("context", counters).toString().toByteArray(Charsets.UTF_8)
    }

    /** as few payloads as fit the nearby limit, in the order given */
    fun encodeEvents(events: List<Event>): List<ByteArray> {
        val payloads = mutableListOf<ByteArray>()
        var batch = mutableListOf<String>()
        var size = 0
        for (event in events) {
            val op = OperationWire.toJson(event).toString()
            val opSize = op.toByteArray(Charsets.UTF_8).size + 1
            if (batch.isNotEmpty() && EMPTY_EVENTS_SIZE + size + opSize > RunProtocol.MAX_PAYLOAD_BYTES) {
                payloads += wrap(batch)
                batch = mutableListOf()
                size = 0
            }
            batch += op
            size += opSize
        }
        if (batch.isNotEmpty()) payloads += wrap(batch)
        return payloads
    }

    /** null when the bytes are not a sync message, so another handler can take them */
    fun decode(bytes: ByteArray): Message? {
        val json = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: JSONException) {
            return null
        }
        return try {
            when (json.optString("type")) {
                HELLO -> {
                    val counters = json.getJSONObject("context")
                    Message.Hello(CausalContext(counters.keys().asSequence().associate { NodeId(it) to counters.getLong(it) }))
                }
                EVENTS -> {
                    val ops = json.getJSONArray("ops")
                    val events = (0 until ops.length()).mapNotNull { runCatching { OperationWire.fromJson(ops.getJSONObject(it)) }.getOrNull() }
                    Message.Events(events, ops.length() - events.size)
                }
                else -> null
            }
        } catch (e: RuntimeException) {
            // a sync message we cannot read, still ours so nobody else tries
            Message.Events(emptyList(), 1)
        }
    }

    private val EMPTY_EVENTS_SIZE = wrapped("").size

    private fun wrap(ops: List<String>): ByteArray = wrapped(ops.joinToString(",")).also {
        require(it.size <= RunProtocol.MAX_PAYLOAD_BYTES) { "one event takes ${it.size} bytes, a payload holds at most ${RunProtocol.MAX_PAYLOAD_BYTES}" }
    }

    // built by hand around the already encoded ops, so the size worked out above is the real size
    private fun wrapped(ops: String): ByteArray = """{"type":"$EVENTS","ops":[$ops]}""".toByteArray(Charsets.UTF_8)

}
