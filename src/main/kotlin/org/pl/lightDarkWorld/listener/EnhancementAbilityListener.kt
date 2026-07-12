package org.pl.lightDarkWorld.listener

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent

import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
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
import org.bukkit.event.player.PlayerToggleFlightEvent
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
import org.pl.lightDarkWorld.manager.EquipmentAttributeManager

import java.util.concurrent.ThreadLocalRandom

/**
 * 10강 전용 액티브 스킬과, 바닐라 어트리뷰트로 표현이 안 되는
 * 활(원거리 데미지%) / 도끼(크리티컬 데미지%) 보정을 담당한다.
 */
class EnhancementAbilityListener : Listener {

    private val maceCooldown = mutableMapOf<UUID, Long>()
    private val dashCooldown = mutableMapOf<UUID, Long>()
    private val tridentCooldown = mutableMapOf<UUID, Long>()

    // 분산 삼지창을 launchProjectile로 생성하는 동안, 그 스폰이 다시
    // onTridentLaunch를 재귀 호출하는 것을 막기 위한 플래그.
    // (launchProjectile은 리턴 전에 ProjectileLaunchEvent를 동기 발생시키는데,
    //  PDC 태그는 리턴 이후에나 붙기 때문에 태그 체크만으로는 재귀를 못 막는다.)
    private var isSpawningTridentBurst = false

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
    // 일반 투척과 급류(Riptide) 발사 두 경로 모두 처리한다.
    // =========================
    @EventHandler
    fun onTridentLaunch(event: ProjectileLaunchEvent) {
        // 버스트 삼지창 생성 도중 발생하는 재귀 호출은 즉시 무시한다.
        if (isSpawningTridentBurst) return

        val trident = event.entity as? Trident ?: return

        // 분산 발사로 생성된 삼지창이 재귀적으로 또 분산되는 것 방지
        if (trident.persistentDataContainer.has(tridentBurstKey, PersistentDataType.BYTE)) return

        val shooter = trident.shooter as? Player ?: return
        val item = shooter.inventory.itemInMainHand
        if (item.type != Material.TRIDENT) return
        if (EnhancementManager.getLevel(item) < 10) return

        // 일반 투척은 취소하고, 대신 분산 버스트로 대체한다.
        event.isCancelled = true
        fireTridentBurst(shooter)
    }

    // 급류(Riptide) 인챈트로 발사한 경우, 삼지창은 던져지지 않고 플레이어가
    // 발사되므로 Trident 투사체 자체가 스폰되지 않는다. 이 경우
    // ProjectileLaunchEvent가 발생하지 않으므로 별도로 감지해야 한다.
    @EventHandler
    fun onTridentRiptide(event: PlayerRiptideEvent) {
        val shooter = event.player
        val item = event.item
        if (item.type != Material.TRIDENT) return
        if (EnhancementManager.getLevel(item) < 10) return

        // 플레이어 발사 자체는 그대로 두고(취소 불가/취소할 필요 없음),
        // 추가로 분산 삼지창 버스트만 발생시킨다.
        fireTridentBurst(shooter)
    }

    // 분산 삼지창(버스트)이 발사자 본인에게 데미지를 주는 것을 방지한다.
// 스폰 직후 히트박스가 겹치거나(특히 급류 발사 시), spread로 인해
// 방향이 살짝 틀어져 자기 자신에게 맞는 경우를 원천 차단.
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

                // 맞든 안 맞든 일정 시간 후 무조건 자동 삭제
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

