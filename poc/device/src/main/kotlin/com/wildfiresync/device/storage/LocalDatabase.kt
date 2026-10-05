package com.wildfiresync.device.storage

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

// TODO run the SQL file from the database-schema MR (V1__initial_schema.sql) on open, nothing creates the tables yet
// TODO figure out where the file lives on an actual tablet

/**
 * example for now, just opens the tablet's sqlite file.
 * don't define tables here, they come from the database-schema MR and have to match the server
 */
class LocalDatabase private constructor(internal val connection: Connection) : AutoCloseable {

    init {
        // schema needs this, sqlite only enforces foreign keys per connection
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
    }

    override fun close() {
        connection.close()
    }

    companion object {

        fun open(file: Path): LocalDatabase =
            LocalDatabase(DriverManager.getConnection("jdbc:sqlite:${file.toAbsolutePath()}"))

        fun inMemory(): LocalDatabase = LocalDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))

    }

}
