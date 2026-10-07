package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * "The floor is lava": which columns of a burning zone are spared as islands of safe ground. A few seeded islands
 * scattered over the zone (always one within reach of its centre), each a small disc. Pure.
 */
public final class LavaIslands {

    /** One island: offset from the zone's centre and its radius. */
    public record Island(double x, double z, double radius) {
    }

    private LavaIslands() {
    }

    /** The islands of a zone of {@code radius}: about one per 3 blocks of radius, at least two. */
    public static List<Island> islands(int radius, long seed) {
        List<Island> out = new ArrayList<>();
        int n = Math.max(2, radius / 3);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * (i + ArenaNoise.rand(seed, 71, i, 0) * 0.8) / n;
            double d = i == 0 ? radius * 0.25 : radius * (0.35 + ArenaNoise.rand(seed, 71, i, 1) * 0.5);
            double ir = 1.6 + ArenaNoise.rand(seed, 71, i, 2) * 0.9;
            out.add(new Island(Math.cos(a) * d, Math.sin(a) * d, ir));
        }
        return out;
    }

    /** Whether the column at ({@code dx}, {@code dz}) from the zone's centre is spared. */
    public static boolean spared(List<Island> islands, double dx, double dz) {
        for (Island is : islands) {
            if (Math.hypot(dx - is.x(), dz - is.z()) <= is.radius()) return true;
        }
        return false;
    }
}
