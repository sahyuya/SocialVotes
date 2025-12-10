package com.github.sahyuya.socialvotes.util

import com.github.sahyuya.socialvotes.SocialVotes
import com.github.sahyuya.socialvotes.data.SVGroup
import com.github.sahyuya.socialvotes.data.SVSign
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.block.Sign

object SignDisplayUtil {

    fun applyFormat(sign: Sign, sv: SVSign) {
        val dm = SocialVotes.dataManager

        val title = Component.text("(")
            .color(NamedTextColor.DARK_GRAY)
            .append(Component.text("Social", NamedTextColor.DARK_AQUA))
            .append(Component.text("Votes", NamedTextColor.GRAY))
            .append(Component.text(")", NamedTextColor.DARK_GRAY))

        val line1 = title
        val line2 = Component.text(sv.name, NamedTextColor.GREEN)
        val line3 = Component.text(sv.creator, NamedTextColor.WHITE)

        val groupHidden = sv.group
            ?.let { dm.groupByName[it] }
            ?.let { !it.showVotesGroup }
            ?: false

        val isVisible = !groupHidden && sv.showVotes

        val line4 =
            if (isVisible)
                Component.text("Votes: ", NamedTextColor.GRAY)
                    .append(Component.text("${sv.votes}", NamedTextColor.GOLD))
            else
                Component.text("Votes: 非公開", NamedTextColor.RED)

        sign.line(0, line1)
        sign.line(1, line2)
        sign.line(2, line3)
        sign.line(3, line4)
    }

    // 単体看板を更新
    fun updateSingle(sign: SVSign) {
        val world = Bukkit.getWorld(sign.world) ?: return
        val block = world.getBlockAt(sign.x, sign.y, sign.z)
        val state = block.state as? Sign ?: return

        applyFormat(state, sign)
        state.update(true)
    }

    // グループ内すべての看板を更新
    fun updateGroup(group: SVGroup) {
        val dm = SocialVotes.dataManager
        for (sid in group.signIds) {
            val sign = dm.signById[sid] ?: continue
            updateSingle(sign)
        }
    }
}
