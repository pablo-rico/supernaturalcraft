package org.papiricoh.supernaturalcraft.entity.boss.crowley;

/**
 * Crowley's numbers, kept apart from the entity so they can be tested without a world (v0.35). Three phases, a third of his
 * true health each ({@code ProgressionScale}), each in its own room of the asylum: the Hall, the Cell Wing, the Throne Room.
 * He is a strategist, not a brawler: he takes {@link #RESIST} of a blow while his people stand about him, and all of it once
 * they are down ({@link #share}). Damage numbers are before his multiplier ({@code Balance.bossDamage} × config). Windups end
 * on the clips' moments ({@link CrowleyAssets#HIT_TICKS}).
 */
public final class CrowleyBalance {

    public static final int PHASES = 3;

    private CrowleyBalance() {
    }

    /** Health fraction below which {@code phase} ends (phase 1 at 2/3, phase 2 at 1/3): no gaps between phases. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks in {@code phase}. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 32;
            default -> 26;
        };
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return CrowleyAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- damage policy -----------------------------------------------------------------------------------------------------
    /** The share of a blow he takes while at least {@link #GUARDED_BY} of his people stand within {@link #GUARD_RANGE}. */
    public static final float RESIST = 0.5f;
    public static final int GUARDED_BY = 2;
    public static final double GUARD_RANGE = 12;

    public static float share(int guards) {
        return guards >= GUARDED_BY ? RESIST : 1f;
    }

    // --- his attacks -------------------------------------------------------------------------------------------------------
    /** "Release the hounds": red rings where they come through, this many hounds a ring, at most so many about. */
    public static final int HOUNDS_WINDUP = 30, HOUNDS_PER_CALL = 2, MAX_HOUNDS = 4;
    public static final float HOUND_RING_RADIUS = 1.6f, HOUND_SCORCH_DAMAGE = 6f;
    /** Called hounds are visible this long (their paws scorch the floor); then the old rules (holy water, Reveal) apply. */
    public static final int HOUNDS_REVEAL = 20 * 12;
    /** The snap: he is behind a hunter, a violet line marks the shove; whoever is on it is thrown and hurt. */
    public static final int SNAP_WINDUP = 18;
    public static final float SNAP_REACH = 6f, SNAP_WIDTH = 1.6f, SNAP_DAMAGE = 9f, SNAP_PUSH = 1.6f;
    /** The red smoke: a yellow zone; inside it hunters are blinded and his people moved about. */
    public static final int SMOKE_WINDUP = 24, SMOKE_TICKS = 100, SMOKE_BLIND = 60;
    public static final float SMOKE_RADIUS = 4f;
    /** "You're fired": a red 120° cone of flying contracts. */
    public static final int PAPER_WINDUP = 22;
    public static final float PAPER_REACH = 9f, PAPER_DAMAGE = 12f;
    /** "Call the office": suits come through the doors (spawn points of the room), at most so many about. */
    public static final int CALL_WINDUP = 26, SUITS_PER_CALL = 2, MAX_SUITS = 4;
    /** Phase 2's patients: a wave this big when the cell wing opens and every so often after, at most so many about. */
    public static final int PATIENTS_PER_WAVE = 4, MAX_PATIENTS = 8, PATIENT_WAVE_EVERY = 320;
    /** Phase 3 · Hellfire: red lines across the throne room, this many, this long, burning this long. */
    public static final int FIRE_WINDUP = 30, FIRE_LINES = 3, FIRE_TICKS = 60;
    public static final float FIRE_LENGTH = 14f, FIRE_WIDTH = 1.4f, FIRE_DAMAGE = 14f;
    /** Phase 3 · Collection: a hunter with an open crossroads deal is marked; the hounds go for them this long. */
    public static final int COLLECT_WINDUP = 20, COLLECT_TICKS = 160;
    /** Phase 3: he starts seated and rises once the phase is half gone. */
    public static final float RISE_AT = 0.5f;
    /** Crossroads demons called in the throne room. */
    public static final int CROSSROADS_DEMONS = 2;

    // --- his people --------------------------------------------------------------------------------------------------------
    public static final float SUIT_HEALTH = 36f, SUIT_DAMAGE = 7f;
    public static final float PATIENT_HEALTH = 20f, PATIENT_DAMAGE = 5f;

    // --- his fall ----------------------------------------------------------------------------------------------------------
    /** He kneels this long before the talk is offered; the talk waits this long for an answer (silence = execute). */
    public static final int KNEEL_TICKS = 60, TALK_TIMEOUT = 20 * 60;
    public static final int DEATH_TICKS = 140;

    // --- the companion -----------------------------------------------------------------------------------------------------
    /** Retreats at 0 health for this long (world ticks), then comes back to his owner (or to his seat). */
    public static final long RETREAT_TICKS = 20L * 60 * 10;
    public static final float COMPANION_HEALTH = 120f, COMPANION_DAMAGE = 10f;
    /** He blinks to his owner beyond this distance; while seated he keeps within this radius of his seat. */
    public static final double FOLLOW_BLINK = 24, SEAT_RADIUS = 6;
    /** Hounds he calls to his side (bound, timed), and how often. */
    public static final int COMPANION_HOUNDS = 2, COMPANION_HOUNDS_COOLDOWN = 20 * 45, COMPANION_SNAP_COOLDOWN = 20 * 8;
    /** His remarks: at most one every so often. */
    public static final int REMARK_COOLDOWN = 20 * 90;
    /** One debt-free deal a day per hunter; the favour (a crossroads debt forgiven) once a week; intrigue every few days. */
    public static final int FAVOUR_DAYS = 7, INTRIGUE_DAYS = 3;
    public static final int LUCK_TICKS = 20 * 60 * 20, REVEAL_TICKS = 20 * 120;
    public static final int TRADE_EMERALDS = 8;

    // --- the Contract Book -------------------------------------------------------------------------------------------------
    /** XP levels per 10 max health of the bound mob (at least {@link #BOOK_MIN_LEVELS}); it serves this long; then a rest. */
    public static final int BOOK_MIN_LEVELS = 2, BOOK_SERVE_TICKS = 20 * 60 * 5, BOOK_COOLDOWN = 20 * 60 * 2;
    public static final float BOOK_MAX_HEALTH = 300f;

    public static int bookCost(float maxHealth) {
        return Math.max(BOOK_MIN_LEVELS, (int) Math.ceil(maxHealth / 10f));
    }
}
