package net.trivernis.chunkmaster.lib

import net.trivernis.chunkmaster.Chunkmaster
import org.apache.commons.lang.exception.ExceptionUtils
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager
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
                Pair("radius", "integer DEFAULT -1"),
                Pair("shape", "text NOT NULL DEFAULT 'square'")
            )
        ),
        Pair(
            "world_properties",
            listOf(
                Pair("name", "text PRIMARY KEY"),
                Pair("center_x", "integer NOT NULL DEFAULT 0"),
                Pair("center_z", "integer NOT NULL DEFAULT 0")
            )
        ),
        Pair(
            "paper_pending_chunks",
            listOf(
                Pair("id", "integer PRIMARY KEY AUTOINCREMENT"),
                Pair("task_id", "integer NOT NULL"),
                Pair("chunk_x", "integer NOT NULL"),
                Pair("chunk_z", "integer NOT NULL")
            )
        )
    )
    private val needUpdate = HashSet<Pair<String, Pair<String, String>>>()
    private val needCreation = HashSet<String>()
    private var connection: Connection? = null
    private var activeTasks = 0

    /**
     * Returns the connection to the database
     */
    fun getConnection(): Connection? {
        if (this.connection != null) {
            return this.connection
        }
        try {
            Class.forName("org.sqlite.JDBC")
            this.connection = DriverManager.getConnection("jdbc:sqlite:${chunkmaster.dataFolder.absolutePath}/" +
                    "${chunkmaster.config.getString("database.filename")}")
            return this.connection
        } catch (e: Exception) {
            chunkmaster.logger.severe(chunkmaster.langManager.getLocalized("DATABASE_CONNECTION_ERROR"))
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
    fun executeStatement(sql: String, values: HashMap<Int, Any>, callback: ((ResultSet?) -> Unit)?) {
        val connection = getConnection()
        activeTasks++
        if (connection != null) {
            try {
                val statement = connection.prepareStatement(sql)
                for (parameterValue in values) {
                    statement.setObject(parameterValue.key, parameterValue.value)
                }
                statement.execute()
                val res: ResultSet? = statement.resultSet
                if (callback != null) {
                    callback(res)
                }
                statement.close()
            } catch (e: Exception) {
                chunkmaster.logger.severe(chunkmaster.langManager.getLocalized("SQL_ERROR", e.toString()))
                chunkmaster.logger.info(ExceptionUtils.getStackTrace(e))
            } finally {
                activeTasks--
                if (activeTasks == 0) {
                    connection.close()
                    this.connection = null
                }
            }
        } else {
            chunkmaster.logger.severe(chunkmaster.langManager.getLocalized("NO_DATABASE_CONNECTION"))
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
                chunkmaster.logger.finest(chunkmaster.langManager.getLocalized("CREATE_TABLE_DEFINITION", table, tableDef))
                executeStatement(tableDef, HashMap(), null)
            } catch (e: Exception) {
                chunkmaster.logger.severe(chunkmaster.langManager.getLocalized("TABLE_CREATE_ERROR", table))
                chunkmaster.logger.severe(e.message)
                chunkmaster.logger.info(ExceptionUtils.getStackTrace(e))
            }
        }
        for (table in needUpdate) {
            val updateSql = "ALTER TABLE ${table.first} ADD COLUMN ${table.second.first} ${table.second.second}"
            try {
                executeStatement(updateSql, HashMap(), null)
                chunkmaster.logger.finest(chunkmaster.langManager.getLocalized("UPDATE_TABLE_DEFINITION", table.first, updateSql))
            } catch (e: Exception) {
                chunkmaster.logger.severe(chunkmaster.langManager.getLocalized("UPDATE_TABLE_FAILED", table.first, updateSql))
                chunkmaster.logger.severe(e.message)
                chunkmaster.logger.info(ExceptionUtils.getStackTrace(e))
            }
        }
    }
}