package org.pl.lightDarkWorld

import org.bukkit.plugin.java.JavaPlugin
import org.pl.lightDarkWorld.listener.EnchantTableListener
import org.pl.lightDarkWorld.listener.InventoryListener

class RandomEnchantPlugin : JavaPlugin() {

    companion object {
        lateinit var instance: RandomEnchantPlugin
            private set
    }

    override fun onEnable() {
        instance = this

        server.pluginManager.registerEvents(EnchantTableListener(), this)
        server.pluginManager.registerEvents(InventoryListener(), this)

        println("********************")
    }


}