package org.papiricoh.supernaturalcraft.bowl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixDiagnosisTest {

    /** An ingredient is the set of item names it takes (a tag holds several). */
    private static final BiPredicate<Set<String>, String> TAKES = Set::contains;

    private static MixDiagnosis.Gap gap(List<BowlLiquid> want, List<BowlLiquid> have, List<Set<String>> ingredients, List<String> items) {
        return MixDiagnosis.compare(want, have, ingredients, items, TAKES);
    }

    @Test
    void anExactMixHasNoGap() {
        var g = gap(List.of(BowlLiquid.HOLY_WATER, BowlLiquid.WATER), List.of(BowlLiquid.WATER, BowlLiquid.HOLY_WATER),
                List.of(Set.of("salt"), Set.of("bone")), List.of("bone", "salt"));
        assertEquals(0, g.size());
        assertEquals(4, g.matched());
        assertFalse(g.near(4), "a match is not a near miss");
    }

    @Test
    void itNamesWhatIsMissingAndWhatIsExtra() {
        var g = gap(List.of(BowlLiquid.HOLY_WATER, BowlLiquid.HOLY_WATER), List.of(BowlLiquid.HOLY_WATER, BowlLiquid.WATER),
                List.of(Set.of("spider_eye"), Set.of("ectoplasm"), Set.of("amethyst")), List.of("ectoplasm", "dirt", "spider_eye"));
        assertEquals(List.of(BowlLiquid.HOLY_WATER), g.missingLiquids());
        assertEquals(List.of(BowlLiquid.WATER), g.extraLiquids());
        assertEquals(List.of(2), g.missingIngredients(), "the amethyst");
        assertEquals(List.of(1), g.extraItems(), "the dirt");
        assertEquals(4, g.size());
        assertTrue(g.lacksSomething() && g.holdsSomethingExtra());
    }

    @Test
    void aTagIngredientNeverStealsAnItemAnotherNeeds() {
        // The tag takes gold or salt; the second ingredient only salt. Greedy would hand the salt to the tag.
        var g = gap(List.of(), List.of(), List.of(Set.of("salt", "gold"), Set.of("salt")), List.of("salt", "gold"));
        assertEquals(0, g.size(), "gold answers the tag, salt the other");
    }

    @Test
    void repeatedIngredientsCountEachOne() {
        var g = gap(List.of(), List.of(), List.of(Set.of("chain"), Set.of("chain"), Set.of("iron")), List.of("chain", "iron"));
        assertEquals(List.of(1), g.missingIngredients(), "one chain short");
        assertEquals(2, g.matched());
    }

    @Test
    void onlyACloseMixIsANearMiss() {
        var oneOff = gap(List.of(BowlLiquid.HOLY_WATER), List.of(BowlLiquid.HOLY_WATER),
                List.of(Set.of("a"), Set.of("b"), Set.of("c")), List.of("a", "b"));
        assertTrue(oneOff.near(4), "one ingredient short");
        var stranger = gap(List.of(BowlLiquid.HOLY_WATER), List.of(BowlLiquid.WATER),
                List.of(Set.of("a"), Set.of("b"), Set.of("c")), List.of("x"));
        assertFalse(stranger.near(4), "nothing in common");
        var halfway = gap(List.of(), List.of(), List.of(Set.of("a"), Set.of("b"), Set.of("c"), Set.of("d")), List.of("a"));
        assertFalse(halfway.near(4), "a quarter of the recipe is not nearly it");
        var tooMuch = gap(List.of(), List.of(), List.of(Set.of("a"), Set.of("b")), List.of("a", "b", "x", "y", "z", "w"));
        assertFalse(tooMuch.near(2), "four things too many");
    }
}
