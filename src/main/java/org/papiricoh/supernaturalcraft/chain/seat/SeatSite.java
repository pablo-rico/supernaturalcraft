package org.papiricoh.supernaturalcraft.chain.seat;

import org.papiricoh.supernaturalcraft.chain.ChainFaction;

import java.util.Objects;

/** Pure, seed-stable sites on opposite sides of the world's origin, like the Author's cabin ring. */
public final class SeatSite {
    public static final int MIN_DISTANCE = 3000, MAX_DISTANCE = 5000;

    /** Horizontal centre of the seat; height is chosen once by the server and persisted. */
    public record Site(int x, int z) {}

    private SeatSite() {}

    public static Site of(long seed, ChainFaction faction) {
        Objects.requireNonNull(faction);
        long hash = mix(seed ^ 0x53454154534CL);
        double angle = (hash >>> 11) * 0x1.0p-53 * Math.PI * 2;
        if (faction == ChainFaction.ANGEL) angle += Math.PI;
        double radius = MIN_DISTANCE + 16 + (mix(hash) >>> 11) * 0x1.0p-53 * (MAX_DISTANCE - MIN_DISTANCE - 32);
        return new Site((int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius));
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
