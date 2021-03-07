package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdStats(private val chunkmaster: Chunkmaster) : Subcommand {
    override val name = "stats"

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        return sender.server.worlds.map { it.name }.toMutableList()
    }

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (args.isNotEmpty()) {
            val world = sender.server.getWorld(args[0])
            if (world == null) {
                sender.sendMessage(
                    chunkmaster.langManager.getLocalized("STATS_HEADER") + "\n" +
                            chunkmaster.langManager.getLocalized("WORLD_NOT_FOUND", args[0])
                )
                return false
            }
            sender.sendMessage(getWorldStatsMessage(sender, world))
        } else {
            sender.sendMessage(getServerStatsMessage(sender))
        }

        return true
    }

    private fun getWorldStatsMessage(sender: CommandSender, world: World): String {
        var message = """
            ${chunkmaster.langManager.getLocalized("STATS_WORLD_NAME", world.name)}
            ${chunkmaster.langManager.getLocalized("STATS_ENTITY_COUNT", world.entities.size)}
            ${chunkmaster.langManager.getLocalized("STATS_LOADED_CHUNKS", world.loadedChunks.size)}
            ${chunkmaster.langManager.getLocalized("STATS_GENERATING", chunkmaster.generationManager.tasks.find { it.generationTask.world == world } != null)}
        """.trimIndent()
        return message
    }

    private fun getServerStatsMessage(sender: CommandSender): String {
        val runtime = Runtime.getRuntime()
        val memUsed = runtime.maxMemory() - runtime.freeMemory()
        var message = "\n" + """
            ${chunkmaster.langManager.getLocalized("STATS_HEADER")}
            
            ${chunkmaster.langManager.getLocalized("STATS_SERVER")}
            ${chunkmaster.langManager.getLocalized("STATS_SERVER_VERSION", sender.server.version)}
            ${chunkmaster.langManager.getLocalized("STATS_PLUGIN_VERSION", chunkmaster.description.version)}
            ${chunkmaster.langManager.getLocalized(
            "STATS_MEMORY",
            memUsed / 1000000,
            runtime.maxMemory() / 1000000,
            (memUsed.toFloat() / runtime.maxMemory().toFloat()) * 100
        )}
            ${chunkmaster.langManager.getLocalized("STATS_CORES", runtime.availableProcessors())}
            
            ${chunkmaster.langManager.getLocalized("STATS_PLUGIN_LOADED_CHUNKS", chunkmaster.generationManager.loadedChunkCount)}
        """.trimIndent()
        for (world in sender.server.worlds) {
            message += "\n\n" + getWorldStatsMessage(sender, world)
        }
        return message
    }
}