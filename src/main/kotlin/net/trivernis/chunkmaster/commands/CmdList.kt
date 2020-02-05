package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdList(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "list"

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        return emptyList<String>().toMutableList()
    }

    /**
     * Lists all running or paused generation tasks.
     */
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        val runningTasks = chunkmaster.generationManager.tasks
        val pausedTasks = chunkmaster.generationManager.pausedTasks

        if (runningTasks.isEmpty() && pausedTasks.isEmpty()) {
            sender.spigot().sendMessage(*ComponentBuilder("There are no generation tasks.")
                .color(ChatColor.BLUE).create())
        } else if (runningTasks.isEmpty()) {
            val response = ComponentBuilder("Currently paused generation tasks:").color(ChatColor.WHITE).bold(true)
            for (taskEntry in pausedTasks) {
                val genTask = taskEntry.generationTask
                response.append("\n - ").color(ChatColor.WHITE).bold(false)
                response.append("#${taskEntry.id}").color(ChatColor.BLUE).append(" - ").color(ChatColor.WHITE)
                response.append(genTask.world.name).color(ChatColor.GREEN).append(" - Progress: ").color(ChatColor.WHITE)
                response.append("${genTask.count} chunks").color(ChatColor.BLUE)

                if (genTask.stopAfter > 0) {
                    response.append(" (${(genTask.count.toDouble()/genTask.stopAfter.toDouble())*100}%).")
                }
            }
            sender.spigot().sendMessage(*response.create())
        } else {
            val response = ComponentBuilder("Currently running generation tasks:").color(ChatColor.WHITE).bold(true)
            for (task in runningTasks) {
                val genTask = task.generationTask
                response.append("\n - ").color(ChatColor.WHITE).bold(false)
                    .append("#${task.id}").color(ChatColor.BLUE).append(" - ").color(ChatColor.WHITE)
                    .append(genTask.world.name).color(ChatColor.GREEN).append(" - Progress: ").color(ChatColor.WHITE)
                    .append("${"%.1f".format(genTask.count)} chunks").color(ChatColor.BLUE)
                    .append(", ETA: ").color(ChatColor.WHITE)
                    .append("")

                if (genTask.stopAfter > 0) {
                    response.append(" (${(genTask.count.toDouble()/genTask.stopAfter.toDouble())*100}%).")
                }
            }
            sender.spigot().sendMessage(*response.create())
        }
        return true
    }
}