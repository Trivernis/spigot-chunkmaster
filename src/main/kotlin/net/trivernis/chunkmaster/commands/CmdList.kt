package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import net.trivernis.chunkmaster.lib.generation.taskentry.TaskEntry
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
            sender.sendMessage(chunkmaster.langManager.getLocalized("NO_GENERATION_TASKS"))
        } else if (runningTasks.isEmpty()) {
            var response = chunkmaster.langManager.getLocalized("PAUSED_TASKS_HEADER")
            for (taskEntry in pausedTasks) {
                response += getGenerationEntry(taskEntry)
            }
            sender.sendMessage(response)
        } else {
            var response = chunkmaster.langManager.getLocalized("RUNNING_TASKS_HEADER")
            for (task in runningTasks) {
                response += getGenerationEntry(task)
            }
            sender.sendMessage(response)
        }
        return true
    }

    /**
     * Returns the report string for one generation task
     */
    private fun getGenerationEntry(task: TaskEntry): String {
        val genTask = task.generationTask
        val percentage = if (genTask.radius > 0)
            " (%.1f".format(genTask.shape.progress()*100) + "%)."
        else
            ""
        return "\n" + chunkmaster.langManager.getLocalized("TASKS_ENTRY",
            task.id, genTask.world.name, genTask.state.toString(), genTask.count, percentage)
    }
}