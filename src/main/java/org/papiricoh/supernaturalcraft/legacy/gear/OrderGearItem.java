package org.papiricoh.supernaturalcraft.legacy.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A piece of the Men of Letters' gear a rank brings (v0.17): the ring (research a tenth faster while carried or worn) and the
 * Aquarian Star (one more research desk at once). What they do is read with {@code LegacyOrder.wears}.
 */
public class OrderGearItem extends Item {

    public OrderGearItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft." + BuiltInRegistries.ITEM.getKey(this).getPath()).withStyle(ChatFormatting.GRAY));
    }
}
