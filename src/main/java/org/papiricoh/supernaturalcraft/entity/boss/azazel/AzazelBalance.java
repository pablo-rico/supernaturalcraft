package org.papiricoh.supernaturalcraft.entity.boss.azazel;

/**
 * Azazel's numbers, kept apart from the entity so they can be tested without a world. Two phases,
 * split at half his health; the first boss a hunter meets, so everything here is gentler than Lucifer.
 */
public final class AzazelBalance {

    public static final int PHASES = 2;
    /** In the second phase he goes to smoke and rushes the target every this many attacks. */
    public static final int DASH_EVERY = 5;
    /** Below this share of his health (in the last phase) he is enraged: shorter gaps, quicker windups. */
    public static final float ENRAGE_BELOW = 0.1f;

    private AzazelBalance() {
    }

    /** The share of his health at which {@code phase} ends. */
    public static float threshold(int phase) {
        return 0.5f;
    }

    /** His health with {@code players} challengers. */
    public static double health(double base, double perExtraPlayer, int players) {
        return base * (1 + perExtraPlayer * (Math.max(1, players) - 1));
    }

    /** Ticks between his attacks. */
    public static int attackGap(int phase) {
        return phase <= 1 ? 40 : 30;
    }

    /** How much more a hit lands for: 25% more while he recovers from an attack, and more again while the rails hold him. */
    public static float vulnerability(boolean recovering, boolean trapped, double trappedMultiplier) {
        return (recovering ? 1.25f : 1f) * (trapped ? (float) trappedMultiplier : 1f);
    }
}
