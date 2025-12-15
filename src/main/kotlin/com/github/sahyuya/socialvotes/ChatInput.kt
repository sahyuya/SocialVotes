package com.github.sahyuya.socialvotes

import com.github.sahyuya.socialvotes.gui.DetailGUI
import com.github.sahyuya.socialvotes.util.SignDisplayUtil
import com.github.sahyuya.socialvotes.util.TimeParser
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.UUID

object ChatInput : Listener {

    data class InputState(
        val action: DetailGUI.ChatAction,
        val signId: Int
    )

    /* =====================
       外部呼び出し用 API
       ===================== */
    private val states = mutableMapOf<UUID, InputState>()
    fun start(uuid: UUID, state: InputState) {
        states[uuid] = state
    }
    fun cancel(uuid: UUID) {
        states.remove(uuid)
    }

    /* =====================
       チャット入力処理
       ===================== */
    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        val p = e.player
        val state = states.remove(p.uniqueId) ?: return
        e.isCancelled = true
        val dm = SocialVotes.dataManager
        val sign = dm.signById[state.signId]
        if (sign == null) {
            p.sendMessage("§c対象の看板が存在しません。")
            cancel(p.uniqueId)
            return
        }
        val group = sign.group?.let { dm.groupByName[it] }
        val msg = e.message.trim()

        when (state.action) {

            /* ---------------------
               看板名変更
               --------------------- */
            DetailGUI.ChatAction.RENAME_SIGN -> {
                if (msg.isBlank()) {
                    p.sendMessage("§c変更をキャンセルしました。")
                    return
                }
                sign.name = msg
                dm.save()
                Bukkit.getScheduler().runTask(
                    SocialVotes.instance,
                    Runnable {
                        SignDisplayUtil.updateSingle(sign)
                        p.sendMessage("§a看板名を「$msg」に変更しました。")
                    }
                )
            }

            /* ---------------------
               看板単体 最大投票数
               --------------------- */
            DetailGUI.ChatAction.SET_SIGN_MAX -> {
                val v = msg.toIntOrNull()
                if (v == null || v <= 0) {
                    p.sendMessage("§7変更は行われませんでした。")
                    return
                }
                // グループ制限を超えないよう補正
                val groupMax = group?.maxVotesPerPlayer ?: Int.MAX_VALUE
                val newValue = minOf(v, groupMax)
                sign.maxVotesPerSign = newValue
                dm.save()
                Bukkit.getScheduler().runTask(SocialVotes.instance, Runnable {
                    SignDisplayUtil.updateSingle(sign)
                    p.sendMessage("§a看板の最大投票数を $newValue 票に設定しました。")
                })
            }

            /* ---------------------
               グループ全体 最大投票数
               --------------------- */
            DetailGUI.ChatAction.SET_GROUP_MAX -> {
                val groupName = sign.group
                if (groupName == null) {
                    p.sendMessage("§cこの看板はグループに属していません。")
                    cancel(p.uniqueId)
                    return
                }
                val group = dm.groupByName[groupName]
                if (group == null) {
                    p.sendMessage("§cグループが存在しません。")
                    cancel(p.uniqueId)
                    return
                }
                val v = msg.toIntOrNull()
                if (v == null || v <= 0) {
                    p.sendMessage("§7変更は行われませんでした。")
                    return
                }
                group.maxVotesPerPlayer = v
                // 所属看板の制限を超えていたら切り下げ
                for (sid in group.signIds) {
                    val s = dm.signById[sid] ?: continue
                    if (s.maxVotesPerSign > v) {
                        s.maxVotesPerSign = v
                    }
                }
                dm.save()
                Bukkit.getScheduler().runTask(SocialVotes.instance, Runnable {
                        SignDisplayUtil.updateGroup(group)
                        p.sendMessage("§aグループ全体の最大投票数を $v 票に変更しました。")
                })
            }

            /* ---------------------
               グループ　投票開始時刻
               --------------------- */
            DetailGUI.ChatAction.SET_START_TIME -> {
                if (msg.isBlank()) return
                group ?: return
                if (msg == "0") {
                    group.startTime = null
                } else {
                    group.startTime = TimeParser.parse(msg) ?: return
                }
                dm.save()
                Bukkit.getScheduler().runTask(SocialVotes.instance,
                    Runnable {
                        SignDisplayUtil.updateGroup(group)
                        p.sendMessage("§a時刻設定を更新しました。")
                    }
                )
            }

            /* ---------------------
               グループ　投票終了時刻
               --------------------- */
            DetailGUI.ChatAction.SET_END_TIME -> {
                if (msg.isBlank()) return
                group ?: return
                if (msg == "0") {
                    group.endTime = null
                } else {
                    group.endTime = TimeParser.parse(msg) ?: return
                }
                dm.save()
                Bukkit.getScheduler().runTask(SocialVotes.instance,
                    Runnable {
                        SignDisplayUtil.updateGroup(group)
                        p.sendMessage("§a時刻設定を更新しました。")
                    }
                )
            }
        }
        cancel(p.uniqueId)
    }
}
