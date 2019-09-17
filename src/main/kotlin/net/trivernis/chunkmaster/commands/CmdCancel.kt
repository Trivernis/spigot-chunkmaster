package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.CommandSender

class CmdCancel(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "cancel"

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        return if (args.isNotEmpty() && args[0].toIntOrNull() != null) {
            if (chunkmaster.generationManager.removeTask(args[0].toInt())) {
                sender.sendMessage("Task ${args[0]} canceled.")
                true
            } else {
                sender.spigot().sendMessage(*ComponentBuilder("Task ${args[0]} not found!")
                    .color(ChatColor.RED).create())
                false
            }
        } else {
            sender.spigot().sendMessage(*ComponentBuilder("You need to provide a task id to cancel.")
                .color(ChatColor.RED).create())
            false
        }
    }
}