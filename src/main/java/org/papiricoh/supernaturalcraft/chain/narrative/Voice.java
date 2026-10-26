package org.papiricoh.supernaturalcraft.chain.narrative;

import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.Temperament;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.Locale;

/**
 * Whose words a line is in (pure): a demon's, a loyal angel's, or a fallen angel's. Builds the lang keys of
 * {@link VoiceLines}: {@code chain.supernaturalcraft.voice.<voice>.<situation>.<temperament|any>.<n>}, n from 0.
 */
public enum Voice {
    DEMON, ANGEL, FALLEN;

    public static final String PREFIX = "chain.supernaturalcraft.voice.";

    public static Voice of(ChainFaction faction, ChainFaction.Branch branch) {
        if (faction == ChainFaction.DEMON) return DEMON;
        return branch == ChainFaction.Branch.FALLEN ? FALLEN : ANGEL;
    }

    public static Voice of(Officer o) {
        return of(o.faction, o.branch);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** One line's key; {@code temperament} null for the lines any officer of this voice may say. */
    public String key(Situation s, Temperament temperament, int n) {
        return PREFIX + id() + "." + s.id() + "." + (temperament == null ? "any" : temperament.id()) + "." + n;
    }

    /**
     * A line for this officer: drawn among the voice's shared lines and its temperament's own, by {@code salt} (a day, a
     * tick: what keeps it from repeating). Null if the voice has no line for the situation (never, once the lang is full).
     */
    public static String line(Officer o, Situation s, long salt) {
        return line(of(o), Temperament.of(o), s, GenSeed.mix(o.nameSeed ^ GenSeed.mix(salt)));
    }

    public static String line(Voice v, Temperament t, Situation s, long seed) {
        int any = VoiceLines.count(v, s, null), own = VoiceLines.count(v, s, t);
        if (any + own == 0) return null;
        int pick = (int) Math.floorMod(GenSeed.mix(seed), (long) (any + own));
        return pick < any ? v.key(s, null, pick) : v.key(s, t, pick - any);
    }
}
