package org.papiricoh.supernaturalcraft.chain;

/**
 * The four ranks of a chain of command (pure), lowest first. {@link #cap()} is how many officers of the rank a faction keeps
 * alive at most (1 / 3 / 6 / 10 = 20). Lords and Lieutenants hold a seat; Captains and Soldiers only exist as records until
 * an event materializes them.
 */
public enum ChainRank {
    SOLDIER(10),
    CAPTAIN(6),
    LIEUTENANT(3),
    LORD(1);

    private final int cap;

    ChainRank(int cap) {
        this.cap = cap;
    }

    public int cap() {
        return cap;
    }

    public boolean seated() {
        return this == LIEUTENANT || this == LORD;
    }

    /** The rank above (LORD stays LORD). */
    public ChainRank up() {
        return this == LORD ? LORD : values()[ordinal() + 1];
    }

    /** The rank below (SOLDIER stays SOLDIER). */
    public ChainRank down() {
        return this == SOLDIER ? SOLDIER : values()[ordinal() - 1];
    }

    public static int totalCap() {
        int n = 0;
        for (ChainRank r : values()) n += r.cap;
        return n;
    }
}
