package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The key to the Men of Letters' bunker, from Henry (v0.17): used on the bunker's door, it opens or shuts it. */
public class BunkerKeyItem extends Item {

    public BunkerKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.bunker_key").withStyle(ChatFormatting.GRAY));
    }
}
