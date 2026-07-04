package org.pl.lightDarkWorld.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * 디버그용 데미지 체크 리스너
 */
class DamageCheckListener : Listener {

    @EventHandler
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return

        attacker.sendMessage("§6[데미지] §f${event.damage}")
    }
}
