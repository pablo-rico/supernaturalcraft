package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

/** Lucifer Uncaged's numbers, kept pure so they can be checked without a world. */
public final class UncagedBalance {

    public static final int PHASES = 6;

    private UncagedBalance() {
    }

    /** The share of health at which {@code phase} (1–5) ends: five sixths, four sixths, … one sixth. */
    public static float threshold(int phase) {
        return (PHASES - phase) / (float) PHASES;
    }

    /** His true health with {@code challengers}: the power curve's (Michael's level), plus a share per extra challenger. */
    public static float health(double perExtraPlayer, int challengers) {
        return ProgressionScale.healthFor(ProgressionScale.of(Boss.LUCIFER_UNCAGED).trueHealth(), Math.max(1, challengers), (float) perExtraPlayer);
    }

    /** Ticks between attacks in each phase (before the last-stand speed-up). */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 30;
            case 2 -> 26;
            case 3 -> 22;
            case 4 -> 20;
            case 5 -> 18;
            default -> 14;
        };
    }

    /** How large he stands in each phase: from a tall man to a towering archangel. */
    public static float scale(int phase) {
        return 1.6f + (Math.max(1, Math.min(PHASES, phase)) - 1) * 0.16f;
    }

    /** Pairs of wings shown in each phase (the wings come in at P2, P4 and P6). */
    public static int wingPairs(int phase) {
        return phase >= 6 ? 3 : phase >= 4 ? 2 : phase >= 2 ? 1 : 0;
    }

    /** Whether the Enochian chains still hang from his wrists and neck. */
    public static boolean chained(int phase) {
        return phase <= 1;
    }
}
