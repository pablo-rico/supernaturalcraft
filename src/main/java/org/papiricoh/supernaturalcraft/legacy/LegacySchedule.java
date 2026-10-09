package org.papiricoh.supernaturalcraft.legacy;

/**
 * When Henry Winchester calls (v0.17), pure. The first dawn after a hunter first beats Lucifer ({@code main/devil_went_down})
 * he is due; turned away (or ignored), he comes back {@link LegacyRules#HENRY_RETURN_DAYS} dawns later; once the hunter has
 * joined he no longer comes calling: he lives in the bunker.
 *
 * <p>In {@link Legacy}: {@code henry} NONE with {@code henryDay} = -1 is "not yet", NONE or DECLINED with a day is "due from
 * that day", JOINED is "never again".
 */
public final class LegacySchedule {

    /** Day-time window counted as dawn (sunrise to early morning). */
    public static final long DAWN_START = 23000, DAWN_END = 1500;

    private LegacySchedule() {
    }

    /** The world day a day time falls in (a dawn belongs to the day it opens). */
    public static long dayIndex(long dayTime) {
        return Math.floorDiv(dayTime + 1000, 24000);
    }

    public static boolean isDawn(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= DAWN_START || t < DAWN_END;
    }

    /** Whether Henry comes to this hunter now. */
    public static boolean visits(Legacy legacy, long dayTime) {
        if (legacy.member() || legacy.henry() == Legacy.HENRY_JOINED || legacy.henryDay() < 0) return false;
        return isDawn(dayTime) && dayIndex(dayTime) >= legacy.henryDay();
    }

    /** Lucifer has just fallen to this hunter: Henry is due from the next dawn (if he was not already). */
    public static Legacy luciferBeaten(Legacy legacy, long dayTime) {
        if (legacy.member() || legacy.henry() != Legacy.HENRY_NONE || legacy.henryDay() >= 0) return legacy;
        return legacy.withHenry(Legacy.HENRY_NONE, dayIndex(dayTime) + 1);
    }

    /** Turned away (or left standing): he calls again some dawns later. */
    public static Legacy declined(Legacy legacy, long dayTime) {
        if (legacy.member()) return legacy;
        return legacy.withHenry(Legacy.HENRY_DECLINED, dayIndex(dayTime) + LegacyRules.HENRY_RETURN_DAYS);
    }
}
