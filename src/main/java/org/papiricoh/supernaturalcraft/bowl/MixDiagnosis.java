package org.papiricoh.supernaturalcraft.bowl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * How far a bowl's mix is from a recipe (pure, tested in JUnit): which liquids and ingredients it lacks, and which of what
 * it holds has no place in it. A backlash names the nearest spell the caster knows and what was wrong with the mix
 * ({@link BowlBacklash}), so a hunter who misread a page is told so instead of just being burned.
 *
 * <p>Ingredients are matched to what the bowl holds as a maximum bipartite matching (an item may answer to several
 * ingredients: a tag, say), so the gap is never larger than it has to be.
 */
public final class MixDiagnosis {

    /** A gap no larger than this is a near miss worth naming; anything further is just the wrong mix. */
    public static final int NEAR = 3;

    /**
     * What a mix lacks and holds in excess, against one recipe.
     *
     * @param missingLiquids     the recipe's liquids the bowl lacks (with repeats)
     * @param extraLiquids       the bowl's liquids the recipe does not take (with repeats)
     * @param missingIngredients indices into the recipe's ingredients nothing in the bowl answered to
     * @param extraItems         indices into the bowl's items no ingredient asked for
     * @param matched            how many of the recipe's liquids and ingredients the bowl did hold
     */
    public record Gap(List<BowlLiquid> missingLiquids, List<BowlLiquid> extraLiquids, List<Integer> missingIngredients,
                      List<Integer> extraItems, int matched) {

        /** Everything wrong, counted: 0 is a match. */
        public int size() {
            return missingLiquids.size() + extraLiquids.size() + missingIngredients.size() + extraItems.size();
        }

        /** Close enough to name: a few things off, and at least half the recipe in the bowl. */
        public boolean near(int recipeSize) {
            int size = size();
            return size > 0 && size <= NEAR && matched >= Math.max(1, (recipeSize + 1) / 2);
        }

        public boolean lacksSomething() {
            return !missingLiquids.isEmpty() || !missingIngredients.isEmpty();
        }

        public boolean holdsSomethingExtra() {
            return !extraLiquids.isEmpty() || !extraItems.isEmpty();
        }
    }

    private MixDiagnosis() {
    }

    /**
     * The gap between a recipe ({@code wantLiquids}, {@code wantIngredients}) and a bowl ({@code haveLiquids},
     * {@code haveItems}); {@code accepts} says whether an ingredient takes an item.
     */
    public static <I, S> Gap compare(List<BowlLiquid> wantLiquids, List<BowlLiquid> haveLiquids, List<I> wantIngredients,
                                     List<S> haveItems, BiPredicate<I, S> accepts) {
        List<BowlLiquid> missingLiquids = new ArrayList<>(wantLiquids);
        List<BowlLiquid> extraLiquids = new ArrayList<>();
        for (BowlLiquid l : haveLiquids) {
            if (!missingLiquids.remove(l)) extraLiquids.add(l);
        }
        int liquidsMatched = wantLiquids.size() - missingLiquids.size();

        // ingredientOf[item] = the ingredient it answers for, or -1.
        int[] ingredientOf = new int[haveItems.size()];
        Arrays.fill(ingredientOf, -1);
        int[] itemOf = new int[wantIngredients.size()];
        Arrays.fill(itemOf, -1);
        for (int ing = 0; ing < wantIngredients.size(); ing++) {
            augment(ing, wantIngredients, haveItems, accepts, itemOf, ingredientOf, new boolean[haveItems.size()]);
        }
        List<Integer> missingIngredients = new ArrayList<>();
        for (int ing = 0; ing < itemOf.length; ing++) if (itemOf[ing] < 0) missingIngredients.add(ing);
        List<Integer> extraItems = new ArrayList<>();
        for (int item = 0; item < ingredientOf.length; item++) if (ingredientOf[item] < 0) extraItems.add(item);
        int matched = liquidsMatched + wantIngredients.size() - missingIngredients.size();
        return new Gap(List.copyOf(missingLiquids), List.copyOf(extraLiquids), List.copyOf(missingIngredients),
                List.copyOf(extraItems), matched);
    }

    /** Kuhn's augmenting path: finds ingredient {@code ing} an item, moving earlier pairings along if it must. */
    private static <I, S> boolean augment(int ing, List<I> ingredients, List<S> items, BiPredicate<I, S> accepts,
                                          int[] itemOf, int[] ingredientOf, boolean[] seen) {
        for (int item = 0; item < items.size(); item++) {
            if (seen[item] || !accepts.test(ingredients.get(ing), items.get(item))) continue;
            seen[item] = true;
            if (ingredientOf[item] < 0 || augment(ingredientOf[item], ingredients, items, accepts, itemOf, ingredientOf, seen)) {
                ingredientOf[item] = ing;
                itemOf[ing] = item;
                return true;
            }
        }
        return false;
    }
}
