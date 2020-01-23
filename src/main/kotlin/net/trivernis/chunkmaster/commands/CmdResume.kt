package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdResume(private val chunkmaster: Chunkmaster): Subcommand {
    override val name = "resume"

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        return emptyList<String>().toMutableList()
    }

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        return if (chunkmaster.generationManager.paused) {
            chunkmaster.generationManager.resumeAll()
            sender.spigot().sendMessage(
                *ComponentBuilder("Resumed all generation tasks.").color(ChatColor.BLUE).create())
            true
        } else {
            sender.spigot().sendMessage(
                *ComponentBuilder("There are no paused generation tasks.").color(ChatColor.RED).create())
            false
        }
    }
}