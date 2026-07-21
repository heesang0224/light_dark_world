package org.hsv.lightDarkWorld.manager

import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object RandomEnchantManager {

    private const val MAX_ENCHANTS = 8

    /**
     * 아이템에 랜덤 바닐라 인첸트를 추가한다.
     * 기존 인첸트는 유지하면서 기존 인첸트들의 레벨을 무작위로 재롤링한다.
     */
    fun enchant(item: ItemStack): Boolean {

        val meta = item.itemMeta ?: return false

        // 최대 인첸트 도달 시 모두 제거 후 새로 추가
        if (meta.enchants.size >= MAX_ENCHANTS) {
            val existing = meta.enchants.keys.toList()
            existing.forEach { meta.removeEnchant(it) }

            val pool = Enchantment.values().toMutableList()
            pool.shuffle()

            var added = 0
            for (enchant in pool) {
                if (added >= MAX_ENCHANTS) break

                val level = Random.nextInt(1, enchant.maxLevel + 1)
                meta.addEnchant(enchant, level, true)
                added++
            }

            item.itemMeta = meta
            return true
        }

        // 기존 인첸트들의 레벨을 무작위로 재롤링
        val existingEnchants = meta.enchants.toMap()
        existingEnchants.forEach { (enchant, _) ->
            meta.removeEnchant(enchant)
            val newLevel = Random.nextInt(1, enchant.maxLevel + 1)
            meta.addEnchant(enchant, newLevel, true)
        }

        // 아직 붙어있지 않은 인첸트만 선택해서 새로 추가
        val available = Enchantment.values()
            .filter { !meta.hasEnchant(it) }

        if (available.isEmpty()) {
            item.itemMeta = meta
            return false
        }

        val newEnchant = available.random()
        val level = Random.nextInt(1, newEnchant.maxLevel + 1)

        meta.addEnchant(newEnchant, level, true)

        item.itemMeta = meta

        return true
    }

}

