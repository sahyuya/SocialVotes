package com.github.sahyuya.socialvotes.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

/**
 * 雛形：インタラクティブにプレイヤーからチャット入力を受け取る場合、
 * 他のクラスが Watcher に UUID を登録して、このリスナーで取得して処理を委譲します。
 *
 * 実装例: DetailGUI がチャット入力待ち状態を登録しておく -> ここで受け取り -> GUI に反映
 */
object ChatInputAwaitManager {
    private val awaiting = mutableMapOf<java.util.UUID, (String) -> Unit>()
    fun await(uuid: java.util.UUID, callback: (String) -> Unit) {
        awaiting[uuid] = callback
    }
    fun cancel(uuid: java.util.UUID) { awaiting.remove(uuid) }
    fun handle(uuid: java.util.UUID, message: String): Boolean {
        val cb = awaiting.remove(uuid) ?: return false
        try {
            cb(message)
            return true
        } catch (ex: Exception) {
            return false
        }
    }
}

class ChatInputListener : Listener {
    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        val p = e.player
        val msg = e.message
        if (ChatInputAwaitManager.handle(p.uniqueId, msg)) {
            e.isCancelled = true
        }
    }
}
