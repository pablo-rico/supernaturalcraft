package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

/**
 * Abaddon's numbers, kept apart from the entity so they can be tested without a world (v0.21). Three phases, a third of her true
 * health each (24 000 alone on the power curve, {@code ProgressionScale}): the Knight of Hell, Possession, the Brand of the
 * Knights. Damage numbers are before her multiplier ({@code Balance.bossDamage} × config). Windups end on the clips' moments
 * ({@link AbaddonAssets#HIT_TICKS}).
 *
 * <p><b>Damage policy.</b> She takes {@link #RESIST} of every blow, except the First Blade's, a Marked hunter's, or any while a
 * devil's trap holds her ({@link #TRAPPED_TICKS}, then she breaks it and is immune to traps for {@link #TRAP_COOLDOWN}); forced
 * out of a knight she is stunned {@link #STUN_TICKS} and takes ×{@link #STUN_VULNERABILITY} ({@link #share}).
 */
public final class AbaddonBalance {

    public static final int PHASES = 3;

    private AbaddonBalance() {
    }

    /** Health fraction below which {@code phase} ends (phase 1 ends at 2/3, phase 2 at 1/3): no gaps between phases. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between her attacks in {@code phase}. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 36;
            case 2 -> 30;
            default -> 24;
        };
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return AbaddonAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- damage policy -----------------------------------------------------------------------------------------------------
    /** The share of an ordinary blow she takes. */
    public static final float RESIST = 0.35f;
    /** Stunned after being forced out of a knight: this much longer, and this much harder hit. */
    public static final int STUN_TICKS = 100;
    public static final float STUN_VULNERABILITY = 2f;
    /** A devil's trap holds her this long (full damage), then she breaks it and ignores traps for a while. */
    public static final int TRAPPED_TICKS = 120, TRAP_COOLDOWN = 600;

    /**
     * The share of a blow she takes.
     *
     * @param full    the First Blade, a Marked attacker, or a devil's trap holding her
     * @param stunned just forced out of a knight
     */
    public static float share(boolean full, boolean stunned) {
        return (full ? 1f : RESIST) * (stunned ? STUN_VULNERABILITY : 1f);
    }

    // --- P1 · the Knight of Hell -------------------------------------------------------------------------------------------
    /** The herald's mark: a red circle follows its target this long, holds still this long, then the strike lands in it. */
    public static final int MARK_FOLLOW = 30, MARK_HOLD = 12;
    public static final float MARK_RADIUS = 2.5f, MARK_DAMAGE = 16f;
    /** The knights run at the struck spot this fast. */
    public static final double MARK_CONVERGE_SPEED = 1.3;
    /** The hellfire sweep: a red cone (120°) this deep, its fire this long on whoever it catches. */
    public static final float SWEEP_REACH = 9f, SWEEP_DAMAGE = 13f;
    public static final int SWEEP_WINDUP = 24, SWEEP_FIRE_SECONDS = 4;
    /** Her retinue: knights called at once, and at most so many of each about her. */
    public static final int KNIGHTS_PER_CALL = 2, MAX_KNIGHTS = 3, MAX_HERALDS = 1;
    public static final int SUMMON_WINDUP = 26;
    /** With no retinue left, she calls it again after this long. */
    public static final int SUMMON_EVERY = 400;

    // --- P2 · Possession ---------------------------------------------------------------------------------------------------
    /** The smoke out of her vessel: a yellow lane to the knight, struck as she passes. */
    public static final int SMOKE_WINDUP = 20, SMOKE_FLIGHT = 12, SMOKE_BLIND = 40;
    public static final float SMOKE_WIDTH = 2.4f, SMOKE_DAMAGE = 8f;
    /** She rides a knight at most this long, then not again for this long after she leaves it. */
    public static final int POSSESS_TICKS = 320, POSSESS_COOLDOWN = 300;
    /** A possessed knight hits this much harder and moves this much faster. */
    public static final float POSSESSED_DAMAGE = 1.6f, POSSESSED_SPEED = 1.35f;

    // --- P3 · the Brand of the Knights -------------------------------------------------------------------------------------
    /** The telekinetic pin: a violet circle on each hunter (so many at most); whoever is still in it is held this long. */
    public static final int PIN_WINDUP = 26, PIN_TICKS = 40, PIN_MAX = 3;
    public static final float PIN_RADIUS = 2.2f, PIN_DAMAGE = 6f;
    /** The Brand: red rings round her at these radii, each with green gaps; whoever stands on a ring but not in a gap burns. */
    public static final float[] BRAND_RINGS = {4.5f, 8.5f, 12.5f};
    public static final float BRAND_BAND = 1.4f, BRAND_GAP_RADIUS = 1.8f, BRAND_DAMAGE = 22f;
    public static final int BRAND_WINDUP = 40, BRAND_GAPS = 2;

    /** The ring {@code distance} from the Brand's centre stands on, or -1. */
    public static int brandRing(double distance) {
        for (int i = 0; i < BRAND_RINGS.length; i++) if (Math.abs(distance - BRAND_RINGS[i]) <= BRAND_BAND) return i;
        return -1;
    }

    /** Whether a point on ring {@code ring} (-1: none) burns, its nearest gap on that ring being {@code nearestGap} away. */
    public static boolean brandHits(int ring, double nearestGap) {
        return ring >= 0 && nearestGap > BRAND_GAP_RADIUS;
    }

    // --- her retinue -------------------------------------------------------------------------------------------------------
    /** The knight: his slash (an arc this wide and deep), its rest, and his guard (blocks blows from the front while up). */
    public static final float KNIGHT_HEALTH = 60f, KNIGHT_ARMOR = 14f, KNIGHT_DAMAGE = 8f;
    public static final float SLASH_REACH = 3.4f, SLASH_ARC = 110f;
    public static final int SLASH_COOLDOWN = 30, GUARD_COOLDOWN = 100;
    /** Chance a knight raises his shield when struck from the front (or when a hunter closes in). */
    public static final float GUARD_CHANCE = 0.35f;
    /** The herald: his own mark and his hellfire cast (a red circle at the target, then the fire). */
    public static final float HERALD_HEALTH = 32f, HERALD_MARK_DAMAGE = 8f, HERALD_CAST_DAMAGE = 6f, HERALD_CAST_RADIUS = 1.6f;
    public static final int HERALD_MARK_COOLDOWN = 220, HERALD_CAST_COOLDOWN = 80;

    // --- her fall ----------------------------------------------------------------------------------------------------------
    /** Her death: she burns from inside ({@code death} clip, held); it ends here. */
    public static final int DEATH_TICKS = 200;
    /** The Knights' Cleaver's wave: this long a lane, this hard (before Ascension), this often. */
    public static final float CLEAVE_REACH = 9f, CLEAVE_WIDTH = 2f, CLEAVE_DAMAGE = 10f;
    public static final int CLEAVE_COOLDOWN = 120, CLEAVE_FIRE_SECONDS = 3;
}
