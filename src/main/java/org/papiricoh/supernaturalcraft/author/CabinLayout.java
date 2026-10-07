package org.papiricoh.supernaturalcraft.author;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Author's cabin as a plan (pure, tested in JUnit): every block as a vanilla block-state string in local
 * coordinates. Local +Z is where the door looks (south before rotation), Y=0 is the floor, and the cabin is centred
 * on X=0, Z=0: a log cabin of 9×11 (7×9 inside), a porch with a rocking chair and steps, a gable roof with a chimney,
 * and inside a desk with his typewriter, a chair, bookshelves, a hearth, empty bottles and drafts on the floor.
 *
 * <p>No block here has a block entity (no chests, beds, lecterns, signs, banners, pots…): the Author's arena erases and
 * restores the cabin through {@code ArenaController.mutate}, which refuses them.
 */
public final class CabinLayout {

    /** The plan's extent, local: the chimney sticks out at X=-6, the steps at Z=9. */
    public static final int MIN_X = -6, MAX_X = 5, MIN_Z = -6, MAX_Z = 9, MAX_Y = 10;
    /** How far below the floor the foundation may reach down to the ground. */
    public static final int FOUNDATION = 8;
    /** Above the plan, the builder clears this high over its footprint (leaves, grass, snow). */
    public static final int CLEAR_TO = 13;

    /** The typewriter on the desk, the chair before it (where he sits), the door's lower half, the room's middle. */
    public static final int[] TYPEWRITER = {1, 2, -4}, CHAIR = {1, 1, -3}, DOOR = {0, 1, 5}, CENTRE = {0, 1, 0};

    /** One block of the plan. */
    public record Cell(int x, int y, int z, String state) {
    }

    private static final String AIR = "minecraft:air";
    private static final List<Cell> CELLS = build();

    private CabinLayout() {
    }

    /** Every block, in placing order (later cells never overlap earlier ones: each position appears once). */
    public static List<Cell> cells() {
        return CELLS;
    }

    /** The distinct block states the plan uses. */
    public static Set<String> palette() {
        Set<String> out = new LinkedHashSet<>();
        for (Cell c : CELLS) out.add(c.state());
        return out;
    }

    /** The block id of a state string ({@code minecraft:spruce_stairs[facing=north]} → {@code minecraft:spruce_stairs}). */
    public static String blockId(String state) {
        int b = state.indexOf('[');
        return b < 0 ? state : state.substring(0, b);
    }

    // --- rotation ------------------------------------------------------------------------------------

