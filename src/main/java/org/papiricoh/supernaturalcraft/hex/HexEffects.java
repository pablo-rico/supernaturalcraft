package org.papiricoh.supernaturalcraft.hex;

import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;

/** Registers the hex bags' bowl spell effect, {@code make_hex_bag}. */
public final class HexEffects {

    private HexEffects() {
    }

    public static void bootstrap() {
        BowlSpellEffect.register(MakeHexBagEffect.ID, MakeHexBagEffect.CODEC);
    }
}
