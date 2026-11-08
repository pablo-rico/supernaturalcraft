package org.papiricoh.supernaturalcraft.hands;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;

import java.util.List;

/**
 * One of the five Hands of God (v0.42): a relic touched by God's own power, left on Earth. Not a weapon: three of them (or more)
 * are what Amara's rite asks for, and the arena keeps them as its stake (returned if the fight is lost). Lore in the tooltip
 * ({@code tooltip.supernaturalcraft.hand.<id>}).
 */
public class HandItem extends Item {

    private final S11Assets.Hand hand;

    public HandItem(S11Assets.Hand hand, Properties properties) {
        super(properties);
        this.hand = hand;
    }

    public S11Assets.Hand hand() {
        return hand;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.hand." + hand.item).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.hand_of_god").withStyle(ChatFormatting.GOLD));
    }
}
