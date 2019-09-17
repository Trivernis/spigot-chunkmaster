package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CmdGenerate(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "generate"

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        var worldName = ""
        var stopAfter = -1
        if (sender is Player) {
            if (args.isNotEmpty()) {
                if (args[0].toIntOrNull() != null) {
                    stopAfter = args[0].toInt()
                } else {
                    worldName = args[0]
                }
                if (args.size > 1 && args[1].toIntOrNull() != null) {
                    stopAfter = args[1].toInt()
                }
            } else {
                worldName = sender.world.name
            }
        } else {
            if (args.isNotEmpty()) {
                worldName = args[0]
                if (args.size > 1 && args[1].toIntOrNull() != null) {
                    stopAfter = args[1].toInt()
                }
            } else {
                sender.spigot().sendMessage(
                    *ComponentBuilder("You need to provide a world name").color(ChatColor.RED).create())
                return false
            }
        }
        val world = chunkmaster.server.getWorld(worldName)
        return if (world != null) {
            chunkmaster.generationManager.addTask(world, stopAfter)
            sender.spigot().sendMessage(*ComponentBuilder("Generation task for world ").color(ChatColor.BLUE)
                .append(worldName).color(ChatColor.GREEN).append(" until ").color(ChatColor.BLUE)
                .append(if (stopAfter > 0) "$stopAfter chunks" else "WorldBorder").color(ChatColor.GREEN)
                .append(" successfully created").color(ChatColor.BLUE).create())
            true
        } else {
            sender.spigot().sendMessage(*ComponentBuilder("World ").color(ChatColor.RED)
                .append(worldName).color(ChatColor.GREEN).append(" not found!").color(ChatColor.RED).create())
            false
        }
    }
}