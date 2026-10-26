package org.papiricoh.supernaturalcraft.entity.boss.dick;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The penthouse boardroom of Roman Enterprises (v0.25, pure, laid through {@code HorsemenGround} and given back by the arena):
 * a floor of polished deepslate tiles with a border of smooth quartz, a red runner to the boardroom table on the north side
 * (dark oak on fences, chairs down both sides), glass walls between deepslate pillars round the edge with sea lanterns set in
 * the floor before them, and a gold "R" inlaid at the centre. Every cleared floor cell is listed by {@link #FLOOR_AIR}, so the
 * Flood can cover exactly the open floor ({@link #FLOOD}). Seeded by nothing: always the same, under {@link #BUDGET} blocks.
 */
public final class BoardroomGround {

    public static final int BUDGET = 9000, MARGIN = 1, WALL_HEIGHT = 4;
    /** What a cleared cell just above the open floor holds (cave air: still air, but the Flood's cells are those). */
    public static final String FLOOR_AIR = "minecraft:cave_air";
    public static final String FLOOD = "supernaturalcraft:leviathan_goo[dried=false]";
    /** The table: on the north side (negative z), this long and this far from the centre. */
    public static final int TABLE_HALF = 4, TABLE_Z = -7;

    private BoardroomGround() {
    }

    public static List<ArenaCell> plan(int radius) {
        Map<Long, ArenaCell> cells = new LinkedHashMap<>();
        int in = radius - MARGIN;
        for (int dx = -in; dx <= in; dx++) {
            for (int dz = -in; dz <= in; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > in * in) continue;
                boolean border = d2 > (in - 1) * (in - 1);
                String floor = border ? "minecraft:smooth_quartz" : (Math.abs(dx) + Math.abs(dz)) % 7 == 0 ? "minecraft:polished_deepslate"
                        : "minecraft:deepslate_tiles";
                if (Math.abs(dx) <= 1 && dz > TABLE_Z && dz < -2) floor = "minecraft:red_wool";
                put(cells, dx, 0, dz, floor);
                put(cells, dx, 1, dz, FLOOR_AIR);
                for (int y = 2; y <= WALL_HEIGHT; y++) put(cells, dx, y, dz, "minecraft:air");
            }
        }
        logo(cells);
        table(cells);
        walls(cells, in);
        List<ArenaCell> out = new ArrayList<>(cells.values());
        return out.size() > BUDGET ? out.subList(0, BUDGET) : out;
    }

    /** A gold "R" (5×5) at the centre of the floor. */
    private static void logo(Map<Long, ArenaCell> cells) {
        String[] r = {"XXXX.", "X...X", "XXXX.", "X..X.", "X...X"};
        for (int row = 0; row < r.length; row++) {
            for (int col = 0; col < 5; col++) {
                if (r[row].charAt(col) == 'X') put(cells, col - 2, 0, row - 2, "minecraft:gold_block");
            }
        }
    }

    /** The boardroom table along x, dark oak slabs on fence legs, a chair facing it on each side of every second seat. */
    private static void table(Map<Long, ArenaCell> cells) {
        for (int dx = -TABLE_HALF; dx <= TABLE_HALF; dx++) {
            for (int dz = TABLE_Z - 1; dz <= TABLE_Z; dz++) {
                if (Math.abs(dx) == TABLE_HALF || dx == 0) put(cells, dx, 1, dz, "minecraft:dark_oak_fence");
                put(cells, dx, 2, dz, "minecraft:dark_oak_slab[type=bottom]");
            }
            if (dx % 2 == 0) {
                put(cells, dx, 1, TABLE_Z - 2, "minecraft:dark_oak_stairs[facing=north]");
                put(cells, dx, 1, TABLE_Z + 1, "minecraft:dark_oak_stairs[facing=south]");
            }
        }
    }

    /** Glass walls between deepslate pillars at the edge, a sea lantern set in the floor before every pillar. */
    private static void walls(Map<Long, ArenaCell> cells, int in) {
        for (int a = 0; a < 360; a += 2) {
            int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * in), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * in);
            boolean pillar = a % 30 == 0;
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                put(cells, dx, y, dz, pillar ? "minecraft:polished_deepslate" : y == WALL_HEIGHT ? "minecraft:deepslate_tile_slab[type=bottom]"
                        : "minecraft:black_stained_glass");
            }
            if (pillar) {
                int ix = (int) Math.round(Math.cos(Math.toRadians(a)) * (in - 2)), iz = (int) Math.round(Math.sin(Math.toRadians(a)) * (in - 2));
                put(cells, ix, 0, iz, "minecraft:sea_lantern");
            }
        }
    }

    private static void put(Map<Long, ArenaCell> cells, int dx, int dy, int dz, String block) {
        cells.put(((long) (dx & 0xFFFF) << 32) | ((long) (dz & 0xFFFF) << 16) | (dy & 0xFFFF), new ArenaCell(dx, dy, dz, block));
    }
}
