package org.papiricoh.supernaturalcraft.magic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Hexed ink (v0.21): ink steeped in the bowl in one school's magic ({@code school} component, fixed by the potion poured into
 * the distillation). Lends a sigil written with it that school's touch.
 */
public class HexedInkItem extends Item {

    public HexedInkItem(Properties props) {
        super(props);
    }

    public static ItemStack of(School school) {
        ItemStack stack = new ItemStack(AllItems.HEXED_INK.get());
        stack.set(AllDataComponents.SCHOOL.get(), school);
        return stack;
    }

    public static School school(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.SCHOOL.get(), School.NONE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        School school = school(stack);
        if (school.isNone()) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.hexed_ink.inert").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.hexed_ink.school",
                    Component.translatable("school.supernaturalcraft." + school.id())).withColor(school.primary()));
        }
    }
}
