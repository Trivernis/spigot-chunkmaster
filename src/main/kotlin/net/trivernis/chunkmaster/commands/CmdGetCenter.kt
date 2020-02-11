package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CmdGetCenter(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "getCenter";

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        if (args.size == 1) {
            return sender.server.worlds.filter { it.name.indexOf(args[0]) == 0 }
                .map {it.name}.toMutableList()
        }
        return emptyList<String>().toMutableList()
    }

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        val worldName: String = if (sender is Player) {
            if (args.isNotEmpty()) {
                args[0]
            } else {
                sender.world.name;
            }
        } else {
            if (args.isEmpty()) {
                sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NAME_REQUIRED"))
                return false
            } else {
                args[0]
            }
        }
        if (chunkmaster.generationManager.worldCenters.isEmpty()) {
            chunkmaster.generationManager.loadWorldCenters() {
                sendCenterInfo(sender, worldName)
            }
            return true
        }
        sendCenterInfo(sender, worldName)
        return true
    }

    /**
     * Sends the center information
     */
    private fun sendCenterInfo(sender: CommandSender, worldName: String) {
        var center = chunkmaster.generationManager.worldCenters[worldName]
        if (center == null) {
            val world = sender.server.worlds.find { it.name == worldName }
            if (world == null) {
                sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NOT_FOUND", worldName))
                return
            }
            center = Pair(world.spawnLocation.chunk.x, world.spawnLocation.chunk.z)
        }
        sender.sendMessage(chunkmaster.langManager.getLocalized("CENTER_INFO", worldName, center.first, center.second))
    }
}