package org.pl.lightDarkWorld.manager

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.pl.lightDarkWorld.RandomEnchantPlugin
import org.pl.lightDarkWorld.util.ItemUtil
import kotlin.random.Random

/**
 * 모루 강화 결과.
 * 강화 레벨, 소모 XP 등 메시지 출력에 필요한 정보를 함께 담는다.
 */
sealed class EnhancementResult {
    data class Max(val level: Int) : EnhancementResult()
    data class InsufficientXp(val required: Int, val current: Int) : EnhancementResult()
    data class Success(val level: Int, val xpCost: Int) : EnhancementResult()
    data class Fail(val level: Int, val xpCost: Int) : EnhancementResult()
    data class Destroyed(val level: Int, val xpCost: Int) : EnhancementResult()
}

/**
 * settings.yml의 강화 시스템 설정(max-enhancement, enhancement-cost,
 * enhancement-success-rate, enhancement-break-rate)을 읽어
 * 아이템의 강화 시도를 처리한다.
 *
 * 강화 레벨은 아이템의 PersistentDataContainer에 저장되며,
 * 강화 레벨에 따른 표시(★)는 ItemUtil.setEnhancementLore가 담당한다.
 */
object EnhancementManager {

    private val LEVEL_KEY: NamespacedKey
        get() = NamespacedKey(RandomEnchantPlugin.instance, "enhancement_level")

    /**
     * 아이템의 현재 강화 레벨을 가져온다. 강화된 적 없으면 0.
     */
    fun getLevel(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        return meta.persistentDataContainer.getOrDefault(LEVEL_KEY, PersistentDataType.INTEGER, 0)
    }

    private fun setLevel(item: ItemStack, level: Int) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(LEVEL_KEY, PersistentDataType.INTEGER, level)
        item.itemMeta = meta
    }

    /**
     * 아이템에 강화를 1회 시도한다.
     * - 최대 강화 레벨이면 비용을 소모하지 않고 Max 반환
     * - XP가 부족하면 비용을 소모하지 않고 InsufficientXp 반환
     * - 충분하면 XP를 소모한 뒤 성공률에 따라 Success / Fail / Destroyed 중 하나를 반환
     *   (Destroyed는 enhancement-break-rate가 0보다 클 때만 발생하며, 호출부에서
     *    아이템을 실제로 제거해야 한다 — 이 함수는 아이템을 파괴하지 않는다)
     *
     * xpCostOverride, successRateOverride, breakRateOverride를 전달하면 설정 대신 해당 값으로 시도한다.
     */
    fun attempt(
        player: Player,
        item: ItemStack,
        xpCostOverride: Int? = null,
        successRateOverride: Int? = null,
        breakRateOverride: Int? = null
    ): EnhancementResult {

        val settings = RandomEnchantPlugin.instance.configManager.settings

        val maxLevel = settings.getInt("max-enhancement", 10)
        val currentLevel = getLevel(item)

        if (currentLevel >= maxLevel) {
            return EnhancementResult.Max(currentLevel)
        }

        val nextLevel = currentLevel + 1
        val cost = xpCostOverride ?: settings.getInt("enhancement-cost.$nextLevel", nextLevel * 2)

        if (player.level < cost) {
            return EnhancementResult.InsufficientXp(cost, player.level)
        }

        // 비용은 성공/실패와 무관하게 소모한다.
        ItemUtil.consumeXP(player, cost)

        val successRate = successRateOverride ?: settings.getInt("enhancement-success-rate.$nextLevel", 50)
        val successRoll = Random.nextInt(1, 101)

        if (successRoll <= successRate) {
            setLevel(item, nextLevel)
            ItemUtil.setEnhancementLore(item, nextLevel)
            return EnhancementResult.Success(nextLevel, cost)
        }

        // 강화 실패 — 파괴 확률 체크 (5강부터 settings.yml에 정의됨, 그 외엔 기본 0)
        val breakRate = breakRateOverride ?: settings.getInt("enhancement-break-rate.$nextLevel", 0)

        if (breakRate > 0) {
            val breakRoll = Random.nextInt(1, 101)
            if (breakRoll <= breakRate) {
                return EnhancementResult.Destroyed(currentLevel, cost)
            }
        }

        return EnhancementResult.Fail(currentLevel, cost)
    }
}