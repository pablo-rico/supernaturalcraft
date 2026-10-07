package org.papiricoh.supernaturalcraft.bowl.spell;

import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsHooks;

/** Registers the spells' bowl effects (locate, purify, bind, banish, revive_pet) and plugs in the pet ledger. */
public final class SpellEffects {

    private SpellEffects() {
    }

    public static void bootstrap() {
        BowlSpellEffect.register(LocateEffect.ID, LocateEffect.CODEC);
        BowlSpellEffect.register(PurifyEffect.ID, PurifyEffect.CODEC);
        BowlSpellEffect.register(BindEffect.ID, BindEffect.CODEC);
        BowlSpellEffect.register(BanishEffect.ID, BanishEffect.CODEC);
        BowlSpellEffect.register(RevivePetEffect.ID, RevivePetEffect.CODEC);
        CrossroadsHooks.petRevival = PetRevivals.HOOK;
    }
}
