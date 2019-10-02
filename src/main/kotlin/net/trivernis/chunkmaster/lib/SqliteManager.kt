package net.trivernis.chunkmaster.lib

import net.trivernis.chunkmaster.Chunkmaster
import org.apache.commons.lang.exception.ExceptionUtils
import org.sqlite.SQLiteConnection
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

class SqliteManager(private val chunkmaster: Chunkmaster) {
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
                Pair("stop_after", "integer DEFAULT -1")
            )
        )
    )
    private val needUpdate = HashSet<Pair<String, Pair<String, String>>>()
    private val needCreation = HashSet<String>()

    /**
     * Returns the connection to the database
     */
    fun getConnection(): Connection? {
        try {
            Class.forName("org.sqlite.JDBC")
            return DriverManager.getConnection("jdbc:sqlite:${chunkmaster.dataFolder.absolutePath}/" +
                    "${chunkmaster.config.getString("database.filename")}")
        } catch (e: Exception) {
            chunkmaster.logger.severe("Could not get database connection.")
            chunkmaster.logger.severe(e.message)
        }
        return null
    }

    /**
     * Checks for and performs an update
     */
    fun init() {
        this.checkUpdate()
        this.performUpdate()
    }

    /**
     * Checks which tables need an update or creation.
     */
    private fun checkUpdate() {
        val meta = getConnection()!!.metaData

        for (table in tables) {
            val resTables = meta.getTables(null, null, table.first, null)

            if (resTables.next()) { // table exists
                for (column in table.second) {
                    val resColumn = meta.getColumns(null, null, table.first, column.first)
                    if (!resColumn.next()) {
                        needUpdate.add(Pair(table.first, column))
                    }
                    resColumn.close()
                }
            } else {
                needCreation.add(table.first)
            }
            resTables.close()
        }
    }

    /**
     * Executes a sql statement on the database.
     */
    fun executeStatement(sql: String, values: HashMap<Int, Any>, callback: ((ResultSet) -> Unit)?) {
        val connection = getConnection()
        if (connection != null) {
            try {
                val statement = connection.prepareStatement(sql)
                for (parameterValue in values) {
                    statement.setObject(parameterValue.key, parameterValue.value)
                }
                statement.execute()
                val res = statement.resultSet
                if (callback != null) {
                    callback(res)
                }
                statement.close()
            } catch (e: Exception) {
                chunkmaster.logger.severe("An error occured on sql $sql. ${e.message}")
                chunkmaster.logger.info(ExceptionUtils.getStackTrace(e))
            } finally {
                connection.close()
            }
        } else {
            chunkmaster.logger.severe("Could not execute sql $sql. No database connection established.")
        }
    }

    /**
     * Creates or updates tables that needed an update.
     */
    private fun performUpdate() {
        for (table in needCreation) {
            try {
                var tableDef = "CREATE TABLE IF NOT EXISTS $table ("

                for (column in tables.find{it.first == table}!!.second) {
                    tableDef += "${column.first} ${column.second},"
                }
                tableDef = tableDef.substringBeforeLast(",") + ");"
                chunkmaster.logger.info("Creating table $table with definition $tableDef")
                executeStatement(tableDef, HashMap(), null)
            } catch (e: Exception) {
                chunkmaster.logger.severe("Error creating table $table.")
                chunkmaster.logger.severe(e.message)
                chunkmaster.logger.info(ExceptionUtils.getStackTrace(e))
            }
        }
        for (table in needUpdate) {
            val updateSql = "ALTER TABLE ${table.first} ADD COLUMN ${table.second.first} ${table.second.second}"
            try {
                executeStatement(updateSql, HashMap(), null)
                chunkmaster.logger.info("Updated table ${table.first} with sql $updateSql")
            } catch (e: Exception) {
                chunkmaster.logger.severe("Failed to update table ${table.first} with sql $updateSql")
                chunkmaster.logger.severe(e.message)
                chunkmaster.logger.info(ExceptionUtils.getStackTrace(e))
            }
        }
    }
}