package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lookups over the loaded bowl spell recipes: which spells exist, which recipes teach one. */
public final class BowlSpells {

    private BowlSpells() {
    }

    public static List<RecipeHolder<BowlSpellRecipe>> all(RecipeManager recipes) {
        return recipes.getAllRecipesFor(AllRecipes.BOWL_SPELL.get()).stream()
                .sorted(Comparator.comparing(h -> h.id().toString())).toList();
    }

    public static ResourceLocation spellOf(RecipeHolder<BowlSpellRecipe> holder) {
        return holder.value().spell(holder.id());
    }

    /** Every recipe that the spell {@code spell} teaches. */
    public static List<RecipeHolder<BowlSpellRecipe>> recipesFor(RecipeManager recipes, ResourceLocation spell) {
        return all(recipes).stream().filter(h -> spellOf(h).equals(spell)).toList();
    }

    /** Distinct spell ids, sorted. */
    public static List<ResourceLocation> allSpells(RecipeManager recipes) {
        return all(recipes).stream().map(BowlSpells::spellOf).distinct().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    /** A spell picked at random, each weighted by the sum of its recipes' page weights; null if none. */
    @Nullable
    public static ResourceLocation randomSpell(RecipeManager recipes, RandomSource random) {
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
        for (var h : all(recipes)) weights.merge(spellOf(h), h.value().pageWeight(), Integer::sum);
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (var e : new ArrayList<>(weights.entrySet())) {
            roll -= e.getValue();
            if (roll < 0) return e.getKey();
        }
        return null;
    }

    /** Lang key of a spell's name; {@code + ".desc"} for its description. */
    public static String nameKey(ResourceLocation spell) {
        return "bowl_spell." + spell.getNamespace() + "." + spell.getPath();
    }
}
