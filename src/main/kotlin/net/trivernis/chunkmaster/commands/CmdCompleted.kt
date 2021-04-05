package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdCompleted(private val plugin: Chunkmaster) : Subcommand {
    override val name = "completed"

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        plugin.sqliteManager.completedGenerationTasks.getCompletedTasks().thenAccept { tasks ->
            val worlds = tasks.map { it.world }.toHashSet()
            var response = "\n" + plugin.langManager.getLocalized("COMPLETED_TASKS_HEADER") + "\n\n"

            for (world in worlds) {
                response += plugin.langManager.getLocalized("COMPLETED_WORLD_HEADER", world) + "\n"

                for (task in tasks.filter { it.world == world }) {
                    response += plugin.langManager.getLocalized(
                        "COMPLETED_TASK_ENTRY",
                        task.id,
                        task.radius,
                        task.center.x,
                        task.center.z,
                        task.shape
                    ) + "\n"
                }
                response += "\n"
            }
            sender.sendMessage(response)
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        return mutableListOf()
    }
}