    /**
     * Local X/Z turned by {@code quarter} clockwise quarter turns (as vanilla's {@code Rotation}: NONE, CLOCKWISE_90,
     * CLOCKWISE_180, COUNTERCLOCKWISE_90), so the door looks south, west, north or east.
     */
    public static int[] rotate(int x, int z, int quarter) {
        return switch (Math.floorMod(quarter, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    /** A local point to world coordinates, for a cabin whose floor centre is {@code (ox, oy, oz)}. */
    public static int[] toWorld(int[] local, int ox, int oy, int oz, int quarter) {
        int[] r = rotate(local[0], local[2], quarter);
        return new int[]{ox + r[0], oy + local[1], oz + r[1]};
    }

    /** The plan's world box {minX, minY, minZ, maxX, maxY, maxZ}, foundation and cleared air included. */
    public static int[] box(int ox, int oy, int oz, int quarter) {
        int[] a = rotate(MIN_X, MIN_Z, quarter), b = rotate(MAX_X, MAX_Z, quarter);
        return new int[]{ox + Math.min(a[0], b[0]), oy - FOUNDATION, oz + Math.min(a[1], b[1]),
                ox + Math.max(a[0], b[0]), oy + CLEAR_TO, oz + Math.max(a[1], b[1])};
    }

    // --- the plan ------------------------------------------------------------------------------------

    private static List<Cell> build() {
        Map<Long, Cell> m = new LinkedHashMap<>();
        Plan p = new Plan(m);
        String cobble = "minecraft:cobblestone", floor = "minecraft:oak_planks", planks = "minecraft:spruce_planks";

        // Floor and sill: cobblestone under the walls, oak boards inside, spruce on the porch; three steps.
        for (int x = -4; x <= 4; x++) {
            for (int z = -5; z <= 5; z++) p.set(x, 0, z, wall(x, z) ? cobble : floor);
            for (int z = 6; z <= 8; z++) p.set(x, 0, z, planks);
        }
        for (int x = -1; x <= 1; x++) p.set(x, 0, 9, "minecraft:spruce_stairs[facing=north,half=bottom,shape=straight]");

        // Walls of horizontal logs, upright logs at the corners, three courses high.
        for (int y = 1; y <= 3; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (!wall(x, z)) {
                        p.set(x, y, z, AIR);
                        continue;
                    }
                    boolean corner = Math.abs(x) == 4 && Math.abs(z) == 5;
                    p.set(x, y, z, corner ? "minecraft:spruce_log[axis=y]"
                            : Math.abs(z) == 5 ? "minecraft:spruce_log[axis=x]" : "minecraft:spruce_log[axis=z]");
                }
            }
        }
        // The door, the windows (panes joined along their wall).
        p.put(0, 1, 5, "minecraft:spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]");
        p.put(0, 2, 5, "minecraft:spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]");
        String paneX = "minecraft:glass_pane[east=true,west=true]", paneZ = "minecraft:glass_pane[north=true,south=true]";
        for (int x : new int[]{-2, 2}) p.put(x, 2, 5, paneX);
        for (int x = -1; x <= 1; x++) p.put(x, 2, -5, paneX);
        for (int z = -1; z <= 1; z++) p.put(4, 2, z, paneZ);
        for (int z = 2; z <= 3; z++) p.put(-4, 2, z, paneZ);

        // Wall plate and ceiling.
        for (int x = -4; x <= 4; x++) {
            for (int z = -5; z <= 5; z++) {
                p.set(x, 4, z, Math.abs(x) == 4 ? "minecraft:spruce_log[axis=z]" : Math.abs(z) == 5 ? "minecraft:spruce_log[axis=x]" : planks);
            }
        }
        // Gable roof along Z, ridge on X=0, one block of overhang at each end; planked gables with a little window.
        for (int y = 4; y <= 8; y++) {
            int edge = 9 - y;
            for (int z = -6; z <= 5; z++) {
                p.set(-edge, y, z, "minecraft:spruce_stairs[facing=east,half=bottom,shape=straight]");
                p.set(edge, y, z, "minecraft:spruce_stairs[facing=west,half=bottom,shape=straight]");
            }
            if (y >= 5) {
                for (int x = -edge + 1; x <= edge - 1; x++) {
                    for (int z = -4; z <= 4; z++) p.set(x, y, z, AIR);
                    p.set(x, y, -5, planks);
                    p.set(x, y, 5, planks);
                }
            }
        }
        for (int z = -6; z <= 5; z++) p.set(0, 9, z, "minecraft:spruce_slab[type=bottom,waterlogged=false]");
        p.put(0, 6, 5, paneX);
        p.put(0, 6, -5, paneX);

        // The porch: a lean-to roof of slabs on four fence posts, a railing with a gap for the steps.
        for (int x = -5; x <= 5; x++) for (int z = 6; z <= 9; z++) p.set(x, 4, z, "minecraft:spruce_slab[type=bottom,waterlogged=false]");
        for (int x = -4; x <= 4; x++) for (int y = 1; y <= 3; y++) for (int z = 6; z <= 8; z++) p.set(x, y, z, AIR);
        for (int y = 1; y <= 3; y++) {
            p.put(-4, y, 8, y == 1 ? fence(true, false, true, false) : fence(false, false, false, false));
            p.put(4, y, 8, y == 1 ? fence(false, true, true, false) : fence(false, false, false, false));
        }
        p.put(-3, 1, 8, fence(true, true, false, false));
        p.put(-2, 1, 8, fence(false, true, false, false));
        p.put(2, 1, 8, fence(true, false, false, false));
        p.put(3, 1, 8, fence(true, true, false, false));
        for (int z = 6; z <= 7; z++) {
            p.put(-4, 1, z, fence(false, false, true, true));
            p.put(4, 1, z, fence(false, false, true, true));
        }
        // The rocking chair (a stair between two open trapdoors) and a little table with a bottle on it.
        p.put(2, 1, 6, "minecraft:spruce_stairs[facing=north,half=bottom,shape=straight]");
        p.put(1, 1, 6, "minecraft:spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]");
        p.put(3, 1, 6, "minecraft:spruce_trapdoor[facing=east,half=bottom,open=true,powered=false,waterlogged=false]");
        p.put(-2, 1, 6, "minecraft:spruce_slab[type=top,waterlogged=false]");
        p.put(-2, 2, 6, bottles(1));

        // The chimney on the west side, and the hearth behind iron bars.
        for (int y = 0; y <= MAX_Y; y++) for (int x = -6; x <= -5; x++) for (int z = -1; z <= 0; z++) p.set(x, y, z, cobble);
        for (int y = 1; y <= 3; y++) for (int z = -1; z <= 0; z++) p.put(-4, y, z, cobble);
        for (int z = -1; z <= 0; z++) {
            p.put(-4, 1, z, "minecraft:iron_bars[north=true,south=true,east=false,west=false,waterlogged=false]");
            p.put(-3, 0, z, "minecraft:stone_bricks");
        }

        // The desk under the back window, the typewriter on it, paper and whiskey; his chair.
        for (int x = 0; x <= 2; x++) p.put(x, 1, -4, "minecraft:spruce_stairs[facing=north,half=top,shape=straight]");
        p.put(TYPEWRITER[0], TYPEWRITER[1], TYPEWRITER[2], "supernaturalcraft:typewriter[facing=south]");
        p.put(0, 2, -4, "minecraft:white_carpet");
        p.put(2, 2, -4, bottles(2));
        p.put(CHAIR[0], CHAIR[1], CHAIR[2], "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight]");
        // Bookshelves along the east wall, an armchair facing the hearth, a couch under the front window.
        for (int z = -2; z <= 1; z++) for (int y = 1; y <= 2; y++) p.put(3, y, z, "minecraft:bookshelf");
        p.put(-2, 1, 1, "minecraft:oak_stairs[facing=east,half=bottom,shape=straight]");
        for (int x = 2; x <= 3; x++) p.put(x, 1, 4, "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight]");
        // A rug, the lantern, a dead plant by the door, empty bottles in the corners, drafts thrown on the floor.
        for (int x = -1; x <= 1; x++) for (int z = 0; z <= 2; z++) p.put(x, 1, z, "minecraft:red_carpet");
        p.put(0, 3, 0, "minecraft:lantern[hanging=true,waterlogged=false]");
        p.put(-3, 1, 4, "minecraft:spruce_slab[type=top,waterlogged=false]");
        p.put(-3, 2, 4, "minecraft:potted_dead_bush");
        p.put(-3, 1, -4, bottles(3));
        p.put(3, 1, -4, bottles(2));
        for (int[] d : new int[][]{{2, -2}, {-1, -2}, {-3, 2}, {3, 3}, {1, 3}}) p.put(d[0], 1, d[1], "minecraft:white_carpet");

        return List.copyOf(new ArrayList<>(m.values()));
    }

    /** Whether local (x, z) lies on the cabin's walls. */
    private static boolean wall(int x, int z) {
        return Math.abs(x) == 4 && Math.abs(z) <= 5 || Math.abs(z) == 5 && Math.abs(x) <= 4;
    }

    /** Empty whiskey bottles: unlit brown candles. */
    private static String bottles(int n) {
        return "minecraft:brown_candle[candles=" + n + ",lit=false,waterlogged=false]";
    }

    private static String fence(boolean east, boolean west, boolean north, boolean south) {
        return "minecraft:spruce_fence[east=" + east + ",north=" + north + ",south=" + south + ",waterlogged=false,west=" + west + "]";
    }

    /** Writes into the map: {@link #set} overwrites, {@link #put} too (details go last). */
    private record Plan(Map<Long, Cell> m) {
        void set(int x, int y, int z, String state) {
            long key = ((long) (x + 64) << 20) | ((long) (y + 64) << 10) | (z + 64);
            m.remove(key);
            m.put(key, new Cell(x, y, z, state));
        }

        void put(int x, int y, int z, String state) {
            set(x, y, z, state);
        }
    }
}
