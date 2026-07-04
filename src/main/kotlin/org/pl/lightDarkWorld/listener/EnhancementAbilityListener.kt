package org.pl.lightDarkWorld.listener

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
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
import java.util.concurrent.ThreadLocalRandom

/**
 * 10강 전용 액티브 스킬과, 바닐라 어트리뷰트로 표현이 안 되는
 * 활(원거리 데미지%) / 도끼(크리티컬 데미지%) 보정을 담당한다.
 */
class EnhancementAbilityListener : Listener {

    private val maceCooldown = mutableMapOf<UUID, Long>()

    private val bowLevelKey get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_bow_level")
    private val tridentBurstKey get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_trident_burst")

    // =========================
    // 도끼: 크리티컬 데미지% 보정
    // =========================





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
        val percent = settings.getDouble("enhancement-attributes.bow.damage_percent.$level", level * 1.0)
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
        val cooldownMs = settings.getLong("enhancement-abilities.mace.cooldown-seconds", 0) * 1000L

        val now = System.currentTimeMillis()
        val last = maceCooldown[player.uniqueId] ?: 0L

        if (now - last < cooldownMs) {
            val remain = (cooldownMs - (now - last)) / 1000.0
            player.sendActionBar("§c철퇴 스킬 쿨다운: %.1f초".format(remain))
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

        event.isCancelled = true

        val settings = RandomEnchantPlugin.instance.configManager.settings
        val burstCount = settings.getInt("enhancement-abilities.trident.burst-count", 20)
        val spread = settings.getDouble("enhancement-abilities.trident.spread", 0.12)
        val speed = settings.getDouble("enhancement-abilities.trident.speed", 2.5)

        val baseDirection = shooter.location.direction.normalize()
        val random = ThreadLocalRandom.current()
        val world = shooter.world

        repeat(burstCount) {
            val offset = Vector(
                random.nextDouble(-spread, spread),
                random.nextDouble(-spread, spread),
                random.nextDouble(-spread, spread)
            )
            val direction = baseDirection.clone().add(offset).normalize()
            
            val clone = shooter.launchProjectile(Trident::class.java)
            clone.velocity = direction.multiply(speed)
            clone.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
            clone.persistentDataContainer.set(tridentBurstKey, PersistentDataType.BYTE, 1)
        }

        world.playSound(shooter.location, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f)
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

    // =========================
    // 부츠 10강: 우클릭 시 대시 (쿨타임 없음)
    // =========================
    @EventHandler
    fun onBootsDash(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        val boots = event.player.inventory.boots ?: return
        if (boots.type !in setOf(
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
        )) return
        if (EnhancementManager.getLevel(boots) < 10) return

        val player = event.player
        val direction = player.location.direction.normalize()
        val settings = RandomEnchantPlugin.instance.configManager.settings
        val speedMultiplier = settings.getDouble("enhancement-abilities.boots.speed-multiplier", 1.6)
        val yBoost = settings.getDouble("enhancement-abilities.boots.y-boost", 0.5)

        val dash = Vector(direction.x, yBoost, direction.z).multiply(speedMultiplier)
        player.velocity = dash

        player.world.spawnParticle(Particle.CLOUD, player.location.add(0.0, 1.0, 0.0), 20, 0.25, 0.25, 0.25, 0.02)
        player.world.spawnParticle(Particle.SWEEP_ATTACK, player.location.add(0.0, 1.0, 0.0), 6, 0.2, 0.2, 0.2, 0.0)
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f)
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