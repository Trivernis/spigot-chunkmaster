package net.trivernis.chunkmaster.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Subcommand
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CommandChunkmaster(private val chunkmaster: Chunkmaster, private val server: Server): CommandExecutor,
    TabCompleter {
    private val commands = HashMap<String, Subcommand>()

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
        } else if (args.isNotEmpty()){

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
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            when (args[0]) {
                "reload" -> {
                    chunkmaster.reloadConfig()
                    sender.sendMessage("Configuration file reloaded.")
                }
                else -> {
                    if (sender.hasPermission("chunkmaster.${args[0]}")) {
                        return if (commands.containsKey(args[0])) {
                            commands[args[0]]!!.execute(sender, args.slice(1 until args.size))
                        } else {
                            sender.spigot().sendMessage(*ComponentBuilder("Subcommand ").color(ChatColor.RED)
                                .append(args[0]).color(ChatColor.GREEN).append(" not found").color(ChatColor.RED).create())
                            false
                        }
                    } else {
                        sender.spigot().sendMessage(*ComponentBuilder("You do not have permission!")
                            .color(ChatColor.RED).create())
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    fun registerCommands() {
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
    }
}