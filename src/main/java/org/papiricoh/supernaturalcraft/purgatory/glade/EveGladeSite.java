package org.papiricoh.supernaturalcraft.purgatory.glade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

/**
 * Where Eve's glade is (v0.39), once per world: hidden from {@code /locate} (it is no structure), worked out from the seed alone.
 * The candidates lie on a ring round Purgatory's heart, a walk of a few hundred blocks from the landing; the first that stands in
 * the Mother's Wood on level ground is hers ({@code EveGladeWorld} asks the generator), else the first level one, else the first.
 * Pure: the ring and the choice are tested without a world.
 */
public final class EveGladeSite {

    /** The ring round the heart (blocks) and how many candidates on it. */
    public static final int RING_MIN = 220, RING_STEP = 40, CANDIDATES = 12;

    private EveGladeSite() {
    }

    /** The candidates' offsets from Purgatory's heart, in order, for this seed. */
    public static List<int[]> candidates(long seed) {
        Random r = new Random(seed ^ 0xE7E5EEDL);
        double base = r.nextDouble() * Math.PI * 2;
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < CANDIDATES; i++) {
            double a = base + i * 2.399963 + (r.nextDouble() - 0.5) * 0.3;
            int d = RING_MIN + (i % 6) * RING_STEP;
            out.add(new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)});
        }
        return out;
    }

    /**
     * The chosen candidate's index: the first both {@code wooded} and {@code level}; else the first {@code level}; else 0.
     */
    public static int choose(IntPredicate wooded, IntPredicate level) {
        int firstLevel = -1;
        for (int i = 0; i < CANDIDATES; i++) {
            boolean flat = level.test(i);
            if (flat && wooded.test(i)) return i;
            if (flat && firstLevel < 0) firstLevel = i;
        }
        return Math.max(0, firstLevel);
    }

    /** Whether the ground's heights sampled over a candidate are level enough for the clearing. */
    public static boolean level(int[] heights, int maxRelief) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int h : heights) {
            lo = Math.min(lo, h);
            hi = Math.max(hi, h);
        }
        return hi - lo <= maxRelief;
    }
}
