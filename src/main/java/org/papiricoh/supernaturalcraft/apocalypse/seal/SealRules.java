package org.papiricoh.supernaturalcraft.apocalypse.seal;

/**
 * The seals' numbers and their schedule (v0.40), pure: no Minecraft types, so {@code SealRulesTest} checks them without a world.
 *
 * <p><b>The count.</b> It starts when the first hunter beats Azazel. A seal breaks on its own one day after the last one
 * ({@link #DAY} clock ticks, {@code WorldClock.day}), plus a day for every seal event held since then ({@link #nextBreak}); the
 * hunters can hold the count back {@link #DELAY_CAP} days in all. A failed event breaks a seal at once and the wait starts
 * again. The passive count stops at {@link #PASSIVE_MAX}: the last seal is Lilith's death.
 *
 * <p><b>The stage</b> of the omens is one more every three broken seals (0-4); after Lilith the world is <i>risen</i> (the
 * last stage, in full) and after the first Lucifer falls back into the Cage it is an <i>echo</i> (faint: {@link #effectiveStage}).
 *
 * <p><b>The events</b> come to hunters of chapter II (Azazel beaten, Lilith not): at most {@link #MAX_PER_PLAYER} at a time
 * for each, {@link #MAX_PER_WORLD} in the world and {@link #MAX_PER_DAY} a day for each hunter, {@link #MIN_DISTANCE}-{@link
 * #MAX_DISTANCE} blocks away. Each lasts {@link #EVENT_TICKS}: the demons' rite climbs to 100 over that time ({@link #riteStep})
 * and breaks the seal when it gets there; every ward the demons kill pushes it on ({@link #WARD_PUSH}).
 */
public final class SealRules {

    /** The great seals the game counts; the last is Lilith's death. */
    public static final int TOTAL = 12;
    /** The passive count (and the failed events) stop here: only Lilith is left. */
    public static final int PASSIVE_MAX = TOTAL - 1;
    /** Clock ticks in a day. */
    public static final long DAY = 24_000L;
    /** The most days the hunters can hold the count back, over the whole Apocalypse. */
    public static final int DELAY_CAP = 6;
    /** The omens' last stage. */
    public static final int MAX_STAGE = 4;
    /** Seals broken per stage. */
    public static final int PER_STAGE = 3;
    /** From this stage natural angel spawns open and the angels go to war. */
    public static final int ANGEL_STAGE = 2;
    /** From this stage the moon is red. */
    public static final int RED_MOON_STAGE = 2;
    /** Only in the last stage do demons walk by day. */
    public static final int DAYLIGHT_DEMON_STAGE = 4;

    // --- the events ----------------------------------------------------------------------------------------------------------
    /** Half a day: the event's whole length, and the rite's. */
    public static final int EVENT_TICKS = 12_000;
    public static final int MAX_PER_PLAYER = 1, MAX_PER_WORLD = 3, MAX_PER_DAY = 2;
    public static final int MIN_DISTANCE = 48, MAX_DISTANCE = 96;
    /** How often (game ticks) the spawner looks for hunters to call, and the chance it calls each one it may. */
    public static final int CHECK_EVERY = 600;
    public static final float CHANCE = 0.04f;
    /** Game ticks a hunter rests after an event before another can call them. */
    public static final int REST_TICKS = 6_000;
    /** A hunter within this many blocks of the site counts as being there (shares, the HUD). */
    public static final int PRESENCE = 32;
    /** The rite: 100 at the end; each ward slain pushes it on by this much. */
    public static final float RITE_MAX = 100f, WARD_PUSH = 20f;
    /** The rite climbs this much faster with nobody there to see it (the hunter ignored the call). */
    public static final float UNWATCHED_RATE = 1.0f;
    /** How long a warning card stays up (ticks). */
    public static final int CARD_TICKS = 100;

    // --- the rewards ---------------------------------------------------------------------------------------------------------
    /** Bond points with every officer of the Host for an angel who holds a seal; of the Pit for a demon who lets one break. */
    public static final int HOST_REGARD = 6, PIT_REGARD = 6;
    /** Angel-family events a hunter must hold before Castiel warns of the traitor. */
    public static final int URIEL_AFTER = 2;

    private SealRules() {
    }

