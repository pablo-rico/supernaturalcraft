package org.papiricoh.supernaturalcraft.magic;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.magic.mastery.MasteryRules.Rank;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCost;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The one discount rule of v0.21 ({@link ArcanaCost#multiplier}): additive, capped, with the scroll kept apart. */
class ArcanaCostTest {

    /** The default human discount ({@code SNConfig.humanMagicDiscount}). */
    private static final float HUMAN = 0.15f;

    @Test
    void nothingOffForANoviceWithNothing() {
        assertEquals(1f, ArcanaCost.multiplier(false, HUMAN, false, Rank.NONE), 1e-6);
    }

    @Test
    void eachDiscountOnItsOwn() {
        assertEquals(0.85f, ArcanaCost.multiplier(true, HUMAN, false, Rank.NONE), 1e-6, "a human");
        assertEquals(1f - ArcanaCost.AMULET_DISCOUNT, ArcanaCost.multiplier(false, HUMAN, true, Rank.NONE), 1e-6, "the amulet");
        assertEquals(0.90f, ArcanaCost.multiplier(false, HUMAN, true, Rank.NONE), 1e-6);
        assertEquals(0.95f, ArcanaCost.multiplier(false, HUMAN, false, Rank.APPRENTICE), 1e-6);
        assertEquals(0.90f, ArcanaCost.multiplier(false, HUMAN, false, Rank.ADEPT), 1e-6);
        assertEquals(0.85f, ArcanaCost.multiplier(false, HUMAN, false, Rank.MASTER), 1e-6);
    }

    @Test
    void discountsAddUpRatherThanMultiply() {
        // 15 + 10 + 10 = 35 % off: 0.65, not 0.85 * 0.9 * 0.9 = 0.6885.
        assertEquals(0.65f, ArcanaCost.multiplier(true, HUMAN, true, Rank.ADEPT), 1e-6);
        assertEquals(0.80f, ArcanaCost.multiplier(true, HUMAN, false, Rank.APPRENTICE), 1e-6);
        assertEquals(0.75f, ArcanaCost.multiplier(false, HUMAN, true, Rank.MASTER), 1e-6);
    }

    @Test
    void neverMoreThanFortyPercentOff() {
        assertEquals(0.40f, ArcanaCost.MAX_DISCOUNT, 1e-6);
        // 15 + 10 + 15 = 40: exactly the cap.
        assertEquals(0.60f, ArcanaCost.multiplier(true, HUMAN, true, Rank.MASTER), 1e-6);
        // A server that raises the human discount still stops at the cap.
        assertEquals(0.60f, ArcanaCost.multiplier(true, 0.9f, true, Rank.MASTER), 1e-6);
        assertEquals(0.60f, ArcanaCost.multiplier(true, 1.0f, false, Rank.NONE), 1e-6);
        for (boolean human : new boolean[]{false, true}) {
            for (boolean amulet : new boolean[]{false, true}) {
                for (Rank r : Rank.values()) {
                    float m = ArcanaCost.multiplier(human, HUMAN, amulet, r);
                    assertTrue(m >= 1f - ArcanaCost.MAX_DISCOUNT - 1e-6 && m <= 1f, human + "/" + amulet + "/" + r + ": " + m);
                }
            }
        }
    }

    @Test
    void aNegativeHumanDiscountIsNoSurcharge() {
        assertEquals(1f, ArcanaCost.multiplier(true, -0.5f, false, Rank.NONE), 1e-6);
    }

    @Test
    void theScrollHalvesOnTopOfTheCap() {
        float capped = ArcanaCost.multiplier(true, HUMAN, true, Rank.MASTER);
        float base = SpellCost.mana(10, List.of(12f), List.of(), 1f);
        float fromScroll = SpellCost.mana(10, List.of(12f), List.of(), SpellCost.SCROLL_DISCOUNT * capped);
        assertEquals(base * 0.5f * 0.6f, fromScroll, 1e-4, "the scroll's half is not one of the capped discounts");
        assertTrue(fromScroll < base * (1f - ArcanaCost.MAX_DISCOUNT), "a scroll goes below the cap");
    }
}
