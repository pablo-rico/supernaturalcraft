package org.papiricoh.supernaturalcraft.entity.boss.michael;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

/**
 * Michael's numbers, kept apart from the entity so they can be tested without a world. Six phases like Lucifer Uncaged,
 * a sixth of his health each, in three arenas of Heaven (two phases each): the Garden, the War in Heaven and the Throne
 * Room. Phases I–IV he wears his vessel; from V his true form. True health (65 000 alone, the top of the power curve with
 * Lucifer Uncaged) sits above the vanilla cap through a health scale.
 */
public final class MichaelBalance {

    public static final int PHASES = 6;
    /** The phase his true form is revealed in. */
    public static final int ARCHANGEL_PHASE = 5;
    /** The phases his shadow wings show in (before the true form's wings of steel). */
    public static final int SHADOW_WINGS_PHASE = 3;
    /** The phase he takes to the air. */
    public static final int AERIAL_PHASE = 4;

    public static final int EMERGE_TICKS = 160, TRANSITION_TICKS = 80, TRANSFORM_TICKS = 120, DEATH_TICKS = 160;
    /** Into the transform, when the light peaks and the true form replaces the vessel (the clip's 4.5 s). */
    public static final int TRANSFORM_SWAP_TICKS = 90;
    /** Blocks above the ground he starts his descent from. */
    public static final double DESCENT = 8;

    // --- "I need your yes" --------------------------------------------------------------------------
    /** Ticks a hunter has to answer before silence counts as no. */
    public static final int YES_DECIDE_TICKS = 120;
    /** Extra ticks the server accepts an answer after the deadline (latency). */
    public static final int YES_GRACE_TICKS = 20;
    /** Ticks a hunter who said yes stays possessed. */
    public static final int POSSESS_TICKS = 160;
    /** Ticks of Grace Favour (double damage against him) once he lets go. */
    public static final int FAVOR_TICKS = 300;
    /** Ticks the Host hunts a hunter who said no. */
    public static final int MARK_TICKS = 300;
    /** Share of his true max health he takes back while he wears someone, per second. */
    public static final float POSSESS_HEAL_PER_SECOND = 0.0055f;
    /** Damage against him while the hunter has Grace Favour, as a multiplier. */
    public static final float FAVOR_MULTIPLIER = 2.0f;

    // --- the touch to the forehead ----------------------------------------------------------------------
    /** Ticks of warning before the hand closes (the clip's windup). */
    public static final int TOUCH_WINDUP_TICKS = 24;
    /** Share of his true max health dealt to him during the windup that breaks his reach. */
    public static final float TOUCH_BREAK_SHARE = 0.011f;
    /** A shield raised this many ticks before the hand closes parries it. */
    public static final int TOUCH_PARRY_WINDOW = 10;
    public static final int PARRY_STUN_TICKS = 40;
    /** Damage against him while he reels, as a multiplier. */
    public static final float STAGGER_VULNERABILITY = 1.5f;

    // --- the Host ----------------------------------------------------------------------------------------
    /** The phase the Host first comes down in (it comes back, two companies strong, in phase V). */
    public static final int HOST_PHASE = 2;
    /** Soldiers in the Host's one company (phase II), besides its captain; each of the two companies in phase V. */
    public static final int HOST_SOLDIERS = 7, HOST_SOLDIERS_EACH_OF_TWO = 4;
    /** A hunter this close draws him out from behind the Host. */
    public static final double ENGAGE_DISTANCE = 5;
    /** What the Host's blades are multiplied by (a summon: it keeps its v0.12 strength, off the power curve). */
    public static final float HOST_DAMAGE_MULTIPLIER = 1.6f;
    /** Damage against him while the Host is in disorder (its captain fallen). */
    public static final float HOST_BROKEN_MULTIPLIER = 1.3f;

    // --- the Lance ---------------------------------------------------------------------------------------
    public static final int LANCE_PIN_TICKS = 40;
    /** Ticks the lance may stay in the ground before he calls it back whatever else he wanted to do. */
    public static final int LANCE_AWAY_TICKS = 240;
    /** Without the lance: his speed and the gap between his blows, as multipliers. */
    public static final float UNARMED_SPEED = 1.25f, UNARMED_GAP = 0.75f;
    /**
     * His own lance thrown back at him: an exact blow of this share of his true max health (past his multipliers and
     * soft cap; the hard cap still holds it, so 1 = the hard cap's whole worth), and how long he reels.
     */
    public static final float BORROWED_LANCE_SHARE = 1f;
    public static final int BORROWED_STUN_TICKS = 60;
    /** Phase IV: attacks from the air before he comes down to gather himself, and how long he stays down. */
    public static final int AERIAL_ATTACKS_BEFORE_LANDING = 3, LAND_RECOVER_TICKS = 90;
    /** Ticks a stolen lance stays in a hunter's hand before it returns to him. */
    public static final int BORROWED_TICKS = 100;

    /**
     * What a hunter is assumed to take off him each second (an Ascension V weapon), after his mundane multiplier, the soft
     * cap and the time spent dodging and waiting out the Host: only used to estimate how long a fight lasts.
     */
    public static final double ASSUMED_DPS = 90;

    private MichaelBalance() {
    }

    /** The share of his health at which {@code phase} ends. */
    public static float threshold(int phase) {
        return (PHASES - phase) / (float) PHASES;
    }

    /** Which of Heaven's arenas a phase is fought in: 0 the Garden, 1 the War in Heaven, 2 the Throne Room. */
    public static int arenaOf(int phase) {
        return Math.max(0, Math.min(2, (phase - 1) / 2));
    }

    public static boolean archangel(int phase) {
        return phase >= ARCHANGEL_PHASE;
    }

    public static boolean shadowWings(int phase) {
        return phase >= SHADOW_WINGS_PHASE && phase < ARCHANGEL_PHASE;
    }

    /** True health with {@code players} challengers: the power curve's, plus a share per extra challenger. */
    public static double trueHealth(double perExtraPlayer, int players) {
        return ProgressionScale.healthFor(ProgressionScale.of(Boss.MICHAEL).trueHealth(), Math.max(1, players), (float) perExtraPlayer);
    }

    /** Ticks between attacks in each phase. */
    public static int attackGap(int phase) {
        int[] gaps = {32, 30, 28, 26, 24, 20};
        return gaps[Math.max(0, Math.min(gaps.length - 1, phase - 1))];
    }

    /** Ticks between asking a hunter for their yes: often in the last phase. */
    public static int askGap(int phase) {
        return phase >= PHASES ? 500 : 900;
    }

    /** Ticks spent untouchable over a whole fight: descending, every change of phase (the transform the longest), dying. */
    public static int untouchableTicks() {
        return EMERGE_TICKS + TRANSITION_TICKS * (PHASES - 2) + TRANSFORM_TICKS + DEATH_TICKS;
    }

    /** Roughly how long a fight takes, in seconds, with {@code players} hunters hitting at {@link #ASSUMED_DPS}. */
    public static int estimatedSeconds(double perExtraPlayer, int players) {
        double health = trueHealth(perExtraPlayer, players);
        double dps = ASSUMED_DPS * Math.max(1, players);
        return (int) Math.round(health / dps + untouchableTicks() / 20.0);
    }

    /** Whether an answer to his question, sent at {@code now}, still counts (asked at {@code askedAt}). */
    public static boolean answerInTime(long askedAt, long now) {
        return now >= askedAt && now - askedAt <= YES_DECIDE_TICKS + YES_GRACE_TICKS;
    }
}
