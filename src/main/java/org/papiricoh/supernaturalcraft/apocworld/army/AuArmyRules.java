package org.papiricoh.supernaturalcraft.apocworld.army;

/**
 * The numbers of Michael's army in the Apocalypse World (v0.41), pure. His soldiers are angels: only an angel blade truly bites
 * them ({@code AngelRules.MUNDANE_SHARE} of anything else), and an angel blade's killing blow smites them. They patrol the
 * burned land in small squads, hold the checkpoints on its roads and guard the prisoner camps.
 */
public final class AuArmyRules {

    private AuArmyRules() {
    }

    public static final float HEALTH = 44, DAMAGE = 8, ARMOR = 6;
    /** The smite: a palm on the face, close in, now and then. */
    public static final float SMITE_DAMAGE = 9, SMITE_REACH = 2.6f;
    public static final int SMITE_COOLDOWN = 120, SMITE_WINDUP = AuSoldierAssets.HIT_TICKS.getOrDefault("smite", 10);
    /** A guard keeps within this of its post, and walks back to it when it has no quarry. */
    public static final double POST_LEASH = 10, POST_HOME = 2.5;

    // --- patrols ---------------------------------------------------------------------------------------------------------------
    /** How often a hunter in the burned world may draw a patrol (ticks), the chance each time, and its size. */
    public static final int PATROL_EVERY = 400;
    public static final float PATROL_CHANCE = 0.4f;
    public static final int PATROL_MIN = 2, PATROL_MAX = 3;
    /** No new patrol while this many soldiers are already within {@link #PATROL_COUNT_RANGE} of the hunter. */
    public static final int PATROL_CAP = 4;
    public static final double PATROL_COUNT_RANGE = 48, PATROL_MIN_DIST = 26, PATROL_MAX_DIST = 40;

    /** How many soldiers a patrol draws, from a roll 0..1. */
    public static int patrolSize(double roll) {
        int span = PATROL_MAX - PATROL_MIN + 1;
        return PATROL_MIN + Math.min(span - 1, (int) Math.floor(Math.max(0, roll) * span));
    }

    /** Whether a patrol comes, with {@code near} soldiers already round the hunter and a roll 0..1. */
    public static boolean patrolComes(int near, double roll) {
        return near < PATROL_CAP && roll < PATROL_CHANCE;
    }

    // --- the camps -------------------------------------------------------------------------------------------------------------
    /** A camp's guards count while they stand within this of its heart; its prisoners within this of a lock are that cage's. */
    public static final double CAMP_GUARD_RANGE = 40, CAGE_RANGE = 3.5, CAMP_RANGE = 24;
    /** How long a freed prisoner cheers before the little rift takes them (ticks). */
    public static final int CHEER_TICKS = 50;
    /** Hunters this close to a camp when its last cage opens share the freeing. */
    public static final double CREDIT_RANGE = 48;
}
