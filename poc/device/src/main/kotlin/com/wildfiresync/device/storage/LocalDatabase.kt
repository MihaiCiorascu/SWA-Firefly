package com.wildfiresync.device.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * the tablet's sqlite file. creates the tables from the shared schema (assets/db/migration/sqlite) the first time it's
 * opened, don't define tables here, they have to match the server
 */
class LocalDatabase private constructor(private val context: Context, name: String?) :
    SQLiteOpenHelper(context, name, null, MIGRATIONS.size), AutoCloseable {

    internal val db: SQLiteDatabase get() = writableDatabase

    override fun onConfigure(db: SQLiteDatabase) {
        // schema needs this, sqlite only enforces foreign keys per connection
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        onUpgrade(db, 0, MIGRATIONS.size)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        MIGRATIONS.subList(oldVersion, newVersion).forEach { asset -> statements(asset).forEach(db::execSQL) }
    }

    internal fun <T> transaction(block: () -> T): T {
        db.beginTransaction()
        try {
            return block().also { db.setTransactionSuccessful() }
        } finally {
            db.endTransaction()
        }
    }

    // execSQL runs one statement at a time, so drop the comments and split on ;
    private fun statements(asset: String): List<String> =
        context.assets.open(asset).bufferedReader().use { it.readText() }
            .lines()
            .filterNot { it.trimStart().startsWith("--") }
            .joinToString("\n")
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    companion object {

        private val MIGRATIONS = listOf("db/migration/sqlite/V1__initial_schema.sql")

        fun open(context: Context, name: String = "device.db"): LocalDatabase = LocalDatabase(context, name)

        fun inMemory(context: Context): LocalDatabase = LocalDatabase(context, null)

    }

}
