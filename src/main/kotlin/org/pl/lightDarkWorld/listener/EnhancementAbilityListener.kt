package org.pl.lightDarkWorld.listener

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.pl.lightDarkWorld.RandomEnchantPlugin
import org.pl.lightDarkWorld.manager.EnhancementManager
import org.pl.lightDarkWorld.manager.EquipmentAttributeManager
import java.util.UUID
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.LivingEntity

/**
 * 10강 전용 액티브 스킬과, 바닐라 어트리뷰트로 표현이 안 되는
 * 활(원거리 데미지%) / 도끼(크리티컬 데미지%) 보정을 담당한다.
 */
class EnhancementAbilityListener : Listener {

    private val maceCooldown = mutableMapOf<UUID, Long>()

    private val bowLevelKey get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_bow_level")
    private val tridentBurstKey get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_trident_burst")

    // =========================
    // 검 10강: 피격 무적시간 무시 / 도끼: 크리티컬 데미지% 보정
    // =========================
    @EventHandler
    fun onMeleeHit(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val item = player.inventory.itemInMainHand
        val kind = EquipmentAttributeManager.kindOf(item.type)

        when (kind) {
            EquipmentAttributeManager.EquipmentKind.SWORD -> {
                if (EnhancementManager.getLevel(item) >= 10) {
                    val target = event.entity as? LivingEntity
                    if (target != null) {
                        Bukkit.getScheduler().runTask(RandomEnchantPlugin.instance, Runnable {
                            if (!target.isDead) target.noDamageTicks = 0
                        })
                    }
                }
            }

            EquipmentAttributeManager.EquipmentKind.AXE -> {
                val level = EnhancementManager.getLevel(item)
                if (level > 0 && isCriticalHit(player)) {
                    val settings = RandomEnchantPlugin.instance.configManager.settings
                    val percent = settings.getDouble("enhancement-attributes.axe.critical_damage_percent.$level", level * 5.0)
                    event.damage *= (1 + percent / 100.0)
                }
            }

            else -> {}
        }
    }

    /** 바닐라 크리티컬 판정 휴리스틱: 낙하 중 + 비행 중 아님 + 물 안에 없음 */
    private fun isCriticalHit(player: Player): Boolean {
        return player.fallDistance > 0f &&
                !player.isOnGround &&
                !player.isInWater &&
                !player.isGliding &&
                player.velocity.y < 0
    }

    // =========================
    // 활: 원거리 데미지% 보정 (모든 강화 레벨 적용)
    // =========================
    @EventHandler
    fun onShootBow(event: EntityShootBowEvent) {
        val bow = event.bow ?: return
        if (bow.type != Material.BOW) return
        val level = EnhancementManager.getLevel(bow)
        if (level <= 0) return
        event.projectile.persistentDataContainer.set(bowLevelKey, PersistentDataType.INTEGER, level)
    }

    @EventHandler
    fun onArrowHit(event: EntityDamageByEntityEvent) {
        val arrow = event.damager as? Arrow ?: return
        val level = arrow.persistentDataContainer.get(bowLevelKey, PersistentDataType.INTEGER) ?: return
        val settings = RandomEnchantPlugin.instance.configManager.settings
        val percent = settings.getDouble("enhancement-attributes.bow.damage_percent.$level", level * 5.0)
        event.damage *= (1 + percent / 100.0)
    }

    // =========================
    // 철퇴 10강: 우클릭 시 윈드차지 발사 (쿨다운)
    // =========================
    @EventHandler
    fun onMaceRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (item.type != Material.MACE) return
        if (EnhancementManager.getLevel(item) < 10) return

        val player = event.player
        val settings = RandomEnchantPlugin.instance.configManager.settings
        val cooldownMs = settings.getLong("enhancement-abilities.mace.cooldown-seconds", 5) * 1000L

        val now = System.currentTimeMillis()
        val last = maceCooldown[player.uniqueId] ?: 0L

        if (now - last < cooldownMs) {
            val remain = (cooldownMs - (now - last)) / 1000.0
            player.sendMessage("§c철퇴 스킬 쿨다운: %.1f초".format(remain))
            return
        }

