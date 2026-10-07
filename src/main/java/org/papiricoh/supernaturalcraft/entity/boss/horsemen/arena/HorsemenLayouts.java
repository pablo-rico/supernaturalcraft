package org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The ground each Horseman lays over his arena (pure, tested in JUnit): War's battlefield, Famine's dead farm,
 * Pestilence's toxic swamp and Death's living world. Every plan stays inside the arena (a margin from the wall) and under
 * {@link #BUDGET} blocks, and never builds on the centre where the fight begins. Seeded, so a rebuilt arena is the same.
 */
public final class HorsemenLayouts {

    /** The most blocks a plan may change: well inside the arena's snapshot. */
    public static final int BUDGET = 8000;
    /** Cells this close to the wall are left alone. */
    public static final int MARGIN = 2;
    /** Nothing tall is built this close to the centre. */
    public static final int CLEAR_CENTRE = 4;

    private HorsemenLayouts() {
    }

    // --- War: a battlefield -------------------------------------------------------------------------------------

    /** Trenches cut across the field, barbed fences, sandbags, shell craters and the odd burnt-out post. */
    public static List<ArenaCell> battlefield(int radius, long seed) {
        Random r = new Random(seed);
        Plan p = new Plan(radius);
        // Churned earth over the whole field.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!p.inside(dx, dz)) continue;
                int roll = r.nextInt(10);
                p.put(dx, 0, dz, roll < 3 ? "minecraft:coarse_dirt" : roll < 5 ? "minecraft:mud" : roll < 6 ? "minecraft:gravel"
                        : "minecraft:dirt");
            }
        }
        // Two trenches across the field, three blocks wide and two deep, duckboards on the floor.
        double turn = r.nextDouble() * Math.PI;
        for (int t = -1; t <= 1; t += 2) {
            int offset = t * (radius / 2);
            for (int s = -radius; s <= radius; s++) {
                for (int w = -1; w <= 1; w++) {
                    int dx = (int) Math.round(Math.cos(turn) * s - Math.sin(turn) * (offset + w));
                    int dz = (int) Math.round(Math.sin(turn) * s + Math.cos(turn) * (offset + w));
                    if (!p.inside(dx, dz) || Math.abs(s) % 9 == 4) continue; // gaps to cross
                    p.put(dx, 0, dz, "minecraft:air");
                    p.put(dx, -1, dz, "minecraft:air");
                    p.put(dx, -2, dz, w == 0 ? "minecraft:spruce_slab" : "minecraft:mud");
                }
                // Sandbags along the lip.
                for (int w : new int[]{-2, 2}) {
                    int dx = (int) Math.round(Math.cos(turn) * s - Math.sin(turn) * (offset + w));
                    int dz = (int) Math.round(Math.sin(turn) * s + Math.cos(turn) * (offset + w));
                    if (p.inside(dx, dz) && Math.abs(s) % 9 != 4 && r.nextInt(3) == 0) p.put(dx, 1, dz, "minecraft:mud_bricks");
                }
            }
        }
        // Barbed fences: rows of posts with wire (cobweb) between.
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.35 + r.nextDouble() * 0.45);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            boolean alongX = r.nextBoolean();
            for (int k = -3; k <= 3; k++) {
                int dx = cx + (alongX ? k : 0), dz = cz + (alongX ? 0 : k);
                if (!p.inside(dx, dz) || p.near(dx, dz, CLEAR_CENTRE) || p.has(dx, -1, dz)) continue;
                p.put(dx, 1, dz, k % 2 == 0 ? "minecraft:spruce_fence" : "minecraft:cobweb");
            }
        }
        // Craters, and a few burnt posts.
        for (int i = 0; i < 5; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.3 + r.nextDouble() * 0.55);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > 5 || !p.inside(cx + dx, cz + dz) || p.has(cx + dx, -1, cz + dz)) continue;
                    p.put(cx + dx, 0, cz + dz, dx * dx + dz * dz <= 1 ? "minecraft:air" : "minecraft:coarse_dirt");
                    if (dx * dx + dz * dz <= 1) p.put(cx + dx, -1, cz + dz, "minecraft:magma_block");
                }
            }
        }
        for (int i = 0; i < 8; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.3 + r.nextDouble() * 0.6);
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (!p.inside(dx, dz) || p.near(dx, dz, CLEAR_CENTRE) || p.has(dx, -1, dz) || p.has(dx, 1, dz)) continue;
            for (int y = 1; y <= 2 + r.nextInt(2); y++) p.put(dx, y, dz, "minecraft:stripped_dark_oak_log");
        }
        return p.cells();
    }

    /** Where War's four standards are planted: the four quarters of the field, halfway out. */
    public static List<int[]> standardSpots(int radius, long seed) {
        Random r = new Random(seed ^ 0x57A4DL);
        List<int[]> out = new ArrayList<>();
        double turn = r.nextDouble() * Math.PI / 2;
        for (int i = 0; i < 4; i++) {
            double a = turn + i * Math.PI / 2, d = radius * 0.55;
            out.add(new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)});
        }
        return out;
    }

    // --- Famine: dead farmland ------------------------------------------------------------------------------------

    /** Dry farmland in rows of withered wheat and rotting pumpkins, dead bushes, hay going bad, a scarecrow. */
    public static List<ArenaCell> deadFarm(int radius, long seed) {
        Random r = new Random(seed);
        Plan p = new Plan(radius);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!p.inside(dx, dz)) continue;
                boolean row = Math.floorMod(dx, 4) != 0 && !p.near(dx, dz, CLEAR_CENTRE);
                if (row) {
                    // Withered rows: dry farmland under straw-coloured wheat gone to seed, and gaps where it died.
                    int roll = r.nextInt(10);
                    if (roll < 3) {
                        p.put(dx, 0, dz, "minecraft:coarse_dirt");
                        p.put(dx, 1, dz, "minecraft:dead_bush");
                    } else {
                        p.put(dx, 0, dz, "minecraft:farmland[moisture=0]");
                        if (roll < 7) p.put(dx, 1, dz, "minecraft:wheat[age=7]");
                    }
                } else {
                    p.put(dx, 0, dz, r.nextInt(3) == 0 ? "minecraft:podzol" : "minecraft:coarse_dirt");
                    if (!p.near(dx, dz, CLEAR_CENTRE) && r.nextInt(9) == 0) p.put(dx, 1, dz, "minecraft:dead_bush");
                }
            }
        }
        // Rotting heaps: hay and carved pumpkins left out too long.
        for (int i = 0; i < 7; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.3 + r.nextDouble() * 0.55);
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (!p.inside(dx, dz) || p.near(dx, dz, CLEAR_CENTRE)) continue;
            p.put(dx, 0, dz, "minecraft:coarse_dirt");
            p.put(dx, 1, dz, r.nextBoolean() ? "minecraft:hay_block" : "minecraft:carved_pumpkin");
        }
        // A scarecrow.
        double a = r.nextDouble() * Math.PI * 2;
        int sx = (int) Math.round(Math.cos(a) * radius * 0.6), sz = (int) Math.round(Math.sin(a) * radius * 0.6);
        if (p.inside(sx, sz)) {
            p.put(sx, 0, sz, "minecraft:coarse_dirt");
            p.put(sx, 1, sz, "minecraft:spruce_fence");
            p.put(sx, 2, sz, "minecraft:spruce_fence");
            p.put(sx, 3, sz, "minecraft:carved_pumpkin");
        }
        return p.cells();
    }

    // --- Pestilence: a toxic swamp --------------------------------------------------------------------------------

    /** Mud and moss, stagnant pools with lily pads, sickly mushrooms and slime oozing up. */
    public static List<ArenaCell> toxicSwamp(int radius, long seed) {
        Random r = new Random(seed);
        Plan p = new Plan(radius);
        Set<Long> pools = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.35 + r.nextDouble() * 0.5);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            int size = 2 + r.nextInt(2);
            for (int dx = -size; dx <= size; dx++) {
                for (int dz = -size; dz <= size; dz++) {
                    if (dx * dx + dz * dz <= size * size && p.inside(cx + dx, cz + dz) && !p.near(cx + dx, cz + dz, CLEAR_CENTRE)) {
                        pools.add(key(cx + dx, cz + dz));
                    }
                }
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!p.inside(dx, dz)) continue;
                if (pools.contains(key(dx, dz))) {
                    p.put(dx, 0, dz, "minecraft:water");
                    p.put(dx, -1, dz, "minecraft:mud");
                    if (r.nextInt(6) == 0) p.put(dx, 1, dz, "minecraft:lily_pad");
                    continue;
                }
                int roll = r.nextInt(12);
                p.put(dx, 0, dz, roll < 5 ? "minecraft:mud" : roll < 8 ? "minecraft:moss_block" : roll < 10 ? "minecraft:podzol"
                        : roll < 11 ? "minecraft:mycelium" : "minecraft:slime_block");
                if (p.near(dx, dz, CLEAR_CENTRE)) continue;
                int deco = r.nextInt(14);
                if (roll >= 8 && roll < 11 && deco < 2) p.put(dx, 1, dz, deco == 0 ? "minecraft:brown_mushroom" : "minecraft:red_mushroom");
                else if (roll >= 5 && roll < 8 && deco == 2) p.put(dx, 1, dz, "minecraft:moss_carpet");
            }
        }
        // A few dead trees standing in the bog.
        for (int i = 0; i < 4; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.4 + r.nextDouble() * 0.45);
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (!p.inside(dx, dz) || pools.contains(key(dx, dz)) || p.near(dx, dz, CLEAR_CENTRE)) continue;
            int h = 3 + r.nextInt(3);
            for (int y = 1; y <= h; y++) p.put(dx, y, dz, "minecraft:mangrove_log");
            p.put(dx, h + 1, dz, "minecraft:mangrove_roots");
        }
        return p.cells();
    }

    /** Where antidote vials turn up: dry spots out in the swamp, never in a pool. */
    public static List<int[]> vialSpots(int radius, long seed) {
        Random r = new Random(seed ^ 0xA171D07EL);
        List<ArenaCell> plan = toxicSwamp(radius, seed);
        Set<Long> wet = new HashSet<>();
        for (ArenaCell c : plan) if (c.dy() == 0 && c.block().equals("minecraft:water")) wet.add(key(c.dx(), c.dz()));
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double a = (i + r.nextDouble() * 0.6) * Math.PI / 3, d = radius * (0.45 + r.nextDouble() * 0.35);
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (wet.contains(key(dx, dz))) continue;
            out.add(new int[]{dx, dz});
        }
        return out;
    }

    // --- Death: the living world (and, flipped by LimboPalette, the world of the dead) --------------------------

    /** Grass over the ground, flowers, a few oaks and a path of stones: life, so that it can be taken away. */
    public static List<ArenaCell> livingWorld(int radius, long seed) {
        Random r = new Random(seed);
        Plan p = new Plan(radius);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!p.inside(dx, dz)) continue;
                boolean path = Math.abs(dx) <= 1 && dz > CLEAR_CENTRE;
                p.put(dx, 0, dz, path ? "minecraft:cobblestone" : "minecraft:grass_block");
                if (path || p.near(dx, dz, CLEAR_CENTRE)) continue;
                int roll = r.nextInt(16);
                if (roll < 3) p.put(dx, 1, dz, "minecraft:short_grass");
                else if (roll == 3) p.put(dx, 1, dz, "minecraft:poppy");
                else if (roll == 4) p.put(dx, 1, dz, "minecraft:dandelion");
                else if (roll == 5) p.put(dx, 1, dz, "minecraft:cornflower");
            }
        }
        for (int i = 0; i < 5; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radius * (0.5 + r.nextDouble() * 0.3);
            int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
            if (!p.inside(cx, cz) || p.near(cx, cz, CLEAR_CENTRE + 2)) continue;
            int h = 4 + r.nextInt(2);
            for (int y = 1; y <= h; y++) p.put(cx, y, cz, "minecraft:oak_log");
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = h - 1; y <= h + 1; y++) {
                        int reach = y > h ? 1 : 2;
                        if (Math.abs(dx) > reach || Math.abs(dz) > reach || (dx == 0 && dz == 0 && y <= h)) continue;
                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                        if (p.inside(cx + dx, cz + dz)) p.put(cx + dx, y, cz + dz, "minecraft:oak_leaves[persistent=true]");
                    }
                }
            }
        }
        // Lanterns along the path.
        for (int dz = CLEAR_CENTRE + 3; dz < radius - MARGIN; dz += 5) {
            if (p.inside(2, dz)) p.put(2, 1, dz, "minecraft:lantern");
        }
        return p.cells();
    }

    private static long key(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xFFFFFFFFL);
    }

    /** A plan being drawn: one block per cell (the last put wins), inside the arena. */
    private static final class Plan {
        private final int radius;
        private final java.util.LinkedHashMap<Long, ArenaCell> cells = new java.util.LinkedHashMap<>();

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

        private static long key(int dx, int dy, int dz) {
            return ((long) (dx & 0xFFFF) << 32) | ((long) (dy & 0xFF) << 16) | (dz & 0xFFFF);
        }

        boolean has(int dx, int dy, int dz) {
            ArenaCell c = cells.get(key(dx, dy, dz));
            return c != null && (dy >= 0 || c.block().equals("minecraft:air"));
        }

        void put(int dx, int dy, int dz, String block) {
            cells.put(key(dx, dy, dz), new ArenaCell(dx, dy, dz, block));
        }

        List<ArenaCell> cells() {
            return new ArrayList<>(cells.values());
        }
    }
}
