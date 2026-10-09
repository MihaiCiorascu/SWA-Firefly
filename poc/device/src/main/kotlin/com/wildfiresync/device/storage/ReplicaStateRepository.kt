package com.wildfiresync.device.storage

import com.wildfiresync.core.NodeId
import com.wildfiresync.core.OpId
import com.wildfiresync.core.schema.CausalContext

/**
 * SQL for replica_state, the single row with this tablet's node id and DVV state
 */
class ReplicaStateRepository(private val database: LocalDatabase) {

    /** writes the row the first time, a tablet that is already enrolled keeps its node id */
    fun enrol(node: NodeId) {
        database.connection.prepareStatement(
            """
            INSERT INTO replica_state (id, node_id, next_sequence, causal_context) VALUES (1, ?, 1, ?)
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, node.value())
            statement.setString(2, Json.write(CausalContext(emptyMap())))
            statement.executeUpdate()
        }
    }

    fun find(): ReplicaState? =
        database.connection.prepareStatement("SELECT node_id, next_sequence, causal_context FROM replica_state")
            .use { statement ->
                statement.executeQuery().use { result ->
                    if (!result.next()) return null
                    ReplicaState(
                        NodeId(result.getString(1)),
                        result.getLong(2),
                        Json.readCausalContext(result.getString(3)),
                    )
                }
            }

    fun advancePast(dot: OpId) {
        val state = checkNotNull(find()) { "tablet is not enrolled" }
        check(dot.node() == state.node && dot.seq() == state.nextSequence) { "$dot is not this tablet's next dot" }

        database.connection.prepareStatement(
            "UPDATE replica_state SET next_sequence = ?, causal_context = ?",
        ).use { statement ->
            statement.setLong(1, dot.seq() + 1)
            statement.setString(2, Json.write(CausalContext(state.causalContext.counters() + (dot.node() to dot.seq()))))
            statement.executeUpdate()
        }
    }

}

data class ReplicaState(val node: NodeId, val nextSequence: Long, val causalContext: CausalContext) {

    val nextDot: OpId get() = OpId(node, nextSequence)

}
