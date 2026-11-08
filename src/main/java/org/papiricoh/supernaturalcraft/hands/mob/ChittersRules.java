package org.papiricoh.supernaturalcraft.hands.mob;

/**
 * When the Chitters wake (v0.42), pure. In the stories they come up every 27 years to breed and feed, and sleep in between; here
 * the cycle is in world days: {@link #AWAKE_DAYS} awake in every {@link #CYCLE} days, from day {@link #FIRST} on. Their nests'
 * eggs hatch only while they are awake, and they only rise in the deep dark of caves ({@link #MAX_Y}).
 */
public final class ChittersRules {

    /** The cycle, in world days. */
    public static final int CYCLE = 27;
    /** How many days of each cycle they are awake. */
    public static final int AWAKE_DAYS = 3;
    /** The first awake day (day 0 is the world's first): never in a new world's first weeks. */
    public static final long FIRST = 24;
    /** They rise only below this height. */
    public static final int MAX_Y = 40;
    /** A pack's size. */
    public static final int PACK_MIN = 2, PACK_MAX = 4;
    /** No more than this many round one hunter (48 blocks). */
    public static final int CAP_NEAR = 6;
    /** A nest is woven where a pack rises unless one lies this close (a cube's half side, searched block by block). */
    public static final int NEST_SPACING = 8;

    private ChittersRules() {
    }

    /** Whether the Chitters are awake on world day {@code day}. */
    public static boolean awake(long day) {
        if (day < FIRST) return false;
        return Math.floorMod(day - FIRST, (long) CYCLE) < AWAKE_DAYS;
    }

    /** The next day they wake, from {@code day} (itself if awake). */
    public static long nextWaking(long day) {
        if (day < FIRST) return FIRST;
        long into = Math.floorMod(day - FIRST, (long) CYCLE);
        return into < AWAKE_DAYS ? day : day + CYCLE - into;
    }

    /** Whether a pack may rise at height {@code y} with this light (sky and block, 0-15): deep, and pitch dark. */
    public static boolean darkDeep(int y, int skyLight, int blockLight) {
        return y < MAX_Y && skyLight == 0 && blockLight <= 0;
    }

    /** A pack's size from a roll in [0, 1). */
    public static int packSize(double roll) {
        int n = PACK_MIN + (int) Math.floor(roll * (PACK_MAX - PACK_MIN + 1));
        return Math.max(PACK_MIN, Math.min(PACK_MAX, n));
    }
}
