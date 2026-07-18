package org.pl.lightDarkWorld.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 월드 전체 gamerule을 건드리지 않고,
 * 비콘 효과 범위 안에 있는(=비콘발 포션 효과를 현재 보유한) 플레이어에 한해
 * 사망 시 인벤토리를 유지시켜줍니다.
 */
class BeaconKeepInventoryListener : Listener {

    // 플레이어별로 "비콘이 원인"인 현재 활성 효과 종류 추적
    private val beaconEffects = ConcurrentHashMap<UUID, MutableSet<PotionEffectType>>()

    @EventHandler
    fun onPotionEffect(event: EntityPotionEffectEvent) {
        val player = event.entity as? Player ?: return

        when (event.action) {
            EntityPotionEffectEvent.Action.ADDED,
            EntityPotionEffectEvent.Action.CHANGED -> {
                val type = event.modifiedType ?: return
                if (event.cause == EntityPotionEffectEvent.Cause.BEACON) {
                    beaconEffects.getOrPut(player.uniqueId) { mutableSetOf() }.add(type)
                }
            }

            EntityPotionEffectEvent.Action.REMOVED -> {
                val type = event.modifiedType ?: return
                beaconEffects[player.uniqueId]?.remove(type)
            }

            EntityPotionEffectEvent.Action.CLEARED -> {
                beaconEffects.remove(player.uniqueId)
            }

            else -> {}
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val effects = beaconEffects[event.entity.uniqueId]
        if (!effects.isNullOrEmpty()) {
            event.setKeepInventory(true)
            event.drops.clear()
        }
    }

    // 메모리 누수 방지 (쿨다운 맵과 같은 패턴)
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        beaconEffects.remove(event.player.uniqueId)
    }
}