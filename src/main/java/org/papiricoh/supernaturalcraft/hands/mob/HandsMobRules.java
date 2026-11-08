package org.papiricoh.supernaturalcraft.hands.mob;

/**
 * The numbers of season 11's creatures (v0.42), pure: the Thule Society's raised soldiers and their necromancer officers, the
 * banshee and the Chitters. None of them is a boss: they never join {@code #bosses} and do not scale with the hunter.
 */
public final class HandsMobRules {

    private HandsMobRules() {
    }

    // --- Thule soldier ------------------------------------------------------------------------------------------------------
    public static final double SOLDIER_HEALTH = 26, SOLDIER_ARMOR = 4, SOLDIER_DAMAGE = 5, SOLDIER_SPEED = 0.25;
    /** The rifle: fired from {@link #SHOT_MIN}..{@link #SHOT_MAX} blocks after an aim of {@link #AIM_TICKS}. */
    public static final double SHOT_MIN = 4, SHOT_MAX = 22;
    public static final int AIM_TICKS = 14, SHOT_COOLDOWN = 50;
    public static final float SHOT_DAMAGE = 6f;
    /** A rising soldier stands still this long (its {@code rise} clip). */
    public static final int RISE_TICKS = 30;

    /** The chance a shot at {@code distance} blocks hits (the rifle is old and the hands are dead). */
    public static double hitChance(double distance) {
        return Math.max(0.35, Math.min(0.95, 1.05 - distance * 0.03));
    }

    // --- Thule officer ------------------------------------------------------------------------------------------------------
    public static final double OFFICER_HEALTH = 48, OFFICER_ARMOR = 6, OFFICER_DAMAGE = 6, OFFICER_SPEED = 0.24;
    /** He raises a dead soldier every so often, while fewer than {@link #MAX_RAISED} stand near him. */
    public static final int RAISE_COOLDOWN = 220, RAISE_WINDUP = 30, MAX_RAISED = 4;
    /** How far he reaches for a corpse (or bare ground) to raise. */
    public static final double RAISE_REACH = 14;
    /** How long a fallen soldier's corpse can still be raised. */
    public static final int CORPSE_TICKS = 1200;
    /** His black-sun bolt: within {@link #CAST_RANGE}, after a windup. */
    public static final double CAST_RANGE = 18;
    public static final int CAST_WINDUP = 20, CAST_COOLDOWN = 80, CAST_WITHER_TICKS = 80;
    public static final float CAST_DAMAGE = 5f;

    // --- Banshee ------------------------------------------------------------------------------------------------------------
    public static final double BANSHEE_HEALTH = 34, BANSHEE_DAMAGE = 5, BANSHEE_SPEED = 0.3, BANSHEE_FLY = 0.45;
    /** She shows herself only to a hunter this close (or when hurt, or screaming). */
    public static final double REVEAL_RANGE = 8;
    public static final int REVEAL_TICKS = 100;
    /** Her scream: at a target within {@link #SCREAM_RANGE}, after {@link #SCREAM_WINDUP}; it hurts all within {@link #SCREAM_RADIUS}. */
    public static final double SCREAM_RANGE = 10, SCREAM_RADIUS = 7;
    public static final int SCREAM_WINDUP = 30, SCREAM_COOLDOWN = 160, DARKNESS_TICKS = 140;
    public static final float SCREAM_DAMAGE = 6f;
    /** Whom she screams at is marked this long: her claws bite {@link #MARKED_BONUS} times harder on them. */
    public static final int MARK_TICKS = 600;
    public static final float MARKED_BONUS = 1.5f;
    /** Gold is her bane; anything else barely touches a spirit. */
    public static final float GOLD_MULTIPLIER = 3f, OTHER_MULTIPLIER = 0.5f;
    /** After a scream she fades and comes back this long later, near her mark. */
    public static final int VANISH_TICKS = 40;

    // --- Chitter ------------------------------------------------------------------------------------------------------------
    public static final double CHITTER_HEALTH = 22, CHITTER_DAMAGE = 4, CHITTER_SPEED = 0.32;
    /** The pounce: from {@link #POUNCE_MIN}..{@link #POUNCE_MAX} blocks. */
    public static final double POUNCE_MIN = 3, POUNCE_MAX = 7;
    public static final int POUNCE_COOLDOWN = 70;
    public static final float POUNCE_DAMAGE = 5f;
    /** Fire burns the thing in them out twice as fast. */
    public static final float FIRE_MULTIPLIER = 2f;
    /** A screech calls every Chitter this close onto its prey. */
    public static final double SCREECH_CALL = 16;
}
