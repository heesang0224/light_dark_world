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
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.attribute.Attribute
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.pl.lightDarkWorld.RandomEnchantPlugin
import org.pl.lightDarkWorld.manager.EnhancementManager
import java.util.UUID
import org.bukkit.entity.AbstractArrow
import java.util.concurrent.ThreadLocalRandom

/**
 * 10강 전용 액티브 스킬과, 바닐라 어트리뷰트로 표현이 안 되는
 * 활(원거리 데미지%) / 도끼(크리티컬 데미지%) 보정을 담당한다.
 */
class EnhancementAbilityListener : Listener {

    private val maceCooldown = mutableMapOf<UUID, Long>()
    private val tridentCooldown = mutableMapOf<UUID, Long>()

    private var isSpawningTridentBurst = false

    private val bowLevelKey get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_bow_level")
    private val tridentBurstKey get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_trident_burst")

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
        val flatDamage = settings.getDouble("enhancement-attributes.bow.damage.$level", level * 1.0)
        event.damage += flatDamage
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
        val last = maceCooldown[player.uniqueId] ?: 1L

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
    // 삼지창 10강: 분산 발사 (설정 기반)
    // =========================
    @EventHandler
    fun onTridentLaunch(event: ProjectileLaunchEvent) {
        if (isSpawningTridentBurst) return
        val trident = event.entity as? Trident ?: return
        if (trident.persistentDataContainer.has(tridentBurstKey, PersistentDataType.BYTE)) return

        val shooter = trident.shooter as? Player ?: return
        val item = shooter.inventory.itemInMainHand
        if (item.type != Material.TRIDENT) return
        if (EnhancementManager.getLevel(item) < 10) return

        event.isCancelled = true
        fireTridentBurst(shooter)
    }

    @EventHandler
    fun onTridentRiptide(event: PlayerRiptideEvent) {
        val shooter = event.player
        val item = event.item
        if (item.type != Material.TRIDENT) return
        if (EnhancementManager.getLevel(item) < 10) return

        fireTridentBurst(shooter)
    }

    @EventHandler
    fun onTridentBurstSelfHit(event: EntityDamageByEntityEvent) {
        val trident = event.damager as? Trident ?: return
        if (!trident.persistentDataContainer.has(tridentBurstKey, PersistentDataType.BYTE)) return
        if (event.entity == trident.shooter) {
            event.isCancelled = true
        }
    }

    private fun fireTridentBurst(shooter: Player) {
        val settings = RandomEnchantPlugin.instance.configManager.settings
        val cooldownMs = (settings.getDouble("enhancement-abilities.trident.cooldown-seconds", 1.0) * 1000).toLong()
        val now = System.currentTimeMillis()
        val last = tridentCooldown[shooter.uniqueId] ?: 0L

        if (now - last < cooldownMs) {
            val remain = (cooldownMs - (now - last)) / 1000.0
            shooter.sendActionBar("§c삼지창 스킬 쿨다운: %.1f초".format(remain))
            return
        }

        tridentCooldown[shooter.uniqueId] = now

        val burstCount = settings.getInt("enhancement-abilities.trident.burst-count", 15)
        val spread = settings.getDouble("enhancement-abilities.trident.spread", 0.12)
        val speed = settings.getDouble("enhancement-abilities.trident.speed", 2.5)
        val damageMultiplier = settings.getDouble("enhancement-abilities.trident.damage-multiplier", 1.5)
        val despawnTicks = settings.getInt("enhancement-abilities.trident.despawn-seconds", 3) * 20L

        val baseDirection = shooter.location.direction.normalize()
        val random = ThreadLocalRandom.current()
        val world = shooter.world

        isSpawningTridentBurst = true
        try {
            repeat(burstCount) {
                val offset = Vector(
                    random.nextDouble(-spread, spread),
                    random.nextDouble(-spread, spread),
                    random.nextDouble(-spread, spread)
                )
                val direction = baseDirection.clone().add(offset).normalize()

                val clone = shooter.launchProjectile(Trident::class.java)
                clone.velocity = direction.multiply(speed)
                clone.damage = clone.damage * damageMultiplier
                clone.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
                clone.persistentDataContainer.set(tridentBurstKey, PersistentDataType.BYTE, 1)

                Bukkit.getScheduler().runTaskLater(RandomEnchantPlugin.instance, Runnable {
                    if (!clone.isDead) clone.remove()
                }, despawnTicks)
            }
        } finally {
            isSpawningTridentBurst = false
        }

        world.playSound(shooter.location, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f)
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

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        restoreHealthRatio(player)
    }

    private val HEALTH_RATIO_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "quit_health_ratio")

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        if (maxHealth <= 0.0) return

        val ratio = (player.health / maxHealth).coerceIn(0.0, 1.0)
        player.persistentDataContainer.set(HEALTH_RATIO_KEY, PersistentDataType.DOUBLE, ratio)
    }

    private fun restoreHealthRatio(player: Player) {
        val pdc = player.persistentDataContainer
        val ratio = pdc.get(HEALTH_RATIO_KEY, PersistentDataType.DOUBLE) ?: return
        pdc.remove(HEALTH_RATIO_KEY)

        val expectedMax = expectedMaxHealth(player)

        object : BukkitRunnable() {
            var ticks = 0
            override fun run() {
                if (!player.isOnline || player.isDead) {
                    cancel()
                    return
                }

                val currentMax = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                ticks++

                if (currentMax + 0.01 < expectedMax && ticks < 20) return

                player.health = (currentMax * ratio).coerceIn(0.5, currentMax)
                cancel()
            }
        }.runTaskTimer(RandomEnchantPlugin.instance, 1L, 1L)
    }

    private fun expectedMaxHealth(player: Player): Double {
        val base = player.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 20.0
        val inv = player.inventory
        var bonus = 0.0
        for (item in listOf(inv.helmet, inv.chestplate, inv.leggings, inv.boots)) {
            val meta = item?.itemMeta ?: continue
            meta.getAttributeModifiers(Attribute.MAX_HEALTH)?.forEach { bonus += it.amount }
        }
        return base + bonus
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