package org.pl.lightDarkWorld.manager

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.pl.lightDarkWorld.RandomEnchantPlugin

/**
 * 강화 레벨에 따라 장비 종류별 패시브 어트리뷰트(능력치)를 부여한다.
 * 레벨이 바뀔 때마다 이 플러그인이 이전에 추가했던 보너스를 모두 제거한 뒤
 * 새 레벨 기준으로 다시 계산해서 적용한다.
 *
 * 활/도끼는 바닐라 어트리뷰트로 표현이 안 되는 효과(원거리 데미지%, 크리티컬 데미지%)라
 * 여기서는 다루지 않고 EnhancementAbilityListener에서 데미지 보정으로 처리한다.
 * 삼지창/낚싯대는 10강 액티브 스킬만 있고 패시브 스탯은 없다.
 */
object EquipmentAttributeManager {

    enum class EquipmentKind {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS,
        SWORD, AXE, BOW, MACE, TRIDENT, FISHING_ROD,
        NONE
    }

    fun kindOf(material: Material): EquipmentKind = when (material) {
        Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
        Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
        Material.TURTLE_HELMET -> EquipmentKind.HELMET

        Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
        Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
        Material.WOLF_ARMOR -> EquipmentKind.CHESTPLATE

        Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
        Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS -> EquipmentKind.LEGGINGS

        Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
        Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS -> EquipmentKind.BOOTS

        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
        Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.COPPER_SWORD -> EquipmentKind.SWORD

        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE,
        Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.COPPER_AXE -> EquipmentKind.AXE

        Material.BOW -> EquipmentKind.BOW
        Material.MACE -> EquipmentKind.MACE
        Material.TRIDENT -> EquipmentKind.TRIDENT
        Material.FISHING_ROD -> EquipmentKind.FISHING_ROD

        else -> EquipmentKind.NONE
    }

    private val ARMOR_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_armor")
    private val HEALTH_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_health")
    private val SPEED_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_speed")
    private val DAMAGE_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_damage")

    /**
     * 강화 레벨에 맞춰 어트리뷰트를 다시 계산해 적용한다.
     * EnhancementManager.attempt()의 Success 분기에서 호출된다.
     */
    fun apply(item: ItemStack, level: Int) {
        val kind = kindOf(item.type)
        if (kind == EquipmentKind.NONE) return

        val meta = item.itemMeta ?: return
        val settings = RandomEnchantPlugin.instance.configManager.settings

        when (kind) {

            EquipmentKind.HELMET -> {
                meta.removeAttributeModifier(Attribute.ARMOR)
                meta.removeAttributeModifier(Attribute.MAX_HEALTH)
                val armor = settings.getDouble("enhancement-attributes.helmet.armor.$level", level * 0.5)
                val health = settings.getDouble("enhancement-attributes.health.armor.$level", level * 1.0)
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(ARMOR_KEY, armor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                )
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(HEALTH_KEY,health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                )

            }

            EquipmentKind.CHESTPLATE -> {
                meta.removeAttributeModifier(Attribute.ARMOR)
                meta.removeAttributeModifier(Attribute.MAX_HEALTH)
                val armor = settings.getDouble("enhancement-attributes.chestplate.armor.$level", level * 0.5)
                val health = settings.getDouble("enhancement-attributes.chestplate.health.$level", level * 1.0)
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(ARMOR_KEY, armor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                )
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(HEALTH_KEY, health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                )
            }

            EquipmentKind.LEGGINGS -> {
                meta.removeAttributeModifier(Attribute.ARMOR)
                meta.removeAttributeModifier(Attribute.MAX_HEALTH)
                val armor = settings.getDouble("enhancement-attributes.leggings.armor.$level", level * 0.5)
                val health = settings.getDouble("enhancement-attributes.chestplate.health.$level", level * 1.0)
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(ARMOR_KEY, armor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                )
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(HEALTH_KEY, health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                )
            }

            EquipmentKind.BOOTS -> {
                meta.removeAttributeModifier(Attribute.MAX_HEALTH)
                meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED)
                val health = settings.getDouble("enhancement-attributes.boots.health.$level", level * 1.0)
                val speed = settings.getDouble("enhancement-attributes.boots.speed.$level", level * 0.05)
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(HEALTH_KEY, health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                )
                meta.addAttributeModifier(
                    Attribute.MOVEMENT_SPEED,
                    AttributeModifier(SPEED_KEY, speed, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.FEET)
                )
            }

            EquipmentKind.SWORD -> {
                val damage = settings.getDouble("enhancement-attributes.sword.damage.$level", level * 0.5)
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(DAMAGE_KEY, damage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
                )
            }

            EquipmentKind.MACE -> {
                meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE)
                val damage = settings.getDouble("enhancement-attributes.mace.damage.$level", level * 0.5)
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(DAMAGE_KEY, damage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
                )
            }

            EquipmentKind.BOW, EquipmentKind.AXE, EquipmentKind.TRIDENT, EquipmentKind.FISHING_ROD -> {
                // 패시브 어트리뷰트 없음 (액티브/데미지 보정은 EnhancementAbilityListener 담당)
            }

            EquipmentKind.NONE -> {}
        }

        item.itemMeta = meta
    }
}