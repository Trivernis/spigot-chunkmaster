package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CmdPause(private val chunkmaster: Chunkmaster) : Subcommand {
    override val name: String = "pause"

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: List<String>
    ): MutableList<String> {
        return emptyList<String>().toMutableList()
    }

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        return if (!chunkmaster.generationManager.paused) {
            chunkmaster.generationManager.pauseAll()
            sender.sendMessage(chunkmaster.langManager.getLocalized("PAUSE_SUCCESS"))
            true
        } else {
            sender.sendMessage(chunkmaster.langManager.getLocalized("ALREADY_PAUSED"))
            false
        }
    }
}