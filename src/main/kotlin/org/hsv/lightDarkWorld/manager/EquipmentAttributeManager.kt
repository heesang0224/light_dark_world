package org.hsv.lightDarkWorld.manager

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.hsv.lightDarkWorld.RandomEnchantPlugin

/**
 * 강화 레벨에 따라 장비 종류별 패시브 어트리뷰트(능력치)를 부여한다.
 * 레벨이 바뀔 때마다 이 플러그인이 이전에 추가했던 보너스를 모두 제거한 뒤
 * 새 레벨 기준으로 다시 계산해서 적용한다.
 *
 * 주의: 아이템에 커스텀 AttributeModifier를 하나라도 추가하면 마인크래프트는
 * 그 아이템의 바닐라 기본 어트리뷰트 전체(다른 속성까지 포함)를 더 이상 자동
 * 적용하지 않는다. 그래서 무기의 ATTACK_DAMAGE, 네더라이트 방어구의
 * KNOCKBACK_RESISTANCE / ARMOR_TOUGHNESS처럼 바닐라가 원래 주던 값도 함께
 * 다시 명시해줘야 한다.
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

    private val HELMET_ARMOR_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_helmet_armor")
    private val HELMET_HEALTH_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_helmet_health")
    private val HELMET_KB_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_helmet_kb_resist")
    private val HELMET_TOUGHNESS_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_helmet_toughness")
    private val CHESTPLATE_ARMOR_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_chestplate_armor")
    private val CHESTPLATE_HEALTH_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_chestplate_health")
    private val CHESTPLATE_KB_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_chestplate_kb_resist")
    private val CHESTPLATE_TOUGHNESS_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_chestplate_toughness")
    private val LEGGINGS_ARMOR_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_leggings_armor")
    private val LEGGINGS_HEALTH_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_leggings_health")
    private val LEGGINGS_KB_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_leggings_kb_resist")
    private val LEGGINGS_TOUGHNESS_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_leggings_toughness")
    private val BOOTS_ARMOR_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_boots_armor")
    private val BOOTS_HEALTH_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_boots_health")
    private val BOOTS_SPEED_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_boots_speed")
    private val BOOTS_KB_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_boots_kb_resist")
    private val BOOTS_TOUGHNESS_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_boots_toughness")
    private val SWORD_DAMAGE_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_sword_damage")
    private val SWORD_ATTACK_SPEED_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_sword_attack_speed")
    private val MACE_DAMAGE_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_mace_damage")
    private val MACE_ATTACK_SPEED_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_mace_attack_speed")
    private val AXE_DAMAGE_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_axe_damage" )
    private val AXE_ATTACK_SPEED_KEY get() = NamespacedKey(RandomEnchantPlugin.instance, "enh_axe_attack_speed" )

    // 네더라이트 방어구 부위당 바닐라 기본 넉백저항 (10%)
    private const val NETHERITE_KB_RESIST = 0.1

    // 방어구 재질별 바닐라 기본 방어강도(Armor Toughness). 다이아몬드/네더라이트만 값이 있다.
    private fun baseToughnessOf(material: Material): Double = when (material) {
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
        Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS -> 2.0

        Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS -> 3.0

        else -> 0.0
    }

    // 방어구 재질별 바닐라 기본 방어력
    private fun baseArmorOf(material: Material): Double = when (material) {
        Material.LEATHER_HELMET -> 1.0
        Material.LEATHER_CHESTPLATE -> 3.0
        Material.LEATHER_LEGGINGS -> 2.0
        Material.LEATHER_BOOTS -> 1.0

        Material.CHAINMAIL_HELMET -> 2.0
        Material.CHAINMAIL_CHESTPLATE -> 5.0
        Material.CHAINMAIL_LEGGINGS -> 4.0
        Material.CHAINMAIL_BOOTS -> 1.0

        Material.IRON_HELMET -> 2.0
        Material.IRON_CHESTPLATE -> 6.0
        Material.IRON_LEGGINGS -> 5.0
        Material.IRON_BOOTS -> 2.0

        Material.GOLDEN_HELMET -> 2.0
        Material.GOLDEN_CHESTPLATE -> 5.0
        Material.GOLDEN_LEGGINGS -> 3.0
        Material.GOLDEN_BOOTS -> 1.0

        Material.DIAMOND_HELMET -> 2.0
        Material.DIAMOND_CHESTPLATE -> 8.0
        Material.DIAMOND_LEGGINGS -> 6.0
        Material.DIAMOND_BOOTS -> 2.0

        Material.NETHERITE_HELMET -> 3.0
        Material.NETHERITE_CHESTPLATE -> 8.0
        Material.NETHERITE_LEGGINGS -> 6.0
        Material.NETHERITE_BOOTS -> 3.0

        Material.TURTLE_HELMET -> 2.0
        Material.WOLF_ARMOR -> 8.0

        else -> 0.0
    }

    // 검 재질별 바닐라 기본 데미지
    private fun baseSwordDamageOf(material: Material): Double = when (material) {
        Material.WOODEN_SWORD -> 4.0
        Material.STONE_SWORD -> 5.0
        Material.IRON_SWORD -> 6.0
        Material.GOLDEN_SWORD -> 4.0
        Material.DIAMOND_SWORD -> 7.0
        Material.NETHERITE_SWORD -> 8.0
        Material.COPPER_SWORD -> 5.0
        else -> 0.0
    }

    // 도끼 재질별 바닐라 기본 데미지
    private fun baseAxeDamageOf(material: Material): Double = when (material) {
        Material.WOODEN_AXE -> 7.0
        Material.STONE_AXE -> 9.0
        Material.IRON_AXE -> 9.0
        Material.GOLDEN_AXE -> 7.0
        Material.DIAMOND_AXE -> 9.0
        Material.NETHERITE_AXE -> 10.0
        Material.COPPER_AXE -> 8.0
        else -> 0.0
    }

    // 철퇴 재질별 바닐라 기본 데미지
    private fun baseMaceDamageOf(material: Material): Double = when (material) {
        Material.MACE -> 6.0
        else -> 0.0
    }

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
                meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE)
                meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS)
                val baseArmor = baseArmorOf(item.type)
                val armorBonus = settings.getDouble("enhancement-attributes.helmet.armor.$level", level * 0.5)
                val health = settings.getDouble("enhancement-attributes.helmet.health.$level", level * 1.0)
                val toughnessBonus = settings.getDouble("enhancement-attributes.helmet.toughness.$level", level * 0.1)
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(HELMET_ARMOR_KEY, baseArmor + armorBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                )
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(HELMET_HEALTH_KEY, health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(HELMET_TOUGHNESS_KEY, baseToughnessOf(item.type) + toughnessBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                )
                if (item.type == Material.NETHERITE_HELMET) {
                    meta.addAttributeModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(HELMET_KB_KEY, NETHERITE_KB_RESIST, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                    )
                }

            }

            EquipmentKind.CHESTPLATE -> {
                meta.removeAttributeModifier(Attribute.ARMOR)
                meta.removeAttributeModifier(Attribute.MAX_HEALTH)
                meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE)
                meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS)
                val baseArmor = baseArmorOf(item.type)
                val armorBonus = settings.getDouble("enhancement-attributes.chestplate.armor.$level", level * 1.0)
                val health = settings.getDouble("enhancement-attributes.chestplate.health.$level", level * 1.0)
                val toughnessBonus = settings.getDouble("enhancement-attributes.chestplate.toughness.$level", level * 0.1)
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(CHESTPLATE_ARMOR_KEY, baseArmor + armorBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                )
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(CHESTPLATE_HEALTH_KEY, health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(CHESTPLATE_TOUGHNESS_KEY, baseToughnessOf(item.type) + toughnessBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                )
                if (item.type == Material.NETHERITE_CHESTPLATE) {
                    meta.addAttributeModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(CHESTPLATE_KB_KEY, NETHERITE_KB_RESIST, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                    )
                }
            }

            EquipmentKind.LEGGINGS -> {
                meta.removeAttributeModifier(Attribute.ARMOR)
                meta.removeAttributeModifier(Attribute.MAX_HEALTH)
                meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE)
                meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS)
                val baseArmor = baseArmorOf(item.type)
                val armorBonus = settings.getDouble("enhancement-attributes.leggings.armor.$level", level * 0.5)
                val health = settings.getDouble("enhancement-attributes.leggings.health.$level", level * 1.0)
                val toughnessBonus = settings.getDouble("enhancement-attributes.leggings.toughness.$level", level * 0.1)
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(LEGGINGS_ARMOR_KEY, baseArmor + armorBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                )
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(LEGGINGS_HEALTH_KEY, health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(LEGGINGS_TOUGHNESS_KEY, baseToughnessOf(item.type) + toughnessBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                )
                if (item.type == Material.NETHERITE_LEGGINGS) {
                    meta.addAttributeModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(LEGGINGS_KB_KEY, NETHERITE_KB_RESIST, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                    )
                }
            }

            EquipmentKind.BOOTS -> {
                meta.removeAttributeModifier(Attribute.MAX_HEALTH)
                meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED)
                meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE)
                meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS)
                meta.removeAttributeModifier(Attribute.ARMOR)
                val baseArmor = baseArmorOf(item.type)
                val health = settings.getDouble("enhancement-attributes.boots.health.$level", level * 1.0)
                val speed = settings.getDouble("enhancement-attributes.boots.speed.$level", level * 0.05)
                val toughnessBonus = settings.getDouble("enhancement-attributes.boots.toughness.$level", level * 0.1)
                val armorBonus = settings.getDouble("enhancement-attributes.boots.armor.$level", level * 0.5)
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(BOOTS_ARMOR_KEY, baseArmor + armorBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                )
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(BOOTS_HEALTH_KEY, health, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                )
                meta.addAttributeModifier(
                    Attribute.MOVEMENT_SPEED,
                    AttributeModifier(BOOTS_SPEED_KEY, speed, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.FEET)
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(BOOTS_TOUGHNESS_KEY, baseToughnessOf(item.type) + toughnessBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                )
                if (item.type == Material.NETHERITE_BOOTS) {
                    meta.addAttributeModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(BOOTS_KB_KEY, NETHERITE_KB_RESIST, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                    )
                }
            }

            EquipmentKind.SWORD -> {
                meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE)
                meta.removeAttributeModifier(Attribute.ATTACK_SPEED)
                val baseDamage = baseSwordDamageOf(item.type)
                val enhanceBonus = settings.getDouble("enhancement-attributes.sword.damage.$level", level * 1.0)
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(SWORD_DAMAGE_KEY, baseDamage + enhanceBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
                )
                // 커스텀 ATTACK_DAMAGE를 추가하면 바닐라 기본 공격속도 수정치(-2.4, 결과 1.6)가
                // 함께 사라지므로 반드시 다시 명시해줘야 한다.
                meta.addAttributeModifier(
                    Attribute.ATTACK_SPEED,
                    AttributeModifier(SWORD_ATTACK_SPEED_KEY, -2.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
                )
            }
            EquipmentKind.AXE -> {
                meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE)
                val baseDamage = baseAxeDamageOf(item.type)
                val enhanceBonus = settings.getDouble("enhancement-attributes.axe.damage.$level", level * 1.0)
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(AXE_DAMAGE_KEY, baseDamage + enhanceBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
                )
                // 커스텀 ATTACK_DAMAGE를 추가하면 바닐라 기본 공격속도 수정치(-2.4, 결과 1.6)가
                // 함께 사라지므로 반드시 다시 명시해줘야 한다.
                meta.addAttributeModifier(
                    Attribute.ATTACK_SPEED,
                    AttributeModifier(AXE_ATTACK_SPEED_KEY, -3.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
                )
            }


            EquipmentKind.MACE -> {
                meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE)
                meta.removeAttributeModifier(Attribute.ATTACK_SPEED)
                val damage = settings.getDouble("enhancement-attributes.mace.damage.$level", level * 0.5)
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(MACE_DAMAGE_KEY, damage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
                )
                // 철퇴 바닐라 기본 공격속도: 0.6 (기본값 4.0에서 -3.4)
                meta.addAttributeModifier(
                    Attribute.ATTACK_SPEED,
                    AttributeModifier(MACE_ATTACK_SPEED_KEY, -3.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)
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