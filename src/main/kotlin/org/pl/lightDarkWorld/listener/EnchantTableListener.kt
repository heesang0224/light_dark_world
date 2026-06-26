package org.pl.lightDarkWorld.listener

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.pl.lightDarkWorld.gui.EnchantGUI

class EnchantTableListener : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {

        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return

        if (block.type != Material.ENCHANTING_TABLE) return

        event.isCancelled = true

        EnchantGUI.open(event.player)

    }

}