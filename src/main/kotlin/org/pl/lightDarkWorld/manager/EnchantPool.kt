package org.pl.lightDarkWorld.manager

import org.bukkit.enchantments.Enchantment

object EnchantPool {

    val ALL: List<Enchantment> by lazy {
        Enchantment::class.java.fields
            .filter { it.type == Enchantment::class.java }
            .mapNotNull { it.get(null) as? Enchantment }
    }
}