package com.github.sahyuya.socialvotes.commands

import com.github.sahyuya.socialvotes.SocialVotes
import com.github.sahyuya.socialvotes.data.SVGroup
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class SvCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (args.isEmpty()) {
            sender.sendMessage("Usage: /sv <subcommand>")
            return true
        }

        when (args[0].lowercase(Locale.getDefault())) {
            "setgroup" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players can create groups.")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage("Usage: /sv setgroup <groupname>")
                    return true
                }
                val groupName = args[1]
                val dm = SocialVotes.dataManager
                if (dm.groupByName.containsKey(groupName)) {
                    sender.sendMessage("Group already exists.")
                    return true
                }
                val g = SVGroup(name = groupName)
                dm.groupByName[groupName] = g
                dm.save()
                sender.sendMessage("Group '$groupName' created. You are the admin (permission handling TBD).")
                return true
            }
            "add" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players.")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage("Usage: /sv add <groupname>")
                    return true
                }
                val groupName = args[1]
                val dm = SocialVotes.dataManager
                val group = dm.groupByName[groupName]
                if (group == null) {
                    sender.sendMessage("Group not found.")
                    return true
                }
                // put player into a temporary 'add mode' state
                AddModeManager.watchPlayerForAdd(sender.uniqueId, groupName)
                sender.sendMessage("Right-click a sign to add it to group '$groupName'. Right-click something else to cancel.")
                return true
            }
            "remove" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players.")
                    return true
                }
                RemoveModeManager.watchPlayerForRemove(sender.uniqueId)
                sender.sendMessage("Right-click a registered SV sign to remove it from its group.")
                return true
            }
            "list" -> {
                val dm = SocialVotes.dataManager
                if (args.size == 1) {
                    // list groups
                    sender.sendMessage("Groups:")
                    dm.groupByName.keys.forEach { sender.sendMessage(" - $it") }
                } else {
                    val group = args[1]
                    val g = dm.groupByName[group]
                    if (g == null) {
                        sender.sendMessage("Group not found.")
                        return true
                    }
                    sender.sendMessage("Signs in group '$group':")
                    g.signIds.forEach { id ->
                        val s = dm.signById[id]
                        if (s != null) {
                            sender.sendMessage("ID: ${s.id} - ${s.name} (座標(x,y,z)：${s.x} ${s.y} ${s.z} world=${s.world})")
                        }
                    }
                }
                return true
            }
            "delhere" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players.")
                    return true
                }
                if (!sender.isOp) {
                    sender.sendMessage("OP only.")
                    return true
                }
                val loc = (sender as Player).location.block.location
                val sign = SocialVotes.dataManager.getSignAt(loc)
                if (sign == null) {
                    sender.sendMessage("No SV sign at your location.")
                    return true
                }
                SocialVotes.dataManager.removeSignById(sign.id)
                sender.sendMessage("Sign ${sign.id} removed (data + expected block removal).")
                return true
            }
            "startvote" -> {
                // minimal implementation: set group to forced open by clearing startTime
                if (args.size < 2) { sender.sendMessage("Usage: /sv startvote <groupname>"); return true }
                val gName = args[1]
                val dm = SocialVotes.dataManager
                val g = dm.groupByName[gName]
                if (g == null) { sender.sendMessage("No such group"); return true }
                g.startTime = null
                dm.save()
                sender.sendMessage("Group $gName forced to start (startTime cleared).")
                return true
            }
            "stopvote" -> {
                if (args.size < 2) { sender.sendMessage("Usage: /sv stopvote <groupname>"); return true }
                val gName = args[1]
                val dm = SocialVotes.dataManager
                val g = dm.groupByName[gName]
                if (g == null) { sender.sendMessage("No such group"); return true }
                g.endTime = System.currentTimeMillis()
                dm.save()
                sender.sendMessage("Group $gName forced to stop.")
                return true
            }
            "allclear" -> {
                if (sender !is Player && !sender.isOp) {
                    sender.sendMessage("OP only.")
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage("Usage: /sv allclear <groupname>")
                    return true
                }

                val group = SocialVotes.dataManager.groupByName[args[1]]
                if (group == null) {
                    sender.sendMessage("Group not found.")
                    return true
                }

                group.signIds.forEach { id ->
                    SocialVotes.dataManager.removeSignById(id)
                }

                SocialVotes.dataManager.groupByName.remove(args[1])
                SocialVotes.dataManager.save()

                sender.sendMessage("Group '${args[1]}' and its signs were completely removed.")
                return true
            }

            else -> {
                sender.sendMessage("Unknown subcommand.")
                return true
            }
        }
    }
}
