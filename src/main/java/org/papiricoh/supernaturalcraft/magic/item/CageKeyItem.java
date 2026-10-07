package org.papiricoh.supernaturalcraft.magic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The Key to the Cage: the activator of the summoning rite. */
public class CageKeyItem extends Item {

    public CageKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.key_to_the_cage").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
