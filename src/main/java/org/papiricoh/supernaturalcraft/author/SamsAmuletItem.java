package org.papiricoh.supernaturalcraft.author;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Sam's amulet: worn (a Curios necklace, or in the hotbar or off hand without Curios, like the Hunter's Amulet) it
 * gives four more hearts and lets its wearer see the powerful nearby through walls; it shines when one is close.
 * The work is done each tick by {@link SamsAmulet}.
 */
public class SamsAmuletItem extends Item {

    public SamsAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.sams_amulet").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.sams_amulet.hearts").withStyle(ChatFormatting.BLUE));
    }
}
