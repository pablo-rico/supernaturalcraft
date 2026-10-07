package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A plain item with one line of lore: {@code tooltip.supernaturalcraft.<id>}. */
public class LoreItem extends Item {

    public LoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft." + BuiltInRegistries.ITEM.getKey(this).getPath())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
