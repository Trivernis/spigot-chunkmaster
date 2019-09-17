package net.trivernis.chunkmaster.lib

import org.bukkit.command.CommandSender

interface Subcommand {
    val name: String
    fun execute(sender: CommandSender, args: List<String>): Boolean
}