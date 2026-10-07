package org.papiricoh.supernaturalcraft.hunter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Warms when something supernatural is near and steadies the wearer's sigils. The proximity
 * check runs from {@link AmuletEvents} so it also works from a Curios slot.
 */
public class HuntersAmuletItem extends Item {

    public HuntersAmuletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.hunters_amulet").withStyle(ChatFormatting.GRAY));
    }
}
