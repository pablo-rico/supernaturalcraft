package org.papiricoh.supernaturalcraft.apocworld.camp;

/** Where an outpost may stand (v0.41), pure: fairly level ground, its corners within {@link #MAX_RELIEF} of its middle. */
public final class CampSites {

    public static final int MAX_RELIEF = 4;

    private CampSites() {
    }

    /** @param tops the ground's top at the middle (first) and the corners */
    public static boolean suitable(int[] tops) {
        if (tops.length == 0) return false;
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int t : tops) {
            lo = Math.min(lo, t);
            hi = Math.max(hi, t);
        }
        return hi - lo <= MAX_RELIEF && Math.abs(tops[0] - lo) <= MAX_RELIEF;
    }
}
