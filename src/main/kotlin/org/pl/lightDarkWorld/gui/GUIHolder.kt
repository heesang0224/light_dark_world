package org.pl.lightDarkWorld.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class GUIHolder : InventoryHolder {

    private lateinit var inventory: Inventory

    var item: ItemStack? = null

    override fun getInventory(): Inventory = inventory

    fun setInventory(inv: Inventory) {
        inventory = inv
    }
}