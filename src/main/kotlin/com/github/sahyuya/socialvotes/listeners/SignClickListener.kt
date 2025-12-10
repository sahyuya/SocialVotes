package com.github.sahyuya.socialvotes.listeners

import com.github.sahyuya.socialvotes.SocialVotes
import com.github.sahyuya.socialvotes.commands.AddModeManager
import com.github.sahyuya.socialvotes.commands.RemoveModeManager
import com.github.sahyuya.socialvotes.commands.UpdateModeManager
import com.github.sahyuya.socialvotes.gui.DetailGUI
import com.github.sahyuya.socialvotes.gui.ResultGUI
import com.github.sahyuya.socialvotes.gui.SimpleGUI
import com.github.sahyuya.socialvotes.gui.VoterListGUI
import com.github.sahyuya.socialvotes.util.SignDisplayUtil
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent

class SignClickListener : Listener {

    @EventHandler
    fun onClick(e: PlayerInteractEvent) {

        if (e.action != Action.RIGHT_CLICK_BLOCK) return

        val p = e.player
        val block = e.clickedBlock ?: return
        val state = block.state
        if (state !is Sign) return

        val loc = block.location
        val dm = SocialVotes.dataManager

        // ----------------------------
        // Add モード
        // ----------------------------
        AddModeManager.getGroupIfWatching(p.uniqueId)?.let { groupName ->
            AddModeManager.cancel(p.uniqueId)

            val sign = dm.getSignAt(loc)
            if (sign == null) {
                p.sendMessage("SV看板ではありません。")
                return
            }

            val group = dm.groupByName[groupName]
            if (group == null) {
                p.sendMessage("そのグループは存在しません。")
                return
            }

            if (group.signIds.size >= 45) {
                p.sendMessage("45個を超えるため追加できません。")
                return
            }

            if (!group.signIds.contains(sign.id)) {
                group.signIds.add(sign.id)
                sign.group = groupName
                dm.save()
                p.sendMessage("ID:${sign.id} をグループ $groupName に追加しました。")

                SignDisplayUtil.applyFormat(state, sign)
                state.update(true)
            }
            return
        }

        // ----------------------------
        // Remove モード
        // ----------------------------
        if (RemoveModeManager.isWatching(p.uniqueId)) {
            RemoveModeManager.cancel(p.uniqueId)

            val sign = dm.getSignAt(loc)
            if (sign == null) {
                p.sendMessage("SV看板ではありません。")
                return
            }

            val old = sign.group
            sign.group = null
            dm.groupByName[old]?.signIds?.remove(sign.id)
            dm.save()

            p.sendMessage("ID:${sign.id} をグループから除外しました。")

            SignDisplayUtil.applyFormat(state, sign)
            state.update(true)
            return
        }

        // ----------------------------
        // Update モード
        // ----------------------------
        if (UpdateModeManager.isWatching(p.uniqueId)) {
            UpdateModeManager.cancel(p.uniqueId)

            val sign = dm.getSignAt(loc)
            if (sign == null) {
                p.sendMessage("SV看板が存在しません。")
                return
            }

            dm.updateSignLocation(sign.id, loc)
            p.sendMessage("SV看板の座標を更新しました。")

            SignDisplayUtil.applyFormat(state, sign)
            state.update(true)
            return
        }

        // ----------------------------
        // 通常動作：ここから
        // ----------------------------

        val sign = dm.getSignAt(loc) ?: return

        // ----------------------------
        // Shift + 右クリック → GUI
        // ----------------------------
        if (p.isSneaking) {
            SimpleGUI.open(p, sign)  // ← 正しい sign を渡す
            return
        }

        // ----------------------------
        // 通常投票処理
        // ----------------------------
        val uuid = p.uniqueId
        val now = System.currentTimeMillis()

        val groupName = sign.group
        if (groupName != null) {
            val group = dm.groupByName[groupName]
            if (group != null) {
                group.startTime?.let { if (now < it) { p.sendMessage("投票期間外（開始前）。"); return } }
                group.endTime?.let { if (now >= it) { p.sendMessage("投票期間外（終了）。"); return } }
            }
        }

        val perSignMap = dm.playerVotesPerSign.computeIfAbsent(sign.id) { mutableMapOf() }
        val usedOnSign = perSignMap.getOrDefault(uuid, 0)

        if (sign.maxVotesPerSign != 0 && usedOnSign >= sign.maxVotesPerSign) {
            p.sendMessage("この看板への投票上限に到達しています。")
            return
        }

        if (groupName != null) {
            val g = dm.groupByName[groupName]
            if (g != null) {
                val gmap = dm.playerVotes.computeIfAbsent(groupName) { mutableMapOf() }
                val usedInGroup = gmap.getOrDefault(uuid, 0)
                val gmax = g.maxVotesPerPlayer

                if (gmax != 0 && usedInGroup >= gmax) {
                    p.sendMessage("このグループでの投票上限に到達しています。")
                    return
                }

                perSignMap[uuid] = usedOnSign + 1
                gmap[uuid] = usedInGroup + 1
            }
        } else {
            perSignMap[uuid] = usedOnSign + 1
        }

        sign.votes += 1
        dm.save()

        p.sendMessage("ID:${sign.id} に投票しました。現在票数：${sign.votes}")

        SignDisplayUtil.applyFormat(state, sign)
        state.update(true)
    }


    /**
     * ----------------------------
     * GUI クリック処理
     * ----------------------------
     */
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val p = e.whoClicked as? Player ?: return
        val top = e.view.topInventory

        when (e.view.title) {

            "Simple Setting" -> {
                if (e.rawSlot >= top.size) return
                e.isCancelled = true
                SimpleGUI.onClick(p, e.rawSlot)
            }

            "SV 詳細設定" -> {
                if (e.rawSlot >= top.size) return
                e.isCancelled = true
                DetailGUI.onClick(p, e.rawSlot)
            }

            "Vote Results" -> {
                e.isCancelled = true
                ResultGUI.onClick(p, e.rawSlot)
            }

            "Voters" -> {
                e.isCancelled = true
                VoterListGUI.onClick(p, e.rawSlot)
            }
        }
    }
}
