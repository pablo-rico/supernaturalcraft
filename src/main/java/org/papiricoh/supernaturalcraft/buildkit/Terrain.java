package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Landforms (pure): floating islands with soft hills, layered soil and a tapering rocky underside hung with dripstone; ponds and
 * streams whose water never spills (the surface sits at the lowest rim block, so every water cell is held by solid ground on
 * every side and below); waterfalls into basins; raised terraces behind retaining walls. Everything records itself in the
 * {@link Ground} it returns or is given, so paths, trees and plants follow it.
 */
public final class Terrain {

    private Terrain() {
    }

    /**
     * A floating island.
     *
     * @param cx      centre column
     * @param cz      centre column
     * @param radius  mean radius (the outline wanders ±20 %)
     * @param top     the surface block's y at the centre before hills
     * @param hills   amplitude of the rolling top, in blocks
     * @param depth   how far the underside hangs below the centre
     * @param surface the top block (grass and its patches); {@code rockTop} is used instead on steep or rim cells
     * @param soil    the 2–4 blocks under the surface
     * @param rock    the body, graded downward ({@link Palette#gradient} over depth works well)
     * @param drips   pointed dripstone and hanging roots under the underside
     */
    public record Island(int cx, int cz, int radius, int top, int hills, int depth, int seed, Brush surface, Brush rockTop,
                         Brush soil, Brush rock, boolean drips) {
    }

    /** The default look of a heavenly island: grass with moss and podzol patches, dirt, then stone grading into calcite. */
    public static Island island(int cx, int cz, int radius, int top, int hills, int depth, int seed) {
        Brush surface = Palette.patches2(seed + 1, 5, "minecraft:grass_block", 14, "minecraft:moss_block", 2, "minecraft:podzol", 1,
                "minecraft:coarse_dirt", 1);
        Brush rockTop = Palette.patches2(seed + 2, 3, "minecraft:mossy_cobblestone", 2, "minecraft:moss_block", 2, "minecraft:stone", 2,
                "minecraft:andesite", 1, "minecraft:coarse_dirt", 1);
        Brush soil = Palette.patches(seed + 3, 3, "minecraft:dirt", 6, "minecraft:coarse_dirt", 2, "minecraft:rooted_dirt", 1);
        Brush rock = Palette.gradientY(top - depth, top - 3, 0.5, seed + 4,
                Brush.of("minecraft:calcite"),
                Palette.patches(seed + 5, 3, "minecraft:calcite", 2, "minecraft:diorite", 2, "minecraft:dripstone_block", 1),
                Palette.patches(seed + 6, 3, "minecraft:stone", 4, "minecraft:andesite", 2, "minecraft:tuff", 1),
                Palette.patches(seed + 7, 3, "minecraft:stone", 3, "minecraft:andesite", 1, "minecraft:dirt", 1));
        return new Island(cx, cz, radius, top, hills, depth, seed, surface, rockTop, soil, rock, true);
    }

    /** Draws an island and returns its ground. */
    public static Ground island(Canvas c, Island s) {
        Ground g = new Ground();
        int r = s.radius();
        int reach = (int) Math.ceil(r * 1.25) + 1;
        Map<Long, Integer> tops = new HashMap<>();
        Map<Long, Double> inner = new HashMap<>();
        for (int x = s.cx() - reach; x <= s.cx() + reach; x++) {
            for (int z = s.cz() - reach; z <= s.cz() + reach; z++) {
                double dx = x - s.cx(), dz = z - s.cz();
                double dist = Math.sqrt(dx * dx + dz * dz);
                double rEff = r * (0.8 + 0.4 * Noise.fbm2(x, z, Math.max(4, r * 0.7), 2, s.seed()));
                double t = 1 - dist / rEff;
                if (t <= 0) continue;
                double hill = (Noise.fbm2(x, z, 11, 3, s.seed() + 1) - 0.45) * 2 * s.hills();
                double rise = smooth(Math.min(1, t / 0.45));
                int h = s.top() + (int) Math.round(hill * rise + t * s.hills() * 0.5) - (t < 0.07 ? 1 : 0);
                tops.put(Ground.col(x, z), h);
                inner.put(Ground.col(x, z), t);
            }
        }
        for (Map.Entry<Long, Integer> e : tops.entrySet()) {
            int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
            g.set(x, z, e.getValue(), inner.get(e.getKey()));
        }
        body(c, g, s);
        return g;
    }