    // =========================
    // 부츠 10강: 스페이스바 두 번 눌러서 대시
    // =========================
    @EventHandler
    fun onBootsDash(event: PlayerToggleFlightEvent) {
        if (!event.isFlying) return

        val player = event.player
        val boots = player.inventory.boots ?: return
        if (boots.type !in setOf(
                Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
            )) return
        if (EnhancementManager.getLevel(boots) < 10) return

        val now = System.currentTimeMillis()
        val last = dashCooldown[player.uniqueId] ?: 0L
        val settings = RandomEnchantPlugin.instance.configManager.settings
        val cooldownSeconds = settings.getDouble("enhancement-abilities.boots.cooldown-seconds", 1.0)
        val cooldownMs = (cooldownSeconds * 1000).toLong()

        if (now - last < cooldownMs) {
            event.isCancelled = true

            // 쿨다운 중에도 클라이언트는 스페이스바 두 번으로 비행 진입을 시도하고 있어서
            // 그냥 취소만 하면(특히 연타 시) 낙하하지 않고 계속 공중에 떠있는
            // 무한 호버링 현상이 생긴다. 비행 상태를 명시적으로 끄고 아래로
            // 살짝 밀어서 실제로 떨어지게 만든다.
            player.isFlying = false
            val v = player.velocity
            player.velocity = Vector(v.x, minOf(v.y, -0.1), v.z)
            return
        }

        dashCooldown[player.uniqueId] = now

        // 비행 모드 진입 취소
        event.isCancelled = true

        val direction = player.location.direction.normalize()
        val speedMultiplier = settings.getDouble("enhancement-abilities.boots.speed-multiplier", 1.0)
        val yBoost = settings.getDouble("enhancement-abilities.boots.y-boost", 0.3)

        val dash = Vector(direction.x, yBoost, direction.z).multiply(speedMultiplier)
        player.velocity = dash

        // 낙하 데미지 활성화 (양수로 설정하면 낙뎀 계산)
        player.fallDistance = 0.1f

        player.world.spawnParticle(Particle.CLOUD, player.location.add(0.0, 1.0, 0.0), 20, 0.25, 0.25, 0.25, 0.02)
        player.world.spawnParticle(Particle.SWEEP_ATTACK, player.location.add(0.0, 1.0, 0.0), 6, 0.2, 0.2, 0.2, 0.0)
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f)
    }
    @EventHandler
    fun onArmorChange(event: PlayerArmorChangeEvent) {

        if (event.slot != org.bukkit.inventory.EquipmentSlot.FEET) return
        syncBootsFlight(event.player, event.newItem)
    }

    // 서버 재시작/재접속처럼 10강 부츠를 이미 신은 채로 로그인하는 경우
    // PlayerArmorChangeEvent가 발생하지 않아 allowFlight가 켜지지 않는 문제를 보완한다.
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        val boots = player.inventory.boots
        if (boots != null) {
            syncBootsFlight(player, boots)
        }

        restoreHealthRatio(player)
    }

    private val HEALTH_RATIO_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "quit_health_ratio")

    // 퇴장 시점의 "현재체력/최대체력" 비율을 저장해둔다.
    // 강화 방어구로 늘어난 최대체력(MAX_HEALTH 어트리뷰트)이 있는 상태로 서버를 나가면,
    // 재접속 시 엔티티가 로드되는 순간 체력이 먼저 바닐라 기본 최대체력(20)에 맞춰
    // 깎인 뒤에야 장비 어트리뷰트가 재적용되어 최대체력만 복구되고 현재체력은
    // 깎인 채로 남는 문제가 있다. 비율을 저장해뒀다가 재접속 후 복원해서 해결한다.
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

        // 로그인 시 이미 장착돼 있던 방어구는 장비 어트리뷰트(최대체력) 재계산이
        // 1틱만으로는 안 끝나는 경우가 있어서, 실제로 반영될 때까지 몇 틱 더
        // 기다린다(최대 20틱 = 1초, 안전장치). 그 전에 비율을 적용하면
        // 아직 낮은 maxHealth 기준으로 계산돼서 추가 체력이 안 채워진다.
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

    // 현재 장착된 방어구 기준으로 실제로 도달해야 할 최대체력을 직접 계산한다.
    // (엔진의 어트리뷰트 재계산 타이밍에 의존하지 않기 위한 기준값)
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

    private fun syncBootsFlight(player: Player, boots: org.bukkit.inventory.ItemStack) {
        val is10Boots = boots.type in setOf(
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
        ) && EnhancementManager.getLevel(boots) >= 10

        if (is10Boots) {
            player.allowFlight = true
        } else if (player.gameMode != org.bukkit.GameMode.CREATIVE &&
            player.gameMode != org.bukkit.GameMode.SPECTATOR) {
            // 다른 플러그인(엘리트라 등)이 켜둔 게 아니라면 원상 복구
            player.allowFlight = false
        }
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