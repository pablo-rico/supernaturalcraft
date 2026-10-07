package org.papiricoh.supernaturalcraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A carved rune, ready to be graved into a weapon at the Hellforge. Null rune = a blank. */
public class RuneItem extends Item {

    private final @Nullable Rune rune;

    public RuneItem(@Nullable Rune rune, Properties properties) {
        super(properties);
        this.rune = rune;
    }

    public @Nullable Rune rune() {
        return rune;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        if (rune == null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.rune.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.rune." + rune.getSerializedName()).withStyle(s -> s.withColor(rune.color)));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.rune.fit." + rune.fit.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
