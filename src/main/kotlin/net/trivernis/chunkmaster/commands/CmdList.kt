package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.CommandSender

class CmdList(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "list"

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        val runningTasks = chunkmaster.generationManager.tasks
        if (runningTasks.isEmpty()) {
            sender.spigot().sendMessage(*ComponentBuilder("There are no running generation tasks.")
                .color(ChatColor.BLUE).create())
        } else {
            val response = ComponentBuilder("Currently running generation tasks:").color(ChatColor.WHITE)
            for (task in runningTasks) {
                response.append("\n - #${task.id}: ${task.generationTask.world.name}, Progress ${task.generationTask.count}")
                    .color(ChatColor.BLUE)
            }
            sender.spigot().sendMessage(*response.create())
        }
        return true
    }
}