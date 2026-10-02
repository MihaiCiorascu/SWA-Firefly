package com.wildfiresync.device.storage

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/**
 * EXAMPLE, work in progress: the SQLite database file the tablet carries with it.
 *
 * This only opens the connection. It does not create any tables: the schema is defined in the database-schema MR
 * (`device/src/main/resources/db/migration/sqlite/V1__initial_schema.sql`) and has to stay identical to the
 * server's, so it must not be redefined here.
 *
 * TODO: apply the schema migrations from the database-schema MR when the database is opened
 * TODO: decide where the database file lives on a real tablet
 */
class LocalDatabase private constructor(internal val connection: Connection) : AutoCloseable {

    init {
        // required by the schema: SQLite only enforces foreign keys when they are enabled on each connection
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
