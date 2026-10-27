package org.papiricoh.supernaturalcraft.chain;

import java.util.ArrayList;
import java.util.List;

/**
 * The chains of command (pure). Fallen angels are not a faction of their own: they are the {@link Branch#FALLEN} branch of
 * the angelic chain, and serve on the demonic side.
 *
 * <p>Saved by {@link #id()} and by ordinal (the front's owners): append only. Everything that differs between factions asks
 * the faction (its {@link Side}, whether it {@link #talks()} or can be {@link #joinable() joined}, where its seat stands)
 * instead of comparing against DEMON or ANGEL, so a new faction is mostly a new value here plus its content (see CLAUDE.md,
 * "La cadena de mando: cambiarla o ampliarla").
 */
public enum ChainFaction {
    DEMON("demon", Side.HELL, true, 0),
    ANGEL("angel", Side.HEAVEN, true, Math.PI),
    /** v0.28: the Leviathans of SucroCorp. Hostile to everyone; never talk, never take a hunter in. */
    LEVIATHAN("leviathan", Side.NONE, false, Math.PI / 2);

    /** Which sworn players count as its own: a demon player serves Hell's chain, an angel Heaven's; nobody serves NONE. */
    public enum Side {
        HELL,
        HEAVEN,
        NONE
    }

    private final String id;
    private final Side side;
    private final boolean talks;
    private final double seatOffset;

    ChainFaction(String id, Side side, boolean talks, double seatOffset) {
        this.id = id;
        this.side = side;
        this.talks = talks;
        this.seatOffset = seatOffset;
    }

    public String id() {
        return id;
    }

    public Side side() {
        return side;
    }

    /** Whether its officers speak with hunters (conversations, parleys, messages). */
    public boolean talks() {
        return talks;
    }

    /** Whether a sworn player can hold a post in it. */
    public boolean joinable() {
        return side != Side.NONE;
    }

    /** Its seat's angle from the demons' seat, round the world's origin (radians); the two first seats never move. */
    public double seatOffset() {
        return seatOffset;
    }

    /** Whether its ranks go by titles of its own ({@code chain.supernaturalcraft.rank.<faction>.<rank>}): SucroCorp's corporate ones. */
    public boolean ownTitles() {
        return this == LEVIATHAN;
    }

    /**
     * Whether the chain dies with its founder (the boss on its first throne): no interregnum, no heir, nobody new in its ranks,
     * and its ground shared out among the rest. The Leviathans are nothing without Dick Roman.
     */
    public boolean fallsWithFounder() {
        return this == LEVIATHAN;
    }

    /** Whether it fields a fallen branch beside its loyal one. */
    public boolean hasFallen() {
        return this == ANGEL;
    }

    /** Every chain is at odds with every other: the factions this one fights. */
    public List<ChainFaction> rivals() {
        List<ChainFaction> out = new ArrayList<>();
        for (ChainFaction f : values()) if (f != this) out.add(f);
        return out;
    }

    public static boolean hostile(ChainFaction a, ChainFaction b) {
        return a != b;
    }

    /** The faction a sworn player of {@code side} serves, or null. */
    public static ChainFaction ofSide(Side side) {
        for (ChainFaction f : values()) if (f.side == side && side != Side.NONE) return f;
        return null;
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
