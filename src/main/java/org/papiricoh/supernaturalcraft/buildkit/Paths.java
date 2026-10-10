package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paths that look walked rather than drawn (pure): a smooth Catmull-Rom curve through control points, a core of path blocks
 * fraying into coarse dirt, gravel, packed mud and moss at its edges, or a line of stepping stones; optional kerbs, lamp posts
 * at even spacing alternating sides, and planting along the borders. Paths follow a {@link Ground}: each cell replaces the
 * column's top block, and clears plants over it. Water and missing ground are skipped (bridge them with {@link #bridge}).
 */
public final class Paths {

    private Paths() {
    }

    /**
     * How a path is laid.
     *
     * @param width     full width in blocks (core plus fraying edges)
     * @param core      the middle (e.g. dirt path with a little coarse dirt)
     * @param edge      the fraying edge mix
     * @param stepping  a block for stepping stones instead of a continuous core (null: continuous)
     * @param kerb      a block replacing the ground just outside the edge (flush kerb), or null
     * @param lampPost  a fence or wall id for lamp posts, or null
     * @param lampEvery spacing of lamp posts along the path, in blocks
     * @param lampBase  the post's foot block
     * @param border    plants for the border strip (weighted: id, weight, …; tall plants by id), or null
     */
    public record Style(double width, Brush core, Brush edge, String stepping, Brush kerb, String lampPost, int lampEvery, String lampBase,
                        Object[] border) {
    }

    /** A country path: dirt path fraying into coarse dirt, gravel and moss, flowers and grass along it. */
    public static Style countryPath(double width, int seed) {
        return new Style(width,
                Palette.patches2(seed, 2, "minecraft:dirt_path", 8, "minecraft:coarse_dirt", 1),
                Palette.patches2(seed + 1, 1.5, "minecraft:coarse_dirt", 3, "minecraft:gravel", 1, "minecraft:packed_mud", 1,
                        "minecraft:moss_block", 1, "minecraft:dirt_path", 2),
                null, null, null, 0, null,
                new Object[]{"minecraft:short_grass", 8, "minecraft:fern", 2, "minecraft:oxeye_daisy", 1, "minecraft:azure_bluet", 1,
                        "minecraft:tall_grass", 2, "minecraft:cornflower", 1});
    }

    // --- curves ------------------------------------------------------------------------------------------------------------

    /** Points along a Catmull-Rom spline through {x, z} control points, about {@code step} blocks apart: {x, z, arcLength}. */
    public static List<double[]> spline(List<int[]> ctrl, double step) {
        List<double[]> out = new ArrayList<>();
        if (ctrl.size() < 2) throw new IllegalArgumentException("a path needs two points");
        double arc = 0;
        double[] last = null;
        for (int i = 0; i + 1 < ctrl.size(); i++) {
            int[] p0 = ctrl.get(Math.max(0, i - 1)), p1 = ctrl.get(i), p2 = ctrl.get(i + 1), p3 = ctrl.get(Math.min(ctrl.size() - 1, i + 2));
            double segLen = Math.hypot(p2[0] - p1[0], p2[1] - p1[1]);
            int n = Math.max(2, (int) Math.ceil(segLen / step));
            for (int k = 0; k < n; k++) {
                double t = k / (double) n;
                double x = cr(p0[0], p1[0], p2[0], p3[0], t), z = cr(p0[1], p1[1], p2[1], p3[1], t);
                if (last != null) arc += Math.hypot(x - last[0], z - last[1]);
                last = new double[]{x, z, arc};
                out.add(last);
            }
        }
        int[] e = ctrl.get(ctrl.size() - 1);
        arc += Math.hypot(e[0] - last[0], e[1] - last[1]);
        out.add(new double[]{e[0], e[1], arc});
        return out;
    }

