package net.trivernis.chunkmaster

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.commands.*
import net.trivernis.chunkmaster.lib.generation.GenerationManager
import net.trivernis.chunkmaster.lib.SqlUpdateManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager

class Chunkmaster: JavaPlugin() {
    lateinit var sqliteConnection: Connection
    var dbname: String? = null
    lateinit var generationManager: GenerationManager
    private lateinit var tpsTask: BukkitTask
    var mspt = 50L      // keep track of the milliseconds per tick
        private set

    /**
     * On enable of the plugin
     */
    override fun onEnable() {
        PaperLib.suggestPaper(this)
        configure()
        initDatabase()
        generationManager = GenerationManager(this, server)
        generationManager.init()

        getCommand("chunkmaster")?.setExecutor(CommandChunkmaster(this, server))
        getCommand("chunkmaster")?.aliases = mutableListOf("chm", "chunkm", "cmaster")

        server.pluginManager.registerEvents(ChunkmasterEvents(this, server), this)

        tpsTask = server.scheduler.runTaskTimer(this, Runnable {
            val start = System.currentTimeMillis()
            server.scheduler.runTaskLater(this, Runnable {
                mspt = System.currentTimeMillis() - start
            }, 1)
        }, 1, 300)
    }

    /**
     * Stop all tasks and close database connection on disable
     */
    override fun onDisable() {
        logger.info("Stopping all generation tasks...")
        generationManager.stopAll()
        sqliteConnection.close()
    }

    /**
     * Cofigure the config file
     */
    private fun configure() {
        dataFolder.mkdir()
        config.addDefault("generation.period", 2L)
        config.addDefault("generation.chunks-skips-per-step", 4)
        config.addDefault("generation.mspt-pause-threshold", 500L)
        config.addDefault("generation.pause-on-join", true)
        config.options().copyDefaults(true)
        saveConfig()
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

            val updateManager = SqlUpdateManager(sqliteConnection, this)
            updateManager.checkUpdate()
            updateManager.performUpdate()
            logger.info("Database fully initialized.")
        } catch(e: Exception) {
            logger.warning("Failed to init database: ${e.message}")
        }
    }
}