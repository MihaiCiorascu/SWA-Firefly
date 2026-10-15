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
        database.db.execSQL(
            """
            INSERT INTO replica_state (id, node_id, next_sequence, causal_context) VALUES (1, ?, 1, ?)
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            arrayOf(node.value(), Json.write(CausalContext(emptyMap()))),
        )
    }

    fun find(): ReplicaState? =
        database.db.rawQuery("SELECT node_id, next_sequence, causal_context FROM replica_state", null).use { result ->
            if (!result.moveToFirst()) return null
            ReplicaState(
                NodeId(result.getString(0)),
                result.getLong(1),
                Json.readCausalContext(result.getString(2)),
            )
        }

    fun advancePast(dot: OpId) {
        val state = checkNotNull(find()) { "tablet is not enrolled" }
        check(dot.node() == state.node && dot.seq() == state.nextSequence) { "$dot is not this tablet's next dot" }

        database.db.execSQL(
            "UPDATE replica_state SET next_sequence = ?, causal_context = ?",
            arrayOf<Any>(dot.seq() + 1, Json.write(CausalContext(state.causalContext.counters() + (dot.node() to dot.seq())))),
        )
    }

    /**
     * takes in what this tablet holds after a sync. if a peer gave back dots of this tablet it had lost, the next
     * sequence moves past them so a new event never reuses one
     */
    fun observe(held: CausalContext) {
        val state = checkNotNull(find()) { "tablet is not enrolled" }
        val counters = state.causalContext.counters().toMutableMap()
        held.counters().forEach { (node, counter) -> counters.merge(node, counter, ::maxOf) }
        val next = maxOf(state.nextSequence, (counters[state.node] ?: 0L) + 1)

        database.db.execSQL(
            "UPDATE replica_state SET next_sequence = ?, causal_context = ?",
            arrayOf<Any>(next, Json.write(CausalContext(counters))),
        )
    }

}

data class ReplicaState(val node: NodeId, val nextSequence: Long, val causalContext: CausalContext) {

    val nextDot: OpId get() = OpId(node, nextSequence)

}
