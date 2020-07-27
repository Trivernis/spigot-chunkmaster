package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdReload(private val chunkmaster: Chunkmaster) : Subcommand {
    override val name = "reload"

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        return emptyList<String>().toMutableList()
    }

    /**
     * Reload command to reload the config and restart the tasks.
     */
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        sender.sendMessage(chunkmaster.langManager.getLocalized("CONFIG_RELOADING"))
        chunkmaster.generationManager.stopAll()
        chunkmaster.reloadConfig()
        chunkmaster.generationManager.startAll()
        chunkmaster.langManager.loadProperties()
        sender.sendMessage(chunkmaster.langManager.getLocalized("CONFIG_RELOADED"))
        return true
    }
}