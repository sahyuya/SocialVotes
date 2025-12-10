package com.github.sahyuya.socialvotes.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import com.github.sahyuya.socialvotes.SocialVotes

class ProtectListener : Listener {
    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val loc = e.block.location
        val dm = SocialVotes.dataManager
        val sign = dm.getSignAt(loc)
        if (sign != null) {
            // Only allow OP or admin permission to break
            val p = e.player
            if (!p.isOp && !p.hasPermission("socialvotes.admin")) {
                e.isCancelled = true
                p.sendMessage("You cannot break an SV sign. Use /sv delhere (OP) or use the plugin's delete function.")
            } else {
                // OP breaking will remove data too
                dm.removeSignById(sign.id)
            }
        }
    }
}
