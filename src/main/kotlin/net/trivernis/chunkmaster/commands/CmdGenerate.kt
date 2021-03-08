package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CmdGenerate(private val chunkmaster: Chunkmaster) : Subcommand {
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
                .map { it.name }.toMutableList()
        } else if (args.size == 2) {
            if (args[0].toIntOrNull() != null) {
                return shapes.filter { it.indexOf(args[1]) == 0 }.toMutableList()
            }
        } else if (args.size > 2) {
            if (args[1].toIntOrNull() != null) {
                return shapes.filter { it.indexOf(args[2]) == 0 }.toMutableList()
            }
        }
        return emptyList<String>().toMutableList()
    }

    val shapes = listOf("circle", "square")


    /**
     * Creates a new generation task for the world and chunk count.
     */
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        var worldName = ""
        var blockRadius: Pair<Int, Int> = Pair(-1, 0)
        var shape = "square"

        if (sender is Player) {
            worldName = sender.world.name
        }
        if (args.isEmpty()) {
            return if (sender is Player) {
                createTask(sender, worldName, blockRadius.first, blockRadius.second, shape)
            } else {
                sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NAME_REQUIRED"))
                false
            }
        }

        var parsedRadius = parseRadius(args[0])

        if (parsedRadius != null && sender.server.worlds.find { it.name == args[0] } == null) {
            if (sender !is Player) {
                sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NAME_REQUIRED"))
                return false
            }
            blockRadius = parsedRadius
        } else {
            worldName = args[0]
        }

        if (args.size == 1) {
            return createTask(sender, worldName, blockRadius.first, blockRadius.second, shape)
        }

        parsedRadius = parseRadius(args[1])
        when {
            parsedRadius != null -> blockRadius = parsedRadius
            args[1] in shapes -> shape = args[1]
            else -> {
                sender.sendMessage(chunkmaster.langManager.getLocalized("INVALID_ARGUMENT", 2, args[1]))
                return false
            }
        }
        if (args.size == 2) {
            return createTask(sender, worldName, blockRadius.first, blockRadius.second, shape)
        }
        if (args[2] in shapes) {
            shape = args[2]
        } else {
            sender.sendMessage(chunkmaster.langManager.getLocalized("INVALID_ARGUMENT", 3, args[2]))
            return false
        }

        return createTask(sender, worldName, blockRadius.first, blockRadius.second, shape)
    }

    /**
     * Creates the task with the given arguments.
     */
    private fun createTask(sender: CommandSender, worldName: String, blockRadius: Int, startRadius: Int, shape: String): Boolean {
        val world = chunkmaster.server.getWorld(worldName)
        val allTasks = chunkmaster.generationManager.allTasks

        return if (world != null && (allTasks.find { it.generationTask.world == world }) == null) {
            chunkmaster.generationManager.addTask(world, if (blockRadius > 0) blockRadius / 16 else -1, shape, startRadius/16)
            sender.sendMessage(
                chunkmaster.langManager
                    .getLocalized(
                        "TASK_CREATION_SUCCESS",
                        worldName,
                        if (blockRadius > 0) {
                            chunkmaster.langManager.getLocalized("TASK_UNIT_RADIUS", blockRadius) + if (startRadius > 0) {
                                chunkmaster.langManager.getLocalized("TASK_CREATION_STARTING_AT", startRadius)
                            } else {
                                ""
                            }
                        } else {
                            chunkmaster.langManager.getLocalized("TASK_UNIT_WORLDBORDER")
                        },
                        shape
                    )
            )
            true
        } else if (world == null) {
            sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NOT_FOUND", worldName))
            false
        } else {
            sender.sendMessage(chunkmaster.langManager.getLocalized("TASK_ALREADY_EXISTS", worldName))
            return false
        }
    }

    /**
     * Tries to parse a radius that can also be a range
     */
    private fun parseRadius(arg: String): Pair<Int, Int>? {
        val radiusRegex = Regex("^(\\d+)(-(\\d+))?\$")
        val matches = radiusRegex.matchEntire(arg)

        if (matches != null) {
            val firstRadius = matches.groupValues[1].toInt()
            var radius = Pair(firstRadius, firstRadius)

            if (matches.groupValues.size >2) {
                radius = Pair(matches.groupValues[3].toInt(), firstRadius)
            }
            if (radius.first < radius.second) {
                return null
            }
            return radius
        }
        return null
    }
}