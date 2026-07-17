package org.pl.lightDarkWorld.listener

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.pl.lightDarkWorld.gui.EnhancementGUII
import org.pl.lightDarkWorld.gui.GUIHolder
import org.pl.lightDarkWorld.manager.EnhancementManager
import org.pl.lightDarkWorld.manager.EnhancementResult
import org.pl.lightDarkWorld.util.ItemUtil

class AnvilListener : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return

        if (block.type != Material.ANVIL) return

        // 웅크린 채 우클릭 시 바닐라 모루 GUI로 우회 (이름 변경 등)
        if (event.player.isSneaking) return

        event.isCancelled = true

        EnhancementGUII.open(event.player)
    }



    @EventHandler
    fun onClick(event: InventoryClickEvent) {

        val holder = event.inventory.holder as? GUIHolder ?: return
        if (holder.guiType != "enhance") return

        val player = event.whoClicked as Player

        // =========================
        // GUI 내부 클릭
        // =========================
        if (event.clickedInventory == event.view.topInventory) {

            when (event.rawSlot) {

                EnhancementGUII.CLOSE_SLOT -> {
                    event.isCancelled = true
                    player.closeInventory()
                }

                EnhancementGUII.ITEM_SLOT -> {
                    event.isCancelled = true

                    val item = holder.item ?: return

                    when (val result = EnhancementManager.attempt(player, item)) {

                        is EnhancementResult.Max -> {
                            player.sendMessage("§e이미 최대 강화 레벨입니다. (${result.level}강)")
                            player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f)
                        }

                        is EnhancementResult.InsufficientXp -> {
                            player.sendMessage("§c경험치가 부족합니다. (필요: ${result.required}, 보유: ${result.current})")
                            player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f)
                        }

                        is EnhancementResult.Success -> {
                            holder.item = item
                            event.inventory.setItem(EnhancementGUII.ITEM_SLOT, item)
                            player.sendMessage("§a강화 성공! (${result.level}강, 경험치 ${result.xpCost} 소모)")
                            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f)
                        }

                        is EnhancementResult.Fail -> {
                            holder.item = item
                            event.inventory.setItem(EnhancementGUII.ITEM_SLOT, item)
                            player.sendMessage("§c강화 실패... (현재 ${result.level}강, 경험치 ${result.xpCost} 소모)")
                            player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f)
                        }

                        is EnhancementResult.Destroyed -> {
                            holder.item = null
                            event.inventory.setItem(EnhancementGUII.ITEM_SLOT, null)
                            player.sendMessage("§4강화 실패로 아이템이 파괴되었습니다! (경험치 ${result.xpCost} 소모)")
                            player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
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

        if (!ItemUtil.canEnhance(clicked)) {
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

        event.inventory.setItem(EnhancementGUII.ITEM_SLOT, guiItem)

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
        if (holder.guiType != "enhance") return

        val item = holder.item ?: return

        val leftover = event.player.inventory.addItem(item)

        if (leftover.isNotEmpty()) {
            leftover.values.forEach {
                event.player.world.dropItemNaturally(event.player.location, it)
            }
        }

        holder.item = null
    }
}