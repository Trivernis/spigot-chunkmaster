package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import net.trivernis.chunkmaster.lib.generation.TaskEntry
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdCancel(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "cancel"

    /**
     * TabComplete for subcommand cancel.
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        val genManager = chunkmaster.generationManager
        val allTasks = genManager.allTasks
        return allTasks.filter {it.id.toString().indexOf(args[0]) == 0}
            .map { it.id.toString() }.toMutableList()
    }

    /**
     * Cancels the generation task if it exists.
     */
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        return if (args.isNotEmpty() && args[0].toIntOrNull() != null) {
            if (chunkmaster.generationManager.removeTask(args[0].toInt())) {
                sender.sendMessage(chunkmaster.langManager.getLocalized("TASK_CANCELED", args[0]))
                true
            } else {
                sender.sendMessage(chunkmaster.langManager.getLocalized("TASK_NOT_FOUND", args[0]))
                false
            }
        } else {
            sender.sendMessage(chunkmaster.langManager.getLocalized("TASK_ID_REQUIRED"));
            false
        }
    }
}