package org.papiricoh.supernaturalcraft.nephilim;

import org.papiricoh.supernaturalcraft.nephilim.rite.OpenApocalypseRiftEffect;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * v0.41 core's ritual effects, registered from {@code RitualEffect.bootstrap()} (and their JEI names
 * {@code jei.supernaturalcraft.effect.<ns>.<path>} in {@code NephilimData}).
 */
public final class NephilimRites {

    private NephilimRites() {
    }

    public static void bootstrap() {
        RitualEffect.register(OpenApocalypseRiftEffect.ID, OpenApocalypseRiftEffect.CODEC);
        RitualEffect.register(org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusSummoning.Rite.ID,
                org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusSummoning.Rite.CODEC);
        RitualEffect.register(org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselSummoning.Rite.ID,
                org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselSummoning.Rite.CODEC);
        RitualEffect.register(org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackSummoning.Rite.ID,
                org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackSummoning.Rite.CODEC);
        RitualEffect.register(org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowSummoning.Rite.ID,
                org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowSummoning.Rite.CODEC);
        RitualEffect.register(org.papiricoh.supernaturalcraft.empty.OpenTheEmptyEffect.ID, org.papiricoh.supernaturalcraft.empty.OpenTheEmptyEffect.CODEC);
    }
}
