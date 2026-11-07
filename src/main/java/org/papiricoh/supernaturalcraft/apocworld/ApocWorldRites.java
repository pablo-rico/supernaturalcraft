package org.papiricoh.supernaturalcraft.apocworld;

import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * v0.41: this slice's ritual effects, registered from {@code RitualEffect.bootstrap()} with
 * {@code RitualEffect.register(id, codec)} (and their JEI names {@code jei.supernaturalcraft.effect.<ns>.<path>} in the lang).
 */
public final class ApocWorldRites {

    private ApocWorldRites() {
    }

    public static void bootstrap() {
        RitualEffect.register(SummonAuMichaelEffect.ID, SummonAuMichaelEffect.CODEC);
    }
}
