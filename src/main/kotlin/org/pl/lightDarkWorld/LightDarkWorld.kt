package org.pl.lightDarkWorld

import org.bukkit.plugin.java.JavaPlugin
import org.checkerframework.checker.units.qual.t
import org.pl.lightDarkWorld.config.ConfigManager
import org.pl.lightDarkWorld.listener.EnchantTableListener
import org.pl.lightDarkWorld.listener.InventoryListener
import org.pl.lightDarkWorld.listener.AnvilListener
import org.pl.lightDarkWorld.listener.EnhancementAbilityListener
import org.pl.lightDarkWorld.command.EnhanceCommand
import org.pl.lightDarkWorld.command.NightVisonCommand


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
        server.pluginManager.registerEvents(EnhancementAbilityListener(), this)


        configManager = ConfigManager(this)
        configManager.load()

        // 명령어 등록
        getCommand("enhance")?.setExecutor(EnhanceCommand())
        getCommand("nightvision")?.setExecutor(NightVisonCommand())

        logger.info("LightDarkWorld Plugin enabled")
    }


}