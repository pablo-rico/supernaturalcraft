package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/**
 * The spell bowl as carried: its contents ride along in {@link AllDataComponents#BOWL_CONTENTS}.
 * Held in the main hand it takes both hands (see {@link BowlCarry}); stowed, it keeps everything.
 */
public class SpellBowlItem extends BlockItem {

    public SpellBowlItem(Block block, Properties props) {
        super(block, props);
    }

    public static BowlContents contents(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.BOWL_CONTENTS.get(), BowlContents.EMPTY);
    }

    /** Lang key of a liquid's name. */
    public static String liquidKey(BowlLiquid liquid) {
        return "bowl_liquid.supernaturalcraft." + liquid.id();
    }

    /** A dose's name: the liquid's, or for blood whose it is. */
    public static Component doseName(Dose dose) {
        if (dose.kind() == BowlLiquid.BLOOD && dose.ownerName().isPresent()) {
            return Component.translatable("bowl_liquid.supernaturalcraft.blood_of", dose.ownerName().get());
        }
        return Component.translatable(liquidKey(dose.kind()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BowlContents contents = contents(stack);
        if (contents.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.spell_bowl.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!contents.liquids().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.spell_bowl.liquids",
                    contents.liquids().size(), BowlContents.MAX_DOSES).withStyle(ChatFormatting.GRAY));
            for (Dose dose : contents.liquids()) {
                tooltip.add(Component.literal("  ").append(doseName(dose)).withStyle(s -> s.withColor(dose.color() & 0xFFFFFF)));
            }
        }
        if (contents.itemCount() > 0) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.spell_bowl.ingredients",
                    contents.itemCount(), BowlContents.MAX_ITEMS).withStyle(ChatFormatting.GRAY));
            for (ItemStack item : contents.stacks()) {
                tooltip.add(Component.literal("  ").append(item.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.spell_bowl.carry").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
