package com.wildfiresync.device.storage

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

// TODO figure out where the file lives on an actual tablet

/**
 * the tablet's sqlite file. creates the tables from the shared schema (db/migration/sqlite) the first time it's opened,
 * don't define tables here, they have to match the server
 */
class LocalDatabase private constructor(internal val connection: Connection) : AutoCloseable {

    init {
        // schema needs this, sqlite only enforces foreign keys per connection
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        migrate()
    }

    internal fun <T> transaction(block: () -> T): T {
        connection.autoCommit = false
        try {
            return block().also { connection.commit() }
        } catch (e: Throwable) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    override fun close() {
        connection.close()
    }

    // sqlite's user_version holds the last schema version that was run on this file, 0 for a new file
    private fun migrate() {
        val version = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA user_version").use { it.next(); it.getInt(1) }
        }
        MIGRATIONS.drop(version).forEachIndexed { index, resource ->
            transaction {
                connection.createStatement().use { statement ->
                    statements(resource).forEach(statement::execute)
                    statement.execute("PRAGMA user_version = ${version + index + 1}")
                }
            }
        }
    }

    companion object {

        private val MIGRATIONS = listOf("/db/migration/sqlite/V1__initial_schema.sql")

        fun open(file: Path): LocalDatabase =
            LocalDatabase(DriverManager.getConnection("jdbc:sqlite:${file.toAbsolutePath()}"))

        fun inMemory(): LocalDatabase = LocalDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))

        // jdbc runs one statement at a time, so drop the comments and split on ;
        private fun statements(resource: String): List<String> {
            val sql = LocalDatabase::class.java.getResource(resource)?.readText()
                ?: error("schema $resource is missing from the classpath")
            return sql.lines()
                .filterNot { it.trimStart().startsWith("--") }
                .joinToString("\n")
                .split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

    }

}
