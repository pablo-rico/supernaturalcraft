package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The Blank Page eating itself: over the last chapter the paper is erased from its edges inward, in a ragged seeded
 * front, until only a disc of {@link #SAFE_RADIUS} round the centre is left. Each column of the page has the moment
 * (a fraction of the erosion, in (0, 1]) at which it goes; the columns inside the safe disc never do. Pure.
 */
public final class BlankPageErosion {

    /** Radius of the disc of page that is never erased. */
    public static final int SAFE_RADIUS = 7;
    /** Ticks into the chapter before the page starts to go, and how long it takes to reach the safe disc. */
    public static final int DELAY = 300, DURATION = 4800;
    /** How deep an erased column is carved: deep enough that whoever falls through is rescued. */
    public static final int DEPTH = StormLayout.PIT_DEPTH;
    private static final int SALT = 0xB1A;

    /** A column of the page and the moment it is erased. */
    public record Column(int dx, int dz, double at) {
    }

    private BlankPageErosion() {
    }

    /** How far the erosion has gone {@code chapterTicks} into the chapter (0 before it starts, 1 when it is done). */
    public static double progress(int chapterTicks) {
        return Math.max(0, Math.min(1, (chapterTicks - DELAY) / (double) DURATION));
    }

    /** Whether a column is inside the disc that is never erased. */
    public static boolean safe(int dx, int dz) {
        return dx * dx + dz * dz <= SAFE_RADIUS * SAFE_RADIUS;
    }

    /**
     * The moment (0..1) the column goes: the rim first, the edge of the safe disc last, with a ragged front (smooth
     * noise round the circle and across it). {@link Double#POSITIVE_INFINITY} for the safe disc.
     */
    public static double moment(int radius, long seed, int dx, int dz) {
        if (safe(dx, dz)) return Double.POSITIVE_INFINITY;
        double d = Math.sqrt(dx * dx + dz * dz);
        double depth = Math.max(0, Math.min(1, (radius - d) / Math.max(1.0, radius - SAFE_RADIUS - 0.5)));
        double a = ArenaShapes.angle(dx, dz);
        // Noise sampled on a circle (seamless round the page) plus a finer grain across it.
        double lobes = ArenaNoise.smooth(seed, SALT, Math.cos(a) * 9, Math.sin(a) * 9, 3.0) - 0.5;
        double grain = ArenaNoise.smooth(seed, SALT + 1, dx, dz, 2.5) - 0.5;
        double t = depth * 0.82 + lobes * 0.36 + grain * 0.14 + 0.06;
        return Math.max(0.001, Math.min(1, t));
    }

    /** Every erodible column of a page of {@code radius}, in the order it is erased. */
    public static List<Column> order(int radius, long seed) {
        List<Column> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || safe(dx, dz)) continue;
                out.add(new Column(dx, dz, moment(radius, seed, dx, dz)));
            }
        }
        out.sort(Comparator.comparingDouble(Column::at).thenComparingInt(Column::dx).thenComparingInt(Column::dz));
        return Collections.unmodifiableList(out);
    }

    /** How many columns of {@code order} are gone at {@code progress} (none before the erosion starts). */
    public static int due(List<Column> order, double progress) {
        if (progress <= 0) return 0;
        int lo = 0, hi = order.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (order.get(mid).at() <= progress) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /**
     * The radius within which the page is still whole once {@code eroded} columns of {@code order} are gone: the
     * nearest erased column, less one (never below the safe disc).
     */
    public static int intactRadius(List<Column> order, int eroded, int radius) {
        double nearest = radius + 1;
        for (int i = 0; i < eroded && i < order.size(); i++) {
            Column c = order.get(i);
            nearest = Math.min(nearest, Math.sqrt(c.dx() * c.dx() + c.dz() * c.dz()));
        }
        return Math.max(SAFE_RADIUS, Math.min(radius, (int) Math.floor(nearest) - 1));
    }
}
