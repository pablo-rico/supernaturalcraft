package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

/**
 * The Grand Witch's numbers, kept apart from the entity so they can be tested without a world (v0.21, P6). Three phases, a third
 * of her true health each (27 000 alone on the power curve): the Coven's Shield, the Matriarch's Hexes, the Mass Taglock.
 * Damage numbers are before her multiplier ({@code Balance.bossDamage} × config). Windups end on the clips' moments
 * ({@link GrandWitchAssets#HIT_TICKS}).
 *
 * <p><b>Phase I.</b> {@link #CHANNELERS} Coven Witches channel into her: while any of them still channels, every blow on her is
 * swallowed ({@link #shielded}). A channeler whose own shield breaks (blows, salt, Break Hex) is interrupted for
 * {@link #INTERRUPT_TICKS}; a dead one is gone for good.
 *
 * <p><b>Phase III.</b> {@link #MASS_FOLLOW} ticks a red circle follows every hunter (their taglocks, all at once), then the
 * curse lands on each one who is not standing in one of the green circles Rowena draws ({@link #SAFE_CIRCLES}).
 */
public final class GrandWitchBalance {

    public static final int PHASES = 3;

    private GrandWitchBalance() {
    }

    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 50;
            case 2 -> 30;
            default -> 26;
        };
    }

    public static int cue(String clip) {
        return GrandWitchAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- P1 · the Coven's Shield ---------------------------------------------------------------------------------------------
    /** Her channelers: so many, this far from her. */
    public static final int CHANNELERS = 3;
    public static final double CHANNEL_DISTANCE = 7;
    /** A channeler whose shield breaks stops channelling this long. */
    public static final int INTERRUPT_TICKS = 240;
    /** The beams are re-sent this often (late joiners see them too). */
    public static final int BEAM_REFRESH = 40;

    /** Whether she is shielded: in phase I, while any channeler still channels. */
    public static boolean shielded(int phase, int channeling) {
        return phase == 1 && channeling > 0;
    }

    // --- P2 · the Matriarch's Hexes ------------------------------------------------------------------------------------------
    /** Frost Hex: a blue circle under each hunter (so many at most); frost and a heavy slow. */
    public static final float FROST_RADIUS = 2.5f, FROST_DAMAGE = 10f;
    public static final int FROST_WINDUP = 26, FROST_SLOW_TICKS = 60, FROST_MAX = 4;
    /** Inferno: a red circle round her; fire. */
    public static final float INFERNO_RADIUS = 6f, INFERNO_DAMAGE = 16f;
    public static final int INFERNO_WINDUP = 30, INFERNO_FIRE_SECONDS = 5;
    /** Crush: a violet circle on the hunter; whoever is still in it is crushed and pinned a moment. */
    public static final float CRUSH_RADIUS = 2.2f, CRUSH_DAMAGE = 18f;
    public static final int CRUSH_WINDUP = 24, CRUSH_PIN_TICKS = 30;

    // --- P3 · the Mass Taglock -----------------------------------------------------------------------------------------------
    /** The red circle follows each hunter this long, then holds this long; the curse then lands. */
    public static final int MASS_FOLLOW = 40, MASS_HOLD = 14;
    public static final float MASS_RADIUS = 1.6f, MASS_DAMAGE = 20f;
    public static final int MASS_CURSE_TICKS = 300;
    /** Rowena's green circles: so many, this wide, this far (at most) from a hunter. */
    public static final int SAFE_CIRCLES = 2;
    public static final float SAFE_RADIUS = 2.5f;
    public static final double SAFE_REACH = 7;
    /** Smoke Veil: she blinks behind a hunter (a yellow circle where she lands first). */
    public static final float VEIL_RADIUS = 2.4f, VEIL_DAMAGE = 12f;
    public static final int VEIL_WINDUP = 20, VEIL_BLIND = 40;

    /** Whether a hunter {@code nearestSafe} blocks from the nearest green centre escapes the mass curse. */
    public static boolean spared(double nearestSafe) {
        return nearestSafe <= SAFE_RADIUS;
    }

    // --- Rowena at her side ---------------------------------------------------------------------------------------------------
    /** Rowena throws a bolt at the coven this often. */
    public static final int ALLY_BOLT_EVERY = 100;
    public static final float ALLY_BOLT_POTENCY = 1.5f;

    // --- her fall --------------------------------------------------------------------------------------------------------------
    public static final int DEATH_TICKS = 140;
    /** The Coven Staff: its hex (before Ascension), its reach, its curse. */
    public static final float STAFF_DAMAGE = 9f, STAFF_RANGE = 18f, STAFF_WITCHCRAFT_MANA = 0.8f, STAFF_WITCHCRAFT_POTENCY = 1.25f;
    public static final int STAFF_COOLDOWN = 30, STAFF_CURSE_TICKS = 160;
}
