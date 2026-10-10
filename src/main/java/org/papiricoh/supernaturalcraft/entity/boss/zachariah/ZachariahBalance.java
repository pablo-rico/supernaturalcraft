package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;

/**
 * Zachariah's numbers, kept apart from the entity so they can be tested without a world (v0.18). Four phases, a quarter of his
 * true health each (50 000 alone on the power curve, {@code ProgressionScale}): Intake, Review, It Was Already Written, Final
 * Judgment. Shares are of true max health, never fixed points (the v0.15 rule); damage numbers are before his multiplier
 * ({@code Balance.bossDamage} × config); his clerks' are a minion's, off the curve.
 */
public final class ZachariahBalance {

    public static final int PHASES = 4;

    private ZachariahBalance() {
    }

    /** Health fraction below which {@code phase} ends (quarters: 3/4, 1/2, 1/4). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks in {@code phase}: the paperwork piles up faster. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 34;
            case 3 -> 30;
            default -> 24;
        };
    }

    /** The arena round his office: the whole square office (its corners) and a margin. */
    public static final int ARENA_RADIUS = (int) Math.ceil(Math.max(ZachariahOfficeLayout.RADIUS, ZachariahOfficeLayout.WRAP_WINDOW) * Math.sqrt(2)) + 2;

    // --- "It was already written" (phases III and IV) -------------------------------------------------------------------
    /** Attacks announced ahead in {@code phase} (none before III). */
    public static int docketSize(int phase) {
        return phase >= 4 ? 5 : phase == 3 ? 3 : 0;
    }

    /** A foretold attack's telegraph shows this much earlier than usual. */
    public static final int FORETOLD_LEAD = 60;
    /** A revision's entry is struck through this long before it changes. */
    public static final int REVISION_WARNING = 20;
    /** The revision of a phase comes after this many of its foretold attacks. */
    public static final int REVISION_AFTER = 2;

    // --- the office wrap ------------------------------------------------------------------------------------------------
    /**
     * How far to move something {@code rel} blocks from the office's centre on one axis: past the window by a whole
     * {@link ZachariahOfficeLayout#WRAP_SHIFT} back toward the other side (0 inside it). Only the first shift: anything further
     * out than one shift is not in the office.
     */
    public static int wrapShift(double rel) {
        int w = ZachariahOfficeLayout.WRAP_WINDOW, s = ZachariahOfficeLayout.WRAP_SHIFT;
        if (rel > w && rel <= w + s) return -s;
        if (rel < -w && rel >= -w - s) return s;
        return 0;
    }

    /** {@code rel} folded into the window (where a spot of the repeating office is, seen from inside it). */
    public static double fold(double rel) {
        double r = rel;
        for (int i = 0; i < 4 && wrapShift(r) != 0; i++) r += wrapShift(r);
        return r;
    }

    // --- the attacks ----------------------------------------------------------------------------------------------------
    /** Paper Storm: a cone of memos this long and wide, so many memos, each this hard. */
    public static final float STORM_REACH = 14f, STORM_HALF_ANGLE = 35f, MEMO_DAMAGE = 5f, MEMO_SPEED = 0.9f;
    public static final int STORM_MEMOS = 18, MEMO_LIFE = 50;
    /** Rubber Stamp: a 3×3 square on each of up to three hunters; a stamped hunter is DENIED (PAPERWORK) this long. */
    public static final float STAMP_HALF = 1.5f, STAMP_DAMAGE = 14f;
    public static final int STAMP_TARGETS = 3, DENIED_TICKS = 160;
    /** Clerks: so many at once, while fewer than {@link #CLERK_CAP} stand. */
    public static final int CLERKS = 3, CLERK_CAP = 2;
    public static final float CLERK_HEALTH = 40f, CLERK_DAMAGE = 5f;
    /** Precedent: where each hunter stood this many ticks ago (and where they stand now), struck this wide. */
    public static final int PRECEDENT_DELAY = 60, TRAIL_EVERY = 5;
    public static final float PRECEDENT_RADIUS = 1.8f, PRECEDENT_DAMAGE = 12f;
    /** Cubicle Shuffle: the partitions stand this long. */
    public static final int SHUFFLE_TICKS = 240;
    /** Termination Notice: due after this long; this share of the marked hunter's max health, split with whoever stands near. */
    public static final int TERMINATION_TICKS = 100;
    public static final float TERMINATION_SHARE = 0.4f, SHARE_RADIUS = 3f, DESK_RADIUS = 1.5f;
    /** Reassignment: a hunter caught in it is moved one tile, with a light blow. */
    public static final float REASSIGN_RADIUS = 1.6f, REASSIGN_DAMAGE = 6f;
    /** Wing Buffet (III+): a cone of wings before him. */
    public static final float BUFFET_REACH = 7f, BUFFET_HALF_ANGLE = 60f, BUFFET_DAMAGE = 10f, BUFFET_SHOVE = 2.2f;
    /** Smite of Heaven (IV): golden lines out from him, a safe ring between {@link #SMITE_SAFE_INNER} and {@link #SMITE_SAFE_OUTER}. */
    public static final int SMITE_LINES = 8;
    public static final float SMITE_LENGTH = 24f, SMITE_HALF_WIDTH = 1.2f, SMITE_DAMAGE = 20f, SMITE_SAFE_INNER = 9f, SMITE_SAFE_OUTER = 11f;
    /** Overdue Smite: a hunter whose form went overdue is struck where they stand. */
    public static final float OVERDUE_RADIUS = 2.5f, OVERDUE_DAMAGE = 18f;
    /** Blink: he steps in when his target is this far, or out of sight this long. */
    public static final double BLINK_DISTANCE = 18;
    public static final int BLINK_SIGHT_TICKS = 60;

    /** Whether a point ({@code dx}, {@code dz}) from a stamp's centre is under it. */
    public static boolean underStamp(double dx, double dz) {
        return Math.abs(dx) <= STAMP_HALF && Math.abs(dz) <= STAMP_HALF;
    }

    /** What each of {@code sharers} takes of a Termination Notice on a hunter of {@code maxHealth} (nobody escapes it alone). */
    public static float terminationEach(float maxHealth, int sharers) {
        return maxHealth * TERMINATION_SHARE / Math.max(1, sharers);
    }

    /** Whether {@code distance} from him is safe from the Smite of Heaven's lines (the ring). */
    public static boolean smiteSafe(double distance) {
        return distance >= SMITE_SAFE_INNER && distance <= SMITE_SAFE_OUTER;
    }

    /** Whether a point {@code along} and {@code across} a smite line (from him outward) is struck. */
    public static boolean smiteHits(double along, double across) {
        return along >= 0 && along <= SMITE_LENGTH && Math.abs(across) <= SMITE_HALF_WIDTH && !smiteSafe(along);
    }

    // --- his entrance and fall ------------------------------------------------------------------------------------------
    /** His death lasts this long; at {@link #DEATH_BURST} the light leaves him. */
    public static final int DEATH_TICKS = 120, DEATH_BURST = 80;
    /** A stagger (a clip, and a moment open to blows). */
    public static final int STAGGER_TICKS = 40;
    public static final float STAGGER_VULNERABILITY = 1.3f;
}