    private static double cr(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    /** For every column within {@code reach} of the curve: {distance, arc length at the nearest point, side (±1)}. */
    public static Map<Long, double[]> near(List<double[]> pts, double reach) {
        Map<Long, double[]> out = new HashMap<>();
        for (int i = 0; i < pts.size(); i++) {
            double[] p = pts.get(i);
            double[] q = pts.get(Math.min(pts.size() - 1, i + 1)), o = pts.get(Math.max(0, i - 1));
            double tx = q[0] - o[0], tz = q[1] - o[1];
            int ir = (int) Math.ceil(reach) + 1;
            for (int x = (int) Math.floor(p[0]) - ir; x <= (int) Math.ceil(p[0]) + ir; x++) {
                for (int z = (int) Math.floor(p[1]) - ir; z <= (int) Math.ceil(p[1]) + ir; z++) {
                    double d = Math.hypot(x - p[0], z - p[1]);
                    if (d > reach) continue;
                    long k = Ground.col(x, z);
                    double[] cur = out.get(k);
                    if (cur == null || d < cur[0]) {
                        double side = Math.signum(tx * (z - p[1]) - tz * (x - p[0]));
                        out.put(k, new double[]{d, p[2], side == 0 ? 1 : side});
                    }
                }
            }
        }
        return out;
    }

    // --- laying ---------------------------------------------------------------------------------------------------------------

    /** Lays a path through control points {x, z} over the ground. Returns the columns it paved. */
    public static List<int[]> path(Canvas c, Ground g, List<int[]> through, Style s, int seed) {
        List<double[]> pts = spline(through, 0.3);
        double half = s.width() / 2;
        Map<Long, double[]> near = near(pts, half + 3);
        List<int[]> paved = new ArrayList<>();
        for (Map.Entry<Long, double[]> e : near.entrySet()) {
            int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
            if (!g.has(x, z) || g.water(x, z)) continue;
            double d = e.getValue()[0];
            int t = g.top(x, z);
            double fray = (Noise.value2(x, z, 2.5, seed) - 0.5) * 1.2;
            String cur = c.get(x, t, z);
            boolean ground = cur != null && Kinds.soil(cur) || cur != null && St.path(cur).equals("moss_block");
            if (!ground) continue;
            if (d <= half + fray) {
                String state;
                if (s.stepping() != null) {
                    double arc = e.getValue()[1];
                    boolean stone = Math.floorMod((int) Math.floor(arc), 2) == 0 && d < half - 0.2;
                    if (!stone) continue;
                    state = s.stepping();
                } else {
                    state = d <= half - 1 + fray * 0.5 ? s.core().at(x, t, z) : s.edge().at(x, t, z);
                }
                c.set(x, t, z, state);
                clearAbove(c, x, t, z);
                paved.add(new int[]{x, z});
            } else if (s.kerb() != null && d <= half + 1) {
                c.set(x, t, z, s.kerb().at(x, t, z));
                clearAbove(c, x, t, z);
            } else if (s.border() != null && d <= half + 2.6 && Noise.chance(x, 0, z, seed + 3, 0.55)) {
                Scatter.plant(c, x, t + 1, z, Scatter.pick(x, z, seed + 4, s.border()), seed);
            }
        }
        if (s.lampPost() != null && s.lampEvery() > 0) lamps(c, g, pts, s, seed);
        return paved;
    }

    private static void clearAbove(Canvas c, int x, int t, int z) {
        for (int y = t + 1; y <= t + 2; y++) {
            String a = c.get(x, y, z);
            if (a != null && (Kinds.soilPlant(a) || Kinds.passable(a) && !Kinds.air(a))) c.carve(x, y, z);
        }
    }

    private static void lamps(Canvas c, Ground g, List<double[]> pts, Style s, int seed) {
        double next = s.lampEvery() / 2.0;
        int side = 1;
        for (int i = 1; i < pts.size(); i++) {
            double[] p = pts.get(i);
            if (p[2] < next) continue;
            next += s.lampEvery();
            double[] o = pts.get(i - 1);
            double tx = p[0] - o[0], tz = p[1] - o[1], len = Math.hypot(tx, tz);
            if (len == 0) continue;
            double nx = -tz / len * side, nz = tx / len * side;
            double off = s.width() / 2 + 1.2;
            int x = (int) Math.round(p[0] + nx * off), z = (int) Math.round(p[1] + nz * off);
            side = -side;
            if (!g.has(x, z) || g.water(x, z)) continue;
            int t = g.top(x, z);
            if (!c.isAir(x, t + 1, z) && !Kinds.soilPlant(c.get(x, t + 1, z))) continue;
            Dir arm = Dir.toward((int) Math.round(-nx * 2), (int) Math.round(-nz * 2));
            for (int y = t + 1; y <= t + 5; y++) if (!c.isAir(x, y, z) && !Kinds.soilPlant(c.get(x, y, z)) && !Kinds.leaves(c.get(x, y, z))) arm = null;
            c.carve(x, t + 2, z);
            Lighting.lampPost(c, x, t + 1, z, 3, s.lampBase(), s.lampPost(), arm, false);
        }
    }

    /**
     * A footbridge from (x0, z0) to (x1, z1) (a straight line along X or Z), its deck ends at {@code y} (the floor row), rising
     * by one in the middle when long enough: plank deck with slab ramps, fence railings with posts at the ends carrying lanterns,
     * and an arch of upside-down stairs under the deck's edges.
     */
    public static void bridge(Canvas c, int x0, int z0, int x1, int z1, int y, int width, Wood w) {
        Dir along = Dir.toward(x1 - x0, z1 - z0);
        Dir across = along.cw();
        int len = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0)) + 1;
        int half = width / 2;
        for (int i = 0; i < len; i++) {
            int rise = len >= 7 && i >= 2 && i < len - 2 ? 1 : 0;
            boolean ramp = len >= 7 && (i == 1 || i == len - 2);
            for (int k = -half - 1; k <= half + 1; k++) {
                int x = x0 + along.dx * i + across.dx * k, z = z0 + along.dz * i + across.dz * k;
                boolean rail = Math.abs(k) == half + 1;
                int deckY = y + rise;
                if (ramp) c.set(x, y, z, rail ? w.planks() : w.slabTop());
                if (!ramp) c.set(x, deckY, z, rail ? St.axis(w.strippedLog(), along.axis()) : w.planks());
                if (ramp) c.set(x, y + 1, z, rail ? w.fence() : w.slabBottom());
                if (rail) c.set(x, (ramp ? y + 1 : deckY + 1), z, w.fence());
                if (!rail && !ramp) for (int h = 1; h <= 3; h++) c.setIfEmpty(x, deckY + h, z, St.AIR);
                if (!rail && rise == 1 && !ramp && Math.abs(k) == half) {
                    // The arch: an upside-down stair under each deck edge, its slope toward the bank.
                    Dir bank = i < len / 2 ? along.opposite() : along;
                    c.setIfAir(x, deckY - 1, z, w.stairs(bank.opposite(), true));
                }
            }
        }
        for (int i : new int[]{0, len - 1}) {
            for (int k : new int[]{-half - 1, half + 1}) {
                int x = x0 + along.dx * i + across.dx * k, z = z0 + along.dz * i + across.dz * k;
                c.set(x, y + 1, z, w.fence());
                c.set(x, y + 2, z, St.lantern(false));
            }
        }
    }
}
