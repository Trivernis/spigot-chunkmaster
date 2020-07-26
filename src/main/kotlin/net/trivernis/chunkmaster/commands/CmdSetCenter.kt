package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CmdSetCenter(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "setCenter";

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        if (args.size == 1) {
            if (args[0].toIntOrNull() == null) {
                return sender.server.worlds.filter { it.name.indexOf(args[0]) == 0 }
                    .map {it.name}.toMutableList()
            }
        }
        return emptyList<String>().toMutableList();
    }

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        val world: String
        val centerX: Int
        val centerZ: Int

        if (sender is Player) {
            when {
                args.isEmpty() -> {
                    world = sender.world.name
                    centerX = sender.location.chunk.x
                    centerZ = sender.location.chunk.z
                }
                args.size == 2 -> {
                    world = sender.world.name
                    if (args[0].toIntOrNull() == null || args[1].toIntOrNull() == null) {
                        sender.sendMessage(chunkmaster.langManager.getLocalized("COORD_INVALID", args[0], args[1]))
                        return false
                    }
                    centerX = args[0].toInt()
                    centerZ = args[1].toInt()
                }
                else -> {
                    if (!validateThreeArgs(sender, args)) {
                        return false
                    }
                    world = args[0]
                    centerX = args[1].toInt()
                    centerZ = args[2].toInt()
                }
            }
        } else {
            if (args.size < 3) {
                sender.sendMessage(chunkmaster.langManager.getLocalized("TOO_FEW_ARGUMENTS"))
                return false
            } else {
                if (!validateThreeArgs(sender, args)) {
                    return false
                }
                world = args[0]
                centerX = args[1].toInt()
                centerZ = args[2].toInt()
            }
        }
        chunkmaster.generationManager.worldProperties.setWorldCenter(world, Pair(centerX, centerZ))
        sender.sendMessage(chunkmaster.langManager.getLocalized("CENTER_UPDATED", world, centerX, centerZ))
        return true
    }

    /**
     * Validates the command values with three arguments
     */
    private fun validateThreeArgs(sender: CommandSender, args: List<String>): Boolean {
        return if (sender.server.worlds.none { it.name == args[0] }) {
            sender.sendMessage(chunkmaster.langManager.getLocalized("WORLD_NOT_FOUND", args[0]))
            false
        } else if (args[1].toIntOrNull() == null || args[2].toIntOrNull() == null) {
            sender.sendMessage(chunkmaster.langManager.getLocalized("COORD_INVALID", args[1], args[2]))
            false
        } else {
            true
        }
    }
}