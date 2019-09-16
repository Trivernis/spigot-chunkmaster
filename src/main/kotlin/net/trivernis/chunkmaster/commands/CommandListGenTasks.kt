package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CommandListGenTasks(private val chunkmaster: Chunkmaster): CommandExecutor {
    /**
     * Responds with a list of running generation tasks.
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val runningTasks = chunkmaster.generationManager.tasks
        val response = ComponentBuilder("Currently running generation tasks").color(ChatColor.BLUE)
        for (task in runningTasks) {
            response.append("\n - #${task.id}: ${task.generationTask.world}, Progress ${task.generationTask.count}")
        }
        response.color(ChatColor.GREEN)
        sender.spigot().sendMessage(*response.create())
        return true
    }
}