package net.trivernis.chunkmaster.lib

import net.trivernis.chunkmaster.Chunkmaster
import java.lang.Exception
import java.sql.Connection

class SqlUpdateManager(private val connnection: Connection, private val chunkmaster: Chunkmaster) {
    private val tables = listOf(
        Pair(
            "generation_tasks",
            listOf(
                Pair("id", "integer PRIMARY KEY AUTOINCREMENT"),
                Pair("center_x", "integer NOT NULL DEFAULT 0"),
                Pair("center_z", "integer NOT NULL DEFAULT 0"),
                Pair("last_x", "integer NOT NULL DEFAULT 0"),
                Pair("last_z", "integer NOT NULL DEFAULT 0"),
                Pair("world", "text UNIQUE NOT NULL DEFAULT 'world'"),
                Pair("stop_after", "integer DEFAULT -1"),
                Pair("autostart", "integer DEFAULT 1")
            )
        )
    )
    private val needUpdate = HashSet<Pair<String, Pair<String, String>>>()
    private val needCreation = HashSet<String>()

    /**
     * Checks which tables need an update or creation.
     */
    fun checkUpdate() {
        val meta = connnection.metaData

        for (table in tables) {
            val resTables = meta.getTables(null, null, table.first, null)

            if (resTables.next()) { // table exists
                for (column in table.second) {
                    val resColumn = meta.getColumns(null, null, table.first, column.first)
                    if (!resColumn.next()) {
                        needUpdate.add(Pair(table.first, column))
                    }
                }
            } else {
                needCreation.add(table.first)
            }
        }
    }

    /**
     * Creates or updates tables that needed an update.
     */
    fun performUpdate() {
        for (table in needCreation) {
            try {
                var tableDef = "CREATE TABLE IF NOT EXISTS $table ("

                for (column in tables.find{it.first == table}!!.second) {
                    tableDef += "${column.first} ${column.second},"
                }
                tableDef = tableDef.substringBeforeLast(",") + ");"
                chunkmaster.logger.info("Creating table $table with definition $tableDef")
                val stmt = connnection.prepareStatement(tableDef)
                stmt.execute()
                stmt.close()
            } catch (err: Exception) {
                chunkmaster.logger.severe("Error creating table $table.");
            }
        }
        for (table in needUpdate) {
            val updateSql = "ALTER TABLE ${table.first} ADD COLUMN ${table.second.first} ${table.second.second}"
            try {
                val stmt = connnection.prepareStatement(updateSql)
                stmt.execute()
                stmt.close()
                chunkmaster.logger.info("Updated table ${table.first} with sql $updateSql")
            } catch (e: Exception) {
                chunkmaster.logger.severe("Failed to update table ${table.first} with sql $updateSql")
            }
        }
    }
}