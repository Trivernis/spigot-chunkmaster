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
        var blockRadius = -1
        var shape = "square"

        if (sender is Player) {
            worldName = sender.world.name
        }
        if (args.isEmpty()) {
            if (sender is Player) {
                return createTask(sender, worldName, blockRadius, shape)
            } else {
                sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NAME_REQUIRED"))
                return false
            }
        }
        if (args[0].toIntOrNull() != null && sender.server.worlds.find { it.name == args[0] } == null) {
            if (sender !is Player) {
                sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NAME_REQUIRED"))
                return false
            }
            blockRadius = args[0].toInt()
        } else {
            worldName = args[0]
        }

        if (args.size == 1) {
            return createTask(sender, worldName, blockRadius, shape)
        }

        when {
            args[1].toIntOrNull() != null -> blockRadius = args[1].toInt()
            args[1] in shapes -> shape = args[1]
            else -> {
                sender.sendMessage(chunkmaster.langManager.getLocalized("INVALID_ARGUMENT", 2, args[1]))
                return false
            }
        }
        if (args.size == 2) {
            return createTask(sender, worldName, blockRadius, shape)
        }
        if (args[2] in shapes) {
            shape = args[2]
        } else {
            sender.sendMessage(chunkmaster.langManager.getLocalized("INVALID_ARGUMENT", 3, args[2]))
            return false
        }

        return createTask(sender, worldName, blockRadius, shape)
    }

    /**
     * Creates the task with the given arguments.
     */
    private fun createTask(sender: CommandSender, worldName: String, blockRadius: Int, shape: String): Boolean {
        val world = chunkmaster.server.getWorld(worldName)
        val allTasks = chunkmaster.generationManager.allTasks
        return if (world != null && (allTasks.find { it.generationTask.world == world }) == null) {
            chunkmaster.generationManager.addTask(world, if (blockRadius > 0) blockRadius / 16 else -1, shape)
            sender.sendMessage(
                chunkmaster.langManager
                    .getLocalized(
                        "TASK_CREATION_SUCCESS",
                        worldName,
                        if (blockRadius > 0) {
                            chunkmaster.langManager.getLocalized("TASK_UNIT_RADIUS", blockRadius)
                        } else {
                            chunkmaster.langManager.getLocalized("TASK_UNIT_WORLDBORDER")
                        },
                        shape
                    )
            )
            true
        } else if (world == null) {
            sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NOT_FOUND", worldName));
            false
        } else {
            sender.sendMessage(chunkmaster.langManager.getLocalized("TASK_ALREADY_EXISTS", worldName))
            return false
        }
    }
}