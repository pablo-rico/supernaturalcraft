package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

/**
 * Metatron's numbers, kept apart from the entity so they can be tested without a world. Four phases,
 * a quarter of his health each; true health (the power curve's) above the vanilla cap, like every boss on Lucifer's base.
 */
public final class MetatronBalance {

    public static final int PHASES = 4;
    /** The phase he takes to his lectern, and stays there. */
    public static final int LECTERN_PHASE = 3;
    /** In his last phase the Word is spoken every this many attacks. */
    public static final int WORD_EVERY = 4;
    /** Ticks between rewrites of the ground in his last phase. */
    public static final int REWRITE_EVERY = 500;
    /** How long a rewritten column or hole lasts before the ground puts itself back. */
    public static final int REWRITE_LASTS = 400;

    private MetatronBalance() {
    }

    public static float threshold(int phase) {
        return (PHASES - phase) / (float) PHASES;
    }

    /** His true health with {@code players} challengers: the power curve's, plus a share per extra challenger. */
    public static float health(double perExtraPlayer, int players) {
        return ProgressionScale.healthFor(ProgressionScale.of(Boss.METATRON).trueHealth(), Math.max(1, players), (float) perExtraPlayer);
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 34;
            case 2 -> 30;
            case 3 -> 26;
            default -> 22;
        };
    }

    public static float scale(int phase) {
        return phase >= LECTERN_PHASE ? 1.3f : 1.0f;
    }

    public static boolean hasHand(int phase) {
        return phase >= 2;
    }

    public static boolean hasBook(int phase) {
        return phase >= LECTERN_PHASE;
    }

    public static boolean onLectern(int phase) {
        return phase >= LECTERN_PHASE;
    }
}
