package org.papiricoh.supernaturalcraft.ritual;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** The altar's offerings plus whatever the player is lighting the ritual with. */
public record RitualInput(List<ItemStack> offerings, ItemStack activator) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index < offerings.size() ? offerings.get(index) : activator;
    }

    @Override
    public int size() {
        return offerings.size() + 1;
    }
}
