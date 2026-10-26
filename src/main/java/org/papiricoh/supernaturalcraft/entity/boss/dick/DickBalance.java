package org.papiricoh.supernaturalcraft.entity.boss.dick;

/**
 * Dick Roman's numbers, kept apart from the entity so they can be tested without a world (v0.25). Four phases, a quarter of
 * his true health each: the Executive, the Maw, the Copy (he wears a hunter's face among doubles), the Flood (the boardroom
 * drowns in goo). Damage numbers are before his multiplier ({@code Balance.bossDamage} × config). Windups end on the clips'
 * blows ({@link DickAssets#HIT_TICKS}).
 */
public final class DickBalance {

    public static final int PHASES = 4;

    private DickBalance() {
    }

    /** Health fraction below which {@code phase} ends (1 → 3/4, 2 → 1/2, 3 → 1/4): no gaps between phases. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks in {@code phase}. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 34;
            case 3 -> 30;
            default -> 26;
        };
    }

    /** Ticks from a clip's start to its blow (the art's timings). */
    public static int cue(String clip) {
        return DickAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- P1 · the Executive ----------------------------------------------------------------------------------------------
    /** Downsizing: a red ring round him from his feet to this far, thrown out of it. */
    public static final float DOWNSIZE_RADIUS = 6.5f, DOWNSIZE_DAMAGE = 10f, DOWNSIZE_PUSH = 1.4f;
    public static final int DOWNSIZE_WINDUP = 24;
    /** Sign Here: a violet lane to each hunter (up to so many); whoever stands in it is bound by the contract (rooted). */
    public static final float SIGN_WIDTH = 2f, SIGN_REACH = 16f, SIGN_DAMAGE = 6f;
    public static final int SIGN_WINDUP = 26, SIGN_MAX = 3, SIGN_ROOT = 50;
    /** Staff Meeting: so many employees (disguised Leviathans) walk in at yellow circles near the edge and show their maws. */
    public static final int STAFF = 2, STAFF_WINDUP = 20;
    public static final float STAFF_RADIUS = 1.5f;

    // --- P2 · the Maw ----------------------------------------------------------------------------------------------------
    /** A bite: a red cone (120°) this deep. */
    public static final float BITE_REACH = 5f, BITE_DAMAGE = 14f;
    public static final int BITE_WINDUP = 16;
    /** Swallow: a red lane; the first hunter in it is held in his maw, chewed every second, then spat out down the lane. */
    public static final float SWALLOW_REACH = 10f, SWALLOW_WIDTH = 1.8f, SWALLOW_DAMAGE = 4f, SPIT_DAMAGE = 8f, SPIT_PUSH = 2.2f;
    public static final int SWALLOW_WINDUP = 22, SWALLOW_HOLD = 50;
    /** Goo: a red circle on each hunter (and one more); black goo stays there this long. */
    public static final float GOO_RADIUS = 2.5f, GOO_DAMAGE = 6f;
    public static final int GOO_WINDUP = 28, GOO_TICKS = 240, GOO_MAX = 4;

    // --- P3 · the Copy ---------------------------------------------------------------------------------------------------
    /** He wears a hunter's face this long among so many doubles; hidden, he takes only this share of harm. Borax shows him. */
    public static final int COPY_TICKS = 320, COPY_DOUBLES = 3, COPY_WINDUP = 24;
    public static final float HIDDEN_HARM = 0.15f, COPY_RING = 6f;
    /** A double: as frail as a man, as hungry as a Leviathan. */
    public static final float DOUBLE_HEALTH = 16f, DOUBLE_DAMAGE = 5f;
    /** Found out by borax, he staggers: this long, every blow landing for this much more. */
    public static final int EXPOSED_TICKS = 80;
    public static final float EXPOSED_HARM = 1.5f;

    // --- P4 · the Flood --------------------------------------------------------------------------------------------------
    /** The goo on the floor: what it does each second to whoever wades in it (and slows them). */
    public static final float FLOOD_DAMAGE = 2f;
    /** Tentacles: a red lane from the goo to each hunter (up to so many). */
    public static final float TENTACLE_WIDTH = 1.6f, TENTACLE_REACH = 14f, TENTACLE_DAMAGE = 12f;
    public static final int TENTACLE_WINDUP = 22, TENTACLE_MAX = 3;
    /** The surge: the goo comes back over every dried patch, after a yellow warning this long. */
    public static final int SURGE_WINDUP = 50;
    /** How much borax dries round where it lands (blocks). */
    public static final double DRY_RADIUS = 2.5;

    /** His fall: a long death, then he bursts. */
    public static final int DEATH_TICKS = 140;
}
