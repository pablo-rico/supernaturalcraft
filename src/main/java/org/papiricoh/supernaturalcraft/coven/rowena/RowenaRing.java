package org.papiricoh.supernaturalcraft.coven.rowena;

import java.util.function.IntPredicate;

/**
 * Where Rowena's house may stand (v0.21, pure; the Author's cabin's pattern, {@code author.CabinSite}): {@link #CANDIDATES}
 * chunks picked from the world seed on a ring {@link #MIN}–{@link #MAX} blocks from the origin (close enough for the early
 * game, far enough to need her calling card), each at its own angle and radius. The house stands at the first candidate whose
 * ground is quiet (a forest's edge, flat and dry; {@link #choose}); if none is, at the last one. Shared by the structure
 * ({@link RowenaPlacement}, {@link RowenaHouseStructure}) and by her calling card, so the card always points where the world
 * builds it.
 */
public final class RowenaRing {

    public static final int CANDIDATES = 24;
    /** The ring, in blocks from the origin (the chunk's middle block lies within it). */
    public static final int MIN = 1200, MAX = 3200;
    /** Kept clear of the ring's edges: the chunk's middle may be up to 12 blocks from the sampled point. */
    private static final int MARGIN = 24;
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    private static final long SALT = 0x50E3A7L;

    private RowenaRing() {
    }

    /** The candidate chunks for {@code seed}, as {@code [k][0] = chunk x, [k][1] = chunk z}. Deterministic. */
    public static int[][] chunks(long seed) {
        return chunks(seed, MIN, MAX);
    }

    public static int[][] chunks(long seed, int min, int max) {
        long state = seed ^ SALT;
        state = mix(state);
        double base = unit(state) * Math.PI * 2;
        int[][] out = new int[CANDIDATES][];
        int lo = min + MARGIN, span = Math.max(1, max - min - 2 * MARGIN);
        for (int k = 0; k < CANDIDATES; k++) {
            state = mix(state + k);
            double jitter = (unit(state) - 0.5) * 0.3;
            state = mix(state);
            double radius = lo + unit(state) * span;
            double angle = base + k * GOLDEN_ANGLE + jitter;
            int cx = Math.floorDiv((int) Math.round(Math.cos(angle) * radius), 16);
            int cz = Math.floorDiv((int) Math.round(Math.sin(angle) * radius), 16);
            out[k] = new int[]{cx, cz};
        }
        return out;
    }

    /** @return which candidate chunk ({@code cx}, {@code cz}) is, or -1 if it is none */
    public static int indexOf(int[][] chunks, int cx, int cz) {
        for (int k = 0; k < chunks.length; k++) if (chunks[k][0] == cx && chunks[k][1] == cz) return k;
        return -1;
    }

    /** The middle block of a chunk, as the world counts it ({@code ChunkPos.getMiddleBlockX}). */
    public static int middle(int chunk) {
        return (chunk << 4) + 8;
    }

    /**
     * The candidate that holds the house: the first whose ground is {@code quiet}, else the last. {@code quiet} is only
     * asked about candidates before the answer.
     */
    public static int choose(IntPredicate quiet) {
        for (int k = 0; k < CANDIDATES - 1; k++) if (quiet.test(k)) return k;
        return CANDIDATES - 1;
    }

    /** Which way the house's door looks, as quarter turns ({@code RowenaHouseLayout.rotate}). */
    public static int rotation(long seed) {
        return (int) Math.floorMod(mix(seed ^ 0x4E7AL), 4L);
    }

    /** Whether ground heights sampled around a spot are flat and dry enough for her house. */
    public static boolean flat(int[] tops, int[] floors, int seaLevel) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < tops.length; i++) {
            if (tops[i] != floors[i] || tops[i] < seaLevel) return false;
            min = Math.min(min, tops[i]);
            max = Math.max(max, tops[i]);
        }
        return max - min <= 3;
    }

    private static long mix(long z) {
        z = (z + 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double unit(long z) {
        return (z >>> 11) * 0x1.0p-53;
    }
}
