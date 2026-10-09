package com.wildfiresync.device.storage

import com.wildfiresync.core.NodeId
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDatabaseTest {

    @Test
    fun `creates the schema tables`() {
        LocalDatabase.inMemory().use { database ->
            val tables = database.connection.createStatement().use { statement ->
                statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { result ->
                    buildSet { while (result.next()) add(result.getString(1)) }
                }
            }
            assertEquals(setOf("events", "replica_state", "incidents", "reports", "field_conflicts"), tables)
        }
    }

    @Test
    fun `reopening a file keeps its data and does not create the tables again`() {
        val file = Files.createTempFile("device", ".db")
        try {
            LocalDatabase.open(file).use { ReplicaStateRepository(it).enrol(NodeId("tablet-a")) }

            LocalDatabase.open(file).use { database ->
                ReplicaStateRepository(database).enrol(NodeId("tablet-b"))
                assertEquals(NodeId("tablet-a"), ReplicaStateRepository(database).find()?.node)
            }
        } finally {
            file.deleteIfExists()
        }
    }

}
