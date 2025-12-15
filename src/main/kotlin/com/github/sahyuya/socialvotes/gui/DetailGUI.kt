package com.github.sahyuya.socialvotes.gui

import com.github.sahyuya.socialvotes.ChatInput
import com.github.sahyuya.socialvotes.SocialVotes
import com.github.sahyuya.socialvotes.data.SVSign
import com.github.sahyuya.socialvotes.util.SignDisplayUtil
import com.github.sahyuya.socialvotes.util.TimeUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object DetailGUI {
    private val signViewMap: MutableMap<UUID, Int> = mutableMapOf()

    enum class ChatAction {
        RENAME_SIGN,
        SET_SIGN_MAX,
        SET_GROUP_MAX,
        SET_START_TIME,
        SET_END_TIME
    }

    private fun item(mat: Material, name: String, lore: List<String> = listOf()): ItemStack {
        val it = ItemStack(mat)
        val meta = it.itemMeta!!
        meta.setDisplayName(name)
        if (lore.isNotEmpty()) meta.lore = lore
        it.itemMeta = meta
        return it
    }

    fun open(p: Player, sign: SVSign) {
        signViewMap[p.uniqueId] = sign.id

        val inv: Inventory = Bukkit.createInventory(null, 27, "SV 詳細設定")

        // 戻る・遷移
        inv.setItem(0, item(Material.ARROW, "§aシンプルGUIに戻る"))
        inv.setItem(18, item(Material.BOOK, "§b投票結果GUIへ"))

        // 情報
        val signStatus = if (sign.showVotes) "§a公開" else "§c非公開"
        inv.setItem(2, item(
            Material.OAK_SIGN,
            "§a看板情報",
            listOf(
                "§7名前: §f${sign.name}",
                "§7ID: §f${sign.id}",
                "§7得票表示: $signStatus"
            )
        ))

        val group = sign.group?.let { SocialVotes.dataManager.groupByName[it] }
        inv.setItem(11,
            if (group != null) {
                val groupStatus = if (group.showVotesGroup) "§a公開" else "§c非公開"
                item(
                    Material.OAK_HANGING_SIGN,
                    "§bグループ情報",
                    buildList {
                        add("§7グループ名: §f${group.name}")
                        add("§7得票表示: $groupStatus")
                        add("§7看板上限: §e${sign.maxVotesPerSign}")
                        add("§7グループ上限: §e${group.maxVotesPerPlayer}")
                        add("§7期間:")
                        addAll(TimeUtil.formatPeriod(group.startTime, group.endTime))
                    }
                )
            } else {
                item(Material.PAPER, "§bグループ情報", listOf("§cグループ未所属"))
            }
        )

        // 公開切替
        inv.setItem(4, item(Material.ITEM_FRAME, "§e看板 得票公開切替"))
        inv.setItem(13, item(Material.GLOW_ITEM_FRAME, "§eグループ 得票公開切替"))

        // 設定
        inv.setItem(5, item(Material.NAME_TAG, "§6看板名変更"))
        inv.setItem(3, item(Material.PAPER, "§6看板 最大投票数"))
        inv.setItem(12, item(Material.MAP, "§6グループ 最大投票数"))
        inv.setItem(21, item(Material.CLOCK, "§6開始時刻設定"))
        inv.setItem(22, item(Material.COMPASS, "§6終了時刻設定"))

        // リセット
        inv.setItem(7, item(Material.REDSTONE, "§c全プレイヤー 看板得票リセット"))
        inv.setItem(8, item(Material.REDSTONE_BLOCK, "§c全プレイヤー グループ得票リセット"))

        // 削除
        inv.setItem(25, item(Material.TNT_MINECART, "§4看板削除"))
        inv.setItem(26, item(Material.TNT, "§4グループ完全削除"))

        // 装飾
        val white = item(Material.WHITE_STAINED_GLASS_PANE, " ")
        val gray = item(Material.GRAY_STAINED_GLASS_PANE, " ")

        listOf(1,6,9,10,15,16,17,19,24).forEach { inv.setItem(it, white) }
        listOf(14,20,23).forEach { inv.setItem(it, gray) }

        p.openInventory(inv)
    }

    fun onClick(p: Player, slot: Int) {
        val signId = signViewMap[p.uniqueId] ?: return
        val dm = SocialVotes.dataManager
        val sign = dm.signById[signId] ?: return
        val group = sign.group?.let { dm.groupByName[it] }

        when (slot) {

            0 -> SimpleGUI.open(p, sign)
            18 -> ResultGUI.open(p, sign)

            4 -> {
                sign.showVotes = !sign.showVotes
                dm.save()
                SignDisplayUtil.updateSingle(sign)
                open(p, sign)
                p.sendMessage("§a看板の得票公開を切り替えました。")
            }
            13 -> {
                if (group == null) {
                    p.sendMessage("§cグループ未所属です。")
                    return
                }
                // グループ公開状態を反転
                val newState = !group.showVotesGroup
                group.showVotesGroup = newState
                // 所属する全看板へ同期
                for (sid in group.signIds) {
                    val s = dm.signById[sid] ?: continue
                    s.showVotes = newState
                }
                dm.save()
                SignDisplayUtil.updateGroup(group)
                open(p, sign)
                p.sendMessage("§aグループの得票公開を切り替えました。")
            }

            5 -> startChat(p, signId, ChatAction.RENAME_SIGN,
                "新しい看板名を入力してください（空白で変更なし）")

            3 -> startChat(p, signId, ChatAction.SET_SIGN_MAX,
                "最大投票数を入力してください（空白=変更なし / 0=制限なし）")

            12 -> startChat(p, signId, ChatAction.SET_GROUP_MAX,
                "最大投票数を入力してください（空白=変更なし / 0=制限なし）")

            21 -> startChat(p, signId, ChatAction.SET_START_TIME,
                "開始時刻を入力してください\n0=解除 / 空白=変更なし")

            22 -> startChat(p, signId, ChatAction.SET_END_TIME,
                "終了時刻を入力してください\n0=解除 / 空白=変更なし")

            7 -> {
                // ① この看板の全投票を合計
                val perSignMap = dm.playerVotesPerSign[sign.id] ?: mutableMapOf()
                val totalRemoved = perSignMap.values.sum()

                // ② 看板の合計票を減算
                sign.votes = (sign.votes - totalRemoved).coerceAtLeast(0)

                // ③ 各プレイヤーのグループ票も減算
                sign.group?.let { gName ->
                    val gmap = dm.playerVotes[gName] ?: mutableMapOf()
                    perSignMap.forEach { (uuid, count) ->
                        if (count > 0) {
                            gmap[uuid] = (gmap.getOrDefault(uuid, 0) - count).coerceAtLeast(0)
                        }
                    }
                    dm.playerVotes[gName] = gmap
                }

                // ④ この看板の投票履歴を完全削除
                dm.playerVotesPerSign.remove(sign.id)
                dm.save()

                // ⑤ 表示更新
                updateSignDisplay(sign)
                p.sendMessage("§a看板 ${sign.id} の全プレイヤー投票をリセットしました。")
            }
            8 -> {
                if (group == null) return
                val gName = sign.group ?: return
                val group = dm.groupByName[gName] ?: return
                // ① グループ内すべての看板を処理
                for (sid in group.signIds) {
                    val s = dm.signById[sid] ?: continue
                    val perSignMap = dm.playerVotesPerSign[sid] ?: continue
                    // 全投票数を合計
                    val removed = perSignMap.values.sum()
                    // 看板合計票を減算
                    s.votes = (s.votes - removed).coerceAtLeast(0)
                    // 投票履歴を削除
                    dm.playerVotesPerSign.remove(sid)
                    // 表示更新
                    updateSignDisplay(s)
                }
                // ② グループ全体のプレイヤー票を全削除
                dm.playerVotes.remove(gName)
                dm.save()
                p.sendMessage("§aグループ '$gName' の全プレイヤー投票をリセットしました。")
            }

            25 -> {
                dm.removeSignById(sign.id)
                p.closeInventory()
                p.sendMessage("§c看板を削除しました。")
            }
            26 -> {
                if (group == null) return
                group.signIds.toList().forEach {
                    dm.removeSignById(it)
                }
                dm.groupByName.remove(group.name)
                dm.save()
                p.closeInventory()
                p.sendMessage("§4グループを完全削除しました。")
            }
        }
    }
    private fun startChat(p: Player, signId: Int, action: ChatAction, msg: String) {
        p.closeInventory()
        ChatInput.start(p.uniqueId, ChatInput.InputState(action, signId))
        msg.lines().forEach { p.sendMessage("§e$it") }
    }

    private fun updateSignDisplay(sign: SVSign) {
        val world = Bukkit.getWorld(sign.world) ?: return
        val block = world.getBlockAt(sign.x, sign.y, sign.z)

        val state = block.state
        if (state !is org.bukkit.block.Sign) return

        SignDisplayUtil.applyFormat(state, sign)
        state.update(true)
    }
}
