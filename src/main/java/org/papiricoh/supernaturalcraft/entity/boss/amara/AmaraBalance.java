package org.papiricoh.supernaturalcraft.entity.boss.amara;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

/** The numbers of the fight against the Darkness, kept pure so they can be unit-tested. */
public final class AmaraBalance {

    public static final int WELLS = 4;
    /** Consumption thresholds: darkness and vignette, then slowness and mana drain, then harm. */
    public static final float DARKNESS_AT = 30, SLOW_AT = 60, HARM_AT = 100, MAX_CONSUMPTION = 100;
    /** Block light that counts as lit (stops consumption, empowers hits on a lit core). */
    public static final int LIT = 8, DIM = 4, LANCE_PROOF = 12;

    private AmaraBalance() {
    }

    /**
     * What a hit on her core is worth: the lit wells weaken her, holy strikes bite, and a core
     * standing in light is more vulnerable still.
     */
    public static float damageMultiplier(int litWells, boolean holy, boolean coreLit) {
        return (0.4f + 0.15f * litWells) * (holy ? 1.5f : 0.6f) * (coreLit ? 1.25f : 1f);
    }

    /** Change in a player's Consumption per second, for the block light at their head. */
    public static float consumptionPerSecond(int blockLight, boolean inVoid) {
        float d = 0;
        if (blockLight >= LIT) d -= 10;
        else if (blockLight < DIM) d += 2;
        if (inVoid) d += 5;
        return d;
    }

    public static float clampConsumption(float c) {
        return Math.max(0, Math.min(MAX_CONSUMPTION, c));
    }

    /** Her true health with {@code challengers}: the power curve's, plus a share per extra challenger. */
    public static float health(double perExtra, int challengers) {
        return scaled(ProgressionScale.trueHealth(Boss.AMARA), perExtra, challengers);
    }

    /** Health scaled for the number of challengers. */
    public static float scaled(double base, double perExtra, int challengers) {
        return (float) (base * (1 + perExtra * (Math.max(1, challengers) - 1)));
    }

    /** v0.42: how much less health she starts with for each Hand of God spent beyond the rite's three (at most two count). */
    public static final float PER_EXTRA_HAND = 0.05f;
    public static final int MAX_EXTRA_HANDS = 2;

    /** The share of her health the extra Hands take before the fight begins. */
    public static float handWeakening(int extraHands) {
        return PER_EXTRA_HAND * Math.max(0, Math.min(MAX_EXTRA_HANDS, extraHands));
    }
}
