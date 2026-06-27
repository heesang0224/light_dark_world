package org.pl.lightDarkWorld.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager(
    private val plugin: JavaPlugin
) {

    lateinit var settings: YamlConfiguration
        private set

    fun load() {

        val file = File(plugin.dataFolder, "settings.yml")

        if (!file.exists()) {
            plugin.saveResource("settings.yml", false)
        }

        settings = YamlConfiguration.loadConfiguration(file)
    }

    fun reload() {
        load()
    }

}