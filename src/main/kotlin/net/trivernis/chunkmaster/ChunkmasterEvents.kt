package net.trivernis.chunkmaster

import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ChunkmasterEvents(private val chunkmaster: Chunkmaster, private val server: Server) : Listener {

    private val pauseOnPlayerCount: Int
        get() {
            return chunkmaster.config.getInt("generation.pause-on-player-count")
        }
    private var playerPaused = false

    /**
     * Autostart generation tasks
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (server.onlinePlayers.size == pauseOnPlayerCount) {
            if (!playerPaused) {
                if (chunkmaster.generationManager.pausedTasks.isNotEmpty()) {
                    chunkmaster.logger.info(chunkmaster.langManager.getLocalized("RESUME_PLAYER_LEAVE"))
                }
                chunkmaster.generationManager.resumeAll()
            } else if (chunkmaster.generationManager.paused) {
                chunkmaster.logger.info(chunkmaster.langManager.getLocalized("PAUSE_MANUALLY"))
                playerPaused = chunkmaster.generationManager.paused
            }
        }
    }

    /**
     * Autostop generation tasks
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (server.onlinePlayers.size >= pauseOnPlayerCount) {
            if (chunkmaster.generationManager.tasks.isNotEmpty()) {
                chunkmaster.logger.info(chunkmaster.langManager.getLocalized("PAUSE_PLAYER_JOIN"))
            }
            playerPaused = chunkmaster.generationManager.paused
            chunkmaster.generationManager.pauseAll()
        }
    }
}