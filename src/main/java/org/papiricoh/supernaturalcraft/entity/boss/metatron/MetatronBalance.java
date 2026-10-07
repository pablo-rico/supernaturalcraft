package org.papiricoh.supernaturalcraft.entity.boss.metatron;

/**
 * Metatron's numbers, kept apart from the entity so they can be tested without a world. Four phases,
 * a quarter of his health each; true health above the vanilla cap, like Lucifer Uncaged's.
 */
public final class MetatronBalance {

    public static final int PHASES = 4;
    /** Vanilla health; the rest is {@link #healthScale}. */
    public static final double BASE_HEALTH = 600;
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

    /** True health per point of vanilla health with {@code players} challengers. */
    public static float healthScale(double multiplier, double perExtraPlayer, int players) {
        return (float) (multiplier * (1 + perExtraPlayer * (Math.max(1, players) - 1)));
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
