package org.papiricoh.supernaturalcraft.cain;

import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRing;

/**
 * Where Cain's farm may stand (v0.21, pure): Rowena's ring of candidates ({@link RowenaRing}) drawn with a seed of its own
 * ({@link #SALT}) on a farther ring, {@link #MIN}–{@link #MAX} blocks from the origin (a mid-game journey; Rowena's clue and his
 * address lead there). The farm stands at the first candidate on quiet ground ({@link RowenaRing#choose}).
 */
public final class CainRing {

    public static final int MIN = 1800, MAX = 4000;
    /** Mixed into the world seed so his candidates are not Rowena's. */
    static final long SALT = 0xCA1F_A8E5L;

    private CainRing() {
    }

    /** The candidate chunks for {@code seed}, as {@code [k][0] = chunk x, [k][1] = chunk z}. Deterministic. */
    public static int[][] chunks(long seed, int min, int max) {
        return RowenaRing.chunks(seed ^ SALT, min, max);
    }

    public static int[][] chunks(long seed) {
        return chunks(seed, MIN, MAX);
    }

    /** Which way the farmhouse looks, as quarter turns ({@code CainFarmLayout.rotate}). */
    public static int rotation(long seed) {
        return RowenaRing.rotation(seed ^ SALT);
    }
}
