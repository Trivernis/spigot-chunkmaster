package net.trivernis.chunkmaster

import net.trivernis.chunkmaster.commands.CommandGenerate
import net.trivernis.chunkmaster.commands.CommandListGenTasks
import net.trivernis.chunkmaster.commands.CommandRemoveGenTask
import net.trivernis.chunkmaster.lib.GenerationManager
import net.trivernis.chunkmaster.lib.Spiral
import net.trivernis.chunkmaster.lib.SqlUpdateManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager
import kotlin.math.log

class Chunkmaster: JavaPlugin() {
    lateinit var sqliteConnection: Connection
    var dbname: String? = null
    lateinit var generationManager: GenerationManager
    private lateinit var tpsTask: BukkitTask
    var mspt = 50L      // keep track of the milliseconds per tick
        get() = field

    /**
     * On enable of the plugin
     */
    override fun onEnable() {
        configure()
        initDatabase()
        generationManager = GenerationManager(this, server)
        generationManager.init()
        getCommand("generate")?.setExecutor(CommandGenerate(this))
        getCommand("listgentasks")?.setExecutor(CommandListGenTasks(this))
        getCommand("removegentask")?.setExecutor(CommandRemoveGenTask(this))
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

            val updateManager = SqlUpdateManager(sqliteConnection, this)
            updateManager.checkUpdate()
            updateManager.performUpdate()
            logger.info("Database fully initialized.")
        } catch(e: Exception) {
            logger.warning("Failed to init database: ${e.message}")
        }
    }
}