package org.hsv.lightDarkWorld.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.hsv.lightDarkWorld.manager.EnhancementManager
import org.hsv.lightDarkWorld.util.ItemUtil

class EnhanceCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {


        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용 가능합니다.")
            return true
        }

        if (!sender.hasPermission("lightdarkworld.enhance")) {
            sender.sendMessage("§c권한이 없습니다.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§c사용법: /enhance <레벨(0-10)>")
            return true
        }

        val item = sender.inventory.itemInMainHand

        if (item.type.isAir) {
            sender.sendMessage("§c손에 든 아이템이 없습니다.")
            return true
        }

        if (!ItemUtil.canEnhance(item)) {
            sender.sendMessage("§c강화할 수 없는 아이템입니다.")
            return true
        }

        val levelStr = args[0]
        val level = try {
            levelStr.toInt()
        } catch (e: NumberFormatException) {
            sender.sendMessage("§c올바른 숫자를 입력해주세요. /enhance <레벨>")
            return true
        }

        val settings = org.hsv.lightDarkWorld.RandomEnchantPlugin.instance.configManager.settings
        val maxLevel = settings.getInt("max-enhancement", 10)

        if (level < 0 || level > maxLevel) {
            sender.sendMessage("§c강화 레벨은 0부터 $maxLevel 사이여야 합니다.")
            return true
        }

        // 현재 레벨
        val currentLevel = EnhancementManager.getLevel(item)

        if (level == currentLevel) {
            sender.sendMessage("§c이미 ${level}강인 아이템입니다.")
            return true
        }

        // 강화 레벨 설정
        if (level == 0) {
            // 강화 제거 (레벨 0)
            EnhancementManager.removeEnhancement(item)
            sender.sendMessage("§a강화 레벨이 제거되었습니다. (0강)")
        } else {
            // 강화 설정 및 어트리뷰트 적용
            EnhancementManager.setEnhancementLevel(item, level)
            sender.sendMessage("§a아이템이 ${level}강으로 설정되었습니다.")
        }

        return true
    }
}