    /** The clock tick at which the next seal breaks on its own: a day after the last, a day more for each event held since. */
    public static long nextBreak(long lastBreak, int heldSince) {
        return lastBreak + DAY * (1L + Math.max(0, heldSince));
    }

    /** Whether the passive count breaks a seal now. */
    public static boolean breaksNow(boolean started, int broken, long now, long lastBreak, int heldSince) {
        return started && broken < PASSIVE_MAX && now >= nextBreak(lastBreak, heldSince);
    }

    /** Whether another held event may still push the count back ({@code delayed}: the days already won). */
    public static boolean canDelay(int delayed, int broken) {
        return delayed < DELAY_CAP && broken < PASSIVE_MAX;
    }

    /** Whole days left until the next break, rounded up (0 when due or stopped). */
    public static int daysLeft(long now, long next) {
        if (next < 0 || now >= next) return 0;
        return (int) ((next - now + DAY - 1) / DAY);
    }

    /** The omens' stage for {@code broken} seals (0-4); risen is the last. */
    public static int stage(int broken, boolean risen) {
        if (risen) return MAX_STAGE;
        return Math.max(0, Math.min(MAX_STAGE, broken / PER_STAGE));
    }

    /**
     * The stage the world's consequences run at: the stage, or the faint first stage once the Cage is shut again (an echo:
     * the omens dimmed, no plagues, no extra demons by day).
     */
    public static int effectiveStage(int broken, boolean risen, boolean echo) {
        return echo ? 1 : stage(broken, risen);
    }

    /** How strongly the sky shows it (0-1): the stage over the last, a quarter of it in an echo. */
    public static float omenStrength(int broken, boolean risen, boolean echo) {
        float s = stage(broken, risen) / (float) MAX_STAGE;
        return echo ? s * 0.25f : s;
    }

    /** Whether a hunter is in chapter II: Azazel beaten, Lilith not yet. */
    public static boolean chapterTwo(boolean azazel, boolean lilith) {
        return azazel && !lilith;
    }

    /** Whether the spawner may call {@code hunter} now. */
    public static boolean mayCall(boolean chapterTwo, int activeForHunter, int activeInWorld, int todayForHunter, long now, long lastEvent,
                                  int broken, boolean risen) {
        if (!chapterTwo || risen || broken >= PASSIVE_MAX) return false;
        if (activeForHunter >= MAX_PER_PLAYER || activeInWorld >= MAX_PER_WORLD || todayForHunter >= MAX_PER_DAY) return false;
        return lastEvent < 0 || now - lastEvent >= REST_TICKS;
    }

    /** How far the rite climbs in {@code ticks}, watched or not. */
    public static float riteStep(int ticks, boolean watched) {
        return RITE_MAX * ticks / EVENT_TICKS * (watched ? 1f : UNWATCHED_RATE);
    }

    /** The rite after a ward has been slain. */
    public static float wardSlain(float progress) {
        return Math.min(RITE_MAX, progress + WARD_PUSH);
    }

    /** What an event comes to. */
    public enum Outcome {
        /** Still going. */
        RUNNING,
        /** Every one of the demons' drivers is gone (the ritualists dead, the hex broken): the seal holds. */
        HELD,
        /** The rite reached 100, or every ward was slain: the seal breaks. */
        BROKEN
    }

    /**
     * The outcome now: {@code drivers} alive (ritualists, attackers, the hex), {@code wards} alive of {@code wardsAtStart}.
     * The demons win ties: a rite at 100 breaks the seal even as its last ritualist falls.
     */
    public static Outcome outcome(float progress, int drivers, int wards, int wardsAtStart) {
        if (progress >= RITE_MAX - 1.0e-4f) return Outcome.BROKEN;
        if (wardsAtStart > 0 && wards <= 0) return Outcome.BROKEN;
        if (drivers <= 0) return Outcome.HELD;
        return Outcome.RUNNING;
    }

    /** The day index of a clock tick. */
    public static long dayOf(long clock) {
        return Math.floorDiv(clock, DAY);
    }

    /** Index (1-12) of the seal that breaks next after {@code broken} have. */
    public static int nextSeal(int broken) {
        return Math.min(TOTAL, broken + 1);
    }
}
