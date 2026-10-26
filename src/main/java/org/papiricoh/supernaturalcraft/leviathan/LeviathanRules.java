package org.papiricoh.supernaturalcraft.leviathan;

/**
 * The numbers of the Leviathans (v0.25), pure (tested in JUnit). The Old Ones shrug off most harm and knit back together; borax
 * burns them and stops the knitting, and a blade through the neck at the right moment takes the head off. The head then crawls
 * back on its tongue: keep it away from the body ({@link #HEAD_LOST_RANGE} for {@link #HEAD_LOST_TICKS}), bag it or borax it
 * and the Leviathan is done for.
 */
public final class LeviathanRules {

    private LeviathanRules() {
    }

    /** The share of any ordinary harm a Leviathan takes (borax, a burning wound and the absolute weapons take it whole). */
    public static final float DAMAGE_TAKEN = 0.35f;
    /** Health it knits back each second, unless borax is burning in it. */
    public static final float REGEN_PER_SECOND = 1.0f;
    /** A sharp blow (a sword or an axe) at or under this share of its health takes its head off. */
    public static final float DECAPITATE_FRACTION = 0.5f;
    /** The head and its body rejoin within this distance (blocks). */
    public static final double REJOIN_RANGE = 1.2;
    /** The share of its health a rejoined Leviathan stands up with. */
    public static final float REJOIN_FRACTION = 0.5f;
    /** Farther than this from its body (blocks)... */
    public static final double HEAD_LOST_RANGE = 12;
    /** ...for this long (ticks), the head is lost and the Leviathan dies. */
    public static final int HEAD_LOST_TICKS = 100;
    /** How long a headless body waits for its head before it dies anyway (ticks). */
    public static final int HEADLESS_TICKS = 1200;
    /** Borax: how long it burns ({@code borax_burn}, ticks), and what it does to a Leviathan on contact. */
    public static final int BORAX_BURN_TICKS = 200;
    public static final float BORAX_DAMAGE = 6.0f;
    /** The burn's harm each second, to a Leviathan only. */
    public static final float BORAX_BURN_DAMAGE = 1.0f;

    // --- infiltration -----------------------------------------------------------------------------------------------------
    /** How often (ticks) the night is checked for a villager taken, and the chance each check, per hunter near a village. */
    public static final int INFILTRATION_PERIOD = 1200;
    public static final float INFILTRATION_CHANCE = 0.08f;
    /** The village around a hunter, its Leviathans counted in the same radius. */
    public static final double VILLAGE_RADIUS = 48;
    /** At most this many Leviathans in a village... */
    public static final int MAX_PER_VILLAGE = 3;
    /** ...and never the last villagers: at least this many must be left. */
    public static final int MIN_VILLAGERS_LEFT = 2;
    /** The share of takings that happen in plain sight of the hunter (the villager screams; the Leviathan comes out showing). */
    public static final float WITNESS_CHANCE = 0.25f;
    /** Within this distance (blocks) and in sight, a hunter sees a villager taken. */
    public static final double WITNESS_RANGE = 24;

    /** The milestones of the hunt (advancements, granted by code): one killed, one caught, a head taken off, a head finished. */
    public static final String SLAIN = "main/hello_cruel_world", RED_HANDED = "main/meet_the_new_boss",
            DECAPITATED = "main/slash_fiction", HEAD_FINISHED = "main/out_with_the_old";

    /** A disguised Leviathan hands out a free sample this often at most (ticks). */
    public static final int SAMPLE_COOLDOWN = 2400;

    /** Sedation from SucroCorp food (ticks). */
    public static final int SLAMMER_SEDATION = 3600, BURGER_SEDATION = 2400;

    /** The harm a Leviathan takes from {@code amount} ({@code whole}: borax, a burning wound, an absolute weapon). */
    public static float damageTaken(float amount, boolean whole) {
        return whole ? amount : amount * DAMAGE_TAKEN;
    }

    /** Whether a blow takes its head off: sharp, and at or under the threshold after the blow (or the blow would kill it). */
    public static boolean decapitates(boolean sharp, float healthAfter, float max) {
        return sharp && max > 0 && healthAfter <= max * DECAPITATE_FRACTION;
    }

    /** Health knit back this second. */
    public static float regen(boolean burning) {
        return burning ? 0 : REGEN_PER_SECOND;
    }

    /** Whether a head this far ({@code distSq}) from its body, away for {@code ticksAway}, is lost. */
    public static boolean headLost(double distSq, int ticksAway) {
        return distSq > HEAD_LOST_RANGE * HEAD_LOST_RANGE && ticksAway >= HEAD_LOST_TICKS;
    }

    /** Whether a head this far from its body rejoins it. */
    public static boolean rejoins(double distSq) {
        return distSq <= REJOIN_RANGE * REJOIN_RANGE;
    }

    /** Whether a taking that happens ({@link #infiltrates}) happens in plain sight, on {@code roll}. */
    public static boolean witnessed(float roll) {
        return roll < WITNESS_CHANCE;
    }

    /** Whether a village with these many Leviathans and villagers loses one more villager on a check rolling {@code roll}. */
    public static boolean infiltrates(int leviathans, int villagers, float roll) {
        return leviathans < MAX_PER_VILLAGE && villagers > MIN_VILLAGERS_LEFT && roll < INFILTRATION_CHANCE;
    }
}
