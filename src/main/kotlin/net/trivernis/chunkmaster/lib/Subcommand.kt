package net.trivernis.chunkmaster.lib

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

interface Subcommand {
    val name: String
    fun execute(sender: CommandSender, args: List<String>): Boolean
    fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>): MutableList<String>
}