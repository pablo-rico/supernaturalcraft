package org.papiricoh.supernaturalcraft.legacy.research;

/**
 * The pace of research (v0.17), pure: every topic has a next level, each costs more, takes longer and gives less, so research
 * never ends but its bonuses close on a cap.
 */
public final class ResearchMath {

    /** Growth of the field notes a level costs. */
    public static final double COST_GROWTH = 1.35;
    /** Most field notes one research ever asks (a stack). */
    public static final int MAX_NOTES = 64;
    /** Ticks the first level takes (3 minutes). */
    public static final long BASE_TICKS = 3 * 60 * 20;
    /** Growth of the time a level takes. */
    public static final double TIME_GROWTH = 1.2;
    /** Longest one research ever takes (40 minutes). */
    public static final long MAX_TICKS = 40 * 60 * 20;
    /** Share of the remaining bonus each level keeps back. */
    public static final double BONUS_DECAY = 0.85;
    /** Most extra damage a creature's file ever gives against it. */
    public static final double CREATURE_DAMAGE_CAP = 0.30;

    private ResearchMath() {
    }

    /** Field notes level {@code n} (0 = the first) costs, from a base. */
    public static int notes(int base, int n) {
        double c = Math.ceil(Math.max(1, base) * Math.pow(COST_GROWTH, Math.max(0, n)));
        return (int) Math.min(MAX_NOTES, c);
    }

    /** Ticks level {@code n} takes; {@code speed} &gt; 1 is faster (the ring: 1/0.9). */
    public static long ticks(int n, double speed) {
        double t = Math.min(MAX_TICKS, BASE_TICKS * Math.pow(TIME_GROWTH, Math.max(0, n)));
        return Math.max(20, Math.round(t / Math.max(0.1, speed)));
    }

    /** The bonus after {@code levels} levels, closing on {@code cap}: cap × (1 − 0.85ⁿ). */
    public static double bonus(double cap, int levels) {
        return cap * (1 - Math.pow(BONUS_DECAY, Math.max(0, levels)));
    }

    /** Extra damage (a share, 0..0.30) a creature's file of {@code levels} gives against it. */
    public static double creatureDamage(int levels) {
        return bonus(CREATURE_DAMAGE_CAP, levels);
    }
}
