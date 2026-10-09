package org.papiricoh.supernaturalcraft.legacy.cases;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The little set piece a case's scenario puts on the ground (pure, JUnit-tested): vanilla block-state strings relative to the
 * site's centre at ground level (Y=0 = the first block above the ground). Written through an {@code ArenaController} so the
 * world is put back when the case closes, which is why nothing here has a block entity. Every piece fits within
 * {@link #RADIUS} blocks of the centre.
 */
public final class CaseLayout {

    /** No block of a piece lies farther than this from the centre (horizontally). */
    public static final int RADIUS = 8;

    public record Cell(int x, int y, int z, String state) {
    }

    private CaseLayout() {
    }

    /** The piece for {@code scenario} (empty for an unknown one). */
    public static List<Cell> piece(String scenario) {
        Map<Long, Cell> m = new LinkedHashMap<>();
        switch (scenario) {
            case "barn" -> barn(m);
            case "village" -> farmstead(m);
            case "graveyard" -> graveyard(m);
            case "haunted_house" -> house(m);
            case "night_woods" -> camp(m);
            case "mine" -> mine(m);
            default -> {
            }
        }
        return List.copyOf(new ArrayList<>(m.values()));
    }

    /** Where a scenario's creatures wait, relative to the centre (at Y=0). */
    public static int[][] lairs(String scenario) {
        return switch (scenario) {
            case "barn" -> new int[][]{{0, 0, -2}, {-1, 0, 1}, {1, 0, 2}, {0, 0, 0}, {-2, 0, -1}};
            case "mine" -> new int[][]{{0, 0, 1}, {-1, 0, 3}, {1, 0, 4}, {0, 0, 5}, {2, 0, 2}};
            default -> new int[][]{{0, 0, 0}, {2, 0, 1}, {-2, 0, -1}, {1, 0, -2}, {-1, 0, 2}};
        };
    }

    private static void barn(Map<Long, Cell> m) {
        String planks = "minecraft:spruce_planks", log = "minecraft:oak_log[axis=y]";
        for (int x = -3; x <= 3; x++) for (int z = -4; z <= 4; z++) set(m, x, -1, z, "minecraft:coarse_dirt");
        for (int y = 0; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -4; z <= 4; z++) {
                    boolean wall = Math.abs(x) == 3 || Math.abs(z) == 4;
                    if (!wall) continue;
                    boolean corner = Math.abs(x) == 3 && Math.abs(z) == 4;
                    boolean doorway = z == 4 && Math.abs(x) <= 1 && y <= 2;
                    boolean broken = (x * 3 + z * 5 + y * 7 & 7) == 0 && !corner;
                    if (doorway || broken) continue;
                    set(m, x, y, z, corner ? log : planks);
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -5; z <= 5; z++) {
                int y = 4 + Math.min(4 - Math.abs(x), 2);
                if ((x + z & 3) == 0) continue;
                set(m, x, Math.min(y, 6), z, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]");
            }
        }
        set(m, -2, 0, -3, "minecraft:hay_block[axis=y]");
        set(m, -2, 1, -3, "minecraft:hay_block[axis=x]");
        set(m, 2, 0, -3, "minecraft:hay_block[axis=z]");
        set(m, 2, 0, 2, "minecraft:cobweb");
        set(m, -2, 2, 3, "minecraft:cobweb");
        set(m, 0, 0, -1, "minecraft:red_carpet");
    }

    private static void farmstead(Map<Long, Cell> m) {
        // A well, a fenced pen, hay and a cart: the edge of a hamlet where people have gone missing.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(m, x, -1, z, x == 0 && z == 0 ? "minecraft:water" : "minecraft:cobblestone");
                if (x != 0 || z != 0) set(m, x, 0, z, "minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
            }
        }
        set(m, -1, 1, -1, "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        set(m, 1, 1, 1, "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        for (int x = 3; x <= 6; x++) {
            set(m, x, 0, -4, fence(x > 3, x < 6, false, true));
            set(m, x, 0, -1, fence(x > 3, x < 6, true, false));
        }
        for (int z = -3; z <= -2; z++) {
            set(m, 3, 0, z, fence(false, false, true, true));
            set(m, 6, 0, z, fence(false, false, true, true));
        }
        set(m, -4, 0, 3, "minecraft:hay_block[axis=y]");
        set(m, -5, 0, 3, "minecraft:hay_block[axis=x]");
        set(m, -4, 1, 3, "minecraft:hay_block[axis=y]");
        set(m, 4, 0, 4, "minecraft:spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
        set(m, -3, 0, -3, "minecraft:red_wool");
    }

    private static void graveyard(Map<Long, Cell> m) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                boolean edge = Math.abs(x) == 5 || Math.abs(z) == 5;
                if (edge && !(z == 5 && Math.abs(x) <= 1)) {
                    boolean ew = Math.abs(z) == 5;
                    set(m, x, 0, z, "minecraft:iron_bars[east=" + ew + ",north=" + !ew + ",south=" + !ew + ",waterlogged=false,west=" + ew + "]");
                }
                if (!edge && (x * 7 + z * 3 & 5) == 0) set(m, x, -1, z, "minecraft:coarse_dirt");
            }
        }
        for (int x = -3; x <= 3; x += 2) {
            for (int z = -3; z <= 1; z += 4) {
                set(m, x, 0, z, "minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
                set(m, x, -1, z + 1, "minecraft:podzol");
            }
        }
        set(m, 0, 0, -4, "minecraft:mossy_cobblestone");
        set(m, 0, 1, -4, "minecraft:mossy_cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
        set(m, 4, 0, 3, "minecraft:dead_bush");
        set(m, -4, 0, -2, "minecraft:dead_bush");
    }

    private static void house(Map<Long, Cell> m) {
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) set(m, x, -1, z, (x + z & 1) == 0 ? "minecraft:oak_planks" : "minecraft:dark_oak_planks");
        for (int y = 0; y <= 4; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    if (Math.abs(x) != 4 && Math.abs(z) != 4) continue;
                    boolean door = z == 4 && x == 0 && y <= 1;
                    boolean window = y == 2 && (x == 0 || z == 0) && !(Math.abs(x) == 4 && Math.abs(z) == 4);
                    int ruin = 4 - (Math.abs(x * 5 + z * 3) % 3);
                    if (door || window || y > ruin) continue;
                    set(m, x, y, z, (x * 3 + y + z & 3) == 0 ? "minecraft:mossy_cobblestone" : y == 0 ? "minecraft:cobblestone" : "minecraft:stripped_dark_oak_log[axis=y]");
                }
            }
        }
        for (int[] w : new int[][]{{-3, 2, -3}, {3, 3, -3}, {-3, 1, 3}, {2, 2, 0}}) set(m, w[0], w[1], w[2], "minecraft:cobweb");
        set(m, -2, 0, -2, "minecraft:spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
        set(m, 2, 0, -3, "minecraft:spruce_slab[type=top,waterlogged=false]");
        set(m, 2, 1, -3, "minecraft:white_candle[candles=2,lit=false,waterlogged=false]");
    }

    private static void camp(Map<Long, Cell> m) {
        // A camp in the woods, left in a hurry: a torn tent, logs round a dead fire, a lantern.
        for (int z = -2; z <= 1; z++) {
            set(m, -3, 0, z, "minecraft:white_wool");
            set(m, -1, 0, z, "minecraft:white_wool");
            set(m, -2, 1, z, z == -1 ? "minecraft:red_carpet" : "minecraft:white_wool");
        }
        set(m, 2, 0, 0, "minecraft:oak_log[axis=x]");
        set(m, 3, 0, 0, "minecraft:oak_log[axis=x]");
        set(m, 1, 0, 2, "minecraft:oak_log[axis=z]");
        set(m, 2, -1, 2, "minecraft:coarse_dirt");
        set(m, 2, 0, 2, "minecraft:coal_block");
        set(m, 3, 0, 3, "minecraft:lantern[hanging=false,waterlogged=false]");
        set(m, -4, 0, 3, "minecraft:red_carpet");
        set(m, 0, 0, -4, "minecraft:red_carpet");
    }

    private static void mine(Map<Long, Cell> m) {
        // A boarded mine mouth: a frame of logs and planks over a sloping shaft lined with rails.
        for (int z = 0; z <= 6; z++) {
            for (int x = -2; x <= 2; x++) {
                boolean side = Math.abs(x) == 2;
                for (int y = 0; y <= 3; y++) {
                    if (side || y == 3) set(m, x, y, z, side && z % 3 == 0 ? "minecraft:oak_log[axis=y]" : y == 3 ? "minecraft:oak_planks" : "minecraft:cobblestone");
                    else set(m, x, y, z, "minecraft:air");
                }
            }
            set(m, 0, -1, z, "minecraft:gravel");
            set(m, 0, 0, z, "minecraft:rail[shape=north_south,waterlogged=false]");
        }
        set(m, -1, 0, 0, "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        set(m, 1, 0, 0, "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        set(m, -1, 2, 1, "minecraft:lantern[hanging=true,waterlogged=false]");
        set(m, 1, 1, 5, "minecraft:cobweb");
    }

    private static String fence(boolean east, boolean west, boolean north, boolean south) {
        return "minecraft:oak_fence[east=" + east + ",north=" + north + ",south=" + south + ",waterlogged=false,west=" + west + "]";
    }

    private static void set(Map<Long, Cell> m, int x, int y, int z, String state) {
        long key = ((long) (x + 64) << 20) | ((long) (y + 64) << 10) | (z + 64);
        m.remove(key);
        m.put(key, new Cell(x, y, z, state));
    }
}
