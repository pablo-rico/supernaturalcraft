package org.papiricoh.supernaturalcraft.legacy.bunker;

import java.util.function.IntPredicate;

/**
 * Where the Men of Letters' bunker may stand (pure, tested in JUnit): {@link #CANDIDATES} chunks picked from the world seed
 * on a ring {@code min}–{@code max} blocks from the origin (default {@link #MIN}–{@link #MAX}; the server config may move it
 * for worlds where it is not yet placed), each at its own angle (the golden angle apart, with a seeded jitter) and radius.
 * The bunker stands at the first candidate whose ground is quiet ({@link #choose}), else at the last one. Its own salt keeps it
 * away from the Author's cabin's candidates.
 */
public final class BunkerSite {

    public static final int CANDIDATES = 32;
    public static final int MIN = 3000, MAX = 5000;
    private static final int MARGIN = 24;
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    private static final long SALT = 0x1E8A40DL;

    private BunkerSite() {
    }

    /** The candidate chunks for {@code seed} on the ring, as {@code [k][0] = chunk x, [k][1] = chunk z}. Deterministic. */
    public static int[][] chunks(long seed, int min, int max) {
        if (max < min) {
            int t = min;
            min = max;
            max = t;
        }
        long state = mix(seed ^ SALT);
        double base = unit(state) * Math.PI * 2;
        int[][] out = new int[CANDIDATES][];
        int lo = min + MARGIN, span = Math.max(1, max - min - 2 * MARGIN);
        for (int k = 0; k < CANDIDATES; k++) {
            state = mix(state + k);
            double jitter = (unit(state) - 0.5) * 0.3;
            state = mix(state);
            double radius = lo + unit(state) * span;
            double angle = base + k * GOLDEN_ANGLE + jitter;
            out[k] = new int[]{Math.floorDiv((int) Math.round(Math.cos(angle) * radius), 16),
                    Math.floorDiv((int) Math.round(Math.sin(angle) * radius), 16)};
        }
        return out;
    }

    public static int indexOf(int[][] chunks, int cx, int cz) {
        for (int k = 0; k < chunks.length; k++) if (chunks[k][0] == cx && chunks[k][1] == cz) return k;
        return -1;
    }

    /** The middle block of a chunk ({@code ChunkPos.getMiddleBlockX}). */
    public static int middle(int chunk) {
        return (chunk << 4) + 8;
    }

    /** The candidate that holds the bunker: the first {@code quiet} one, else the last. */
    public static int choose(IntPredicate quiet) {
        for (int k = 0; k < CANDIDATES - 1; k++) if (quiet.test(k)) return k;
        return CANDIDATES - 1;
    }

    /** Which way the hut's door looks, as quarter turns ({@link BunkerLayout#rotate}). */
    public static int rotation(long seed) {
        return (int) Math.floorMod(mix(seed ^ 0xB0B0L), 4L);
    }

    /** Whether sampled ground heights (tops = surface, floors = ocean floor) are dry and gentle enough for the hut. */
    public static boolean quiet(int[] tops, int[] floors, int seaLevel) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < tops.length; i++) {
            if (tops[i] != floors[i] || tops[i] <= seaLevel) return false;
            min = Math.min(min, tops[i]);
            max = Math.max(max, tops[i]);
        }
        return max - min <= 4;
    }

    /** Whether a ring was asked for the right way round and is wide enough to hold candidates. */
    public static boolean validRing(int min, int max) {
        return min >= 0 && max - min >= 2 * MARGIN + 16;
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
