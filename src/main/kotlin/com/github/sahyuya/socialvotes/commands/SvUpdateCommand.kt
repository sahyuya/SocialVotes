package com.github.sahyuya.socialvotes.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SvUpdateCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }
        UpdateModeManager.watchPlayer(sender.uniqueId)
        sender.sendMessage("Right-click a sign to set its new position for the next sign click update.")
        return true
    }
}

object UpdateModeManager {
    private val watching = HashSet<java.util.UUID>()
    fun watchPlayer(uuid: java.util.UUID) { watching.add(uuid) }
    fun isWatching(uuid: java.util.UUID) = watching.contains(uuid)
    fun cancel(uuid: java.util.UUID) { watching.remove(uuid) }
}
