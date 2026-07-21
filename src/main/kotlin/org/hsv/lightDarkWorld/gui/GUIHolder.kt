package org.hsv.lightDarkWorld.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.hsv.lightDarkWorld.manager.EnhancementOffer

class GUIHolder : InventoryHolder {

    private lateinit var inventory: Inventory

    var item: ItemStack? = null
    var guiType: String? = null // "enchant" or "enhance"

    // For enhancement GUI: store current generated offers
    var offers: List<EnhancementOffer>? = null

    override fun getInventory(): Inventory = inventory

    fun setInventory(inv: Inventory) {
        inventory = inv
    }
}