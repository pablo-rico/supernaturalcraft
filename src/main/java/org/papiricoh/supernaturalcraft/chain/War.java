package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

/**
 * Open war between the Pit and the Host (pure rules; the state lives in {@link ChainSavedData}). Every {@link #INTERVAL} days
 * the sides may declare war ({@link #CHANCE}); it lasts {@link #MIN_DAYS}–{@link #MAX_DAYS} days, in which skirmishes break out
 * {@link #SKIRMISH_MULTIPLIER} times as often and every victory across the sides scores. The side ahead at the end takes a
 * sector of the front and its Lord a level; the other is shaken ({@link #LOSER_DESTABILIZED} days).
 */
public final class War {
    public static final int INTERVAL = 14, MIN_DAYS = 3, MAX_DAYS = 5, SKIRMISH_MULTIPLIER = 3, LOSER_DESTABILIZED = 2;
    public static final double CHANCE = 0.6;
    /** A skirmish between the sides breaks out on a given day with this chance (times {@link #SKIRMISH_MULTIPLIER} at war). */
    public static final double CLASH_CHANCE = 0.25;
    /**
     * Points, for the war's score, prestige and the front ({@link Front#push}): a skirmish won, a duel of champions, a named
     * officer of the other side killed by a hunter sworn to yours.
     */
    public static final int SKIRMISH = 2, CHAMPION = 3, HUNTER_KILL = 1;
    /** A Lord who has been wronged (an officer killed by the other side) names a champion; the duel is due within this many days. */
    public static final int VOW_DAYS = 3;

    private War() {
    }

    public static boolean rolls(GenSeed.Rng rng, long day, long lastWarEnd) {
        return day - lastWarEnd >= INTERVAL && rng.chance(CHANCE);
    }

    public static int days(GenSeed.Rng rng) {
        return rng.range(MIN_DAYS, MAX_DAYS);
    }

    /** The winner by score, or null on a draw. */
    public static ChainFaction winner(int demonScore, int angelScore) {
        return demonScore == angelScore ? null : demonScore > angelScore ? ChainFaction.DEMON : ChainFaction.ANGEL;
    }

}
