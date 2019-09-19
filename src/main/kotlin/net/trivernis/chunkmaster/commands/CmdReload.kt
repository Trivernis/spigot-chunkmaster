package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdReload(private val chunkmaster: Chunkmaster): Subcommand {
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
        sender.spigot().sendMessage(*ComponentBuilder("Reloading config and restarting tasks...")
            .color(ChatColor.YELLOW).create())
        chunkmaster.generationManager.stopAll()
        chunkmaster.reloadConfig()
        chunkmaster.generationManager.startAll()
        sender.spigot().sendMessage(*ComponentBuilder("Config reload complete!").color(ChatColor.GREEN).create())
        return true
    }
}