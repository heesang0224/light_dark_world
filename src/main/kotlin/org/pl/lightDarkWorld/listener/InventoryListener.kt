package org.pl.lightDarkWorld.listener

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.pl.lightDarkWorld.gui.EnchantGUI

import org.pl.lightDarkWorld.gui.GUIHolder
import org.pl.lightDarkWorld.manager.RandomEnchantManager
import org.pl.lightDarkWorld.manager.EnhancementManager
import org.pl.lightDarkWorld.util.ItemUtil
import org.pl.lightDarkWorld.RandomEnchantPlugin
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType


class InventoryListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {

        val holder = event.inventory.holder as? GUIHolder ?: return
        val player = event.whoClicked as Player

        // -------------------------
        // Enchant GUI handling
        // -------------------------
        if (holder.guiType == "enchant") {

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

                        // 랜덤 인첸트 전 청금석 비용 확인
                        val lapisCost = RandomEnchantPlugin.instance.configManager.settings.getInt("cost.lapis", 4)

                        if (!ItemUtil.consumeLapis(player, lapisCost)) {
                            player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f)
                            return
                        }

                        if (RandomEnchantManager.enchant(item)) {
                            holder.item = item
                            event.inventory.setItem(EnchantGUI.ITEM_SLOT, item)

                            // 플레이어별 인첸트 횟수 증가
                            val countKey = NamespacedKey(RandomEnchantPlugin.instance, "enchant_count")
                            val pdc = player.persistentDataContainer
                            val current = pdc.getOrDefault(countKey, PersistentDataType.INTEGER, 0)
                            val updated = current + 1
                            pdc.set(countKey, PersistentDataType.INTEGER, updated)

                            if (updated % 5 == 0) {
                                if (RandomEnchantManager.enchant(item)) {
                                    holder.item = item
                                    event.inventory.setItem(EnchantGUI.ITEM_SLOT, item)
                                    player.sendMessage("§a3회 인첸트 달성 — 추가 인첸트가 적용되었습니다.")
                                }
                            }
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

            event.inventory.setItem(EnchantGUI.ITEM_SLOT, guiItem)

            if (clicked.amount > 1) {
                clicked.amount--
            } else {
                event.currentItem = null
            }

            event.isCancelled = true
            return
        }

        // -------------------------
        // Enhancement GUI handling
        // -------------------------
        if (holder.guiType == "enhance") {
            // Enhancement GUI is handled in AnvilListener to avoid duplicate processing
            return
        }


        // if not handled GUI, ignore
        event.isCancelled = true
    }


    @EventHandler
    fun onClose(event: InventoryCloseEvent) {

        val holder = event.inventory.holder as? GUIHolder ?: return
        if (holder.guiType != "enchant") return

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