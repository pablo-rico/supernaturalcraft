package org.papiricoh.supernaturalcraft.entity.boss.dagon;

/**
 * Dagon's numbers (v0.41, pure, tested by {@code DagonBalanceTest}). Three phases of a third of her health each, before the
 * door of Kelly Kline's safe house. She rarely moves her feet: violet for her telekinesis, red for her hellfire.
 * <ol>
 *   <li><b>Prince of Hell</b>: a flick of the hand that hurls whoever stands in a violet lane aside, a slowly closing fist (a
 *   violet circle under a hunter: long to close, hard when it does), and a palm of hellfire down a red lane.</li>
 *   <li><b>Hellfire</b>: a ring of fire swept round her (a red band: only close to her is safe) and a finger that pins a hunter
 *   in the air (a small violet circle).</li>
 *   <li><b>The Guard Unbroken</b>: lifted off the ground, she shrieks (a red ring round her: thrown back and dazed) and does
 *   all of it faster.</li>
 * </ol>
 * Every attack shows on the ground in its windup and lands only in ACTIVE.
 */
public final class DagonBalance {

    public static final int PHASES = 3;
    public static final int TRANSITION = 50, EMERGE = 50;

    private DagonBalance() {
    }

    /** Health fraction below which {@code phase} ends (no gaps between phases). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between her attacks. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 38;
            case 2 -> 30;
            default -> 22;
        };
    }

    public static int cue(String clip) {
        return DagonAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    /** Her death lasts the {@code death} clip. */
    public static int deathTicks() {
        return DagonAssets.CLIP_TICKS.getOrDefault("death", 80);
    }

    // --- prince of hell --------------------------------------------------------------------------------------------
    /** The fling: a lane; whoever is in it is hurled sideways. */
    public static final float FLING_LENGTH = 14f, FLING_WIDTH = 2.4f, FLING_DAMAGE = 12f, FLING_PUSH = 1.8f;
    public static final int FLING_WINDUP = 18;
    /** The crush: a circle under a hunter that closes slowly. */
    public static final float CRUSH_RADIUS = 2.5f, CRUSH_DAMAGE = 22f;
    public static final int CRUSH_WINDUP = 34;
    /** The palm of hellfire: a lane, and a burn. */
    public static final float PALM_LENGTH = 16f, PALM_WIDTH = 2.0f, PALM_DAMAGE = 15f;
    public static final int PALM_WINDUP = 20, PALM_BURN = 4;

    // --- hellfire --------------------------------------------------------------------------------------------------
    /** The ring of fire: everything between {@link #RING_INNER} and {@link #RING_OUTER} of her. */
    public static final float RING_INNER = 2.5f, RING_OUTER = 10f, RING_DAMAGE = 16f;
    public static final int RING_WINDUP = 30, RING_BURN = 5;
    /** The pin: held in the air (levitation) and hurt. */
    public static final float PIN_RADIUS = 1.8f, PIN_DAMAGE = 10f;
    public static final int PIN_WINDUP = 16, PIN_HOLD = 40;

    // --- the guard unbroken ----------------------------------------------------------------------------------------
    /** The shriek round her. */
    public static final float SCREAM_RADIUS = 8f, SCREAM_DAMAGE = 14f, SCREAM_PUSH = 1.6f;
    public static final int SCREAM_WINDUP = 24, SCREAM_DAZE = 80;
    /** Too far, or out of sight this long: she drags the hunter back to her. */
    public static final float PULL_DISTANCE = 20f, PULL = 1.6f;
    public static final int PULL_NO_SIGHT = 60;
}
