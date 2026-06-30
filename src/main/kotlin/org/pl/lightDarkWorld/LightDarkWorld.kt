package org.pl.lightDarkWorld

import org.bukkit.plugin.java.JavaPlugin
import org.pl.lightDarkWorld.config.ConfigManager
import org.pl.lightDarkWorld.listener.EnchantTableListener
import org.pl.lightDarkWorld.listener.InventoryListener
import org.pl.lightDarkWorld.listener.AnvilListener

class RandomEnchantPlugin : JavaPlugin() {

    companion object {
        lateinit var instance: RandomEnchantPlugin
            private set
    }
    lateinit var configManager: ConfigManager
        private set

    override fun onEnable() {
        instance = this

        server.pluginManager.registerEvents(EnchantTableListener(), this)
        server.pluginManager.registerEvents(InventoryListener(), this)
        server.pluginManager.registerEvents(AnvilListener(), this)


        configManager = ConfigManager(this)
        configManager.load()
        println("********************")
    }


}