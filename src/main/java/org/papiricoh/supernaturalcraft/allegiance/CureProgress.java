package org.papiricoh.supernaturalcraft.allegiance;

/**
 * The demon cure (pure): purified blood, three times, on three different nights (as Sam's trials had it, the blood
 * goes in a night at a time). A rite on a night already used does nothing.
 */
public final class CureProgress {

    public static final int NIGHTS = 3;
    /** Ticks after a cure (or ripping out Grace) before a side may be chosen again: three days. */
    public static final long CHOOSE_AGAIN_TICKS = 3 * 24000L;

    private CureProgress() {
    }

    /** The night a game day-time falls in: from dusk (12000) to the next dusk is one night. */
    public static long nightIndex(long dayTime) {
        return Math.floorDiv(dayTime + 12000, 24000);
    }

    /** Whether a cure rite may advance now ({@code lastNight} = -1 for none yet). */
    public static boolean canAdvance(int stage, long lastNight, long night) {
        return stage < NIGHTS && night != lastNight;
    }

    /** Whether this stage completes the cure. */
    public static boolean complete(int stage) {
        return stage >= NIGHTS;
    }
}
