package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shapes the layouts share: rings without diagonal steps (so bars and shelves join up), discs, angles. Pure. */
public final class ArenaShapes {

    /** One cell of a ring, with the angle (radians, 0..2π) it sits at. */
    public record RingCell(int x, int z, double angle) {
    }

    private ArenaShapes() {
    }

    /**
     * A circle of {@code radius} rasterised so that consecutive cells always share a side (no diagonal steps):
     * a wall of bars or shelves along it has no see-through corners. Cells are in angular order, without repeats.
     */
    public static List<RingCell> ring(double radius) {
        List<RingCell> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int steps = Math.max(32, (int) Math.ceil(2 * Math.PI * radius * 4));
        int px = Integer.MIN_VALUE, pz = 0;
        for (int i = 0; i <= steps; i++) {
            double a = 2 * Math.PI * i / steps;
            // A hair under the radius, so that a half-integer radius does not leave a one-cell spur at the axes.
            double rr = radius - 1e-7;
            int x = (int) Math.round(Math.cos(a) * rr), z = (int) Math.round(Math.sin(a) * rr);
            if (px != Integer.MIN_VALUE && x != px && z != pz) {
                // A diagonal step: fill the corner closer to the true circle.
                double e1 = Math.abs(Math.hypot(x, pz) - radius), e2 = Math.abs(Math.hypot(px, z) - radius);
                int cx = e1 <= e2 ? x : px, cz = e1 <= e2 ? pz : z;
                if (seen.add(pack(cx, cz))) out.add(new RingCell(cx, cz, angle(cx, cz)));
            }
            if (seen.add(pack(x, z))) out.add(new RingCell(x, z, angle(x, z)));
            px = x;
            pz = z;
        }
        return out;
    }

    /** The angle of a column round the centre, 0..2π. */
    public static double angle(double x, double z) {
        double a = Math.atan2(z, x);
        return a < 0 ? a + 2 * Math.PI : a;
    }

    /** The smallest difference between two angles, 0..π. */
    public static double angleDiff(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return d > Math.PI ? 2 * Math.PI - d : d;
    }

    /** Distance from point (x, z) to the segment (ax, az)-(bx, bz). */
    public static double segmentDistance(double x, double z, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az;
        double len2 = vx * vx + vz * vz;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * vx + (z - az) * vz) / len2));
        double qx = ax + vx * t - x, qz = az + vz * t - z;
        return Math.sqrt(qx * qx + qz * qz);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
