package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

/**
 * Gabriel's numbers, kept apart from the entity so they can be tested without a world (v0.14). Four phases, one channel each,
 * a quarter of his true health each (600 vanilla × {@link #healthScale}, about 1500 alone). Every channel's rule is timed
 * here: the laugh track, the applause, the quiz rounds, the heart monitor, the spokesmen's shuffle.
 */
public final class GabrielBalance {

    public static final int PHASES = 4;
    /** Vanilla health; the rest is {@link #healthScale}. */
    public static final double BASE_HEALTH = 600;

    // --- CH 2, the sitcom -----------------------------------------------------------------------------------------------
    /** The LAUGH sign: lit this long (he can't be touched, the gags play), then dark this long (he takes more). */
    public static final int LAUGH_TICKS = 120, QUIET_TICKS = 160;
    /** What he takes while the sign is dark. */
    public static final float QUIET_VULNERABILITY = 1.3f;
    /** The APPLAUSE: nobody has hit him for this long in a quiet spell → he bows and heals this share of his health. */
    public static final int APPLAUSE_AFTER = 80;
    public static final float APPLAUSE_HEAL = 0.02f;

    // --- CH 5, the game show --------------------------------------------------------------------------------------------
    /** A round of the quiz every this many ticks; the buzzer this long after the question. */
    public static final int QUIZ_EVERY = 500, QUIZ_ANSWER_TICKS = 160;
    /** Three answers, three platforms. */
    public static final int QUIZ_CHOICES = 3;
    /** Right: strength for the hunter, a stun for him. Wrong (or on no platform): the punishment's damage. */
    public static final int QUIZ_STRENGTH_TICKS = 100, QUIZ_STUN_TICKS = 60;
    public static final float QUIZ_WRONG_DAMAGE = 8f;

    // --- CH 7, the hospital ---------------------------------------------------------------------------------------------
    /** The heart monitor: a beep every this many ticks at the start of the phase, quickening to {@link #BEAT_FASTEST}. */
    public static final int BEAT_SLOWEST = 24, BEAT_FASTEST = 14;
    /** A hit within this many ticks of a beep is critical, for this much. */
    public static final int BEAT_WINDOW = 4;
    public static final float BEAT_CRIT = 1.5f;
    /** A nurse double that reaches him heals this share of his health. */
    public static final float NURSE_HEAL = 0.03f;

    // --- CH 9, the commercial -------------------------------------------------------------------------------------------
    /** Five spokesmen at five podiums; the podiums shuffle every this many ticks, or when one is struck. */
    public static final int SPOKESMEN = 5, SHUFFLE_EVERY = 200;

    // --- Allegiance (v0.13) ---------------------------------------------------------------------------------------------
    /** An angel loses this much Grace a second during his commercial breaks (the phase changes). */
    public static final float ANGEL_GRACE_DRAIN = 5f;
    /** A demon is the villain of the episode: his blows hurt them this much more. */
    public static final float DEMON_DAMAGE_TAKEN = 1.15f;

    // --- the gags, the punishments, the commercial break ---------------------------------------------------------------
    /** A stunned host (a right answer) takes this much more. */
    public static final float STUN_VULNERABILITY = 1.25f;
    /** A pie in the face: blind this long. */
    public static final int PIE_BLIND_TICKS = 60;
    /** Banana peels: how many each gag drops, how long they lie, the slip (slowness and the shove). */
    public static final int PEELS = 4, PEEL_TICKS = 240, SLIP_TICKS = 40;
    public static final double SLIP_SHOVE = 0.9;
    /** The falling piano: its warning, its reach and its blow. */
    public static final int PIANO_WARNING = 40;
    public static final float PIANO_RADIUS = 3f, PIANO_DAMAGE = 14f;
    /** Extras walking in through the sitcom's doors, each gag. */
    public static final int EXTRAS = 3;
    /** The game show's trapdoors stay open this long; the pit's floor is foam (slime). */
    public static final int TRAPDOOR_TICKS = 50;
    /** The first round comes this long after the channel opens. */
    public static final int FIRST_QUIZ = 160;
    /** Nurses: this many every this many ticks, and how long one may walk before it gives up. */
    public static final int NURSES = 2, NURSE_EVERY = 320, NURSE_LIFETIME = 600;
    /** A double struck in the commercial: what its punishments do. */
    public static final float DOUBLE_PUNISH_DAMAGE = 6f;
    public static final double PUNISH_TELEPORT = 7;
    /** The real one struck: the podiums shuffle this long after. */
    public static final int STRUCK_SHUFFLE_DELAY = 10;
    /** The six-winged shadow is sent again this often (REVEAL), lasting a little longer. */
    public static final int REVEAL_REFRESH = 20;
    /** A double lives at most this long (extras). */
    public static final int DOUBLE_LIFETIME = 900;

    private GabrielBalance() {
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
            case 1 -> 32;
            case 2 -> 30;
            case 3 -> 26;
            default -> 24;
        };
    }

    /** Where in the laugh track's cycle {@code ticks} falls: the start of its quiet spell, counted from the channel's start. */
    public static int quietStart(int ticks) {
        int cycle = LAUGH_TICKS + QUIET_TICKS;
        return Math.floorDiv(ticks, cycle) * cycle + LAUGH_TICKS;
    }

    /** The hospital's progress, 0 at the start of phase 3 (half his health) to 1 at its floor (a quarter). */
    public static float hospitalProgress(float healthFraction) {
        return Math.max(0f, Math.min(1f, (threshold(2) - healthFraction) / (threshold(2) - threshold(3))));
    }

    /** Whether the LAUGH sign is lit {@code ticks} into the sitcom (it starts lit). */
    public static boolean laughing(int ticks) {
        return Math.floorMod(ticks, LAUGH_TICKS + QUIET_TICKS) < LAUGH_TICKS;
    }

    /** The heart monitor's period {@code progress} (0 at the start of the phase, 1 at its floor) of the way through. */
    public static int beatPeriod(float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        return Math.round(BEAT_SLOWEST + (BEAT_FASTEST - BEAT_SLOWEST) * p);
    }

    /** Whether a hit {@code sinceBeat} ticks after the last beep, with the next one {@code period} after it, lands on the beat. */
    public static boolean onBeat(int sinceBeat, int period) {
        int t = Math.floorMod(sinceBeat, Math.max(1, period));
        return t <= BEAT_WINDOW || period - t <= BEAT_WINDOW;
    }
}
