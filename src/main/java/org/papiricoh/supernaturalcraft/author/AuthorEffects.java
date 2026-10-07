package org.papiricoh.supernaturalcraft.author;

import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;

/** The bowl spell "Find the Author" (Fallen Star, ink, paper, holy water): a map to the cabin. */
public final class AuthorEffects {

    private AuthorEffects() {
    }

    /** Registers the spell's effect types with {@code BowlSpellEffect}. */
    public static void bootstrap() {
        BowlSpellEffect.register(FindTheAuthorEffect.ID, FindTheAuthorEffect.CODEC);
    }
}
