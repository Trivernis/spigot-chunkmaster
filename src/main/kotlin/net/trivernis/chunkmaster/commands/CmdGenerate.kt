package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.pow

class CmdGenerate(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "generate"

    /**
     * TabComplete for generate command.
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        if (args.size == 1) {
            return sender.server.worlds.filter { it.name.indexOf(args[0]) == 0 }
                .map {it.name}.toMutableList()
        } else if (args.size == 2) {
            if (args[0].toIntOrNull() != null) {
                return units.filter {it.indexOf(args[1]) == 0}.toMutableList()
            }
        } else if (args.size > 2) {
            if (args[1].toIntOrNull() != null) {
                return units.filter {it.indexOf(args[2]) == 0}.toMutableList()
            }
        }
        return emptyList<String>().toMutableList()
    }
    val units = listOf("blockradius", "radius", "diameter")


    /**
     * Creates a new generation task for the world and chunk count.
     */
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        var worldName = ""
        var stopAfter = -1
        if (sender is Player) {
            if (args.isNotEmpty()) {
                if (args[0].toIntOrNull() != null) {
                    stopAfter = args[0].toInt()
                    worldName = sender.world.name
                } else {
                    worldName = args[0]
                }
                if (args.size > 1) {
                    if (args[1].toIntOrNull() != null) {
                        stopAfter = args[1].toInt()
                    } else if (args[1] in units && args[0].toIntOrNull() != null) {
                        stopAfter = getStopAfter(stopAfter, args[1])
                    } else {
                        worldName = args[1]
                    }
                }
                if (args.size > 2 && args[2] in units && args[1].toIntOrNull() != null) {
                    stopAfter = getStopAfter(stopAfter, args[2])
                }
            } else {
                worldName = sender.world.name
            }
        } else {
            if (args.isNotEmpty()) {
                worldName = args[0]
                if (args.size > 1) {
                    if (args[1].toIntOrNull() != null) {
                        stopAfter = args[1].toInt()
                    }
                }
                if (args.size > 2 && args[2] in units) {
                    stopAfter = getStopAfter(stopAfter, args[2])
                }
            } else {
                sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NAME_REQUIRED"))
                return false
            }
        }
        return createTask(sender, worldName, stopAfter)
    }

    /**
     * Returns stopAfter for a given unit
     */
    private fun getStopAfter(number: Int, unit: String): Int {
        if (unit in units) {
            return when (unit) {
                "radius" -> {
                    ((number * 2)+1).toDouble().pow(2.0).toInt()
                }
                "diameter" -> {
                    number.toDouble().pow(2.0).toInt()
                }
                "blockradius" -> {
                    ((number.toDouble()+1)/8).pow(2.0).toInt()
                }
                else -> number
            }
        }
        return number
    }

    /**
     * Creates the task with the given arguments.
     */
    private fun createTask(sender: CommandSender, worldName: String, stopAfter: Int): Boolean {
        val world = chunkmaster.server.getWorld(worldName)
        val allTasks = chunkmaster.generationManager.allTasks
        return if (world != null && (allTasks.find { it.generationTask.world == world }) == null) {
            chunkmaster.generationManager.addTask(world, stopAfter)
            sender.sendMessage(chunkmaster.langManager
                .getLocalized("TASK_CREATION_SUCCESS", worldName, if (stopAfter > 0) "$stopAfter chunks" else "WorldBorder"))
            true
        } else if (world == null){
            sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NOT_FOUND", worldName));
            false
        } else {
            sender.sendMessage(chunkmaster.langManager.getLocalized("TASK_ALREADY_EXISTS", worldName))
            return false
        }
    }
}