package com.github.sahyuya.socialvotes.commands

import com.github.sahyuya.socialvotes.SocialVotes
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class SvTabCompleter : TabCompleter {

    private val subCommands = listOf(
        "setgroup", "add", "remove",
        "allclear", "delhere", "list",
        "startvote", "stopvote"
    )

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {

        // /sv
        if (args.size == 1) {
            val input = args[0].lowercase()
            return subCommands.filter { it.startsWith(input) }.toMutableList()
        }

        // /sv add <group>
        if (args.size == 2 && args[0].equals("add", true)) {
            val input = args[1].lowercase()
            return SocialVotes.dataManager.groupByName.keys
                .filter { it.startsWith(input) }
                .toMutableList()
        }

        // /sv list <group>
        if (args.size == 2 && args[0].equals("list", true)) {
            val input = args[1].lowercase()
            return SocialVotes.dataManager.groupByName.keys
                .filter { it.startsWith(input) }
                .toMutableList()
        }

        // /sv startvote <group>
        if (args.size == 2 && args[0].equals("startvote", true)) {
            val input = args[1].lowercase()
            return SocialVotes.dataManager.groupByName.keys
                .filter { it.startsWith(input) }
                .toMutableList()
        }

        // /sv stopvote <group>
        if (args.size == 2 && args[0].equals("stopvote", true)) {
            val input = args[1].lowercase()
            return SocialVotes.dataManager.groupByName.keys
                .filter { it.startsWith(input) }
                .toMutableList()
        }

        return mutableListOf()
    }
}
