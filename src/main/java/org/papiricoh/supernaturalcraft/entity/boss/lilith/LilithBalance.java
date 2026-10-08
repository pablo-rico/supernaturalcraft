package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

/**
 * Lilith's numbers, kept apart from the entity so they can be tested without a world. Three phases,
 * a third of her health each; a step up from Azazel and a step below Lucifer.
 */
public final class LilithBalance {

    public static final int PHASES = 3;
    /** After each white light she is spent for this long: hits land harder. */
    public static final int EMPTY_TICKS = 40;
    public static final float EMPTY_VULNERABILITY = 1.4f;
    /** Ticks between the two bursts of white light in her last phase. */
    public static final int SECOND_BURST_AFTER = 30;
    /** Cracks a headstone takes before it falls. */
    public static final int HEADSTONE_CRACKS = 2;

    private LilithBalance() {
    }

    /** The share of her health at which {@code phase} ends: two thirds, then one third. */
    public static float threshold(int phase) {
        return (PHASES - phase) / (float) PHASES;
    }

    /** Her true health with {@code players} challengers: the power curve's, plus a share per extra challenger. */
    public static double health(double perExtraPlayer, int players) {
        return ProgressionScale.healthFor(ProgressionScale.of(Boss.LILITH).trueHealth(), Math.max(1, players), (float) perExtraPlayer);
    }

    /** True damage the hunters must deal her to burn a contract: a share of her true max health. */
    public static float contractBreak(float share, float trueMaxHealth) {
        return share * trueMaxHealth;
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 36;
            case 2 -> 30;
            default -> 24;
        };
    }

    /** Seconds a contract runs before the hounds come. */
    public static int contractSeconds(int phase) {
        return switch (phase) {
            case 1 -> 10;
            case 2 -> 8;
            default -> 6;
        };
    }

    /** How many hunters can be under contract at once. */
    public static int contractsAtOnce(int phase) {
        return phase >= 3 ? 2 : 1;
    }

    /** Hounds sent when a contract comes due. */
    public static int houndsOnDue(int phase) {
        return phase >= 3 ? 3 : 2;
    }

    /** Her white light comes every this many attacks. */
    public static int whiteLightEvery(int phase) {
        return switch (phase) {
            case 1 -> 5;
            case 2 -> 4;
            default -> 3;
        };
    }

    /** What a blow is worth against a contract: holy wounds count double. */
    public static float contractCredit(float damage, boolean holy) {
        return holy ? damage * 2 : damage;
    }

    public static float vulnerability(boolean recovering, boolean spent) {
        return (recovering ? 1.25f : 1f) * (spent ? EMPTY_VULNERABILITY : 1f);
    }
}
