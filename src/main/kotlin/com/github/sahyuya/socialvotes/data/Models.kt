package com.github.sahyuya.socialvotes.data

import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID

data class SVSign(
    var id: Int,
    var world: String,
    var x: Int,
    var y: Int,
    var z: Int,
    var name: String,
    var creator: String,
    var votes: Int = 0,
    var showVotes: Boolean = true,
    var group: String? = null,
    var maxVotesPerSign: Int = 1,
    var createdAt: Long = System.currentTimeMillis()
) {
    fun toLocation(): Location? {
        val w = Bukkit.getWorld(world) ?: return null
        return Location(w, x.toDouble(), y.toDouble(), z.toDouble())
    }
}

data class SVGroup(
    var name: String,
    var signIds: MutableList<Int> = mutableListOf(),
    var maxVotesPerPlayer: Int = 1,
    var showVotesGroup: Boolean = true,
    var sortMode: String = "id",
    var startTime: Long? = null,
    var endTime: Long? = null,
    var owner: UUID
)
