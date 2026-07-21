package org.hsv.lightDarkWorld.model

import org.bukkit.inventory.ItemStack

data class EnchantSession(

    var item: ItemStack? = null,

    var enchantCount: Int = 0

)