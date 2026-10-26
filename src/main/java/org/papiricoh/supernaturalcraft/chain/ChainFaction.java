package org.papiricoh.supernaturalcraft.chain;

/**
 * The two chains of command (pure). Fallen angels are not a faction of their own: they are the {@link Branch#FALLEN} branch of
 * the angelic chain, and serve on the demonic side.
 */
public enum ChainFaction {
    DEMON("demon"),
    ANGEL("angel");

    private final String id;

    ChainFaction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ChainFaction byId(String id) {
        for (ChainFaction f : values()) if (f.id.equals(id)) return f;
        return DEMON;
    }

    /** The branch an officer serves in. Demons are always {@code LOYAL}; an angel who Falls switches to {@code FALLEN}. */
    public enum Branch {
        LOYAL,
        FALLEN
    }
}
