package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Abaddon's hall (v0.21, pure, laid through {@code HorsemenGround} and given back by the arena): a floor of polished blackstone
 * bricks worn through to blackstone and gilded stone, the Knights' seal (chiseled blackstone round crying obsidian) at the
 * centre, chain posts round the hall and braziers (a wall post, netherrack, fire: no block entity) at the edge. In the Brand's
 * phase it is passed through {@link #branded}: the bricks crack and the braziers burn soul-blue. Seeded by the arena's centre,
 * so a rebuilt arena is the same; under {@link #BUDGET} blocks, a margin from the wall, nothing standing near the centre.
 */
public final class AbaddonGround {

    public static final int BUDGET = 8000, MARGIN = 2, CLEAR_CENTRE = 4;
    /** Where the braziers stand: this share of the radius out; how many. */
    public static final double BRAZIER_REACH = 0.82;
    public static final int BRAZIERS = 8, CHAIN_POSTS = 6;

    private AbaddonGround() {
    }

    public static List<ArenaCell> plan(int radius, long seed) {
        Random r = new Random(seed);
        Map<Long, ArenaCell> cells = new LinkedHashMap<>();
        int in = radius - MARGIN;
        for (int dx = -in; dx <= in; dx++) {
            for (int dz = -in; dz <= in; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > in * in) continue;
                String floor;
                if (d2 <= 2) floor = "minecraft:crying_obsidian";
                else if (d2 <= 9) floor = "minecraft:chiseled_polished_blackstone";
                else {
                    int roll = r.nextInt(20);
                    floor = roll < 11 ? "minecraft:polished_blackstone_bricks" : roll < 15 ? "minecraft:cracked_polished_blackstone_bricks"
                            : roll < 19 ? "minecraft:blackstone" : "minecraft:gilded_blackstone";
                }
                put(cells, dx, 0, dz, floor);
                // Clear the grass and flowers off the hall's floor.
                put(cells, dx, 1, dz, "minecraft:air");
            }
        }
        // The seal's ring of polished blackstone halfway out.
        int ring = Math.max(CLEAR_CENTRE + 2, radius / 2);
        for (int a = 0; a < 360; a += 3) {
            int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * ring), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * ring);
            if (dx * dx + dz * dz <= in * in) put(cells, dx, 0, dz, "minecraft:polished_blackstone");
        }
        // Braziers at the edge.
        double base = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < BRAZIERS; i++) {
            double a = base + i * Math.PI * 2 / BRAZIERS;
            int dx = (int) Math.round(Math.cos(a) * radius * BRAZIER_REACH), dz = (int) Math.round(Math.sin(a) * radius * BRAZIER_REACH);
            if (dx * dx + dz * dz > in * in || dx * dx + dz * dz < CLEAR_CENTRE * CLEAR_CENTRE) continue;
            put(cells, dx, 1, dz, "minecraft:polished_blackstone_wall");
            put(cells, dx, 2, dz, "minecraft:netherrack");
            put(cells, dx, 3, dz, "minecraft:fire");
        }
        // Chain posts between them: a blackstone foot and three links of chain.
        for (int i = 0; i < CHAIN_POSTS; i++) {
            double a = base + (i + 0.5) * Math.PI * 2 / CHAIN_POSTS;
            double d = radius * (0.55 + r.nextDouble() * 0.2);
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (dx * dx + dz * dz > in * in || dx * dx + dz * dz < CLEAR_CENTRE * CLEAR_CENTRE) continue;
            put(cells, dx, 1, dz, "minecraft:polished_blackstone");
            for (int y = 2; y <= 4; y++) put(cells, dx, y, dz, "minecraft:chain[axis=y]");
        }
        List<ArenaCell> out = new ArrayList<>(cells.values());
        return out.size() > BUDGET ? out.subList(0, BUDGET) : out;
    }

    private static void put(Map<Long, ArenaCell> cells, int dx, int dy, int dz, String block) {
        cells.put(((long) (dx & 0xFFFF) << 32) | ((long) (dz & 0xFFFF) << 16) | (dy & 0xFFFF), new ArenaCell(dx, dy, dz, block));
    }

    /** The Brand's phase: bricks crack, the braziers' fire turns to soul fire (on soul soil). */
    public static String branded(String block) {
        return switch (block) {
            case "minecraft:polished_blackstone_bricks" -> "minecraft:cracked_polished_blackstone_bricks";
            case "minecraft:netherrack" -> "minecraft:soul_soil";
            case "minecraft:fire" -> "minecraft:soul_fire";
            default -> block;
        };
    }
}
