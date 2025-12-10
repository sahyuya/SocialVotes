package com.github.sahyuya.socialvotes.util

import com.github.sahyuya.socialvotes.data.SVSign
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.block.Sign

object SignDisplayUtil {

    fun applyFormat(sign: Sign, sv: SVSign) {
        val title = Component.text("(")
            .color(NamedTextColor.DARK_GRAY)
            .append(Component.text("Social", NamedTextColor.DARK_AQUA))
            .append(Component.text("Votes", NamedTextColor.GRAY))
            .append(Component.text(")", NamedTextColor.DARK_GRAY))

        val line1 = title
        val line2 = Component.text(sv.name, NamedTextColor.GREEN)
        val line3 = Component.text(sv.creator, NamedTextColor.WHITE)

        val line4 =
            if (sv.showVotes)
                Component.text("Votes: ", NamedTextColor.GRAY)
                    .append(Component.text("${sv.votes}", NamedTextColor.GOLD))
            else
                Component.text("Votes: 非公開", NamedTextColor.RED)

        sign.line(0, line1)
        sign.line(1, line2)
        sign.line(2, line3)
        sign.line(3, line4)
    }
}
