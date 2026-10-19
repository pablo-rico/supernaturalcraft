package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * A bowl's contents as recipe input: the ingredients, and the liquids alongside. {@code namedBlood} (v0.21) counts the blood
 * doses that name their owner: a spell aimed at someone ({@code requires_target}) takes one of those as its personal component
 * instead of a taglock, without asking for blood among its liquids.
 */
public record BowlInput(List<ItemStack> items, List<BowlLiquid> liquids, int namedBlood) implements RecipeInput {

    public BowlInput(List<ItemStack> items, List<BowlLiquid> liquids) {
        this(items, liquids, 0);
    }

    public static BowlInput of(BowlContents contents) {
        int named = (int) contents.liquids().stream().filter(d -> d.kind() == BowlLiquid.BLOOD && d.owner().isPresent()).count();
        return new BowlInput(contents.stacks(), contents.liquidKinds(), named);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    /** Liquids count too: a bowl of nothing but water is not empty (vanilla skips empty inputs). */
    @Override
    public boolean isEmpty() {
        return liquids.isEmpty() && items.stream().allMatch(ItemStack::isEmpty);
    }
}
