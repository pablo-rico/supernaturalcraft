package org.papiricoh.supernaturalcraft.entity.boss.michael.arena;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The three Heavens Michael fights in (pure, tested in JUnit), each a plan of {@link ArenaCell}s relative to the surface:
 * <ol start="0">
 *   <li>the Garden (Ash's Heaven): a lawn with white-barked cherry trees, flowers, a bench by a path and golden light;</li>
 *   <li>the War in Heaven: marble ruins, broken columns, holy fire and craters;</li>
 *   <li>the Throne Room: a cloud-white floor inlaid with gold and quartz, a ring of columns and an empty throne.</li>
 * </ol>
 * The arena is pinned once over the {@link #union} of every Heaven's cells and each change of Heaven rewrites those same
 * positions ({@link #blocksOf}): a cell a Heaven does not use becomes air above the surface and its own floor at or below it.
 * Every plan stays inside the arena (a margin from the wall), under {@link #BUDGET}, and keeps the centre clear.
 */
public final class HeavenLayouts {

    public static final int GARDEN = 0, WAR = 1, THRONE = 2, COUNT = 3;
    /** The most blocks one Heaven may change. */
    public static final int BUDGET = 9000;
    /** The most positions the three together may pin (each one remembered by the arena once). */
    public static final int UNION_BUDGET = 14000;
    /** Cells this close to the wall are left alone. */
    public static final int MARGIN = 2;
    /** Nothing tall is built this close to the centre (the altar, where he comes down). */
    public static final int CLEAR_CENTRE = 4;
    /** The highest and lowest a plan builds or digs, relative to the surface. */
    public static final int TOP = 9, BOTTOM = -2;

    public static final String AIR = "minecraft:air";

    private HeavenLayouts() {
    }

    /** One Heaven's plan. */
    public static List<ArenaCell> plan(int which, int radius, long seed) {
        return switch (which) {
            case GARDEN -> garden(radius, seed);
            case WAR -> warInHeaven(radius, seed);
            default -> throneRoom(radius, seed);
        };
    }

    /**
     * Every position any Heaven writes, once, highest first (what stands on a block goes before the block itself), with
     * an empty block: the arena pins these and {@link #blocksOf} fills them in.
     */
    public static List<ArenaCell> union(int radius, long seed) {
        Map<Long, ArenaCell> all = new LinkedHashMap<>();
        for (int w = 0; w < COUNT; w++) {
            for (ArenaCell c : plan(w, radius, seed)) all.putIfAbsent(key(c.dx(), c.dy(), c.dz()), new ArenaCell(c.dx(), c.dy(), c.dz(), ""));
        }
        List<ArenaCell> out = new ArrayList<>(all.values());
        out.sort(Comparator.comparingInt((ArenaCell c) -> -c.dy()).thenComparingInt(ArenaCell::dx).thenComparingInt(ArenaCell::dz));
        return out;
    }

    /** What Heaven {@code which} puts at each of {@code union}'s positions, in the same order. */
    public static List<String> blocksOf(int which, List<ArenaCell> union, int radius, long seed) {
        Map<Long, String> mine = new LinkedHashMap<>();
        for (ArenaCell c : plan(which, radius, seed)) mine.put(key(c.dx(), c.dy(), c.dz()), c.block());
        List<String> out = new ArrayList<>(union.size());
        for (ArenaCell c : union) {
            String b = mine.get(key(c.dx(), c.dy(), c.dz()));
            out.add(b != null ? b : filler(which, c.dx(), c.dy(), c.dz()));
        }
        return out;
    }

    /** What a Heaven leaves where it builds nothing: air above its floor, its floor and the ground under it below. */
    public static String filler(int which, int dx, int dy, int dz) {
        if (dy > 0) return AIR;
        if (dy < 0) return which == THRONE ? "minecraft:smooth_quartz" : which == WAR ? "minecraft:tuff" : "minecraft:dirt";
        return switch (which) {
            case GARDEN -> "minecraft:grass_block";
            case WAR -> "minecraft:calcite";
            default -> "minecraft:white_concrete";
        };
    }

    // --- the Garden ---------------------------------------------------------------------------------------------

    /** A lawn, a ring path lit with golden light, white-barked cherry trees, flowers, and a bench facing the centre. */
    public static List<ArenaCell> garden(int radius, long seed) {
        Random r = new Random(seed);
        Plan p = new Plan(radius);
        int ring = Math.max(CLEAR_CENTRE + 4, (int) Math.round(radius * 0.45));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!p.inside(dx, dz)) continue;
                double d = Math.sqrt(dx * dx + dz * dz);
                boolean path = Math.abs(d - ring) < 0.8 || (Math.abs(dx) <= 0 && d > ring && d < radius - MARGIN - 1);
                if (path) {
                    // Golden light set into the path every few steps.
                    boolean light = (Math.floorMod(dx * 7 + dz * 13, 11) == 0);
                    p.put(dx, 0, dz, light ? "minecraft:ochre_froglight" : "minecraft:dirt_path");
                    continue;
                }
                p.put(dx, 0, dz, "minecraft:grass_block");
                if (p.near(dx, dz, CLEAR_CENTRE)) continue;
                int roll = r.nextInt(20);
                if (roll < 3) p.put(dx, 1, dz, "minecraft:short_grass");
                else if (roll == 3) p.put(dx, 1, dz, "minecraft:lily_of_the_valley");
                else if (roll == 4) p.put(dx, 1, dz, "minecraft:white_tulip");
                else if (roll == 5) p.put(dx, 1, dz, "minecraft:oxeye_daisy");
                else if (roll == 6) p.put(dx, 1, dz, "minecraft:azure_bluet");
            }
        }
        // Cherry trees with white bark, in a loose ring outside the path.
        int trees = 5 + radius / 6;
        for (int i = 0; i < trees; i++) {
            double a = (i + r.nextDouble() * 0.5) * Math.PI * 2 / trees, d = ring + 4 + r.nextDouble() * Math.max(1, radius - MARGIN - ring - 8);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            if (!p.inside(cx, cz) || p.near(cx, cz, ring + 2)) continue;
            int h = 4 + r.nextInt(2);
            for (int y = 1; y <= h; y++) p.put(cx, y, cz, "minecraft:birch_log");
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = h - 1; y <= h + 2; y++) {
                        int reach = y > h ? 1 : 2;
                        if (Math.abs(dx) > reach || Math.abs(dz) > reach || (dx == 0 && dz == 0 && y <= h)) continue;
                        if (Math.abs(dx) == reach && Math.abs(dz) == reach && r.nextInt(3) > 0) continue;
                        if (p.inside(cx + dx, cz + dz)) p.put(cx + dx, y, cz + dz, "minecraft:cherry_leaves[persistent=true]");
                    }
                }
            }
            // Petals fallen round the trunk.
            for (int k = 0; k < 4; k++) {
                int px = cx + r.nextInt(5) - 2, pz = cz + r.nextInt(5) - 2;
                if (p.inside(px, pz) && !p.has(px, 1, pz)) p.put(px, 1, pz, "minecraft:pink_petals[flower_amount=" + (1 + r.nextInt(4)) + "]");
            }
        }
        // A bench on the path, facing the centre (the one Ash sat on).
        int bz = ring + 1;
        if (p.inside(0, bz + 1)) {
            for (int bx = -1; bx <= 1; bx++) {
                p.put(bx, 0, bz, "minecraft:dirt_path");
                p.put(bx, 1, bz, "minecraft:birch_stairs[facing=south]");
            }
            p.put(-2, 1, bz, "minecraft:birch_fence");
            p.put(2, 1, bz, "minecraft:birch_fence");
            p.put(-2, 2, bz, "minecraft:lantern");
            p.put(2, 2, bz, "minecraft:lantern");
        }
        // Lamps of golden light round the lawn.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + Math.PI / 8;
            int lx = (int) Math.round(Math.cos(a) * (ring - 2)), lz = (int) Math.round(Math.sin(a) * (ring - 2));
            if (!p.inside(lx, lz) || p.near(lx, lz, CLEAR_CENTRE)) continue;
            p.put(lx, 1, lz, "minecraft:birch_fence");
            p.put(lx, 2, lz, "minecraft:ochre_froglight");
        }
        return p.cells();
    }

    // --- the War in Heaven -----------------------------------------------------------------------------------------

    /** A field of marble broken by the war: ruined walls and columns, holy fire, craters with fire at the bottom. */
    public static List<ArenaCell> warInHeaven(int radius, long seed) {
        Random r = new Random(seed ^ 0x3A11E5L);
        Plan p = new Plan(radius);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!p.inside(dx, dz)) continue;
                int roll = r.nextInt(12);
                p.put(dx, 0, dz, roll < 4 ? "minecraft:calcite" : roll < 7 ? "minecraft:polished_diorite" : roll < 9 ? "minecraft:quartz_bricks"
                        : roll < 10 ? "minecraft:gravel" : roll < 11 ? "minecraft:tuff" : "minecraft:cracked_stone_bricks");
            }
        }
        // Craters: blown out of the marble, holy fire burning at the bottom.
        int craters = 4 + radius / 10;
        for (int i = 0; i < craters; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.35 + r.nextDouble() * 0.45);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int q = dx * dx + dz * dz;
                    if (q > 9 || !p.inside(cx + dx, cz + dz) || p.near(cx + dx, cz + dz, CLEAR_CENTRE)) continue;
                    if (q <= 2) {
                        p.put(cx + dx, 0, cz + dz, AIR);
                        p.put(cx + dx, -1, cz + dz, "minecraft:soul_fire");
                        p.put(cx + dx, -2, cz + dz, "minecraft:soul_soil");
                    } else if (q <= 5) {
                        p.put(cx + dx, 0, cz + dz, AIR);
                        p.put(cx + dx, -1, cz + dz, "minecraft:tuff");
                    } else {
                        p.put(cx + dx, 0, cz + dz, r.nextBoolean() ? "minecraft:tuff" : "minecraft:basalt");
                    }
                }
            }
        }
        // Broken columns: a quartz shaft of uneven height, its fallen piece lying beside it.
        int columns = 8 + radius / 4;
        for (int i = 0; i < columns; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.25 + r.nextDouble() * 0.65);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            if (!p.inside(cx, cz) || p.near(cx, cz, CLEAR_CENTRE + 1) || p.has(cx, -1, cz)) continue;
            p.put(cx, 1, cz, "minecraft:chiseled_quartz_block");
            int h = 2 + r.nextInt(5);
            for (int y = 2; y <= h; y++) p.put(cx, y, cz, "minecraft:quartz_pillar");
            if (h >= 5 && r.nextBoolean()) p.put(cx, h + 1, cz, "minecraft:gold_block");
            int fx = cx + (r.nextBoolean() ? 2 : -2), fz = cz + r.nextInt(3) - 1;
            for (int k = 0; k < 3; k++) {
                int x = fx, z = fz + k;
                if (p.inside(x, z) && !p.has(x, -1, z) && !p.near(x, z, CLEAR_CENTRE)) p.put(x, 1, z, "minecraft:quartz_pillar[axis=z]");
            }
        }
        // Ruined walls: short runs of quartz brick, broken off at different heights, with holy fire in the rubble.
        int walls = 4 + radius / 8;
        for (int i = 0; i < walls; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.4 + r.nextDouble() * 0.45);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            boolean alongX = r.nextBoolean();
            int len = 3 + r.nextInt(4);
            for (int k = 0; k < len; k++) {
                int dx = cx + (alongX ? k : 0), dz = cz + (alongX ? 0 : k);
                if (!p.inside(dx, dz) || p.near(dx, dz, CLEAR_CENTRE + 1) || p.has(dx, -1, dz)) continue;
                int h = 1 + r.nextInt(3);
                for (int y = 1; y <= h; y++) p.put(dx, y, dz, r.nextInt(4) == 0 ? "minecraft:cracked_stone_bricks" : "minecraft:quartz_bricks");
                if (r.nextInt(3) == 0) p.put(dx, h + 1, dz, "minecraft:smooth_quartz_slab");
            }
        }
        // Holy fire burning loose across the field.
        for (int i = 0; i < 10 + radius / 3; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.3 + r.nextDouble() * 0.6);
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (!p.inside(dx, dz) || p.near(dx, dz, CLEAR_CENTRE + 2) || p.has(dx, -1, dz) || p.has(dx, 1, dz)) continue;
            p.put(dx, 0, dz, "minecraft:soul_soil");
            p.put(dx, 1, dz, "minecraft:soul_fire");
        }
        return p.cells();
    }

    // --- the Throne Room -----------------------------------------------------------------------------------------

    /** A cloud-white floor inlaid with gold, a ring of quartz columns, clouds at the edge and an empty throne on a dais. */
    public static List<ArenaCell> throneRoom(int radius, long seed) {
        Random r = new Random(seed ^ 0x7A20E5L);
        Plan p = new Plan(radius);
        int colRing = radius - MARGIN - 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!p.inside(dx, dz)) continue;
                double d = Math.sqrt(dx * dx + dz * dz);
                String floor;
                if (Math.abs(d - colRing + 2) < 0.6 || Math.abs(d - colRing * 0.5) < 0.6) floor = "minecraft:gold_block";
                else if (Math.abs(dx) <= 1 && dz < 0) floor = (dx == 0) ? "minecraft:smooth_quartz" : "minecraft:gold_block"; // the aisle to the throne
                else if (Math.floorMod(dx + dz, 2) == 0) floor = "minecraft:white_concrete";
                else floor = "minecraft:smooth_quartz";
                if (Math.floorMod(dx * 5 + dz * 3, 17) == 0 && d > CLEAR_CENTRE) floor = "minecraft:pearlescent_froglight";
                p.put(dx, 0, dz, floor);
                // Clouds banked against the wall.
                if (d > radius - MARGIN - 2.2 && r.nextInt(3) > 0) {
                    p.put(dx, 1, dz, "minecraft:white_wool");
                    if (r.nextInt(3) == 0) p.put(dx, 2, dz, "minecraft:white_wool");
                }
            }
        }
        // Columns: a base, a quartz shaft and a capital of gold, round the room.
        int count = Math.max(8, (int) (colRing * Math.PI * 2 / 7));
        int h = 7;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            int cx = (int) Math.round(Math.cos(a) * colRing), cz = (int) Math.round(Math.sin(a) * colRing);
            if (!p.inside(cx, cz) || (Math.abs(cx) <= 3 && cz < 0)) continue; // keep the aisle open
            p.put(cx, 1, cz, "minecraft:chiseled_quartz_block");
            for (int y = 2; y < h; y++) p.put(cx, y, cz, "minecraft:quartz_pillar");
            p.put(cx, h, cz, "minecraft:gold_block");
            p.put(cx, h + 1, cz, "minecraft:chiseled_quartz_block");
        }
        // The dais and the empty throne, at the end of the aisle.
        int tz = -(radius - MARGIN - 5);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = tz - 2; dz <= tz + 2; dz++) {
                if (!p.inside(dx, dz)) continue;
                boolean edge = dz == tz + 2 || Math.abs(dx) == 3;
                p.put(dx, 1, dz, edge ? (dz == tz + 2 && Math.abs(dx) < 3 ? "minecraft:quartz_stairs[facing=north]" : "minecraft:smooth_quartz_slab")
                        : "minecraft:smooth_quartz");
            }
        }
        if (p.inside(0, tz - 1)) {
            p.put(0, 2, tz, "minecraft:quartz_stairs[facing=south]");
            p.put(-1, 2, tz, "minecraft:gold_block");
            p.put(1, 2, tz, "minecraft:gold_block");
            for (int y = 2; y <= 5; y++) p.put(0, y, tz - 1, y == 5 ? "minecraft:gold_block" : "minecraft:chiseled_quartz_block");
            p.put(-1, 3, tz - 1, "minecraft:quartz_pillar");
            p.put(1, 3, tz - 1, "minecraft:quartz_pillar");
            p.put(-1, 4, tz - 1, "minecraft:gold_block");
            p.put(1, 4, tz - 1, "minecraft:gold_block");
            p.put(0, 6, tz - 1, "minecraft:end_rod");
        }
        return p.cells();
    }

    private static long key(int dx, int dy, int dz) {
        return ((long) (dx & 0xFFFF) << 32) | ((long) (dy & 0xFF) << 16) | (dz & 0xFFFF);
    }

    /** A plan being drawn: one block per cell (the last put wins), inside the arena. */
    private static final class Plan {
        private final int radius;
        private final LinkedHashMap<Long, ArenaCell> cells = new LinkedHashMap<>();

        Plan(int radius) {
            this.radius = radius;
        }

        boolean inside(int dx, int dz) {
            int r = radius - MARGIN;
            return dx * dx + dz * dz <= r * r;
        }

        boolean near(int dx, int dz, int r) {
            return dx * dx + dz * dz <= r * r;
        }

        /** Whether something stands at (or, below the surface, was dug out of) this cell. */
        boolean has(int dx, int dy, int dz) {
            ArenaCell c = cells.get(key(dx, dy, dz));
            return c != null && (dy >= 0 || c.block().equals(AIR) || !c.block().isEmpty());
        }

        void put(int dx, int dy, int dz, String block) {
            cells.put(key(dx, dy, dz), new ArenaCell(dx, dy, dz, block));
        }

        List<ArenaCell> cells() {
            return new ArrayList<>(cells.values());
        }
    }
}
