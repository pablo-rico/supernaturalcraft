package org.papiricoh.supernaturalcraft.entity.boss.eve;

import java.util.List;

/**
 * Eve's numbers, kept apart from the entity so they can be tested without a world (v0.39). Four phases, a quarter of her true
 * health each, and no special weakness: the Mother, the Swarm, Mother's Call, the Mother of All. Damage numbers are before her
 * multiplier ({@code Balance.bossDamage} × config × difficulty); her heaviest blows sit with the other bosses of her tier (Dick
 * Roman's bite, Raphael's snap: {@code EveBalanceTest}). Windups end on the clips' blows ({@link EveAssets#HIT_TICKS}).
 */
public final class EveBalance {

    public static final int PHASES = 4;

    private EveBalance() {
    }

    /** Health fraction below which {@code phase} ends (1 → 3/4, 2 → 1/2, 3 → 1/4): no gaps between phases. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between her attacks in {@code phase}: the pressure rises. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 34;
            case 3 -> 28;
            default -> 22;
        };
    }

    /** Ticks from a clip's start to its blow (the art's timings). */
    public static int cue(String clip) {
        return EveAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- P1 · the Mother -------------------------------------------------------------------------------------------------
    /** A backhand of root-clawed fingers: a red cone (120°) this deep. */
    public static final float LASH_REACH = 4.5f, LASH_DAMAGE = 12f, LASH_PUSH = 0.9f;
    public static final int LASH_WINDUP = 14;
    /** Grasping roots: a red circle under each hunter (up to so many); whoever stands in it when they rise is held. */
    public static final float GRASP_RADIUS = 2.2f, GRASP_DAMAGE = 10f;
    public static final int GRASP_WINDUP = 26, GRASP_MAX = 4, GRASP_HOLD = 50, GRASP_ROOTS_LINGER = 80;
    /** Her children: so many climb out at yellow circles (a share by the difficulty), as frail as they are hungry. */
    public static final int BIRTH_BASE = 2, BIRTH_WINDUP = 26, CHILD_CAP = 8;
    public static final float BIRTH_RADIUS = 1.6f, CHILD_HEALTH = 24f, CHILD_DAMAGE = 5f;

    // --- P2 · the Swarm --------------------------------------------------------------------------------------------------
    /** Roaches pour out of her mouth down a red lane; whoever stays in it is bitten every {@link #SWARM_EVERY} ticks. */
    public static final float SWARM_WIDTH = 3f, SWARM_REACH = 14f, SWARM_DAMAGE = 3f;
    public static final int SWARM_WINDUP = 18, SWARM_TICKS = 40, SWARM_EVERY = 10;

    // --- P3 · Mother's Call ----------------------------------------------------------------------------------------------
    /** Her cry: every soul of Purgatory this near turns on the hunters, and a child or two more climbs out. */
    public static final float CALL_RANGE = 48f;
    public static final int CALL_WINDUP = 26, CALL_BIRTHS = 1, CALL_MAX = 6;
    /** A ring of thorns bursts round her: red from {@link #THORN_INNER} out to {@link #THORN_OUTER}, green safety at her feet. */
    public static final float THORN_INNER = 2.5f, THORN_OUTER = 8f, THORN_DAMAGE = 14f;
    public static final int THORN_WINDUP = 22;

    // --- P4 · the Mother of All ------------------------------------------------------------------------------------------
    /** The whole glade erupts: red over all of it but a few green circles, which move each time. */
    public static final float FIELD_DAMAGE = 16f, FIELD_SAFE_RADIUS = 2.5f;
    public static final int FIELD_WINDUP = 40, FIELD_SAFE = 3;
    /** From the last phase her swarm comes down two lanes. */
    public static final int LAST_SWARMS = 2;

    /** Her fall: a long withering. */
    public static final int DEATH_TICKS = 160;

    /** Every damaging blow, for the budget test. */
    public static final List<Float> BLOWS = List.of(LASH_DAMAGE, GRASP_DAMAGE, SWARM_DAMAGE * (SWARM_TICKS / (float) SWARM_EVERY), THORN_DAMAGE,
            FIELD_DAMAGE);
}
