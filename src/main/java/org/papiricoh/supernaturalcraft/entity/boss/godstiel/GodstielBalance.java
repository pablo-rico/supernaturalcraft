package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

/**
 * Godstiel's numbers, kept apart from the entity so they can be tested without a world (v0.39). Five phases, a fifth of his
 * true health each ({@code ProgressionScale}): I The New God, II Judgment, III The Cracks, IV The Voices (the hall of the lab),
 * V The Overflow (out on the reservoir). Damage numbers are before his multiplier ({@code Balance.bossDamage} × config). Windups
 * end on the clips' moments ({@link GodstielAssets#HIT_TICKS}).
 *
 * <p>The inner fight ({@link #struggles}): at the start of phases III, IV and V and halfway through each, Castiel fights what
 * is in him. For {@link #STRUGGLE_TICKS} he doubles over and takes {@link #STRUGGLE_VULNERABILITY} of every blow; a hunter who
 * speaks to him then (right click) keeps him in it a while longer ({@link #struggleExtended}), once each, up to a cap.
 */
public final class GodstielBalance {

    public static final int PHASES = 5;
    /** The phase fought on the reservoir's deck. */
    public static final int RESERVOIR_PHASE = 5;
    /** From this phase the Leviathans are loose in him: the cracks, the mouth, and borax burns. */
    public static final int LEVIATHAN_PHASE = 3;

    private GodstielBalance() {
    }

    /** Health fraction below which {@code phase} ends (a fifth each): no gaps between phases. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks in {@code phase}. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 38;
            case 2 -> 34;
            case 3 -> 30;
            case 4 -> 26;
            default -> 22;
        };
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return GodstielAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- which bones show (the renderer reads these) -------------------------------------------------------------------------

    public static boolean halo(int phase) {
        return phase <= 2;
    }

    /** How many sets of cracks show (0-3). */
    public static int cracks(int phase) {
        return Math.max(0, Math.min(3, phase - 2));
    }

    public static boolean veins(int phase) {
        return phase >= 4;
    }

    public static boolean chestMouth(int phase) {
        return phase >= LEVIATHAN_PHASE;
    }

    public static boolean faceMouth(int phase) {
        return phase >= 5;
    }

    public static boolean tentacles(int phase) {
        return phase >= 4;
    }

    // --- the inner fight -----------------------------------------------------------------------------------------------------

    public static final int STRUGGLE_TICKS = 100, STRUGGLE_EXTEND = 40, STRUGGLE_MAX = 200;
    /** The share of a blow he takes while he struggles. */
    public static final float STRUGGLE_VULNERABILITY = 1.75f;

    /** The health fractions (of his whole bar) at which he falls into the inner fight: each phase from III, at its start and its middle. */
    public static float[] struggles() {
        int n = (PHASES - LEVIATHAN_PHASE + 1) * 2;
        float[] out = new float[n];
        int i = 0;
        for (int phase = LEVIATHAN_PHASE; phase <= PHASES; phase++) {
            float start = threshold(phase - 1), end = threshold(phase);
            out[i++] = start;
            out[i++] = (start + end) / 2f;
        }
        return out;
    }

    /** How long the inner fight lasts once spoken to {@code times} times (each hunter once). */
    public static int struggleExtended(int times) {
        return Math.min(STRUGGLE_MAX, STRUGGLE_TICKS + Math.max(0, times) * STRUGGLE_EXTEND);
    }

    // --- borax ---------------------------------------------------------------------------------------------------------------

    /** Borax on him from phase III: this many reference blows of his tier off his true health, and a short stagger. */
    public static final float BORAX_HITS = 1.5f;
    public static final int BORAX_STAGGER = 20, BORAX_COOLDOWN = 40;

    // --- I · The New God -----------------------------------------------------------------------------------------------------
    /** The snap: a gold circle under every hunter, then light. */
    public static final int SMITE_WINDUP = 26;
    public static final float SMITE_RADIUS = 1.8f, SMITE_DAMAGE = 14f;
    /** The palm: a gold lane ahead, a burst of grace down it; whoever is in it is thrown. */
    public static final int PALM_WINDUP = 20;
    public static final float PALM_REACH = 12f, PALM_WIDTH = 2.2f, PALM_DAMAGE = 16f, PALM_PUSH = 1.4f;
    /** Radiance: a gold ring round him; inside it at the moment, light that burns and blinds. Look away, or get out. */
    public static final int RADIANCE_WINDUP = 30, RADIANCE_BLIND = 60;
    public static final float RADIANCE_RADIUS = 7f, RADIANCE_DAMAGE = 12f;

    // --- II · Judgment -------------------------------------------------------------------------------------------------------
    /** Heretics: angels and demons dragged to kneel in gold rings; judged at the moment, each bursts (kill them first). */
    public static final int JUDGMENT_WINDUP = 70, HERETICS = 3;
    public static final float HERETIC_HEALTH = 30f, HERETIC_BURST_RADIUS = 3.5f, HERETIC_BURST_DAMAGE = 18f;
    /** The pulpit: bolts down gold lanes from him in a fan. */
    public static final int PULPIT_WINDUP = 30, PULPIT_LANES = 4;
    public static final float PULPIT_LENGTH = 14f, PULPIT_WIDTH = 1.8f, PULPIT_DAMAGE = 15f;

    // --- III · The Cracks ----------------------------------------------------------------------------------------------------
    /** The chest mouth lunges: a violet cone close in front. */
    public static final int BITE_WINDUP = 16;
    public static final float BITE_REACH = 5f, BITE_DAMAGE = 22f;
    /** Goo spewed in a violet cone: it pools on the floor (sticky, burning) and dries after a while. */
    public static final int RETCH_WINDUP = 22, GOO_TICKS = 20 * 12;
    public static final float RETCH_REACH = 9f, RETCH_DAMAGE = 10f;

    // --- IV · The Voices -----------------------------------------------------------------------------------------------------
    /** The tentacles sweep all round him: a violet ring. Jump back. */
    public static final int SWEEP_WINDUP = 24;
    public static final float SWEEP_RADIUS = 5.5f, SWEEP_DAMAGE = 18f, SWEEP_PUSH = 1.2f;
    /** The tentacles slam down a violet lane. */
    public static final int SLAM_WINDUP = 22;
    public static final float SLAM_REACH = 11f, SLAM_WIDTH = 2.4f, SLAM_DAMAGE = 20f;

    // --- V · The Overflow ----------------------------------------------------------------------------------------------------
    /** Goo erupts round him: violet circles on the deck, then black geysers. */
    public static final int SURGE_WINDUP = 30, SURGE_SPOTS = 5;
    public static final float SURGE_RADIUS = 2.2f, SURGE_DAMAGE = 18f;
    /** Leviathans climb out of the black water: doubles of him with a master, at most so many about. */
    public static final int DOUBLES_WINDUP = 30, DOUBLES_PER_CALL = 2, MAX_DOUBLES = 4;
    /** The reservoir's water turns black this many cells a tick. */
    public static final int BLACKEN_PER_TICK = 160;

    // --- his fall ------------------------------------------------------------------------------------------------------------
    /** He kneels this long before the hunter is asked; the question waits this long (silence: condemn). */
    public static final int KNEEL_TICKS = 60, TALK_TIMEOUT = 20 * 60;
    public static final int DEATH_TICKS = 140, SINK_TICKS = 80, RISE_TICKS = 50;
    /** Transitions: how long, when he leaves and when he lands (the reservoir move uses the same window). */
    public static final int TRANSITION = 80, LEAVE_AT = 20, ARRIVE_AT = 60;
}
