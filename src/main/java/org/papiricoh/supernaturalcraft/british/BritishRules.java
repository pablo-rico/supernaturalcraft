package org.papiricoh.supernaturalcraft.british;

/**
 * Season 12's British road (v0.41), pure: what a hunter told Dr. Hess, how long the British keep a hunter who joined them, and
 * when Ketch comes for one they have written off. Tested in {@code BritishRulesTest}.
 */
public final class BritishRules {

    private BritishRules() {
    }

    /** What stands between a hunter and the British; saved by ordinal, so only ever appended. */
    public enum Choice {
        /** Hess has not come yet. */
        NONE,
        /** They took the British kit and work with them, for now. */
        JOINED,
        /** They turned Hess down (or said nothing). */
        REFUSED,
        /** They worked with the British until Hess decided they were a liability. */
        BETRAYED;

        public static Choice of(int ordinal) {
            Choice[] v = values();
            return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
        }
    }

    /** Days the British keep a hunter who joined them before Hess decides they are a liability. */
    public static final int JOIN_DAYS = 3;
    /** Days Ketch rests between one night's hunt and the next. */
    public static final int HUNT_REST_DAYS = 1;
    /** The night's window Ketch hunts in, in day ticks (from dusk to before dawn). */
    public static final long NIGHT_START = 14_000, NIGHT_END = 22_000;
    /** The British starter kit: enchanted rounds, one pistol, hex grenades. */
    public static final int KIT_ROUNDS = 12, KIT_GRENADES = 3;

    /** Whether a hunter who joined on {@code joinedDay} has outlived their use by {@code today}. */
    public static boolean betrayalDue(Choice choice, long joinedDay, long today) {
        return choice == Choice.JOINED && today - joinedDay >= JOIN_DAYS;
    }

    /** Whether Ketch is after this hunter (turned Hess down or written off, and he still stands for them). */
    public static boolean hunted(Choice choice, boolean ketchBeaten) {
        return (choice == Choice.REFUSED || choice == Choice.BETRAYED) && !ketchBeaten;
    }

    /** Whether Ketch may come tonight (his last hunt was on {@code lastHuntDay}). */
    public static boolean huntDue(long lastHuntDay, long today) {
        return today - lastHuntDay >= HUNT_REST_DAYS;
    }

    /** Whether {@code time} (day ticks) is within Ketch's night. */
    public static boolean night(long time) {
        long t = Math.floorMod(time, 24000L);
        return t >= NIGHT_START && t < NIGHT_END;
    }

    /** Whether Hess may bring her ultimatum: Amara is beaten and the hunter has not had it. */
    public static boolean ultimatumDue(boolean amaraBeaten, Choice choice) {
        return amaraBeaten && choice == Choice.NONE;
    }
}
