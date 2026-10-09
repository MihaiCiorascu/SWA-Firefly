package com.wildfiresync.device.storage

import com.wildfiresync.core.NodeId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalDatabaseTest {

    private val context = RuntimeEnvironment.getApplication()

    @AfterTest
    fun delete() {
        context.deleteDatabase("test.db")
    }

    @Test
    fun `creates the schema tables`() {
        LocalDatabase.inMemory(context).use { database ->
            val tables = database.db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT IN ('android_metadata', 'sqlite_sequence')",
                null,
            ).use { result ->
                buildSet { while (result.moveToNext()) add(result.getString(0)) }
            }
            assertEquals(setOf("events", "replica_state", "incidents", "reports", "field_conflicts"), tables)
        }
    }

    @Test
    fun `reopening a file keeps its data and does not create the tables again`() {
        LocalDatabase.open(context, "test.db").use { ReplicaStateRepository(it).enrol(NodeId("tablet-a")) }

        LocalDatabase.open(context, "test.db").use { database ->
            ReplicaStateRepository(database).enrol(NodeId("tablet-b"))
            assertEquals(NodeId("tablet-a"), ReplicaStateRepository(database).find()?.node)
        }
    }

}
