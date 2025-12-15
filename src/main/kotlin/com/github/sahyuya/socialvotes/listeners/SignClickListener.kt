package com.github.sahyuya.socialvotes.listeners

import com.github.sahyuya.socialvotes.SocialVotes
import com.github.sahyuya.socialvotes.commands.AddModeManager
import com.github.sahyuya.socialvotes.commands.RemoveModeManager
import com.github.sahyuya.socialvotes.commands.UpdateModeManager
import com.github.sahyuya.socialvotes.gui.*
import com.github.sahyuya.socialvotes.util.SignDisplayUtil
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent

class SignClickListener : Listener {

    @EventHandler
    fun onClick(e: PlayerInteractEvent) {

        if (e.action != Action.RIGHT_CLICK_BLOCK) return

        val p = e.player
        val block = e.clickedBlock ?: return
        val state = block.state as? Sign ?: return
        val dm = SocialVotes.dataManager

        // ==================================================
        // Add モード
        // ==================================================
        AddModeManager.getGroupIfWatching(p.uniqueId)?.let { groupName ->
            AddModeManager.cancel(p.uniqueId)

            val signId = dm.readSignIdFromBlock(state)
            if (signId == null) {
                p.sendMessage("SV看板ではありません。")
                return
            }

            val sign = dm.signById[signId] ?: run {
                p.sendMessage("SV看板データが存在しません。")
                return
            }

            if (sign.group != null) {
                p.sendMessage("この看板は既にグループに所属しています。")
                return
            }

            val group = dm.groupByName[groupName] ?: run {
                p.sendMessage("そのグループは存在しません。")
                return
            }

            if (group.signIds.size >= 45) {
                p.sendMessage("45個を超えるため追加できません。")
                return
            }

            group.signIds.add(sign.id)
            sign.group = groupName
            dm.save()

            p.sendMessage("ID:${sign.id} をグループ $groupName に追加しました。")
            SignDisplayUtil.applyFormat(state, sign)
            state.update(true)
            return
        }

        // ==================================================
        // Remove モード
        // ==================================================
        if (RemoveModeManager.isWatching(p.uniqueId)) {
            RemoveModeManager.cancel(p.uniqueId)

            val signId = dm.readSignIdFromBlock(state)
            if (signId == null) {
                p.sendMessage("SV看板ではありません。")
                return
            }

            val sign = dm.signById[signId] ?: run {
                p.sendMessage("SV看板データが存在しません。")
                return
            }

            val groupName = sign.group ?: run {
                p.sendMessage("この看板はグループに所属していません。")
                return
            }

            val group = dm.groupByName[groupName] ?: return

            // 権限チェック
            val isOp = p.isOp
            val isCreator = group.owner == p.uniqueId

            if (!isOp && !isCreator) {
                p.sendMessage("この操作はグループ作成者またはOPのみ可能です。")
                return
            }

            sign.group = null
            group.signIds.remove(sign.id)
            dm.save()

            p.sendMessage("ID:${sign.id} をグループから除外しました。")
            SignDisplayUtil.applyFormat(state, sign)
            state.update(true)
            return
        }

        // ==================================================
        // Update モード
        // ==================================================
        if (UpdateModeManager.isWatching(p.uniqueId)) {
            UpdateModeManager.cancel(p.uniqueId)

            val signId = dm.readSignIdFromBlock(state)
            if (signId == null) {
                p.sendMessage("SV看板ではありません。")
                return
            }

            val svSign = dm.signById[signId] ?: return

            // 旧実体削除
            svSign.toLocation()?.block?.type = Material.AIR

            // 座標更新
            dm.updateSignLocation(signId, block.location)

            // 新看板にID再付与
            dm.writeSignIdToBlock(state, signId)

            p.sendMessage("SV看板(ID:$signId) の座標を更新しました。")

            SignDisplayUtil.applyFormat(state, svSign)
            state.update(true)
            return
        }

        // ==================================================
        // 通常動作
        // ==================================================
        val signId = dm.readSignIdFromBlock(state) ?: return
        val sign = dm.signById[signId] ?: return

        // Shift + 右クリック → GUI
        if (p.isSneaking) {
            SimpleGUI.open(p, sign)
            return
        }

        // ----------------------------
        // 投票処理
        // ----------------------------
        val uuid = p.uniqueId
        val now = System.currentTimeMillis()

        sign.group?.let { gName ->
            val g = dm.groupByName[gName]
            g?.startTime?.let { if (now < it) { p.sendMessage("投票期間外（開始前）。"); return } }
            g?.endTime?.let { if (now >= it) { p.sendMessage("投票期間外（終了）。"); return } }
        }

        val perSignMap = dm.playerVotesPerSign.computeIfAbsent(sign.id) { mutableMapOf() }
        val usedOnSign = perSignMap.getOrDefault(uuid, 0)

        if (sign.maxVotesPerSign != 0 && usedOnSign >= sign.maxVotesPerSign) {
            p.sendMessage("この看板への投票上限に到達しています。")
            return
        }

        sign.group?.let { gName ->
            val g = dm.groupByName[gName]
            val gmap = dm.playerVotes.computeIfAbsent(gName) { mutableMapOf() }
            val usedInGroup = gmap.getOrDefault(uuid, 0)

            if (g != null && g.maxVotesPerPlayer != 0 && usedInGroup >= g.maxVotesPerPlayer) {
                p.sendMessage("このグループでの投票上限に到達しています。")
                return
            }

            gmap[uuid] = usedInGroup + 1
        }

        perSignMap[uuid] = usedOnSign + 1
        sign.votes++

        dm.save()

        p.sendMessage("ID:${sign.id} に投票しました。現在票数：${sign.votes}")
        SignDisplayUtil.applyFormat(state, sign)
        state.update(true)
    }

    // ==================================================
    // GUI / Chat
    // ==================================================
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val p = e.whoClicked as? Player ?: return
        val top = e.view.topInventory

        when (e.view.title) {
            "Simple Setting" -> { if (e.rawSlot < top.size) { e.isCancelled = true; SimpleGUI.onClick(p, e.rawSlot) } }
            "SV 詳細設定" -> { if (e.rawSlot < top.size) { e.isCancelled = true; DetailGUI.onClick(p, e.rawSlot) } }
            "Vote Results" -> { e.isCancelled = true; ResultGUI.onClick(p, e.rawSlot) }
            "Voters" -> { e.isCancelled = true; VoterListGUI.onClick(p, e.rawSlot) }
        }
    }
}
