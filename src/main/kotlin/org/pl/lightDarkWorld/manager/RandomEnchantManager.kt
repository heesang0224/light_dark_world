package org.pl.lightDarkWorld.manager

import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object RandomEnchantManager {

    private const val MAX_ENCHANTS = 5

    /**
     * 아이템에 랜덤 바닐라 인첸트를 추가한다.
     */
    fun enchant(item: ItemStack): Boolean {

        val meta = item.itemMeta ?: return false

        // 최대 5줄
        if (meta.enchants.size >= MAX_ENCHANTS) {
            return false
        }

        // 아직 붙어있지 않은 인첸트만 선택
        val available = Enchantment.values()
            .filter { !meta.hasEnchant(it) }

        if (available.isEmpty()) {
            return false
        }

        val enchant = available.random()

        val level = Random.nextInt(
            1,
            enchant.maxLevel + 1
        )

        meta.addEnchant(
            enchant,
            level,
            true
        )

        item.itemMeta = meta

        return true
    }
}
