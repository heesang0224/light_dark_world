package org.pl.lightDarkWorld.gui


import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object EnhancementGUII {

    const val ITEM_SLOT = 22
    const val CLOSE_SLOT = 49

    /**
     * @param presetItem 이미 선택된 아이템을 들고 GUI를 열고 싶을 때 전달 (없으면 슬롯을 비워서 연다)
     */
    fun open(player: Player, presetItem: ItemStack? = null) {
        val holder = GUIHolder()
        holder.guiType = "enhance"

        val inv: Inventory = Bukkit.createInventory(
            holder,
            54,
            Component.text("강화", NamedTextColor.DARK_PURPLE)
        )

        holder.setInventory(inv)

        val glass = ItemStack(Material.GRAY_STAINED_GLASS_PANE)

        glass.editMeta {
            it.displayName(Component.text(" "))
        }

        for (i in 0 until 54) {
            inv.setItem(i, glass)
        }

        if (presetItem != null) {
            val item = presetItem.clone()
            item.amount = 1
            holder.item = item
            inv.setItem(ITEM_SLOT, item)
        } else {
            inv.setItem(ITEM_SLOT, null)
        }

        val close = ItemStack(Material.BARRIER)

        close.editMeta {
            it.displayName(Component.text("§c닫기"))
        }

        inv.setItem(CLOSE_SLOT, close)

        player.openInventory(inv)
    }

}