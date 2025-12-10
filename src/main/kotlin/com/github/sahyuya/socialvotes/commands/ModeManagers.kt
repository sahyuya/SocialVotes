package com.github.sahyuya.socialvotes.commands

import java.util.*
import kotlin.collections.HashMap

object AddModeManager {
    private val watching = HashMap<UUID, String>() // uuid -> groupName
    fun watchPlayerForAdd(uuid: UUID, group: String) {
        watching[uuid] = group
    }
    fun getGroupIfWatching(uuid: UUID): String? = watching[uuid]
    fun cancel(uuid: UUID) { watching.remove(uuid) }
}

object RemoveModeManager {
    private val watching = HashSet<UUID>()
    fun watchPlayerForRemove(uuid: UUID) { watching.add(uuid) }
    fun isWatching(uuid: UUID): Boolean = watching.contains(uuid)
    fun cancel(uuid: UUID) { watching.remove(uuid) }
}
