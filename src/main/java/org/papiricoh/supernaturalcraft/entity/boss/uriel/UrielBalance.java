package org.papiricoh.supernaturalcraft.entity.boss.uriel;

/**
 * Uriel's numbers, apart from the entity so they can be tested without a world (v0.40). Two phases, half of his true health each
 * ({@code ProgressionScale}): I, the Soldier, with his escort of rebel angels; II, the Traitor, alone, his wings out. Damage
 * numbers are before his multiplier ({@code Balance.bossDamage} × difficulty). Windups end on the clips' moments
 * ({@link UrielAssets#HIT_TICKS}).
 *
 * <p>Beaten, he does not fall to any blow: only an angel blade ({@code #angelic_blades}) ends him ({@link #FINISH_TICKS} to strike
 * it; past that his grace pulls him up again with {@link #RALLY} of his health).
 */
public final class UrielBalance {

    public static final int PHASES = 2;
    /** Extra true health per challenger beyond the first; his share of a mundane blow; his own damage factor. */
    public static final float HEALTH_PER_PLAYER = 0.5f, MUNDANE = 0.5f, DAMAGE_FACTOR = 1.0f;

    /** Rebels at his side as the fight begins, and the most he keeps (before difficulty). */
    public static final int ESCORT = 3, ESCORT_CAP = 4;

    // --- I · the Soldier --------------------------------------------------------------------------------------------------------
    public static final int SLASH_WINDUP = 16;
    public static final float SLASH_REACH = 4.0f, SLASH_DAMAGE = 9;
    public static final int THRUST_WINDUP = 14;
    public static final float THRUST_REACH = 6.0f, THRUST_WIDTH = 1.6f, THRUST_DAMAGE = 10;
    public static final int PALM_WINDUP = 26;
    public static final float PALM_RADIUS = 3.5f, PALM_DAMAGE = 8;
    /** The crack's slowness (ticks) and how far it reaches from him toward the target. */
    public static final int PALM_SLOW = 60;
    public static final int SMITE_WINDUP = 30;
    public static final float SMITE_REACH = 9.0f, SMITE_DAMAGE = 11;
    public static final int COMMAND_WINDUP = 20;

    // --- II · the Traitor -------------------------------------------------------------------------------------------------------
    public static final int FLASH_WINDUP = 30;
    public static final float FLASH_RADIUS = 7.0f, FLASH_DAMAGE = 7;
    public static final int FLASH_BLIND = 70;
    /** Phase II strikes a little harder and his smite reaches farther. */
    public static final float TRAITOR_DAMAGE = 1.2f, TRAITOR_SMITE_REACH = 12.0f;

    public static final int TRANSITION = 80;
    public static final int DEATH_TICKS = 100;
    /** Ticks to strike the last blow with an angel blade, and the share of health he rallies with if nobody does. */
    public static final int FINISH_TICKS = 400;
    public static final float RALLY = 0.1f;

    private UrielBalance() {
    }

    /** Health fraction below which {@code phase} ends: half each, no gaps. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 0.5f;
    }

    /** Ticks between his attacks in {@code phase}. */
    public static int attackGap(int phase) {
        return phase <= 1 ? 34 : 26;
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return UrielAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    /** How hard his blows land in {@code phase}. */
    public static float phaseDamage(int phase) {
        return phase >= 2 ? TRAITOR_DAMAGE : 1f;
    }
}
