package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/**
 * The contract a deal leaves in your hands: whose soul, for what, and how long until the hounds
 * come (or how it ended). Burn it in a bowl with Break the Deal to make the demon walk again.
 */
public class CrossroadsContractItem extends Item {

    public CrossroadsContractItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        ContractTerms terms = stack.get(AllDataComponents.CONTRACT.get());
        if (terms == null) {
            lines.add(Component.translatable("tooltip.supernaturalcraft.crossroads_contract.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("tooltip.supernaturalcraft.crossroads_contract.soul", terms.ownerName()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.supernaturalcraft.crossroads_contract.wish",
                Component.translatable(DealTerms.keyOf(terms.wish()))).withStyle(ChatFormatting.GRAY));
        lines.add(status(terms, context.level()));
    }

    private static Component status(ContractTerms terms, Level level) {
        return switch (terms.status()) {
            case ContractTerms.PAID -> Component.translatable("tooltip.supernaturalcraft.crossroads_contract.paid").withStyle(ChatFormatting.GOLD);
            case ContractTerms.VOID -> Component.translatable("tooltip.supernaturalcraft.crossroads_contract.void").withStyle(ChatFormatting.GOLD);
            case ContractTerms.COLLECTED -> Component.translatable("tooltip.supernaturalcraft.crossroads_contract.collected").withStyle(ChatFormatting.DARK_GRAY);
            default -> {
                if (level == null) yield Component.translatable("tooltip.supernaturalcraft.crossroads_contract.open").withStyle(ChatFormatting.DARK_RED);
                long now = level.getGameTime();
                if (DealTerms.due(now, terms.dueAt())) {
                    yield Component.translatable("tooltip.supernaturalcraft.crossroads_contract.due").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
                }
                if (DealTerms.lastDay(now, terms.dueAt())) {
                    yield Component.translatable("tooltip.supernaturalcraft.crossroads_contract.hours_left", DealTerms.hoursLeft(now, terms.dueAt()))
                            .withStyle(ChatFormatting.RED);
                }
                yield Component.translatable("tooltip.supernaturalcraft.crossroads_contract.days_left", DealTerms.daysLeft(now, terms.dueAt()))
                        .withStyle(ChatFormatting.DARK_RED);
            }
        };
    }
}
