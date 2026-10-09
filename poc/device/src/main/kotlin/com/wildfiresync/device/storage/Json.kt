package com.wildfiresync.device.storage

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.schema.CausalContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// TODO agree on these encodings with the server, the schema only says the columns are TEXT

internal object Json {

    fun write(context: CausalContext): String {
        val json = JSONObject()
        context.counters().entries.sortedBy { it.key }.forEach { (node, counter) -> json.put(node.value(), counter) }
        return json.toString()
    }

    fun write(ids: Set<UUID>): String = JSONArray(ids.map { it.toString() }.sorted()).toString()

    fun readCausalContext(json: String): CausalContext {
        val counters = JSONObject(json)
        return CausalContext(counters.keys().asSequence().associate { NodeId(it) to counters.getLong(it) })
    }

    fun readUuids(json: String): Set<UUID> {
        val ids = JSONArray(json)
        return (0 until ids.length()).map { UUID.fromString(ids.getString(it)) }.toSet()
    }

}
