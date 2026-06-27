package org.pl.lightDarkWorld.model

import org.bukkit.inventory.ItemStack

data class EnchantSession(

    var item: ItemStack? = null,

    var enchantCount: Int = 0,

    var successChance: Double = 100.0

)