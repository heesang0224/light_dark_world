package org.pl.lightDarkWorld.util

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


object ItemUtil {

    fun canEnchant(item: ItemStack?): Boolean {


        if (item == null || item.type.isAir) return false

        return when (item.type) {

            // 검
            Material.COPPER_SWORD,
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD,

                // 도끼
            Material.COPPER_AXE,
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE,

                // 곡괭이
            Material.COPPER_PICKAXE,
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE,

                // 삽
            Material.COPPER_SHOVEL,
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL,

                // 괭이
            Material.COPPER_HOE,
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.GOLDEN_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE,

                // 활
            Material.BOW,

                // 석궁
            Material.CROSSBOW,

                // 삼지창
            Material.TRIDENT,

                // 철퇴
            Material.MACE,

                //창
            Material.COPPER_SPEAR,
            Material.STONE_SPEAR,
            Material.IRON_SPEAR,
            Material.GOLDEN_SPEAR,
            Material.DIAMOND_SPEAR,
            Material.NETHERITE_SPEAR,
            Material.WOODEN_SPEAR,

                // 방패
            Material.SHIELD,

                // 낚싯대
            Material.FISHING_ROD,

                // 가위
            Material.SHEARS,

                // 책
            Material.BOOK,
            Material.ENCHANTED_BOOK,

                // 투구
            Material.LEATHER_HELMET,
            Material.CHAINMAIL_HELMET,
            Material.IRON_HELMET,
            Material.GOLDEN_HELMET,
            Material.DIAMOND_HELMET,
            Material.NETHERITE_HELMET,
            Material.TURTLE_HELMET,

                // 흉갑
            Material.LEATHER_CHESTPLATE,
            Material.CHAINMAIL_CHESTPLATE,
            Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE,
            Material.DIAMOND_CHESTPLATE,
            Material.NETHERITE_CHESTPLATE,

                // 레깅스
            Material.LEATHER_LEGGINGS,
            Material.CHAINMAIL_LEGGINGS,
            Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS,
            Material.DIAMOND_LEGGINGS,
            Material.NETHERITE_LEGGINGS,

                // 부츠
            Material.LEATHER_BOOTS,
            Material.CHAINMAIL_BOOTS,
            Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS,
            Material.DIAMOND_BOOTS,
            Material.NETHERITE_BOOTS,

                // 늑대 갑옷
            Material.WOLF_ARMOR -> true

            else -> false
        }
    }



    /**
     * 청금석(화폐) 소비. 인벤토리에서 LAPIS_LAZULI 아이템을 찾아 amount만큼 소모한다.
     * 총량을 먼저 확인하여 부족하면 어떤 아이템도 소모하지 않도록 원자적으로 처리한다.
     */
    fun consumeLapis(player: Player, amount: Int): Boolean {
        val inv = player.inventory

        // 총 보유량 계산
        var total = 0
        for (i in 0 until inv.size) {
            val current = inv.getItem(i) ?: continue
            if (current.type != Material.LAPIS_LAZULI) continue
            total += current.amount
        }

        if (total < amount) return false // 부족하면 아무 것도 소모하지 않음

        // 충분하면 실제로 소모
        var remaining = amount
        for (i in 0 until inv.size) {
            val current = inv.getItem(i) ?: continue
            if (current.type != Material.LAPIS_LAZULI) continue

            if (current.amount > remaining) {
                current.amount -= remaining
                inv.setItem(i, current)
                remaining = 0
                break
            } else {
                remaining -= current.amount
                inv.setItem(i, null)
            }
        }

        return remaining == 0
    }
}