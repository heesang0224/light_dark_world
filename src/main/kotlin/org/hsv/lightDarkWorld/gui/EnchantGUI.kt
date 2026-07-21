package org.hsv.lightDarkWorld.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object EnchantGUI {

    const val ITEM_SLOT = 22
    const val CLOSE_SLOT = 49

    fun open(player: Player) {

        val holder = GUIHolder()
        holder.guiType = "enchant"

        val inv: Inventory = Bukkit.createInventory(
            holder,
            54,
            Component.text("랜덤 인첸트", NamedTextColor.AQUA)
        )

        holder.setInventory(inv)

        val glass = ItemStack(Material.GRAY_STAINED_GLASS_PANE)

        glass.editMeta {
            it.displayName(Component.text(" "))
        }

        for (i in 0 until 54) {
            inv.setItem(i, glass)
        }

        inv.setItem(ITEM_SLOT, null)

        val close = ItemStack(Material.BARRIER)

        close.editMeta {
            it.displayName(Component.text("§c닫기"))
        }

        inv.setItem(CLOSE_SLOT, close)

        player.openInventory(inv)
    }

}