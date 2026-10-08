package org.papiricoh.supernaturalcraft.hunter.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Hunter's Gear (v0.15): a hunter's early armour (cap, jacket, jeans, boots). It ascends at the Hellforge
 * from I to IV; each tier hardens it and adds Aegis against the great enemies.
 */
public class HunterGearItem extends ArmorItem {

    public HunterGearItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.hunters_gear").withStyle(ChatFormatting.GRAY));
    }
}
