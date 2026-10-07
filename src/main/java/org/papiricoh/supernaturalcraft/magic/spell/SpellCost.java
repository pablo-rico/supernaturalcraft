package org.papiricoh.supernaturalcraft.magic.spell;

import java.util.List;

/**
 * Mana arithmetic, kept free of game classes so it can be unit-tested: the base cost of the
 * form and effects, multiplied by every modifier, then by any discount.
 */
public final class SpellCost {

    private SpellCost() {
    }

    public static float mana(float formCost, List<Float> effectCosts, List<Float> modifierMultipliers, float discount) {
        float base = formCost;
        for (float c : effectCosts) base += c;
        float mult = 1.0f;
        for (float m : modifierMultipliers) mult *= m;
        return Math.max(0, base * mult * discount);
    }

    /** Scrolls carry their inscription with them: casting from one costs half. */
    public static final float SCROLL_DISCOUNT = 0.5f;
    /** The Hunter's Amulet steadies the hand. */
    public static final float AMULET_DISCOUNT = 0.9f;
}
