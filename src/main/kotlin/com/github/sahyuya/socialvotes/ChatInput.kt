package com.github.sahyuya.socialvotes

import com.github.sahyuya.socialvotes.util.SignDisplayUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.UUID

object ChatInput : Listener {

    enum class Mode {
        SIGN_NAME,
        SIGN_MAX_VOTE,
        GROUP_MAX_VOTE
    }

    data class InputState(
        val mode: Mode,
        val signId: Int
    )

    private val states = mutableMapOf<UUID, InputState>()

    /* =====================
       外部呼び出し用 API
       ===================== */

    fun start(uuid: UUID, state: InputState) {
        states[uuid] = state
    }

    fun cancel(uuid: UUID) {
        states.remove(uuid)
    }

    fun isInputting(uuid: UUID): Boolean =
        states.containsKey(uuid)

    /* =====================
       チャット入力処理
       ===================== */

    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        val p = e.player
        val state = states[p.uniqueId] ?: return

        e.isCancelled = true

        val dm = SocialVotes.dataManager
        val sign = dm.signById[state.signId]
        if (sign == null) {
            p.sendMessage("§c対象の看板が存在しません。")
            cancel(p.uniqueId)
            return
        }

        val msg = e.message.trim()

        when (state.mode) {

            /* ---------------------
               看板名変更
               --------------------- */
            Mode.SIGN_NAME -> {
                if (msg.isBlank()) {
                    p.sendMessage("§c空の名前は設定できません。")
                    return
                }

                sign.name = msg
                dm.save()

                SignDisplayUtil.updateSingle(sign)

                p.sendMessage("§a看板名を「$msg」に変更しました。")
            }

            /* ---------------------
               看板単体 最大投票数
               --------------------- */
            Mode.SIGN_MAX_VOTE -> {
                val v = msg.toIntOrNull()
                if (v == null || v < 0) {
                    p.sendMessage("§c0以上の整数を入力してください。")
                    return
                }

                sign.maxVotesPerSign = v
                dm.save()

                // 表示仕様上、票数表記に影響するため更新
                SignDisplayUtil.updateSingle(sign)

                p.sendMessage("§a看板単体の最大投票数を $v に変更しました。")
            }

            /* ---------------------
               グループ全体 最大投票数
               --------------------- */
            Mode.GROUP_MAX_VOTE -> {
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
                if (v == null || v < 0) {
                    p.sendMessage("§c0以上の整数を入力してください。")
                    return
                }

                group.maxVotesPerPlayer = v
                dm.save()

                // ★ グループ内すべての看板を更新
                SignDisplayUtil.updateGroup(group)

                p.sendMessage("§aグループ全体の最大投票数を $v に変更しました。")
            }
        }

        cancel(p.uniqueId)
    }
}
