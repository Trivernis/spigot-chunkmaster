package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandGenerate(private val chunkmaster: Chunkmaster): CommandExecutor {
    /**
     * Start world generation task on commmand
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            return if (command.testPermission(sender)) {
                var stopAfter = -1

                if (args.isNotEmpty() && args[0].toIntOrNull() != null) {
                    stopAfter = args[0].toInt()
                }
                chunkmaster.generationManager.addTask(sender.world, stopAfter)
                sender.sendMessage("Added generation task for world \"${sender.world.name}\"" +
                        "Stopping after $stopAfter chunks (-1 for world border).")
                true
            } else {
                sender.sendMessage("You do not have permission.")
                true
            }
        } else {
            return if (args.size >= 1) {
                val world = sender.server.getWorld(args[0])
                if (world != null) {
                    var stopAfter = -1

                    if (args.size >=2 && args[1].toIntOrNull() != null) {
                        stopAfter = args[1].toInt()
                    }
                    chunkmaster.generationManager.addTask(world, stopAfter)

                    sender.sendMessage("Added generation task for world \"${world.name}\"" +
                            "Stopping after $stopAfter chunks (-1 for world border).")
                    true
                } else {
                    sender.sendMessage("World \"${args[0]}\" not found")
                    false
                }
            } else {
                sender.sendMessage("You need to specify a world")
                false
            }
        }
    }

}