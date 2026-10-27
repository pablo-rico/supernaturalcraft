package org.papiricoh.supernaturalcraft.chain.seat;

import org.papiricoh.supernaturalcraft.chain.ChainFaction;

import java.util.Objects;

/** Pure, seed-stable sites round the world's origin, like the Author's cabin ring: each faction at its {@link ChainFaction#seatOffset()} from the first. */
public final class SeatSite {
    public static final int MIN_DISTANCE = 3000, MAX_DISTANCE = 5000;

    /** Horizontal centre of the seat; height is chosen once by the server and persisted. */
    public record Site(int x, int z) {}

    private SeatSite() {}

    /** How many alternatives {@link #candidates} offers after the first. */
    public static final int ALTERNATIVES = 64;

    /**
     * The places a stronghold may stand, best first: {@link #of} and then, deterministically, its neighbours along the ring —
     * a few degrees either way and nearer or further out, never leaving {@code MIN..MAX}. The server keeps the first one on dry,
     * fairly level land ({@code SeatPlacement.site}).
     */
    public static java.util.List<Site> candidates(long seed, ChainFaction faction) {
        Site first = of(seed, faction);
        double angle = Math.atan2(first.z(), first.x()), radius = Math.hypot(first.x(), first.z());
        java.util.List<Site> out = new java.util.ArrayList<>();
        out.add(first);
        for (int k = 1; k <= ALTERNATIVES; k++) {
            int step = (k + 1) / 2, sign = k % 2 == 0 ? -1 : 1;
            double a = angle + sign * step * Math.toRadians(4);
            double r = Math.clamp(radius + ((k % 4) - 1.5) * 160 * ((step + 1) / 2), MIN_DISTANCE + 16, MAX_DISTANCE - 16);
            out.add(new Site((int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r)));
        }
        return out;
    }

    public static Site of(long seed, ChainFaction faction) {
        Objects.requireNonNull(faction);
        long hash = mix(seed ^ 0x53454154534CL);
        double angle = (hash >>> 11) * 0x1.0p-53 * Math.PI * 2;
        angle += faction.seatOffset();
        double radius = MIN_DISTANCE + 16 + (mix(hash) >>> 11) * 0x1.0p-53 * (MAX_DISTANCE - MIN_DISTANCE - 32);
        return new Site((int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius));
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
