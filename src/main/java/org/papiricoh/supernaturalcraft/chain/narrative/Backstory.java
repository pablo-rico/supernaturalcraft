package org.papiricoh.supernaturalcraft.chain.narrative;

import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An officer's past, in a few sentences drawn from its {@code nameSeed} (pure, never saved): where it came from, the vessel
 * it wears, how it rose and, for a fallen angel, why it fell. Each piece is a lang key
 * {@code chain.supernaturalcraft.backstory.<voice>.<piece>.<n>} ({@code %1$s}: the officer's name), n below {@link #COUNT}.
 */
public final class Backstory {
    public enum Piece {
        ORIGIN, VESSEL, RISE, FALL;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Fragments written per voice and piece (FALL only for the fallen voice). */
    public static final int COUNT = 8;
    public static final String PREFIX = "chain.supernaturalcraft.backstory.";

    private Backstory() {
    }

    public static String key(Voice voice, Piece piece, int n) {
        return PREFIX + voice.id() + "." + piece.id() + "." + n;
    }

    /** The keys of an officer's backstory, in reading order. A fallen angel keeps its angel's past and adds its Fall. */
    public static List<String> keys(Officer o) {
        return keys(Voice.of(o), o.nameSeed);
    }

    public static List<String> keys(Voice voice, long nameSeed) {
        GenSeed.Rng rng = new GenSeed.Rng(GenSeed.mix(nameSeed ^ 0x4241434BL));
        Voice past = voice == Voice.FALLEN ? Voice.ANGEL : voice;
        List<String> out = new ArrayList<>();
        out.add(key(past, Piece.ORIGIN, rng.nextInt(COUNT)));
        out.add(key(past, Piece.VESSEL, rng.nextInt(COUNT)));
        out.add(key(past, Piece.RISE, rng.nextInt(COUNT)));
        if (voice == Voice.FALLEN) out.add(key(Voice.FALLEN, Piece.FALL, rng.nextInt(COUNT)));
        return out;
    }
}
