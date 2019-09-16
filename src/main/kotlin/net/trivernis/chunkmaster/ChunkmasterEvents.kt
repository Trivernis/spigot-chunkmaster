package net.trivernis.chunkmaster

import org.bukkit.Server
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ChunkmasterEvents(private val chunkmaster: Chunkmaster, private val server: Server): Listener {

    /**
     * Autostart generation tasks
     */
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (server.onlinePlayers.size == 1 && server.onlinePlayers.contains(event.player) ||
                server.onlinePlayers.isEmpty()) {
            chunkmaster.generationManager.startAll()
            chunkmaster.logger.info("Server is empty. Starting chunk generation tasks.")
        }
    }

    /**
     * Autostop generation tasks
     */
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (server.onlinePlayers.size == 1 || server.onlinePlayers.isEmpty()) {
            chunkmaster.generationManager.stopAll()
            chunkmaster.logger.info("Stopping generation tasks because of player join.")
        }
    }
}