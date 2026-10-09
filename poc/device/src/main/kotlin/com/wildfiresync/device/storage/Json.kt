package com.wildfiresync.device.storage

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.schema.CausalContext
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.UUID

// TODO agree on these encodings with the server, the schema only says the columns are TEXT

internal object Json {

    private val mapper = ObjectMapper()

    fun write(value: Any): String = mapper.writeValueAsString(value)

    fun write(context: CausalContext): String =
        write(context.counters().entries.associate { (node, counter) -> node.value() to counter }.toSortedMap())

    fun readCausalContext(json: String): CausalContext =
        CausalContext(mapper.readValue(json, object : TypeReference<Map<String, Long>>() {}).mapKeys { NodeId(it.key) })

    fun readUuids(json: String): Set<UUID> =
        mapper.readValue(json, object : TypeReference<List<String>>() {}).map(UUID::fromString).toSet()

}
