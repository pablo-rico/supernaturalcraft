package org.papiricoh.supernaturalcraft.allegiance.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A vial of Grace (v0.13): what Heaven's messenger gives a hunter who heeds him (the key to the rite Receive Grace), and
 * what an angel who rips out their Grace is left holding.
 */
public class VialOfGraceItem extends Item {

    public VialOfGraceItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.vial_of_grace").withStyle(net.minecraft.ChatFormatting.GOLD));
    }
}
