package org.papiricoh.supernaturalcraft.weapon.curse;

/** Pure numbers for hungry weapons, kept apart so they can be unit-tested. */
public final class CurseLevels {

    /** Souls needed to reach level 1..5. */
    public static final int[] THRESHOLDS = {0, 20, 60, 150, 400};
    public static final int MAX_SATIATION = 100;
    public static final int STARVING = 20, SATED = 80;
    /** Satiation lost every this many ticks while carried. */
    public static final int HUNGER_INTERVAL = 600;
    public static final int FEED_PER_KILL = 25;

    private CurseLevels() {
    }

    public static int levelFor(int souls) {
        int level = 1;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (souls >= THRESHOLDS[i]) level = i + 1;
        }
        return level;
    }

    /** Souls a kill is worth: a quarter of the victim's maximum health, at least one. */
    public static int soulsFor(float victimMaxHealth) {
        return Math.max(1, Math.round(victimMaxHealth / 4f));
    }

    public static boolean starving(int satiation) {
        return satiation < STARVING;
    }

    public static boolean sated(int satiation) {
        return satiation > SATED;
    }
}
