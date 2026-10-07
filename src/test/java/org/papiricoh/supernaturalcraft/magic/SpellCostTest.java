package org.papiricoh.supernaturalcraft.magic;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCost;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellCostTest {

    @Test
    void baseCostIsFormPlusEffects() {
        assertEquals(28f, SpellCost.mana(10, List.of(12f, 6f), List.of(), 1f), 1e-4);
    }

    @Test
    void modifiersMultiply() {
        // Bolt(10) + Smite(12), Empower ×1.6 and Widen ×1.5 → 22 × 2.4
        assertEquals(52.8f, SpellCost.mana(10, List.of(12f), List.of(1.6f, 1.5f), 1f), 1e-3);
    }

    @Test
    void discountsApplyLast() {
        float full = SpellCost.mana(10, List.of(12f), List.of(1.6f), 1f);
        assertEquals(full * SpellCost.SCROLL_DISCOUNT * SpellCost.AMULET_DISCOUNT,
                SpellCost.mana(10, List.of(12f), List.of(1.6f), SpellCost.SCROLL_DISCOUNT * SpellCost.AMULET_DISCOUNT), 1e-4);
    }

    @Test
    void neverNegative() {
        assertEquals(0f, SpellCost.mana(-5, List.of(), List.of(), 1f));
    }
}
