package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.UUID;

/**
 * Bound blood (v0.21): a vial of someone's blood bound to them in the spell bowl ({@link BloodSample} component, as a blood
 * vial's). A reagent rites and the strongest curses ask for; as a bowl ingredient it also names its owner as the target
 * ({@code bowl.PersonalComponent}).
 */
public class BoundBloodItem extends Item {

    public BoundBloodItem(Properties props) {
        super(props);
    }

    public static ItemStack of(UUID owner, String name) {
        ItemStack stack = new ItemStack(AllItems.BOUND_BLOOD.get());
        stack.set(AllDataComponents.BLOOD_SAMPLE.get(), new BloodSample(owner, name));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BloodSample sample = stack.get(AllDataComponents.BLOOD_SAMPLE.get());
        tooltip.add(sample == null
                ? Component.translatable("tooltip.supernaturalcraft.bound_blood.unbound").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.supernaturalcraft.bound_blood.of", sample.name()).withStyle(ChatFormatting.DARK_RED));
    }
}
