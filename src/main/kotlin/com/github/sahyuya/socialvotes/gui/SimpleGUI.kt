package com.github.sahyuya.socialvotes.gui

import com.github.sahyuya.socialvotes.SocialVotes
import com.github.sahyuya.socialvotes.data.SVSign
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.text.SimpleDateFormat
import java.util.*

object SimpleGUI {

    // 追加：GUIを開いたプレイヤーごとに signID を保持する
    private val signViewMap: MutableMap<UUID, Int> = mutableMapOf()

    private fun item(material: Material, name: String, lore: List<String> = listOf()): ItemStack {
        val it = ItemStack(material)
        val meta = it.itemMeta!!
        meta.setDisplayName(name)
        meta.lore = lore
        it.itemMeta = meta
        return it
    }

    fun open(p: Player, sign: SVSign) {

        // ここで signID を保存する
        signViewMap[p.uniqueId] = sign.id

        val inv: Inventory = Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.HOPPER, "Simple Setting")

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")

        // slot0: 看板情報
        inv.setItem(
            0, item(
                Material.OAK_SIGN,
                "§a看板情報",
                listOf(
                    "§f名前: §e${sign.name}",
                    "§f制作者: §b${sign.creator}",
                    "§f作成日: §7${dateFormat.format(Date(sign.createdAt))}"
                )
            )
        )

        val dm = SocialVotes.dataManager

        // slot1: グループ情報
        val group = sign.group?.let { dm.groupByName[it] }
        val groupLore = mutableListOf<String>()

        if (group != null) {
            groupLore.add("§f所属グループ: §a${group.name}")

            val pv = dm.playerVotes[group.name]?.get(p.uniqueId) ?: 0
            val max = group.maxVotesPerPlayer
            val remain = if (max <= 0) "∞" else "${max - pv}"

            groupLore.add("§fあなたの残り票: §e$remain")

            val start = group.startTime
            val end = group.endTime

            groupLore.add("§f期間:")
            if (start == null) groupLore.add(" §7開始未設定（常時可）")
            else groupLore.add(" §7開始: §e${dateFormat.format(Date(start))}")

            if (end == null) groupLore.add(" §7終了未設定（常時可）")
            else groupLore.add(" §7終了: §e${dateFormat.format(Date(end))}")

        } else {
            groupLore.add("§cグループ未所属")
        }

        inv.setItem(1, item(Material.PAPER, "§bグループ情報", groupLore))

        // slot2: 自分の個別投票リセット
        inv.setItem(
            2,
            item(
                Material.REDSTONE,
                "§c個別投票リセット",
                listOf("§7この看板に対する自分の投票数を0に戻す")
            )
        )

        // slot3: グループ投票リセット
        inv.setItem(
            3,
            item(
                Material.GUNPOWDER,
                "§cグループ投票リセット",
                listOf("§7所属グループの自分の票を0に戻す")
            )
        )

        // slot4: 詳細設定
        val canEdit = p.isOp || sign.creator.equals(p.name, ignoreCase = true)
        val lore4 = if (canEdit) listOf("§eクリックで詳細設定へ") else listOf("§c権限がありません")

        inv.setItem(
            4,
            item(
                Material.COMPARATOR,
                "§6詳細設定",
                lore4
            )
        )

        p.openInventory(inv)
    }

    // 追加：InventoryClickEvent で使用する Sign 取得
    fun getViewingSign(player: Player): SVSign? {
        val id = signViewMap[player.uniqueId] ?: return null
        return SocialVotes.dataManager.signById[id]
    }

    fun onClick(p: Player, slot: Int) {

        val sign = getViewingSign(p) ?: return
        val dm = SocialVotes.dataManager
        val uuid = p.uniqueId

        when (slot) {

            // ▼ 個別投票リセット (slot 2)
            2 -> {
                val map = dm.playerVotesPerSign[sign.id] ?: mutableMapOf()
                val used = map.getOrDefault(uuid, 0)

                if (used > 0) {
                    // 看板の総得票数からプレイヤーの使用票を減算
                    sign.votes = (sign.votes - used).coerceAtLeast(0)
                }

                // プレイヤーの投票数を0に
                map[uuid] = 0
                dm.playerVotesPerSign[sign.id] = map

                dm.save()
                updateSignDisplay(sign)
                p.sendMessage("§a看板 ${sign.id} のあなたの個別投票をリセットしました。")
            }

            // ▼ グループ投票リセット (slot 3)
            3 -> {
                val gName = sign.group ?: return
                val group = dm.groupByName[gName] ?: return

                val gmap = dm.playerVotes[gName] ?: mutableMapOf()
                val usedGroup = gmap.getOrDefault(uuid, 0)

                if (usedGroup > 0) {

                    // グループ内のすべての看板ごとにプレイヤーの票を減算
                    for (sid in group.signIds) {
                        val s = dm.signById[sid] ?: continue
                        val perSignMap = dm.playerVotesPerSign[sid] ?: continue
                        val usedOnSign = perSignMap.getOrDefault(uuid, 0)

                        if (usedOnSign > 0) {
                            s.votes = (s.votes - usedOnSign).coerceAtLeast(0)
                            perSignMap[uuid] = 0
                            dm.playerVotesPerSign[sid] = perSignMap
                        }
                    }
                }

                // グループ全体のプレイヤー票を0へ
                gmap[uuid] = 0
                dm.playerVotes[gName] = gmap

                dm.save()
                updateSignDisplay(sign)
                p.sendMessage("§aグループ '$gName' のあなたの票をリセットしました。")
            }

            // ▼ 詳細設定 (slot 4)
            4 -> {
                val canEdit = p.isOp || sign.creator.equals(p.name, true)
                if (!canEdit) {
                    p.sendMessage("§c権限がありません。")
                    return
                }
                p.sendMessage("§e詳細設定GUIは現在準備中です。")
            }
        }
    }
    private fun updateSignDisplay(sign: SVSign) {
        val world = Bukkit.getWorld(sign.world) ?: return
        val block = world.getBlockAt(sign.x, sign.y, sign.z)

        val state = block.state
        if (state !is org.bukkit.block.Sign) return

        com.github.sahyuya.socialvotes.util.SignDisplayUtil.applyFormat(state, sign)
        state.update(true)
    }
}
