package org.pl.lightDarkWorld.manager

import org.bukkit.entity.Player
import org.pl.lightDarkWorld.model.EnchantSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SessionManager {

    private val sessions = ConcurrentHashMap<UUID, EnchantSession>()

    fun get(player: Player): EnchantSession {
        return sessions.computeIfAbsent(player.uniqueId) {
            EnchantSession()
        }
    }

    fun remove(player: Player) {
        sessions.remove(player.uniqueId)
    }

    fun has(player: Player): Boolean {
        return sessions.containsKey(player.uniqueId)
    }
}