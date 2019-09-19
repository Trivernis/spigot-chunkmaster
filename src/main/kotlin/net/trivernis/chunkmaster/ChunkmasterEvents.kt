package net.trivernis.chunkmaster

import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ChunkmasterEvents(private val chunkmaster: Chunkmaster, private val server: Server) : Listener {

    private val pauseOnJoin = chunkmaster.config.getBoolean("generation.pause-on-join")
    private var playerPaused = false

    /**
     * Autostart generation tasks
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (pauseOnJoin) {
            if (server.onlinePlayers.size == 1 && server.onlinePlayers.contains(event.player) ||
                server.onlinePlayers.isEmpty()
            ) {
                if (!playerPaused) {
                    chunkmaster.generationManager.resumeAll()
                    chunkmaster.logger.info("Server is empty. Resuming chunk generation tasks.")
                } else if (chunkmaster.generationManager.paused){
                    chunkmaster.logger.info("Generation was manually paused. Not resuming automatically.")
                    playerPaused = chunkmaster.generationManager.paused
                } else {
                    chunkmaster.logger.info("Generation tasks are already running.")
                }
            }
        }
    }

    /**
     * Autostop generation tasks
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (pauseOnJoin) {
            if (server.onlinePlayers.size == 1 || server.onlinePlayers.isEmpty()) {
                playerPaused = chunkmaster.generationManager.paused
                chunkmaster.generationManager.pauseAll()
                chunkmaster.logger.info("Pausing generation tasks because of player join.")
            }
        }
    }
}