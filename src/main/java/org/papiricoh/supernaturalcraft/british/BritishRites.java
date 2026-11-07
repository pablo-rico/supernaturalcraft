package org.papiricoh.supernaturalcraft.british;

import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * v0.41: this slice's ritual effects, registered from {@code RitualEffect.bootstrap()} with
 * {@code RitualEffect.register(id, codec)} (and their JEI names {@code jei.supernaturalcraft.effect.<ns>.<path>} in the lang):
 * the nephilim's trace and the three rematches.
 */
public final class BritishRites {

    private BritishRites() {
    }

    public static void bootstrap() {
        RitualEffect.register(TraceNephilimEffect.ID, TraceNephilimEffect.CODEC);
        RitualEffect.register(BritishSummonEffect.Boss.KETCH.id(), BritishSummonEffect.KETCH);
        RitualEffect.register(BritishSummonEffect.Boss.HESS.id(), BritishSummonEffect.HESS);
        RitualEffect.register(BritishSummonEffect.Boss.DAGON.id(), BritishSummonEffect.DAGON);
    }
}
