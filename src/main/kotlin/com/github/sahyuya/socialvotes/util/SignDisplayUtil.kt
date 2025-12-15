package com.github.sahyuya.socialvotes.util

import com.github.sahyuya.socialvotes.SocialVotes
import com.github.sahyuya.socialvotes.data.SVGroup
import com.github.sahyuya.socialvotes.data.SVSign
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.block.Sign

object SignDisplayUtil {

    /* ==============================
     * 表示内容の構築（純ロジック）
     * ============================== */
    private fun buildLines(sv: SVSign): List<Component> {

        val title = Component.text("(")
            .color(NamedTextColor.DARK_GRAY)
            .append(Component.text("Social", NamedTextColor.AQUA))
            .append(Component.text("Votes", NamedTextColor.GRAY))
            .append(Component.text(")", NamedTextColor.DARK_GRAY))

        val isVisible = sv.showVotes

        val votesLine =
            if (isVisible)
                Component.text("Votes: ", NamedTextColor.YELLOW)
                    .append(Component.text("${sv.votes}", NamedTextColor.GOLD))
            else
                Component.text("Votes: 非公開", NamedTextColor.RED)

        return listOf(
            title,
            Component.text(sv.name, NamedTextColor.GREEN),
            Component.text(sv.creator, NamedTextColor.WHITE),
            votesLine
        )
    }

    /* ==============================
     * Bukkit Sign への反映
     * ============================== */
    fun applyFormat(sign: Sign, sv: SVSign) {
        val lines = buildLines(sv)
        for (i in 0..3) {
            sign.line(i, lines[i])
        }
    }

    /* ==============================
     * World 解決 + 更新
     * ============================== */
    fun updateSingle(sv: SVSign) {
        val world = Bukkit.getWorld(sv.world) ?: return
        val block = world.getBlockAt(sv.x, sv.y, sv.z)
        val state = block.state as? Sign ?: return

        applyFormat(state, sv)
        state.update(true)
    }

    fun updateGroup(group: SVGroup) {
        val dm = SocialVotes.dataManager
        for (sid in group.signIds) {
            dm.signById[sid]?.let { updateSingle(it) }
        }
    }
}
