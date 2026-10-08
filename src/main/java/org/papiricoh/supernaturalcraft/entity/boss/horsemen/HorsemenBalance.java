package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

/**
 * The Horsemen's shared numbers, kept apart from the entities so they can be tested without a world. War, Famine and
 * Pestilence are mid bosses (three phases, 20 000 true health on the power curve); Death is a big one (four phases,
 * 45 000, Amara's step). True health sits above the vanilla cap through a health scale, as every boss on Lucifer's base.
 */
public final class HorsemenBalance {

    /** Vanilla health (Lucifer's base for every boss); true health is this times the health scale. */
    public static final double MID_BASE_HEALTH = 1000, DEATH_BASE_HEALTH = 1000;
    public static final int EMERGE_TICKS = 100, TRANSITION_TICKS = 80, MOUNT_TICKS = 100, DEATH_TICKS = 140;
    /** Mounted, he is this much quicker on his feet (his horse's, rather). */
    public static final float MOUNTED_SPEED = 1.45f;
    /**
     * What a hunter is assumed to take off a Horseman each second (an Ascension III weapon against the mid three, IV
     * against Death), after his mundane multiplier and the time spent dodging: only used to estimate how long a fight lasts.
     */
    public static final double ASSUMED_DPS = 110, ASSUMED_DEATH_DPS = 120;

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

    /** True health with {@code players} challengers: the power curve's, plus a share per extra challenger. */
    public static double trueHealth(boolean death, double perExtraPlayer, int players) {
        float base = ProgressionScale.of(death ? Boss.DEATH : Boss.WAR).trueHealth();
        return ProgressionScale.healthFor(base, Math.max(1, players), (float) perExtraPlayer);
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
    public static int estimatedSeconds(boolean death, double perExtraPlayer, int players) {
        double health = trueHealth(death, perExtraPlayer, players);
        double dps = (death ? ASSUMED_DEATH_DPS : ASSUMED_DPS) * Math.max(1, players);
        return (int) Math.round(health / dps + untouchableTicks(phases(death)) / 20.0);
    }
}
