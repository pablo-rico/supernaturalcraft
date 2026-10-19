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

/**
 * Lookups over the loaded bowl spell recipes and pages: which spells exist, which recipes a page teaches, which a player knows.
 * A recipe is taught by its group (its {@code spell} field) and, since v0.21, also by a page with its own id (the recipe file's
 * name): {@code break_deal} has a page of its own while the old Purification page still teaches it with its group.
 */
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

    /** The recipe's own id as a page would name it: {@code ns:bowl_spell/break_deal} → {@code ns:break_deal}. */
    public static ResourceLocation ownId(RecipeHolder<BowlSpellRecipe> holder) {
        return holder.id().withPath(p -> p.substring(p.lastIndexOf('/') + 1));
    }

    /** Whether the page (or group) {@code spell} teaches this recipe: its group, or its own id. */
    public static boolean teaches(ResourceLocation spell, RecipeHolder<BowlSpellRecipe> holder) {
        return spellOf(holder).equals(spell) || ownId(holder).equals(spell);
    }

    /** Whether {@code arcana} knows this recipe: its group's page or its own page has been read. */
    public static boolean knows(org.papiricoh.supernaturalcraft.magic.mana.ArcanaData arcana, RecipeHolder<BowlSpellRecipe> holder) {
        return arcana.knowsRite(spellOf(holder)) || arcana.knowsRite(ownId(holder));
    }

    /** The same rule over a plain set of known rites (the client's mirror, {@code ClientArcana.rites()}). */
    public static boolean knows(java.util.Set<ResourceLocation> rites, RecipeHolder<BowlSpellRecipe> holder) {
        return rites.contains(spellOf(holder)) || rites.contains(ownId(holder));
    }

    /**
     * Whether the page {@code page} has nothing left to teach: read itself, or every recipe it teaches is known already by its
     * group or its own id.
     */
    public static boolean knowsPage(java.util.Set<ResourceLocation> rites, RecipeManager recipes, ResourceLocation page) {
        if (rites.contains(page)) return true;
        List<RecipeHolder<BowlSpellRecipe>> taught = recipesFor(recipes, page);
        return !taught.isEmpty() && taught.stream().allMatch(h -> knows(rites, h));
    }

    /** Every recipe that the page {@code spell} teaches (its group's, or the one recipe of that id). */
    public static List<RecipeHolder<BowlSpellRecipe>> recipesFor(RecipeManager recipes, ResourceLocation spell) {
        return all(recipes).stream().filter(h -> teaches(spell, h)).toList();
    }

    /** The registered page {@code id} (school, tier, weight), if the datapacks define one (v0.21). */
    public static java.util.Optional<org.papiricoh.supernaturalcraft.bowl.page.SpellPage> page(net.minecraft.core.RegistryAccess access, ResourceLocation id) {
        return access.registry(org.papiricoh.supernaturalcraft.registry.SNRegistries.BOWL_SPELL_PAGE).flatMap(r -> r.getOptional(id));
    }

    /**
     * A page picked at random from the {@code bowl_spell_page} registry by weight, among pages some loaded recipe answers to;
     * without registered pages, as {@link #randomSpell(RecipeManager, RandomSource)}. Null if none.
     */
    @Nullable
    public static ResourceLocation randomSpell(net.minecraft.core.RegistryAccess access, RecipeManager recipes, RandomSource random) {
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
        access.registry(org.papiricoh.supernaturalcraft.registry.SNRegistries.BOWL_SPELL_PAGE).ifPresent(r -> r.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .forEach(e -> {
                    if (e.getValue().weight() > 0 && !recipesFor(recipes, e.getKey().location()).isEmpty()) {
                        weights.put(e.getKey().location(), e.getValue().weight());
                    }
                }));
        if (weights.isEmpty()) return randomSpell(recipes, random);
        return pick(weights, random);
    }

    /**
     * As {@link #randomSpell(net.minecraft.core.RegistryAccess, RecipeManager, RandomSource)}, among registered pages of
     * {@code school} (any if null) and at most {@code maxTier} (v0.21: a witch's satchel). Null if none fits.
     */
    @Nullable
    public static ResourceLocation randomSpell(net.minecraft.core.RegistryAccess access, RecipeManager recipes, RandomSource random,
                                               @Nullable org.papiricoh.supernaturalcraft.magic.school.School school, int maxTier) {
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
        access.registry(org.papiricoh.supernaturalcraft.registry.SNRegistries.BOWL_SPELL_PAGE).ifPresent(r -> r.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .forEach(e -> {
                    var page = e.getValue();
                    if (page.weight() > 0 && page.tier() <= maxTier && (school == null || page.school() == school)
                            && !recipesFor(recipes, e.getKey().location()).isEmpty()) {
                        weights.put(e.getKey().location(), page.weight());
                    }
                }));
        return pick(weights, random);
    }

    /** Distinct spell ids, sorted. */
    public static List<ResourceLocation> allSpells(RecipeManager recipes) {
        return all(recipes).stream().map(BowlSpells::spellOf).distinct().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    /**
     * A spell group picked at random, each weighted by the sum of its recipes' page weights; null if none. Used where no registry
     * is at hand; the game picks registered pages ({@link #randomSpell(net.minecraft.core.RegistryAccess, RecipeManager, RandomSource)}).
     */
    @Nullable
    public static ResourceLocation randomSpell(RecipeManager recipes, RandomSource random) {
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
        for (var h : all(recipes)) weights.merge(spellOf(h), h.value().pageWeight(), Integer::sum);
        return pick(weights, random);
    }

    @Nullable
    private static ResourceLocation pick(Map<ResourceLocation, Integer> weights, RandomSource random) {
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
