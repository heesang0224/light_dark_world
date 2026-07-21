package org.hsv.lightDarkWorld.manager

import org.bukkit.inventory.ItemStack

/**
 * Represents a single enhancement offer shown in the GUI (like an enchant table option).
 */
data class EnhancementOffer(
    val index: Int,
    val targetLevel: Int,
    val xpCost: Int,
    val successRate: Int,
    val breakRate: Int,
    val preview: ItemStack
)