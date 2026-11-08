package org.papiricoh.supernaturalcraft.hands;

import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * v0.42: this slice's ritual effects, registered from {@code RitualEffect.bootstrap()} with
 * {@code RitualEffect.register(id, codec)} (and their JEI names {@code jei.supernaturalcraft.effect.<ns>.<path>} in the lang):
 * the tracking rite ({@link TrackHandsEffect}).
 */
public final class HandsRites {

    private HandsRites() {
    }

    public static void bootstrap() {
        RitualEffect.register(TrackHandsEffect.ID, TrackHandsEffect.CODEC);
    }
}
