package org.papiricoh.supernaturalcraft.entity.boss.cain;

/**
 * Cain's numbers, kept apart from the entity so they can be tested without a world (v0.21). Three phases, a third of his true
 * health each (19 000 alone on the power curve, {@code ProgressionScale}): the Beekeeper, the Mark awakens, the Father of
 * Murder. Damage numbers are before his multiplier ({@code Balance.bossDamage} × config). Windups end on the clips' blows
 * ({@link CainAssets#HIT_TICKS}).
 */
public final class CainBalance {

    public static final int PHASES = 3;

    private CainBalance() {
    }

    /** Health fraction below which {@code phase} ends (phase 1 ends at 2/3, phase 2 at 1/3): no gaps between phases. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks in {@code phase}. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 38;
            case 2 -> 32;
            default -> 24;
        };
    }

    /** Ticks from a clip's start to its blow (the art's timings). */
    public static int cue(String clip) {
        return CainAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- P1 · the Beekeeper ------------------------------------------------------------------------------------------------
    /** The lunge: three dashes, each down a red lane this long and wide, shown this long before it. */
    public static final int LUNGES = 3;
    public static final float LUNGE_REACH = 11f, LUNGE_WIDTH = 2.2f, LUNGE_DAMAGE = 10f;
    public static final int LUNGE_WINDUP = 18, LUNGE_SPACING = 16;
    /** The swarm: a yellow circle on each hunter (and one more), then the bees, stinging every second. */
    public static final float SWARM_RADIUS = 3.5f, SWARM_DAMAGE = 3f;
    public static final int SWARM_WINDUP = 30, SWARM_TICKS = 60, SWARM_MAX = 4, SWARM_POISON = 80, SWARM_BLIND = 50;

    // --- P2 · the Mark awakens ---------------------------------------------------------------------------------------------
    /** The furrow: a plough's swing ripping a red cone (120°) this deep. */
    public static final float FURROW_REACH = 9f, FURROW_DAMAGE = 12f, FURROW_LIFT = 0.6f;
    public static final int FURROW_WINDUP = 22, FURROW_SCAR_TICKS = 400;
    /** Abel's blood: spikes ring out of the ground between these radii round him. */
    public static final float ABEL_INNER = 2.5f, ABEL_OUTER = 7.5f, ABEL_DAMAGE = 14f;
    public static final int ABEL_WINDUP = 28, ABEL_SPIKE_TICKS = 60;
    /** Thrown tools: a violet lane to each hunter (up to so many), the tools landing this long after the throw. */
    public static final float TOOLS_WIDTH = 1.6f, TOOLS_MAX_REACH = 18f, TOOLS_DAMAGE = 9f;
    public static final int TOOLS_WINDUP = 20, TOOLS_FLIGHT = 6, TOOLS_MAX = 3;

    // --- P3 · the Father of Murder -----------------------------------------------------------------------------------------
    /** The execution: a red circle where his mark stands, then he is behind it and the blade comes down. */
    public static final float EXECUTION_RADIUS = 2.5f, EXECUTION_DAMAGE = 20f;
    public static final int EXECUTION_WINDUP = 26;
    public static final double EXECUTION_BEHIND = 1.6;
    /** The Mark's flare: a red burst this wide round him, green shelters this wide (beside his hives) the only safety. */
    public static final float FLARE_RADIUS = 15f, FLARE_SAFE_RADIUS = 2.4f, FLARE_DAMAGE = 22f;
    public static final int FLARE_WINDUP = 46, FLARE_SHELTERS = 3;
    /** Where a shelter sits when he has no hives (spawned by command): this share of the arena's radius out. */
    public static final double FLARE_SHELTER_REACH = 0.75;

    /** Whether {@code distance} (flat) from the flare's centre is struck, given distances to the shelters (outside an arena). */
    public static boolean flareHits(double distance, double nearestShelter) {
        return flareHits(distance, nearestShelter, FLARE_RADIUS);
    }

    /** As {@link #flareHits(double, double)}, for a flare {@code radius} wide ({@link #flareRadius}). */
    public static boolean flareHits(double distance, double nearestShelter, double radius) {
        return distance <= radius && nearestShelter > FLARE_SAFE_RADIUS;
    }

    /** The flare's reach: the whole yard (one block past the edge of an arena {@code arenaRadius} wide), or {@link #FLARE_RADIUS} with none (≤ 0). */
    public static double flareRadius(double arenaRadius) {
        return arenaRadius > 0 ? arenaRadius + 1 : FLARE_RADIUS;
    }

    /** Whether a point at {@code distance} from him stands in Abel's blood. */
    public static boolean inAbelsRing(double distance) {
        return distance >= ABEL_INNER && distance <= ABEL_OUTER;
    }

    // --- vulnerability and his fall ----------------------------------------------------------------------------------------
    /** The First Blade, his own: it bites him harder. */
    public static final float FIRST_BLADE_VULNERABILITY = 1.5f;
    /** His death: he kneels ({@code death} clip, held) while the Mark is offered; it ends here. */
    public static final int DEATH_TICKS = 200;
    /** How long a hunter has to answer his offer of the Mark (the death, nearly). */
    public static final int OFFER_TICKS = 180;
}
