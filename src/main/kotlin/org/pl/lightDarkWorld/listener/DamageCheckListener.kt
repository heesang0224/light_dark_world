package org.pl.lightDarkWorld.listener

import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * 디버그용 데미지 체크 리스너
 */
class DamageCheckListener : Listener {

    @EventHandler
    fun onAttack(event: EntityDamageByEntityEvent) {
        // 근접 공격: damager가 Player 본인
        // 원거리 공격(화살 등): damager가 Projectile이고, 그 shooter가 Player
        val attacker = event.damager as? Player
            ?: (event.damager as? Projectile)?.shooter as? Player
            ?: return

        attacker.sendMessage("§6[데미지] §f${event.damage}")
    }
}