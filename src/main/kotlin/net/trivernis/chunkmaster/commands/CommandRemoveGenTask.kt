package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CommandRemoveGenTask(private val chunkmaster: Chunkmaster): CommandExecutor {
    /**
     * Stops the specified generation task and removes it from the autostart.
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return if (command.testPermission(sender) && args.size == 1) {
            chunkmaster.generationManager.removeTask(args[0].toInt())
            sender.sendMessage("Task ${args[1].toInt()} canceled.")
            true
        } else {
            sender.sendMessage("Invalid argument.")
            false
        }
    }
}