package org.papiricoh.supernaturalcraft.chain;

/**
 * The five ranks of a chain of command (pure), lowest first. {@link #cap()} is how many officers of the rank a faction keeps
 * alive at most (1 / 2 / 3 / 6 / 10 = 22). The Lord and its Commanders hold court in their faction's stronghold; the rest
 * roam a territory of their own ({@link Territory}). Saved by name, so a rank may be inserted without breaking worlds.
 */
public enum ChainRank {
    SOLDIER(10),
    CAPTAIN(6),
    LIEUTENANT(3),
    COMMANDER(2),
    LORD(1);

    private final int cap;

    ChainRank(int cap) {
        this.cap = cap;
    }

    public int cap() {
        return cap;
    }

    public boolean seated() {
        return this == COMMANDER || this == LORD;
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

    /** The lang key of a rank's title in a faction ({@link ChainFaction#ownTitles}); {@code rank} is the enum name, any case. */
    public static String titleKey(String factionId, String rank) {
        ChainFaction f = ChainFaction.byId(factionId);
        return "chain.supernaturalcraft.rank." + (f.ownTitles() ? f.id() + "." : "") + rank.toLowerCase(java.util.Locale.ROOT);
    }

    public String titleKey(ChainFaction f) {
        return titleKey(f.id(), name());
    }
}
