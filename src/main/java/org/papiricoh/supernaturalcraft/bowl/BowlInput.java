package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** A bowl's contents as recipe input: the ingredients, and the liquids alongside. */
public record BowlInput(List<ItemStack> items, List<BowlLiquid> liquids) implements RecipeInput {

    public static BowlInput of(BowlContents contents) {
        return new BowlInput(contents.stacks(), contents.liquidKinds());
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
