package net.trivernis.chunkmaster.commands

import io.papermc.lib.PaperLib
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CmdTpChunk: Subcommand {
    override val name = "tpchunk"

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        return emptyList<String>().toMutableList()
    }

    /**
     * Teleports the player to a save location in the chunk
     */
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender is Player) {
            if (args.size == 2 && args[0].toIntOrNull() != null && args[1].toIntOrNull() != null) {
                val location = sender.world.getChunkAt(args[0].toInt(), args[1].toInt()).getBlock(8, 60, 8).location

                while (location.block.blockData.material != Material.AIR) {
                    location.y++
                }
                if (PaperLib.isPaper()) {
                    PaperLib.teleportAsync(sender, location)
                } else {
                    sender.teleport(location)
                }
                sender.spigot().sendMessage(*ComponentBuilder("You have been teleportet to chunk")
                    .color(ChatColor.YELLOW).append("${args[0]}, ${args[1]}").color(ChatColor.BLUE).create())
                return true
            } else {
                return false
            }
        } else {
            sender.spigot().sendMessage(*ComponentBuilder("This command can only be executed by a player!")
                .color(ChatColor.RED).create())
            return false
        }
    }
}