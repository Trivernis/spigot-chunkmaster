package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
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
                    } else if (args[1] in units) {
                        when (args[1]) {
                            "radius" -> {
                                stopAfter = ((stopAfter * 2)+1).toDouble().pow(2.0).toInt()
                            }
                            "diameter" -> {
                                stopAfter = stopAfter.toDouble().pow(2.0).toInt()
                            }
                            "blockradius" -> {
                                stopAfter = ((stopAfter*32)+1).toDouble().pow(2.0).toInt()
                            }
                        }
                    } else {
                        worldName = args[1]
                    }
                }
            } else {
                worldName = sender.world.name
            }
        } else {
            if (args.isNotEmpty()) {
                worldName = args[0]
                if (args.size > 1 && args[1].toIntOrNull() != null) {
                    stopAfter = args[1].toInt()
                }
            } else {
                sender.spigot().sendMessage(
                    *ComponentBuilder("You need to provide a world name").color(ChatColor.RED).create())
                return false
            }
        }
        return createTask(sender, worldName, stopAfter)
    }

    /**
     * Creates the task with the given arguments.
     */
    private fun createTask(sender: CommandSender, worldName: String, stopAfter: Int): Boolean {
        val world = chunkmaster.server.getWorld(worldName)
        val allTasks = chunkmaster.generationManager.allTasks
        return if (world != null && (allTasks.find { it.generationTask.world == world }) == null) {
            chunkmaster.generationManager.addTask(world, stopAfter)
            sender.spigot().sendMessage(*ComponentBuilder("Generation task for world ").color(ChatColor.BLUE)
                .append(worldName).color(ChatColor.GREEN).append(" until ").color(ChatColor.BLUE)
                .append(if (stopAfter > 0) "$stopAfter chunks" else "WorldBorder").color(ChatColor.GREEN)
                .append(" successfully created").color(ChatColor.BLUE).create())
            true
        } else if (world == null){
            sender.spigot().sendMessage(*ComponentBuilder("World ").color(ChatColor.RED)
                .append(worldName).color(ChatColor.GREEN).append(" not found!").color(ChatColor.RED).create())
            false
        } else {
            sender.spigot().sendMessage(*ComponentBuilder("Task already exists!").color(ChatColor.RED).create())
            return false
        }
    }
}