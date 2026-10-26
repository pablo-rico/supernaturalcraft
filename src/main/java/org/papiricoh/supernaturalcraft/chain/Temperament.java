package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.Locale;

/**
 * An officer's character (pure): drawn from its {@code nameSeed}, so it never needs saving and the client reads it the same
 * way. It is narrative only, never a strength or a weakness in a fight. It colours what the officer says
 * ({@link org.papiricoh.supernaturalcraft.chain.narrative.VoiceLines}), how its bonds drift ({@link #superiorDrift},
 * {@link #peerDrift}), whether it hears out a hunter who comes to parley ({@link #parleys}) and how readily it throws down a
 * challenge ({@link #challengeChance}).
 */
public enum Temperament {
    /** Wants the post above; resents whoever holds it. */
    AMBITIOUS(-2, -1, 0.5, 0.35),
    /** Serves; warms to its superior, rarely challenges. */
    LOYAL(2, 1, 0.2, 0.05),
    /** Enjoys pain; never bargains with a hunter. */
    CRUEL(-1, -1, 0, 0.25),
    /** Schemes; listens to any offer and weighs it. */
    CUNNING(0, 0, 0.9, 0.15),
    /** Will not stand an insult; quick to take offence, slow to forgive. */
    PROUD(-1, -2, 0.3, 0.3),
    /** Lives for its side's cause; scorns doubt and deals. */
    ZEALOUS(1, 0, 0.1, 0.2);

    private final int superiorDrift, peerDrift;
    private final double parley, challenge;

    Temperament(int superiorDrift, int peerDrift, double parley, double challenge) {
        this.superiorDrift = superiorDrift;
        this.peerDrift = peerDrift;
        this.parley = parley;
        this.challenge = challenge;
    }

    /** The temperament of an officer named by this seed (stable for life, through ranks, scars and a Fall). */
    public static Temperament of(long nameSeed) {
        GenSeed.Rng rng = new GenSeed.Rng(GenSeed.mix(nameSeed ^ 0x54454D50L));
        return values()[rng.nextInt(values().length)];
    }

    public static Temperament of(Officer o) {
        return of(o.nameSeed);
    }

    /** How its regard for its own superior drifts each day (points, before the pull back to zero). */
    public int superiorDrift() {
        return superiorDrift;
    }

    /** How its regard for officers of its own rank drifts each day. */
    public int peerDrift() {
        return peerDrift;
    }

    /** The chance it hears a hunter of the other side out instead of attacking. */
    public double parleys() {
        return parley;
    }

    /** The chance a grudge it holds turns into a challenge on a given day. */
    public double challengeChance() {
        return challenge;
    }

    /** Translation-key segment: {@code ambitious}, {@code loyal}, … */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
