package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

/**
 * Where an officer can be found in the Overworld (pure, seed-stable). The ranks that hold court ({@link ChainRank#seated()})
 * are at their faction's seat. Soldiers roam a ring near the world's origin, where hunters live. Captains roam further out,
 * on the side of the world their seat is on, and Lieutenants further still, on the marches of their stronghold. The site is drawn from the officer and its rank, so a promotion moves it (a
 * captain moves toward the seat) and nothing else does.
 */
public final class Territory {

    /** How far from its site an officer roams, and how close a hunter must come for it to show itself. */
    public static final int ROAM = 48, NOTICE = 64;
    public static final int SOLDIER_MIN = 300, SOLDIER_MAX = 1200, CAPTAIN_MIN = 1200, CAPTAIN_MAX = 2400;
    public static final int LIEUTENANT_MIN = 2400, LIEUTENANT_MAX = 2900;
    /** How far either side of its seat's bearing a captain's territory may lie (radians). */
    public static final double CAPTAIN_SPREAD = Math.PI / 3;

    private Territory() {
    }

    public static SeatSite.Site of(long seed, Officer o) {
        return of(seed, o.faction, o.id, o.rank);
    }

    public static SeatSite.Site of(long seed, ChainFaction faction, int officerId, ChainRank rank) {
        return of(seed, faction, officerId, rank, SeatSite.of(seed, faction));
    }

    /** With the stronghold where the server actually raised it (moved off water by {@code SeatPlacement.site}). */
    public static SeatSite.Site of(long seed, Officer o, SeatSite.Site seat) {
        return of(seed, o.faction, o.id, o.rank, seat);
    }

    /**
     * With the front (v0.27): an officer below court rank roams only sectors its side holds ({@link Front#remap}); the fallen
     * hold Hell's. {@code front} null: as before.
     */
    public static SeatSite.Site of(long seed, Officer o, SeatSite.Site seat, Front front) {
        SeatSite.Site site = of(seed, o.faction, o.id, o.rank, seat);
        if (front == null || o.rank.seated()) return site;
        // A chain with no ground left (a beheaded one) holds out in its seat.
        if (front.held(o.camp()) == 0) return seat;
        double angle = Math.atan2(site.z(), site.x()), radius = Math.hypot(site.x(), site.z());
        double moved = front.remap(angle, o.camp());
        return moved == angle ? site : new SeatSite.Site((int) Math.round(Math.cos(moved) * radius), (int) Math.round(Math.sin(moved) * radius));
    }

    public static SeatSite.Site of(long seed, ChainFaction faction, int officerId, ChainRank rank, SeatSite.Site seat) {
        if (rank.seated()) return seat;
        GenSeed.Rng rng = new GenSeed.Rng(GenSeed.mix(GenSeed.mix(seed ^ 0x5445525249L) ^ GenSeed.mix(officerId * 31L + faction.ordinal())
                ^ GenSeed.mix(rank.ordinal() + 0x9E3779B97F4A7C15L)));
        double angle, radius;
        if (rank == ChainRank.CAPTAIN || rank == ChainRank.LIEUTENANT) {
            boolean captain = rank == ChainRank.CAPTAIN;
            angle = Math.atan2(seat.z(), seat.x()) + (rng.nextDouble() * 2 - 1) * CAPTAIN_SPREAD;
            radius = captain ? CAPTAIN_MIN + rng.nextDouble() * (CAPTAIN_MAX - CAPTAIN_MIN)
                    : LIEUTENANT_MIN + rng.nextDouble() * (LIEUTENANT_MAX - LIEUTENANT_MIN);
        } else {
            angle = rng.nextDouble() * Math.PI * 2;
            radius = SOLDIER_MIN + rng.nextDouble() * (SOLDIER_MAX - SOLDIER_MIN);
        }
        return new SeatSite.Site((int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius));
    }
}
