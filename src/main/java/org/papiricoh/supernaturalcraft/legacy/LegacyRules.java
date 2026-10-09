package org.papiricoh.supernaturalcraft.legacy;

/**
 * The Men of Letters' ranks (v0.17, "The Legacy"), pure: titles, what each needs and what each gives.
 *
 * <p>Rank 0 is "not a member". Henry's offer makes an Aspirant (1); the rest come from finished research: a count and a
 * spread over research kinds ({@code research.TopicKind}). Ranks never go down.
 */
public final class LegacyRules {

    public static final int MAX_RANK = 5;

    /** Title ids ({@code legacy.supernaturalcraft.rank.<id>}), index = rank. */
    public static final String[] TITLES = {"none", "aspirant", "initiate", "scholar", "master", "keeper"};

    /** Finished research a rank needs, index = rank. */
    public static final int[] RESEARCH_NEEDED = {0, 0, 5, 15, 35, 70};

    /** Distinct research kinds a rank needs, index = rank. */
    public static final int[] KINDS_NEEDED = {0, 0, 1, 3, 4, 5};

    /** Research desks a member can run at once, index = rank (the Aquarian Star adds one). */
    public static final int[] SLOTS = {0, 1, 1, 2, 3, 4};

    /** Days before Henry calls again after being turned away. */
    public static final int HENRY_RETURN_DAYS = 3;

    private LegacyRules() {
    }

    public static String title(int rank) {
        return TITLES[Math.max(0, Math.min(MAX_RANK, rank))];
    }

    /** Research desks at once; {@code star} = wears the Aquarian Star. */
    public static int slots(int rank, boolean star) {
        int r = Math.max(0, Math.min(MAX_RANK, rank));
        return r == 0 ? 0 : SLOTS[r] + (star ? 1 : 0);
    }

    /** The deepest research tier (I–V) open to a rank. */
    public static int maxTier(int rank) {
        return Math.max(0, Math.min(MAX_RANK, rank));
    }

    /**
     * The rank a member has earned.
     *
     * @param current the rank now (0 = not a member: research never makes one)
     * @param finished research finished in all
     * @param kinds distinct research kinds finished at least once
     */
    public static int earned(int current, int finished, int kinds) {
        if (current <= 0) return 0;
        int rank = current;
        while (rank < MAX_RANK && finished >= RESEARCH_NEEDED[rank + 1] && kinds >= KINDS_NEEDED[rank + 1]) rank++;
        return rank;
    }

    /** The advancement (impossible, granted by code) that marks a rank. */
    public static String advancement(int rank) {
        return "main/legacy_" + rank;
    }
}
