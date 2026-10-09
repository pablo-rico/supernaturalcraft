package org.papiricoh.supernaturalcraft.legacy.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/**
 * A cursed object ({@code ARTIFACT}, v0.17): "???" until researched at a desk; held in the off hand (or worn in a Curios charm
 * slot) its traits work ({@link ArtifactTraits}), identified or not.
 */
public class CursedArtifactItem extends Item {

    /** Name colour by rarity. */
    public static final ChatFormatting[] RARITY_COLORS = {ChatFormatting.WHITE, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD};

    public CursedArtifactItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ArtifactData d = stack.get(AllDataComponents.ARTIFACT.get());
        if (d == null) return super.getName(stack);
        ChatFormatting color = RARITY_COLORS[Math.max(0, Math.min(3, d.rarity()))];
        if (!d.identified()) {
            return Component.translatable("item.supernaturalcraft.cursed_artifact.unknown",
                    Component.translatable("artifact.supernaturalcraft.form." + d.form())).withStyle(color);
        }
        return Component.literal(d.name()).withStyle(color);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ArtifactData d = stack.get(AllDataComponents.ARTIFACT.get());
        return d != null && d.rarity() >= 3;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ArtifactData d = stack.get(AllDataComponents.ARTIFACT.get());
        if (d == null) return;
        tooltip.add(Component.translatable("artifact.supernaturalcraft.rarity." + Math.max(0, Math.min(3, d.rarity())))
                .withStyle(RARITY_COLORS[Math.max(0, Math.min(3, d.rarity()))]));
        if (!d.identified()) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.cursed_artifact.unknown").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (String boon : d.boons()) {
            tooltip.add(Component.literal("+ ").append(Component.translatable("artifact.supernaturalcraft.trait." + boon))
                    .withStyle(ChatFormatting.GREEN));
        }
        if (!d.curse().isEmpty()) {
            tooltip.add(Component.literal("- ").append(Component.translatable("artifact.supernaturalcraft.trait." + d.curse()))
                    .withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.cursed_artifact.hold").withStyle(ChatFormatting.DARK_GRAY));
    }
}
