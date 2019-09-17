package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.CommandSender

class CmdPause(private val chunkmaster: Chunkmaster) : Subcommand {
    override val name: String = "pause"

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        return if (!chunkmaster.generationManager.paused) {
            chunkmaster.generationManager.pauseAll()
            sender.spigot().sendMessage(
                *ComponentBuilder("Paused all generation tasks.")
                    .color(ChatColor.BLUE).create()
            )
            true
        } else {
            sender.spigot().sendMessage(
                *ComponentBuilder("The generation process is already paused.").color(ChatColor.RED).create()
            )
            false
        }
    }
}