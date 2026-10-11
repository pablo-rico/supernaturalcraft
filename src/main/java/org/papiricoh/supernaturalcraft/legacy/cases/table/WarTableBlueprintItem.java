package org.papiricoh.supernaturalcraft.legacy.cases.table;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The war table's blueprint (v0.19), the reward for researching the order's cartography: crafted with glass, glowstone,
 * copper and dark oak into map table pieces, it comes back out of the grid every time (and can be copied).
 */
public class WarTableBlueprintItem extends Item {

    public WarTableBlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.war_table_blueprint").withStyle(ChatFormatting.GRAY));
    }
}
