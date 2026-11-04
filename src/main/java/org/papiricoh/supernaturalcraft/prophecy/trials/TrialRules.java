package org.papiricoh.supernaturalcraft.prophecy.trials;

/**
 * The Trials that shut the gates of Hell (v0.36), pure. Three in order — {@link #HOUND} (bathe in a hellhound's blood),
 * {@link #SOUL} (bring an innocent soul up out of Hell), {@link #CURE} (cure a demon, {@link #INJECTIONS} injections of
 * purified blood at least {@link #INJECTION_GAP} ticks apart) — and each one begun takes {@link #HEARTS_PER_TRIAL} hearts.
 * Before the last injection the hunter chooses: {@link #DONE} (the seal; {@link #HEARTS_KEPT} stay lost for good) or
 * {@link #STOPPED} (what was taken comes back, {@link #HEARTS_PER_TRIAL} each day, and the Trials can be begun again).
 */
public final class TrialRules {

    public static final int NONE = 0, HOUND = 1, SOUL = 2, CURE = 3, DONE = 4, STOPPED = 5;
    public static final int HEARTS_PER_TRIAL = 2, HEARTS_KEPT = 2;
    public static final int INJECTIONS = 8, INJECTION_GAP = 1000;
    /** The sealed ground: no demon (never a boss) rises within this many blocks of it. */
    public static final int SEAL_RADIUS = 64;

    private TrialRules() {
    }

    public static boolean underway(int trial) {
        return trial >= HOUND && trial <= CURE;
    }

    /** The trial after {@code trial} is done. */
    public static int next(int trial) {
        return underway(trial) ? trial + 1 : trial;
    }

    /** Hearts taken now, given the stage and (when stopped) the hearts still owed. */
    public static int toll(int trial, int owed) {
        if (underway(trial)) return trial * HEARTS_PER_TRIAL;
        if (trial == DONE) return HEARTS_KEPT;
        if (trial == STOPPED) return Math.max(0, owed);
        return 0;
    }

    /** What is still owed after stopping during {@code trial}. */
    public static int owedOnStop(int trial) {
        return underway(trial) ? trial * HEARTS_PER_TRIAL : 0;
    }

    /** What is still owed {@code days} days later. */
    public static int owedAfter(int owed, long days) {
        return (int) Math.max(0, owed - Math.max(0, days) * HEARTS_PER_TRIAL);
    }

    /** Whether an injection may be given {@code since} ticks after the last one ({@code count} so far). */
    public static boolean mayInject(int count, long since) {
        return count == 0 || since >= INJECTION_GAP;
    }

    /** Whether the {@code count}-th injection (1-based) is the last, the one that asks. */
    public static boolean last(int count) {
        return count >= INJECTIONS;
    }

    /** Whether ({@code dx, dz}) from a seal lies in its ground. */
    public static boolean sealed(double dx, double dz) {
        return dx * dx + dz * dz <= (double) SEAL_RADIUS * SEAL_RADIUS;
    }
}
