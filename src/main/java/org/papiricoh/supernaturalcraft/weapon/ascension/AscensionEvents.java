package org.papiricoh.supernaturalcraft.weapon.ascension;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.papiricoh.supernaturalcraft.hunter.gear.HunterGearItem;
import org.papiricoh.supernaturalcraft.reward.michael.GeneralArmorItem;

/**
 * What Ascension does to a weapon's and an armour piece's attributes (v0.15).
 * <ul>
 *   <li>A weapon's own attack damage (its base modifier) is multiplied by {@link Ascension#multiplier}: the tooltip's
 *   "Attack Damage" line shows the ascended number.</li>
 *   <li>Hunter's Gear: armour ×(1 + {@link #GEAR_ARMOR_PER_TIER} per tier) (the full set reaches vanilla's 30 at IV) and
 *   {@link #GEAR_TOUGHNESS_PER_TIER} toughness per tier on each piece (8 for the set at IV).</li>
 *   <li>The General's armour at V: +2 armour on each piece but the boots (+1), +1 toughness each (the set reaches 30 and 20).</li>
 * </ul>
 */
public final class AscensionEvents {

    public static final float GEAR_ARMOR_PER_TIER = 0.25f, GEAR_TOUGHNESS_PER_TIER = 0.5f;

    private AscensionEvents() {
    }

    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        int level = Ascension.level(stack);
        if (level <= 0) return;
        Item item = stack.getItem();
        if (item instanceof ArmorItem armor && Ascension.isArmor(stack)) {
            armour(event, armor, level);
            return;
        }
        float base = baseAttack(event.getDefaultModifiers());
        if (base <= 0) return;
        event.replaceModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                base * Ascension.multiplier(level), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    /** The item's own attack damage modifier (what its tooltip calls Attack Damage, less the hand's 1), or 0. */
    public static float baseAttack(ItemAttributeModifiers modifiers) {
        for (ItemAttributeModifiers.Entry e : modifiers.modifiers()) {
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE) && e.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)) {
                return (float) e.modifier().amount();
            }
        }
        return 0f;
    }

    /** Armour and toughness an ascended piece adds over its material's, at {@code level}. */
    public static float[] armourBonus(ArmorItem armor, int level) {
        if (armor instanceof HunterGearItem) {
            return new float[]{armor.getDefense() * GEAR_ARMOR_PER_TIER * level, GEAR_TOUGHNESS_PER_TIER * level};
        }
        if (armor instanceof GeneralArmorItem && level > Ascension.GENERAL_BASE) {
            return new float[]{armor.getType() == ArmorItem.Type.BOOTS ? 1f : 2f, 1f};
        }
        return new float[]{0f, 0f};
    }

    private static void armour(ItemAttributeModifierEvent event, ArmorItem armor, int level) {
        float[] bonus = armourBonus(armor, level);
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(armor.getEquipmentSlot());
        ResourceLocation id = ResourceLocation.withDefaultNamespace("armor." + armor.getType().getName());
        bump(event, Attributes.ARMOR, id, armor.getDefense(), bonus[0], slot);
        bump(event, Attributes.ARMOR_TOUGHNESS, id, armor.getToughness(), bonus[1], slot);
    }

    private static void bump(ItemAttributeModifierEvent event, Holder<Attribute> attribute, ResourceLocation id, float base, float bonus,
                             EquipmentSlotGroup slot) {
        if (bonus <= 0) return;
        event.replaceModifier(attribute, new AttributeModifier(id, base + bonus, AttributeModifier.Operation.ADD_VALUE), slot);
    }
}
