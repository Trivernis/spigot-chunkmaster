package net.trivernis.chunkmaster.commands

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.ArgParser
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CommandChunkmaster(private val chunkmaster: Chunkmaster, private val server: Server) : CommandExecutor,
    TabCompleter {
    private val commands = HashMap<String, Subcommand>()
    private val argParser = ArgParser()

    init {
        registerCommands()
    }

    /**
     * Tab complete for commands
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>):
            MutableList<String> {
        if (args.size == 1) {
            return commands.keys.filter { it.indexOf(args[0]) == 0 }.toMutableList()
        } else if (args.isNotEmpty()) {

            if (commands.containsKey(args[0])) {
                val commandEntry = commands[args[0]]
                return commandEntry!!.onTabComplete(sender, command, alias, args.slice(1 until args.size))
            }
        }
        return emptyList<String>().toMutableList()
    }

    /**
     * /chunkmaster command to handle all commands
     */
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        bukkitArgs: Array<out String>
    ): Boolean {
        val args = argParser.parseArguments(bukkitArgs.joinToString(" "))

        if (args.isNotEmpty()) {
            if (sender.hasPermission("chunkmaster.${args[0].toLowerCase()}")) {
                return if (commands.containsKey(args[0])) {
                    commands[args[0]]!!.execute(sender, args.slice(1 until args.size))
                } else {
                    sender.sendMessage(chunkmaster.langManager.getLocalized("SUBCOMMAND_NOT_FOUND", args[0]))
                    false
                }
            } else {
                sender.sendMessage(chunkmaster.langManager.getLocalized("NO_PERMISSION"))
            }
            return true
        } else {
            return false
        }
    }

    /**
     * Registers all subcommands.
     */
    private fun registerCommands() {
        val cmdGenerate = CmdGenerate(chunkmaster)
        commands[cmdGenerate.name] = cmdGenerate

        val cmdPause = CmdPause(chunkmaster)
        commands[cmdPause.name] = cmdPause

        val cmdResume = CmdResume(chunkmaster)
        commands[cmdResume.name] = cmdResume

        val cmdCancel = CmdCancel(chunkmaster)
        commands[cmdCancel.name] = cmdCancel

        val cmdList = CmdList(chunkmaster)
        commands[cmdList.name] = cmdList

        val cmdReload = CmdReload(chunkmaster)
        commands[cmdReload.name] = cmdReload

        val cmdTpChunk = CmdTpChunk(chunkmaster)
        commands[cmdTpChunk.name] = cmdTpChunk

        val cmdSetCenter = CmdSetCenter(chunkmaster)
        commands[cmdSetCenter.name] = cmdSetCenter

        val cmdGetCenter = CmdGetCenter(chunkmaster)
        commands[cmdGetCenter.name] = cmdGetCenter

        val cmdStats = CmdStats(chunkmaster)
        commands[cmdStats.name] = cmdStats

        val cmdCompleted = CmdCompleted(chunkmaster)
        commands[cmdCompleted.name] = cmdCompleted
    }
}