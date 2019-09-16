package net.trivernis.chunkmaster

import net.trivernis.chunkmaster.commands.CommandGenerate
import net.trivernis.chunkmaster.commands.CommandListGenTasks
import net.trivernis.chunkmaster.lib.GenerationManager
import net.trivernis.chunkmaster.lib.Spiral
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager
import kotlin.math.log

class Chunkmaster: JavaPlugin() {
    lateinit var sqliteConnection: Connection
    var dbname: String? = null
    lateinit var generationManager: GenerationManager

    override fun onEnable() {
        configure()
        initDatabase()
        generationManager = GenerationManager(this, server)
        generationManager.init()
        getCommand("generate")?.setExecutor(CommandGenerate(this))
        getCommand("listgentasks")?.setExecutor(CommandListGenTasks(this))
        server.pluginManager.registerEvents(ChunkmasterEvents(this, server), this)
    }

    override fun onDisable() {
        logger.info("Stopping all generation tasks...")
        generationManager.stopAll()
        sqliteConnection.close()
    }

    private fun configure() {
        dataFolder.mkdir()
        config.options().copyDefaults(true)
    }

    /**
     * Initializes the database
     */
    private fun initDatabase() {
        logger.info("Initializing Database...")
        try {
            Class.forName("org.sqlite.JDBC")
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:${dataFolder.absolutePath}/chunkmaster.db")
            logger.info("Database connection established.")
            val createTableStatement = sqliteConnection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS generation_tasks (
                    id integer PRIMARY KEY AUTOINCREMENT,
                    center_x integer NOT NULL DEFAULT 0,
                    center_z integer NOT NULL DEFAULT 0,
                    last_x integer NOT NULL DEFAULT 0,
                    last_z integer NOT NULL DEFAULT 0,
                    world text UNIQUE NOT NULL DEFAULT 'world'
                );
            """.trimIndent())
            createTableStatement.execute()
            createTableStatement.close()
            logger.info("Database tables created.")
        } catch(e: Exception) {
            logger.warning("Failed to init database: ${e.message}")
        }
    }
}