        maceCooldown[player.uniqueId] = now
        event.isCancelled = true

        val charge = player.world.spawn(player.eyeLocation, WindCharge::class.java)
        charge.shooter = player
        charge.velocity = player.eyeLocation.direction.normalize().multiply(1.5)

        player.world.playSound(player.location, Sound.ENTITY_WIND_CHARGE_THROW, 1.0f, 1.0f)
    }

    // =========================
    // 삼지창 10강: 20갈래 분산 발사, 바닥에 맞으면 3초 후 삭제
    // =========================
    @EventHandler
    fun onTridentLaunch(event: ProjectileLaunchEvent) {
        val trident = event.entity as? Trident ?: return

        // 분산 발사로 생성된 삼지창이 재귀적으로 또 분산되는 것 방지
        if (trident.persistentDataContainer.has(tridentBurstKey, PersistentDataType.BYTE)) return

        val shooter = trident.shooter as? Player ?: return
        val item = shooter.inventory.itemInMainHand
        if (item.type != Material.TRIDENT) return
        if (EnhancementManager.getLevel(item) < 10) return

        val settings = RandomEnchantPlugin.instance.configManager.settings
        val burstCount = settings.getInt("enhancement-abilities.trident.burst-count", 20)

        val baseVelocity = trident.velocity
        val world = trident.world
        val origin = trident.location

        trident.persistentDataContainer.set(tridentBurstKey, PersistentDataType.BYTE, 1)

        // 원본 1개 + 나머지를 부채꼴로 살짝씩 회전시켜 분산
        for (i in 1 until burstCount) {
            val angle = Math.toRadians((i * (360.0 / burstCount)))
            val spread = baseVelocity.clone().rotateAroundY(angle * 0.3)

            val clone = world.spawn(origin, Trident::class.java)
            clone.shooter = shooter
            clone.velocity = spread
            clone.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
            clone.persistentDataContainer.set(tridentBurstKey, PersistentDataType.BYTE, 1)
        }

        world.playSound(origin, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f)
    }

    @EventHandler
    fun onTridentHit(event: ProjectileHitEvent) {
        val trident = event.entity as? Trident ?: return
        if (!trident.persistentDataContainer.has(tridentBurstKey, PersistentDataType.BYTE)) return
        if (event.hitBlock == null) return // 바닥(블록)에 맞았을 때만

        val settings = RandomEnchantPlugin.instance.configManager.settings
        val despawnTicks = settings.getInt("enhancement-abilities.trident.despawn-seconds", 3) * 20L

        Bukkit.getScheduler().runTaskLater(RandomEnchantPlugin.instance, Runnable {
            if (!trident.isDead) trident.remove()
        }, despawnTicks)
    }

    // =========================
    // 낚싯대 10강: 플레이어를 낚으면 3초간 기절
    // =========================
    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_ENTITY) return
        val caught = event.caught as? Player ?: return

        val rod = event.player.inventory.itemInMainHand
        if (rod.type != Material.FISHING_ROD) return
        if (EnhancementManager.getLevel(rod) < 10) return

        val settings = RandomEnchantPlugin.instance.configManager.settings
        val stunSeconds = settings.getInt("enhancement-abilities.fishing-rod.stun-seconds", 3)

        stunPlayer(caught, stunSeconds * 20L)
    }

    private fun stunPlayer(target: Player, durationTicks: Long) {
        target.sendMessage("§c낚싯줄에 걸려 ${durationTicks / 20}초간 기절했습니다!")

        target.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, durationTicks.toInt(), 0, false, false))
        target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, durationTicks.toInt(), 250, false, false))

        val freezeLoc = target.location.clone()

        object : BukkitRunnable() {
            var ticks = 0L
            override fun run() {
                if (ticks >= durationTicks || !target.isOnline) {
                    cancel()
                    return
                }
                target.velocity = Vector(0.0, target.velocity.y, 0.0)
                freezeLoc.yaw = target.location.yaw
                freezeLoc.pitch = target.location.pitch
                target.teleport(freezeLoc)
                ticks++
            }
        }.runTaskTimer(RandomEnchantPlugin.instance, 0L, 1L)
    }
}