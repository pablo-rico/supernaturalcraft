package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Where Lilith's arena raises its headstones: a loose ring around the altar, far enough apart that
 * each one shelters its own hunter. Pure geometry.
 */
public final class LilithHeadstones {

    public static final double RADIUS = 11;
    public static final double MIN_APART = 4;

    public record Spot(int dx, int dz) {
    }

    private LilithHeadstones() {
    }

    /** {@code count} spots, evenly spread with a little jitter from {@code seed}, starting at {@code turn} radians. */
    public static List<Spot> ring(int count, long seed, double turn) {
        Random r = new Random(seed);
        List<Spot> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double a = turn + Math.PI * 2 * i / count + (r.nextDouble() - 0.5) * (Math.PI / count) * 0.6;
            double d = RADIUS + (r.nextDouble() - 0.5) * 1.6;
            Spot s = new Spot((int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d));
            boolean clash = out.stream().anyMatch(o -> Math.hypot(o.dx - s.dx, o.dz - s.dz) < MIN_APART);
            if (!clash) out.add(s);
        }
        return out;
    }
}
