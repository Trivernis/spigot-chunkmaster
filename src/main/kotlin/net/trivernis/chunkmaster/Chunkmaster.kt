package net.trivernis.chunkmaster

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.commands.*
import net.trivernis.chunkmaster.lib.LanguageManager
import net.trivernis.chunkmaster.lib.generation.GenerationManager
import net.trivernis.chunkmaster.lib.SqliteManager
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.dynmap.DynmapAPI
import java.lang.Exception

class Chunkmaster: JavaPlugin() {
    lateinit var sqliteManager: SqliteManager
    lateinit var generationManager: GenerationManager
    lateinit var langManager: LanguageManager
    private lateinit var tpsTask: BukkitTask
    var dynmapApi: DynmapAPI? = null
        private set
    var mspt = 20      // keep track of the milliseconds per tick
        private set

    /**
     * On enable of the plugin
     */
    override fun onEnable() {
        PaperLib.suggestPaper(this)
        configure()

        val metrics = Metrics(this)

        langManager = LanguageManager(this)
        langManager.loadProperties()

        this.dynmapApi = getDynmap()

        initDatabase()
        generationManager = GenerationManager(this, server)
        generationManager.init()

        getCommand("chunkmaster")?.aliases = mutableListOf("chm", "chunkm", "cmaster")
        getCommand("chunkmaster")?.setExecutor(CommandChunkmaster(this, server))

        server.pluginManager.registerEvents(ChunkmasterEvents(this, server), this)

        if (PaperLib.isPaper() && PaperLib.getMinecraftPatchVersion() >= 225) {
            tpsTask = server.scheduler.runTaskTimer(this, Runnable {
                mspt = 1000/server.currentTick   // use papers exposed tick rather than calculating it
            }, 1, 300)
        } else {
            tpsTask = server.scheduler.runTaskTimer(this, Runnable {
                val start = System.currentTimeMillis()
                server.scheduler.runTaskLater(this, Runnable {
                    mspt = (System.currentTimeMillis() - start).toInt()
                }, 1)
            }, 1, 300)
        }
    }

    /**
     * Stop all tasks and close database connection on disable
     */
    override fun onDisable() {
        logger.info(langManager.getLocalized("STOPPING_ALL_TASKS"))
        generationManager.stopAll()
    }

    /**
     * Cofigure the config file
     */
    private fun configure() {
        dataFolder.mkdir()
        config.addDefault("generation.period", 2L)
        config.addDefault("generation.chunks-per-step", 2)
        config.addDefault("generation.chunk-skips-per-step", 100)
        config.addDefault("generation.mspt-pause-threshold", 500L)
        config.addDefault("generation.pause-on-player-count", 1)
        config.addDefault("generation.max-pending-chunks", 10)
        config.addDefault("generation.max-loaded-chunks", 10)
        config.addDefault("generation.ignore-worldborder", false)
        config.addDefault("database.filename", "chunkmaster.db")
        config.addDefault("language", "en")
        config.addDefault("dynmap", true)
        config.options().copyDefaults(true)
        saveConfig()
    }

    /**
     * Initializes the database
     */
    private fun initDatabase() {
        logger.info(langManager.getLocalized("DB_INIT"))
        try {
            this.sqliteManager = SqliteManager( this)
            sqliteManager.init()
            logger.info(langManager.getLocalized("DB_INIT_FINISHED"))
        } catch(e: Exception) {
            logger.warning(langManager.getLocalized("DB_INIT_EROR", e.message!!))
        }
    }

    private fun getDynmap(): DynmapAPI? {
        val dynmap = server.pluginManager.getPlugin("dynmap")
        return if (dynmap != null && dynmap is DynmapAPI) {
            logger.info(langManager.getLocalized("PLUGIN_DETECTED", "dynmap", dynmap.dynmapVersion))
            dynmap
        } else {
            null
        }
    }
}