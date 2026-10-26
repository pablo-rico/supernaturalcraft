package org.papiricoh.supernaturalcraft.chain;

import java.util.UUID;

/**
 * A hunter's post in their side's chain (pure). A sworn angel or demon may hold a real post, which counts against the rank's
 * cap like any officer's. How high they may rise is set by their faction rank ({@link #cap}): I Soldier, II Captain,
 * III Lieutenant, IV Commander. The throne is never reached by promotion, only by beating the Lord in a duel, and a hunter
 * Lord holds the title, not the stronghold. A sponsor whose regard is at least {@link #SPONSOR} takes them in; the Lord
 * promotes them at {@link #PROMOTION}.
 */
public final class Membership {
    public static final int SPONSOR = 60, PROMOTION = 50;

    private Membership() {
    }

    /** The highest rank a hunter of this faction rank may hold short of the throne, or null below rank I. */
    public static ChainRank cap(int allegianceRank) {
        return switch (Math.min(4, allegianceRank)) {
            case 1 -> ChainRank.SOLDIER;
            case 2 -> ChainRank.CAPTAIN;
            case 3 -> ChainRank.LIEUTENANT;
            case 4 -> ChainRank.COMMANDER;
            default -> null;
        };
    }

    /** Whether {@code rank} is within reach of a hunter capped at {@code cap} (the throne never is). */
    public static boolean within(ChainRank rank, ChainRank cap) {
        return cap != null && rank != ChainRank.LORD && rank.ordinal() <= cap.ordinal();
    }

    /**
     * Swears a hunter in at {@code rank}, or the highest free rank below it. @return the post, or null if they already hold one
     * or every rank up to it is full.
     */
    public static Officer join(Hierarchy h, UUID player, String name, ChainRank rank, long nameSeed) {
        if (h.heldBy(player) != null || rank == ChainRank.LORD) return null;
        for (int r = rank.ordinal(); r >= 0; r--) {
            Officer o = h.recruit(ChainRank.values()[r], Officer.NONE, nameSeed, Officer.PLAYER_TYPE, 0);
            if (o != null) {
                o.player = player;
                o.playerName = name;
                o.log("sworn");
                return o;
            }
        }
        return null;
    }

    /** One step up, if the rank above is free and within the hunter's reach. */
    public static boolean promote(Hierarchy h, Officer post, ChainRank cap) {
        return post.held() && within(post.rank.up(), cap) && post.rank.up() != post.rank && h.promote(post);
    }

    /** Two officers trade posts (a duel for a post won); the tree is mended. */
    public static void swap(Hierarchy h, Officer a, Officer b) {
        ChainRank ra = a.rank;
        a.rank = b.rank;
        b.rank = ra;
        a.log("promoted:" + a.rank.name());
        b.log("demoted:" + b.rank.name());
        if (a.rank == ChainRank.LORD) a.superior = Officer.NONE;
        if (b.rank == ChainRank.LORD) b.superior = Officer.NONE;
        h.reassignOrphans();
    }
}
