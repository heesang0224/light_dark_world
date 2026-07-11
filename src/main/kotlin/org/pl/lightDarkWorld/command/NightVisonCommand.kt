package org.pl.lightDarkWorld.command

import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class NightVisonCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false

        val effect = PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 0, false, false)
        sender.addPotionEffect(effect)
        sender.playSound(sender.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        return true

    }
}