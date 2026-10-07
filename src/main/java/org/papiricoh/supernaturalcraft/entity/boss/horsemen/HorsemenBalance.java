package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

/**
 * The Horsemen's shared numbers, kept apart from the entities so they can be tested without a world. War, Famine and
 * Pestilence are mid bosses (three phases, about 800 true health); Death is a big one (four phases, about 1600, like
 * Metatron). True health sits above the vanilla cap through a health scale, as Metatron's does.
 */
public final class HorsemenBalance {

    /** Vanilla health; true health is this times the health scale. */
    public static final double MID_BASE_HEALTH = 400, DEATH_BASE_HEALTH = 800;
    public static final int EMERGE_TICKS = 100, TRANSITION_TICKS = 80, MOUNT_TICKS = 100, DEATH_TICKS = 140;
    /** Mounted, he is this much quicker on his feet (his horse's, rather). */
    public static final float MOUNTED_SPEED = 1.45f;
    /**
     * What a hunter is assumed to take off a Horseman each second, after his mundane multiplier and the time spent
     * dodging: only used to estimate how long a fight lasts.
     */
    public static final double ASSUMED_DPS = 3.2;

    private HorsemenBalance() {
    }

    public static int phases(boolean death) {
        return death ? 4 : 3;
    }

    /** The share of his health at which {@code phase} ends: thirds, or quarters for Death. */
    public static float threshold(int phases, int phase) {
        return (phases - phase) / (float) phases;
    }

    public static double baseHealth(boolean death) {
        return death ? DEATH_BASE_HEALTH : MID_BASE_HEALTH;
    }

    /** True health per point of vanilla health with {@code players} challengers. */
    public static float healthScale(double multiplier, double perExtraPlayer, int players) {
        return (float) (multiplier * (1 + perExtraPlayer * (Math.max(1, players) - 1)));
    }

    public static double trueHealth(boolean death, double multiplier, double perExtraPlayer, int players) {
        return baseHealth(death) * healthScale(multiplier, perExtraPlayer, players);
    }

    public static int attackGap(int phases, int phase) {
        int[] gaps = phases >= 4 ? new int[]{32, 28, 24, 20} : new int[]{34, 28, 22};
        return gaps[Math.max(0, Math.min(gaps.length - 1, phase - 1))];
    }

    /** Ticks spent untouchable over a whole fight: rising, every change of phase (mounting for the last), dying. */
    public static int untouchableTicks(int phases) {
        return EMERGE_TICKS + TRANSITION_TICKS * Math.max(0, phases - 2) + MOUNT_TICKS + DEATH_TICKS;
    }

    /** Roughly how long a fight takes, in seconds, with {@code players} hunters hitting at {@link #ASSUMED_DPS}. */
    public static int estimatedSeconds(boolean death, double multiplier, double perExtraPlayer, int players) {
        double health = trueHealth(death, multiplier, perExtraPlayer, players);
        double dps = ASSUMED_DPS * Math.max(1, players);
        return (int) Math.round(health / dps + untouchableTicks(phases(death)) / 20.0);
    }
}
