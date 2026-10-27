package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

/**
 * Open war between two chains (pure rules; the state lives in {@link ChainSavedData}). Every {@link #INTERVAL} days two
 * awake factions may go to war ({@link #CHANCE}, {@link #pair}); it lasts {@link #MIN_DAYS}–{@link #MAX_DAYS} days, in which skirmishes break out
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
    public static ChainFaction winner(ChainFaction a, int scoreA, ChainFaction b, int scoreB) {
        return scoreA == scoreB ? null : scoreA > scoreB ? a : b;
    }

    /** Two of the {@code awake} factions to go to war, or null if fewer than two are awake. */
    public static ChainFaction[] pair(GenSeed.Rng rng, java.util.List<ChainFaction> awake) {
        if (awake.size() < 2) return null;
        int i = rng.nextInt(awake.size()), j = rng.nextInt(awake.size() - 1);
        if (j >= i) j++;
        return new ChainFaction[]{awake.get(i), awake.get(j)};
    }

    /** Two of the {@code awake} factions that are at odds ({@code hostile}, v0.29: allies and neutral guilds keep the peace), or null. */
    public static ChainFaction[] pair(GenSeed.Rng rng, java.util.List<ChainFaction> awake,
                                      java.util.function.BiPredicate<ChainFaction, ChainFaction> hostile) {
        java.util.List<ChainFaction[]> pairs = new java.util.ArrayList<>();
        for (int i = 0; i < awake.size(); i++)
            for (int j = i + 1; j < awake.size(); j++)
                if (hostile.test(awake.get(i), awake.get(j))) pairs.add(new ChainFaction[]{awake.get(i), awake.get(j)});
        if (pairs.isEmpty()) return null;
        ChainFaction[] p = rng.pick(pairs);
        return rng.chance(.5) ? p : new ChainFaction[]{p[1], p[0]};
    }

}
