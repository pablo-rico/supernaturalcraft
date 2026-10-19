package org.papiricoh.supernaturalcraft.mark;

import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;

/**
 * The Mark of Cain's numbers (v0.21, pure, tested in JUnit). The Mark's <b>Thirst</b> (0-1) rests for {@link #GRACE_TICKS}
 * after each kill, then climbs {@link #RISE_PER_MINUTE} a minute; every kill slakes it by the souls the victim was worth
 * ({@link CurseLevels#soulsFor}, the cursed weapons' measure) at {@link #RELIEF_PER_SOUL} each, between {@link #MIN_RELIEF} and
 * {@link #MAX_RELIEF}. Cain's honey takes {@link #HONEY_RELIEF} off.
 * <ul>
 *   <li>Healing is cut by half the Thirst ({@link #healFactor}).</li>
 *   <li>From {@link #BLOODLUST} the bearer is in bloodlust: melee blows land {@link #BLOODLUST_MELEE} harder, whispers come, and a
 *   demon's Corruption drains and stops refilling (the Knight of Hell's old bloodlust, now read from the Thirst).</li>
 *   <li>At {@link #FULL} the Mark feeds on its bearer ({@link #FEED_HURT} health every {@link #FEED_INTERVAL} seconds) and a bearer
 *   who dies that is not a demon rises as one ({@link #fallsOnDeath}: a human, or an angel whose grace burns out under it), a
 *   Crossroads Demon; the Thirst is left at {@link #AFTER_FALL}.</li>
 * </ul>
 * The single meter: a Knight of Hell (rank III and up) always bears the Mark, so there is no other bloodlust timer.
 */
public final class MarkRules {

    /** Five minutes of rest after a kill. */
    public static final int GRACE_TICKS = 5 * 60 * 20;
    /** Thirst gained each minute past the grace. */
    public static final float RISE_PER_MINUTE = 0.01f;
    public static final float BLOODLUST = 0.5f, FULL = 1f;
    /** Melee blows of a bearer in bloodlust. */
    public static final float BLOODLUST_MELEE = 1.2f;
    /** What a kill slakes: per soul, and its bounds. */
    public static final float RELIEF_PER_SOUL = 0.01f, MIN_RELIEF = 0.02f, MAX_RELIEF = 0.3f;
    /** What a spoonful of Cain's honey slakes. */
    public static final float HONEY_RELIEF = 0.25f;
    /** At full Thirst, the Mark feeds on its bearer: this much health every this many seconds. */
    public static final float FEED_HURT = 1f;
    public static final int FEED_INTERVAL = 10;
    /** Where the Thirst is left after the Mark has made its bearer a demon. */
    public static final float AFTER_FALL = 0.5f;
    /** The Mark throbs (and the bearer's HUD is told) each time the Thirst climbs past a multiple of this. */
    public static final float PULSE_STEP = 0.1f;
    /**
     * The Thirst a Mark taken again comes back with, once it has been taken off (P6 {@code unmark}; retaken at Cain's grave):
     * "it remembers the way" — straight into bloodlust.
     */
    public static final float RETAKEN_THIRST = 0.5f;
    /** First Blade blows in a bearer's hand (instead of a Knight's ×1.5 of old). */
    public static final float FIRST_BLADE = 3f;

    private MarkRules() {
    }

    /** The Thirst a Mark starts with: none the first time, {@link #RETAKEN_THIRST} if it was ever taken off. */
    public static float retakenThirst(boolean everRemoved) {
        return everRemoved ? RETAKEN_THIRST : 0f;
    }

    public static float clamp(float thirst) {
        return Float.isNaN(thirst) ? 0f : Math.max(0f, Math.min(FULL, thirst));
    }

    /**
     * The Thirst after {@code seconds} more seconds, {@code ticksSinceKill} ticks after the last kill (at the end of those
     * seconds): unchanged within the grace, then rising.
     */
    public static float rise(float thirst, long ticksSinceKill, int seconds) {
        if (ticksSinceKill <= GRACE_TICKS || seconds <= 0) return clamp(thirst);
        long past = Math.min(seconds * 20L, ticksSinceKill - GRACE_TICKS);
        return clamp(thirst + RISE_PER_MINUTE * past / 1200f);
    }

    /** What a kill of a victim with {@code victimMaxHealth} slakes. */
    public static float relief(float victimMaxHealth) {
        float r = CurseLevels.soulsFor(victimMaxHealth) * RELIEF_PER_SOUL;
        return Math.max(MIN_RELIEF, Math.min(MAX_RELIEF, r));
    }

    /** The Thirst after such a kill. */
    public static float afterKill(float thirst, float victimMaxHealth) {
        return clamp(thirst - relief(victimMaxHealth));
    }

    /** What healing a bearer keeps. */
    public static float healFactor(float thirst) {
        return 1f - 0.5f * clamp(thirst);
    }

    public static boolean bloodlust(float thirst) {
        return thirst >= BLOODLUST;
    }

    public static boolean full(float thirst) {
        return thirst >= FULL - 1e-4f;
    }

    /** Melee blows of a bearer at {@code thirst}. */
    public static float meleeFactor(float thirst) {
        return bloodlust(thirst) ? BLOODLUST_MELEE : 1f;
    }

    /** Whether going from {@code before} to {@code after} climbs past a {@link #PULSE_STEP}. */
    public static boolean pulses(float before, float after) {
        return after > before && Math.floor(after / PULSE_STEP + 1e-4) > Math.floor(before / PULSE_STEP + 1e-4);
    }

    /** Whether bloodlust sets in going from {@code before} to {@code after}. */
    public static boolean bloodlustStarts(float before, float after) {
        return !bloodlust(before) && bloodlust(after);
    }

    /** Whether a bearer of {@code faction} who dies at {@code thirst} rises a demon (a demon has nothing left to lose). */
    public static boolean fallsOnDeath(Faction faction, float thirst) {
        return full(thirst) && faction != Faction.DEMON;
    }

    /** Whether a Knight of Hell (and up) of old must be given the Mark (the migration: they always bear it now). */
    public static boolean owedTheMark(Faction faction, int rank, boolean marked) {
        return !marked && faction == Faction.DEMON && rank >= 3;
    }
}
