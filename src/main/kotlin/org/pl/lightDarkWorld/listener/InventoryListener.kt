package org.pl.lightDarkWorld.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.pl.lightDarkWorld.gui.EnchantGUI
import org.pl.lightDarkWorld.gui.GUIHolder
import org.pl.lightDarkWorld.manager.RandomEnchantManager
import org.pl.lightDarkWorld.util.ItemUtil


class InventoryListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {

        val holder = event.inventory.holder as? GUIHolder ?: return
        val player = event.whoClicked as Player

        // =========================
        // GUI 내부 클릭
        // =========================
        if (event.clickedInventory == event.view.topInventory) {

            when (event.rawSlot) {

                EnchantGUI.CLOSE_SLOT -> {
                    event.isCancelled = true
                    player.closeInventory()
                }

                EnchantGUI.ITEM_SLOT -> {

                    event.isCancelled = true

                    val item = holder.item ?: return

                    if (RandomEnchantManager.enchant(item)) {

                        holder.item = item

                        event.inventory.setItem(
                            EnchantGUI.ITEM_SLOT,
                            item
                        )

                    }

                }

                else -> event.isCancelled = true
            }

            return
        }

        // =========================
        // 플레이어 인벤토리 클릭
        // =========================

        val clicked = event.currentItem

        if (!ItemUtil.canEnchant(clicked)) {
            event.isCancelled = true
            return
        }

        if (clicked == null || clicked.type.isAir) {
            event.isCancelled = true
            return
        }

        if (holder.item != null) {
            event.isCancelled = true
            return
        }

        val guiItem = clicked.clone()
        guiItem.amount = 1

        holder.item = guiItem

        event.inventory.setItem(
            EnchantGUI.ITEM_SLOT,
            guiItem
        )

        if (clicked.amount > 1) {
            clicked.amount--
        } else {
            event.currentItem = null
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {

        val holder = event.inventory.holder as? GUIHolder ?: return

        val item = holder.item ?: return

        val leftover = event.player.inventory.addItem(item)

        if (leftover.isNotEmpty()) {

            leftover.values.forEach {

                event.player.world.dropItemNaturally(
                    event.player.location,
                    it
                )

            }

        }

        holder.item = null
    }
}