    /**
     * An island of any outline: {@code shape}'s columns, innerness growing with the distance to its edge ({@code reach} blocks
     * in, it is 1). Heights, layers and underside as {@link #island(Canvas, Island)} (the spec's centre and radius are unused).
     * Use it for compound plots: the union of blobs and strips ({@link Mask#union}, {@link Mask#strip}).
     */
    public static Ground island(Canvas c, Island s, Mask shape, double reach) {
        Box b = shape.bounds();
        Map<Long, Integer> dist = new HashMap<>();
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                if (!shape.contains(x, z)) continue;
                boolean rim = false;
                for (Dir d : Dir.HORIZONTAL) rim |= !shape.contains(x + d.dx, z + d.dz);
                if (rim) {
                    dist.put(Ground.col(x, z), 1);
                    queue.add(new int[]{x, z});
                }
            }
        }
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int d0 = dist.get(Ground.col(p[0], p[1]));
            for (Dir d : Dir.HORIZONTAL) {
                int x = p[0] + d.dx, z = p[1] + d.dz;
                long k = Ground.col(x, z);
                if (!shape.contains(x, z) || dist.containsKey(k)) continue;
                dist.put(k, d0 + 1);
                queue.add(new int[]{x, z});
            }
        }
        Ground g = new Ground();
        for (Map.Entry<Long, Integer> e : dist.entrySet()) {
            int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
            double t = Math.min(1, (e.getValue() - 0.5) / reach);
            double hill = (Noise.fbm2(x, z, 11, 3, s.seed() + 1) - 0.45) * 2 * s.hills();
            int h = s.top() + (int) Math.round(hill * smooth(Math.min(1, t / 0.45))) - (t < 0.07 ? 1 : 0);
            g.set(x, z, h, t);
        }
        body(c, g, s);
        return g;
    }

    /** The layers under every column of {@code g}: surface, soil, rock, a tapering underside with dripstone and roots. */
    private static void body(Canvas c, Ground g, Island s) {
        for (int[] p : g.columns()) {
            int x = p[0], z = p[1];
            int h = g.top(x, z);
            double t = g.inner(x, z);
            int slope = g.slope(x, z);
            boolean rocky = slope >= 2 && slope < 99 || t < 0.06 && Noise.patch2(x, z, 3, s.seed() + 8) < 0.45;
            // A tapering underside: thin at the rim, deepest in the middle, with knuckles of rock hanging lower here and there.
            double knuckle = Math.max(0, Noise.ridged2(x, z, 7, 2, s.seed() + 13) - 0.65) / 0.35;
            double under = 1.5 + s.depth() * Math.pow(t, 1.35) * (0.75 + 0.5 * Noise.fbm2(x, z, 6, 2, s.seed() + 2))
                    + knuckle * 5 * Math.sqrt(t);
            int bottom = h - (int) Math.round(under);
            int soilDepth = (t < 0.15 ? 1 : 2) + Noise.pick(x, 0, z, s.seed() + 9, t < 0.15 ? 2 : 3);
            for (int y = h; y >= bottom; y--) {
                String state;
                if (y == h) state = (rocky ? s.rockTop() : s.surface()).at(x, y, z);
                else if (y > h - soilDepth) state = s.soil().at(x, y, z);
                else state = s.rock().at(x, y, z);
                c.set(x, y, z, state);
            }
            if (s.drips() && t > 0.25 && Noise.chance(x, 1, z, s.seed() + 10, 0.12)) {
                int len = 1 + Noise.pick(x, 2, z, s.seed() + 11, Math.min(4, 1 + (int) (t * 4)));
                c.set(x, bottom, z, "minecraft:dripstone_block");
                hangDripstone(c, x, bottom - 1, z, len);
            } else if (s.drips() && t < 0.3 && Noise.chance(x, 3, z, s.seed() + 12, 0.15)) {
                c.set(x, bottom, z, "minecraft:rooted_dirt");
                c.set(x, bottom - 1, z, "minecraft:hanging_roots[waterlogged=false]");
            }
        }
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /** Pointed dripstone hanging from the block above {@code y}, {@code len} long: base … frustum, tip. */
    public static void hangDripstone(Canvas c, int x, int y, int z, int len) {
        for (int k = 0; k < len; k++) {
            String th;
            int fromTip = len - 1 - k;
            if (fromTip == 0) th = "tip";
            else if (fromTip == 1) th = "frustum";
            else if (k == 0) th = "base";
            else th = "middle";
            c.set(x, y - k, z, St.dripstone(Dir.DOWN, th));
        }
    }

    // --- water ----------------------------------------------------------------------------------------------------------------

    /**
     * Fills a basin over {@code cells} (columns {x, z} that must all have ground, with ground all round them): the water's top
     * is at the lowest top among the rim (the ground columns touching the basin), the bed lies {@code depthAt} below it, the
     * columns above the water are cleared, the bed is {@code bed}, the rim cells beside the water are {@code bank}. Returns the
     * water level, or {@code Integer.MIN_VALUE} when the basin would spill (a cell or rim cell has no ground): nothing is drawn.
     */
    public static int basin(Canvas c, Ground g, List<int[]> cells, java.util.function.ToIntFunction<int[]> depthAt, Brush bed, Brush bank, int seed) {
        Set<Long> in = new HashSet<>();
        for (int[] p : cells) {
            if (!g.has(p[0], p[1])) return Integer.MIN_VALUE;
            in.add(Ground.col(p[0], p[1]));
        }
        Set<Long> rim = new HashSet<>();
        for (int[] p : cells) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long k = Ground.col(p[0] + dx, p[1] + dz);
                    if (in.contains(k)) continue;
                    if (!g.has(p[0] + dx, p[1] + dz) || g.edge(p[0] + dx, p[1] + dz)) return Integer.MIN_VALUE;
                    rim.add(k);
                }
            }
        }
        int level = Integer.MAX_VALUE;
        for (long k : rim) level = Math.min(level, g.top(Canvas.kx(k), Canvas.kz(k)));
        for (int[] p : cells) {
            int x = p[0], z = p[1];
            int old = g.top(x, z);
            int depth = Math.max(1, depthAt.applyAsInt(p));
            int bedY = level - depth;
            for (int y = Math.max(old, level) + 3; y > level; y--) c.carve(x, y, z);
            for (int y = level; y > bedY; y--) c.set(x, y, z, St.water());
            for (int y = Math.min(bedY, old); y <= bedY; y++) c.set(x, y, z, bed.at(x, y, z));
            if (depth >= 2 && Noise.chance(x, 0, z, seed + 1, 0.3)) c.set(x, bedY + 1, z, "minecraft:seagrass");
            g.setTop(x, z, bedY);
            g.markWater(x, z);
        }
        for (long k : rim) {
            int x = Canvas.kx(k), z = Canvas.kz(k);
            int t = g.top(x, z);
            boolean touches = false;
            for (Dir d : Dir.HORIZONTAL) touches |= in.contains(Ground.col(x + d.dx, z + d.dz));
            if (touches && bank != null) c.set(x, t, z, bank.at(x, t, z));
        }
        return level;
    }

    /** A rounded pond of about {@code radius} at (cx, cz), {@code depth} deep in the middle, with lily pads and reeds. */
    public static int pond(Canvas c, Ground g, int cx, int cz, double radius, int depth, int seed) {
        List<int[]> cells = new ArrayList<>();
        int ir = (int) Math.ceil(radius * 1.3);
        for (int x = cx - ir; x <= cx + ir; x++) {
            for (int z = cz - ir; z <= cz + ir; z++) {
                double d = Math.hypot(x - cx, z - cz) / (radius * (0.8 + 0.4 * Noise.value2(x, z, 3, seed)));
                if (d < 1) cells.add(new int[]{x, z});
            }
        }
        Brush bed = Palette.patches(seed + 2, 2, "minecraft:clay", 2, "minecraft:gravel", 2, "minecraft:sand", 1, "minecraft:mud", 2);
        Brush bank = Palette.patches2(seed + 3, 2, "minecraft:mud", 2, "minecraft:grass_block", 3, "minecraft:moss_block", 2,
                "minecraft:gravel", 1, "minecraft:coarse_dirt", 1);
        int level = basin(c, g, cells, p -> 1 + (int) Math.round((1 - Math.min(1, Math.hypot(p[0] - cx, p[1] - cz) / radius)) * (depth - 1)), bed, bank, seed);
        if (level == Integer.MIN_VALUE) return level;
        waterside(c, g, cells, level, seed);
        return level;
    }

    /** Lily pads on open water, sugar cane and tall grass on the banks that are level with the water. */
    public static void waterside(Canvas c, Ground g, List<int[]> cells, int level, int seed) {
        Set<Long> in = new HashSet<>();
        for (int[] p : cells) in.add(Ground.col(p[0], p[1]));
        for (int[] p : cells) {
            int x = p[0], z = p[1];
            if (Noise.chance(x, level, z, seed + 5, 0.1)) c.setIfAir(x, level + 1, z, "minecraft:lily_pad");
        }
        Set<Long> done = new HashSet<>();
        for (int[] p : cells) {
            for (Dir d : Dir.HORIZONTAL) {
                int x = p[0] + d.dx, z = p[1] + d.dz;
                long k = Ground.col(x, z);
                if (in.contains(k) || !done.add(k) || !g.has(x, z)) continue;
                int t = g.top(x, z);
                String s = c.get(x, t, z);
                if (s == null || !Kinds.soil(s) || !c.isAir(x, t + 1, z)) continue;
                double n = Noise.hash01(x, t, z, seed + 6);
                if (t == level && n < 0.3 && !St.path(s).equals("mud") && !St.path(s).equals("gravel")) {
                    int h = 2 + Noise.pick(x, 0, z, seed + 7, 2);
                    for (int k2 = 1; k2 <= h; k2++) c.set(x, t + k2, z, "minecraft:sugar_cane[age=0]");
                } else if (n < 0.55 && St.path(s).equals("grass_block") || St.path(s).equals("moss_block") && n < 0.55) {
                    c.setPair(x, t + 1, z, St.tall(n < 0.42 ? "minecraft:tall_grass" : "minecraft:large_fern"));
                }
            }
        }
    }

    /**
     * A waterfall off a ledge: a spring at ground column (x, z) — a source cut into the top block, walled on its other sides —
     * spilling toward {@code dir} over the drop into a pond dug at the foot ({@code basinRadius}). The pond must sit on ground
     * that is lower than the spring. Returns false (drawing nothing of the fall) if the pond would spill.
     */
    public static boolean waterfall(Canvas c, Ground g, int x, int z, Dir dir, double basinRadius, int seed, Brush ledge) {
        int top = g.top(x, z);
        int fx = x + dir.dx, fz = z + dir.dz;
        int bx = (int) Math.round(fx + dir.dx * (basinRadius - 1)), bz = (int) Math.round(fz + dir.dz * (basinRadius - 1));
        int level = pond(c, g, bx, bz, basinRadius, 2, seed);
        if (level == Integer.MIN_VALUE || level >= top) return false;
        if (!g.water(fx, fz)) return false;
        // The spring: a source in the top block, held on every side but the lip.
        c.set(x, top, z, St.water());
        c.set(x, top - 1, z, ledge.at(x, top - 1, z));
        for (Dir d : Dir.HORIZONTAL) {
            if (d == dir) continue;
            int sx = x + d.dx, sz = z + d.dz;
            if (!g.has(sx, sz) || g.top(sx, sz) < top || c.isAir(sx, top, sz)) c.set(sx, top, sz, ledge.at(sx, top, sz));
        }
        // Over the lip: a flowing cell, then the falling column down to the pond's surface.
        c.set(fx, top, fz, St.of("water", "level", "1"));
        for (int y = top - 1; y > level; y--) c.set(fx, y, fz, St.waterFalling());
        g.markWater(x, z);
        return true;
    }

    /**
     * A stream along a smooth path through control points {x, z}: a channel {@code width} wide whose water lies at the lowest
     * bank (so it may stand level like a long pond), with a gravel and clay bed. Returns its level or {@code Integer.MIN_VALUE}.
     */
    public static int stream(Canvas c, Ground g, List<int[]> through, double width, int seed) {
        List<double[]> pts = Paths.spline(through, 0.25);
        Map<Long, Double> dist = new HashMap<>();
        for (double[] p : pts) {
            int ir = (int) Math.ceil(width);
            for (int x = (int) Math.floor(p[0]) - ir; x <= (int) Math.ceil(p[0]) + ir; x++) {
                for (int z = (int) Math.floor(p[1]) - ir; z <= (int) Math.ceil(p[1]) + ir; z++) {
                    double d = Math.hypot(x - p[0], z - p[1]);
                    dist.merge(Ground.col(x, z), d, Math::min);
                }
            }
        }
        List<int[]> cells = new ArrayList<>();
        for (Map.Entry<Long, Double> e : dist.entrySet()) {
            int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
            if (e.getValue() <= width / 2 + (Noise.value2(x, z, 3, seed) - 0.5) * 0.6) cells.add(new int[]{x, z});
        }
        Brush bed = Palette.patches(seed + 2, 2, "minecraft:gravel", 3, "minecraft:clay", 1, "minecraft:sand", 1);
        Brush bank = Palette.patches2(seed + 3, 2, "minecraft:grass_block", 3, "minecraft:moss_block", 1, "minecraft:gravel", 1, "minecraft:mud", 1);
        int level = basin(c, g, cells, p -> 1 + (dist.get(Ground.col(p[0], p[1])) < width / 4 ? 1 : 0), bed, bank, seed);
        if (level != Integer.MIN_VALUE) waterside(c, g, cells, level, seed);
        return level;
    }

    // --- terraces ----------------------------------------------------------------------------------------------------------

    /**
     * Raises the ground over {@code area}'s columns to {@code newTop} behind a retaining wall: soil fill under a {@code surface}
     * top; the outline columns, where the ground outside is lower, become a wall of {@code wall} (weathered stone, say) capped
     * flush with {@code cap} and buttressed every 4 cells with {@code buttress} stairs (null: none).
     */
    public static void terrace(Canvas c, Ground g, Box area, int newTop, Brush surface, Brush soil, Brush wall, String cap, Family buttress) {
        terrace(c, g, Mask.of(area), newTop, surface, soil, wall, cap, buttress);
    }

    /** A set of columns with a bounding box: the outline of a terrace, a pond, a planting bed. */
    public interface Mask {
        boolean contains(int x, int z);

        Box bounds();

        static Mask of(Box b) {
            return new Mask() {
                public boolean contains(int x, int z) {
                    return b.containsColumn(x, z);
                }

                public Box bounds() {
                    return b;
                }
            };
        }

        /** Columns in any of {@code masks}. */
        static Mask union(Mask... masks) {
            Box b = masks[0].bounds();
            for (Mask m : masks) b = b.union(m.bounds());
            Box bb = b;
            return new Mask() {
                public boolean contains(int x, int z) {
                    for (Mask m : masks) if (m.contains(x, z)) return true;
                    return false;
                }

                public Box bounds() {
                    return bb;
                }
            };
        }

        /** A band about {@code width} wide along the segment (x0, z0) → (x1, z1), its edges wandering a little. */
        static Mask strip(int x0, int z0, int x1, int z1, double width, int seed) {
            int pad = (int) Math.ceil(width) + 2;
            Box b = Box.of(Math.min(x0, x1) - pad, 0, Math.min(z0, z1) - pad, Math.max(x0, x1) + pad, 0, Math.max(z0, z1) + pad);
            double len2 = (double) (x1 - x0) * (x1 - x0) + (double) (z1 - z0) * (z1 - z0);
            return new Mask() {
                public boolean contains(int x, int z) {
                    double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - x0) * (double) (x1 - x0) + (z - z0) * (double) (z1 - z0)) / len2));
                    double px = x0 + t * (x1 - x0), pz = z0 + t * (z1 - z0);
                    return Math.hypot(x - px, z - pz) < width / 2 + (Noise.value2(x, z, 4, seed) - 0.5) * width * 0.4;
                }

                public Box bounds() {
                    return b;
                }
            };
        }

        /** A noisy ellipse: radii {@code rx}, {@code rz}, its edge wandering by about {@code wobble} of the radius. */
        static Mask blob(int cx, int cz, double rx, double rz, double wobble, int seed) {
            Box b = new Box((int) Math.floor(cx - rx * (1 + wobble)) - 1, 0, (int) Math.floor(cz - rz * (1 + wobble)) - 1,
                    (int) Math.ceil(cx + rx * (1 + wobble)) + 1, 0, (int) Math.ceil(cz + rz * (1 + wobble)) + 1);
            return new Mask() {
                public boolean contains(int x, int z) {
                    double u = (x - cx) / rx, v = (z - cz) / rz;
                    double d = Math.sqrt(u * u + v * v);
                    return d < 1 + (Noise.value2(x, z, 3.5, seed) - 0.5) * 2 * wobble;
                }

                public Box bounds() {
                    return b;
                }
            };
        }
    }

    /**
     * Raises (or cuts) the ground inside {@code area} to {@code newTop} behind a retaining wall that follows its outline: soil
     * fill under a {@code surface} top; the outline columns (those with a neighbour outside) become a wall of {@code wall} capped
     * flush with {@code cap}; outside the wall, about every fourth cell, a buttress of {@code buttress} rises to a sloped stair
     * top (null: none).
     */
    public static void terrace(Canvas c, Ground g, Mask area, int newTop, Brush surface, Brush soil, Brush wall, String cap, Family buttress) {
        Box bb = area.bounds();
        List<int[]> rimCells = new ArrayList<>();
        for (int x = bb.x0(); x <= bb.x1(); x++) {
            for (int z = bb.z0(); z <= bb.z1(); z++) {
                if (!area.contains(x, z) || !g.has(x, z)) continue;
                int old = g.top(x, z);
                boolean rim = false;
                for (Dir d : Dir.HORIZONTAL) rim |= !area.contains(x + d.dx, z + d.dz);
                if (rim) rimCells.add(new int[]{x, z});
                int low = old;
                for (Dir d : Dir.HORIZONTAL) low = Math.min(low, g.topOr(x + d.dx, z + d.dz, old));
                for (int y = Math.min(old, low) - 1; y <= newTop; y++) {
                    String s;
                    if (rim) s = y == newTop ? cap : wall.at(x, y, z);
                    else s = y == newTop ? surface.at(x, y, z) : soil.at(x, y, z);
                    if (y > old || rim) c.set(x, y, z, s);
                }
                if (newTop < old) for (int y = newTop + 1; y <= old + 2; y++) c.carve(x, y, z);
                g.set(x, z, newTop, g.inner(x, z));
            }
        }
        if (buttress == null) return;
        for (int[] p : rimCells) {
            if (Math.floorMod(p[0] * 3 + p[1] * 5, 4) != 0) continue;
            for (Dir d : Dir.HORIZONTAL) {
                int ox = p[0] + d.dx, oz = p[1] + d.dz;
                if (area.contains(ox, oz) || !g.has(ox, oz)) continue;
                int base = g.top(ox, oz);
                if (base >= newTop - 1) continue;
                for (int y = base + 1; y < newTop; y++) {
                    String s = y == newTop - 1 ? buttress.stairs(d.opposite(), false) : buttress.block();
                    if (c.isAir(ox, y, oz) || Kinds.soilPlant(c.get(ox, y, oz))) c.set(ox, y, oz, s);
                }
                break;
            }
        }
    }

    /**
     * Levels the ground over {@code area} to surface {@code y}: higher columns are cut down (cleared above), lower ones filled with
     * {@code soil}; the top becomes {@code surface}. Within {@code blend} cells outside the area, columns are eased halfway toward
     * {@code y} so the pad does not end in a step.
     */
    public static void level(Canvas c, Ground g, Box area, int y, Brush surface, Brush soil, int blend) {
        Box outer = area.growXZ(blend);
        for (int x = outer.x0(); x <= outer.x1(); x++) {
            for (int z = outer.z0(); z <= outer.z1(); z++) {
                if (!g.has(x, z)) continue;
                int old = g.top(x, z);
                int target;
                if (area.containsColumn(x, z)) target = y;
                else {
                    int dist = Math.max(Math.max(area.x0() - x, x - area.x1()), Math.max(area.z0() - z, z - area.z1()));
                    target = (int) Math.round(y + (old - y) * (dist / (double) (blend + 1)));
                }
                if (target == old) continue;
                for (int yy = Math.min(old, target) - 1; yy <= Math.max(old, target) + 2; yy++) {
                    if (yy > target) c.carve(x, yy, z);
                    else if (yy == target) c.set(x, yy, z, surface.at(x, yy, z));
                    else if (yy > old - 1) c.set(x, yy, z, soil.at(x, yy, z));
                }
                g.set(x, z, target, g.inner(x, z));
            }
        }
    }
}
