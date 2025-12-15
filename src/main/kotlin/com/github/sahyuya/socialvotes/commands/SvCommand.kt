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
            sender.sendMessage("/sv <subcommand> の様に記述して下さい。")
            return true
        }

        when (args[0].lowercase(Locale.getDefault())) {
            "setgroup" -> {
                if (sender !is Player) {
                    sender.sendMessage("プレイヤーのみ実行できます。")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage("/sv setgroup <groupname> の用に記述して下さい。")
                    return true
                }
                val groupName = args[1]
                val dm = SocialVotes.dataManager
                if (dm.groupByName.containsKey(groupName)) {
                    sender.sendMessage("既にそのグループは存在します。")
                    return true
                }
                val g = SVGroup(
                    name = groupName,
                    owner = sender.uniqueId
                )
                dm.groupByName[groupName] = g
                dm.save()

                sender.sendMessage("投票グループ $groupName を作成し、オーナーになりました。")
                return true
            }
            "add" -> {
                if (sender !is Player) {
                    sender.sendMessage("プレイヤーのみ実行できます。")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage("/sv add <groupname> の様に記述して下さい。")
                    return true
                }
                val groupName = args[1]
                val dm = SocialVotes.dataManager
                val group = dm.groupByName[groupName]
                if (group == null) {
                    sender.sendMessage("グループが見つかりません。")
                    return true
                }
                // put player into a temporary 'add mode' state
                AddModeManager.watchPlayerForAdd(sender.uniqueId, groupName)
                sender.sendMessage("SV看板に右クリックをして $groupName に追加します。 その他ブロックをクリックすることで追加状態を解除します。")
                return true
            }
            "remove" -> {
                if (sender !is Player) {
                    sender.sendMessage("プレイヤーのみ実行できます。")
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
                        sender.sendMessage("グループが見つかりません。")
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
                    sender.sendMessage("プレイヤーのみ実行できます。")
                    return true
                }
                if (!sender.isOp) {
                    sender.sendMessage("OPのみ実行できます。")
                    return true
                }
                val loc = (sender).location.block.location
                val sign = SocialVotes.dataManager.getSignAt(loc)
                if (sign == null) {
                    sender.sendMessage("この座標にSL看板はありません。")
                    return true
                }

                // データ削除
                SocialVotes.dataManager.removeSignById(sign.id)

                sender.sendMessage("SL看板 ${sign.id} を消去しました。")
                return true
            }
            "startvote" -> {
                if (args.size < 2) {
                    sender.sendMessage("/sv startvote <group> の様に記述して下さい。")
                    return true
                }
                val g = SocialVotes.dataManager.groupByName[args[1]]
                if (g == null) {
                    sender.sendMessage("グループが見つかりません。")
                    return true
                }
                val now = System.currentTimeMillis()
                g.startTime = now
                if (g.endTime != null && g.endTime!! <= now) {
                    g.endTime = null
                }
                SocialVotes.dataManager.save()
                sender.sendMessage("§a投票を開始しました。")
                return true
            }
            "stopvote" -> {
                if (args.size < 2) {
                    sender.sendMessage("/sv stopvote <group> の様に記述して下さい。")
                    return true
                }
                val g = SocialVotes.dataManager.groupByName[args[1]]
                if (g == null) {
                    sender.sendMessage("グループが見つかりません。")
                    return true
                }
                val now = System.currentTimeMillis()
                if (g.startTime == null || now < g.startTime!!) {
                    sender.sendMessage("§c投票はまだ開始されていません。")
                    return true
                }
                g.endTime = now
                SocialVotes.dataManager.save()
                sender.sendMessage("§a投票を終了しました。")
                return true
            }
            "allclear" -> {
                if (sender !is Player && !sender.isOp) {
                    sender.sendMessage("OPのみ実行できます。")
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage("/sv allclear <groupname> の様に記述して下さい。")
                    return true
                }

                val group = SocialVotes.dataManager.groupByName[args[1]]
                if (group == null) {
                    sender.sendMessage("グループが見つかりません。")
                    return true
                }

                group.signIds.toList().forEach {
                    SocialVotes.dataManager.removeSignById(it)
                }

                SocialVotes.dataManager.groupByName.remove(args[1])
                SocialVotes.dataManager.save()

                sender.sendMessage("グループ ${args[1]} と所属SL看板を全消去しました。")
                return true
            }
            "help" -> {
                if (sender !is Player) {
                    sender.sendMessage("プレイヤーのみ実行できます。")
                    return true
                }
                sender.sendMessage("§6--- SocialVotes Help ---")
                sender.sendMessage("§e/sv list §7- グループ一覧")
                sender.sendMessage("§e/sv add <group> §7- 看板をグループに追加")
                sender.sendMessage("§e/sv remove §7- 看板をグループから除外")
                sender.sendMessage("§e/sv startvote <group>")
                sender.sendMessage("§e/sv stopvote <group>")

                if (sender.isOp) {
                    sender.sendMessage("§c/sv setgroup <name>")
                    sender.sendMessage("§c/sv allclear <group>")
                    sender.sendMessage("§c/sv delhere")
                }

                return true
            }


            else -> {
                sender.sendMessage("存在しないコマンドです。")
                return true
            }
        }
    }
}
