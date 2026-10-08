package org.papiricoh.supernaturalcraft.entity.boss.raphael.arena;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raphael's abandoned house (v0.16), pure and tested in JUnit: a plan of {@link ArenaCell}s written round the rite. Every cell is
 * relative to the altar's column, {@code dy} counted from the floor (0 is the floor block under the altar, 1 the altar's own
 * level). The house is {@code 21 × 17} (walls at x = ±{@link #HALF_X}, z = ±{@link #HALF_Z}), its porch and front door to the
 * south (+z):
 * <ul>
 *   <li>the west room (the parlour) holds the rite in its middle ({@link #RITUAL}: nothing is built there above the floor), an
 *   overturned table, a toppled bookshelf, a couch, a cold fireplace and two rings of holy oil;</li>
 *   <li>the east room (the kitchen), through a wide doorway in the dividing wall at x = {@link #DIVIDER_X}, holds the other two
 *   rings, a counter and a fallen cupboard;</li>
 *   <li>broken windows ({@link #WINDOWS}) in the outer walls: the storm's lightning comes in through them in lines;</li>
 *   <li>the roof (the ceiling, a half-pitch gable of slabs, its gable ends and the porch roof) is its own group, {@link #roof()}:
 *   the last phase tears it off.</li>
 * </ul>
 * The rings of holy oil ({@link #RINGS}, {@link #ringCells}) are not part of the plan: the fight pours them on the finished floor
 * (the plan keeps those cells clear). Only vanilla blocks without block entities, so the arena can write and restore all of it.
 */
public final class HouseLayout {

    /** The outer walls' half sizes: the house spans x -10..10, z -8..8. */
    public static final int HALF_X = 10, HALF_Z = 8;
    /** The wall between the two rooms. */
    public static final int DIVIDER_X = 4;
    /** The walls' height above the floor; the ceiling is the level above. */
    public static final int WALL_TOP = 4, CEILING = 5;
    /** The rite in the middle of the parlour: nothing above the floor within this many blocks of the altar (both axes). */
    public static final int RITUAL = 3;
    /** The porch, south of the front wall. */
    public static final int PORCH_HALF_X = 5, PORCH_DEPTH = 3;
    /** How far the plan reaches below the floor (the foundation) and above it (the roof's ridge). */
    public static final int BOTTOM = -2, TOP = 11;
    /** The smallest arena the house fits in, with its margin. */
    public static final int MIN_RADIUS = 16;
    /** A ring of holy oil is the border of a square this many cells from its centre (5 × 5, a 3 × 3 floor inside). */
    public static final int RING_HALF = 2;

    public static final String AIR = "minecraft:air";
    public static final String FLOOR = "minecraft:dark_oak_planks";

    /** A column of the house, relative to the altar. */
    public record Spot(int dx, int dz) {
        public int distanceSq() {
            return dx * dx + dz * dz;
        }
    }

    /**
     * A broken window: its column in the wall and the way into the house; lightning runs {@code length} blocks inward from it.
     */
    public record Window(int dx, int dz, int inX, int inZ, int length) {
    }

    /** The centres of the four rings of holy oil: two in the parlour, two in the kitchen. */
    public static final List<Spot> RINGS = List.of(new Spot(-7, -5), new Spot(-7, 5), new Spot(7, -4), new Spot(7, 4));

    /** Where his garrison takes its posts in the second phase (inside, clear of the rings and the rite). */
    public static final List<Spot> POSTS = List.of(new Spot(-4, -6), new Spot(3, 6), new Spot(6, 0), new Spot(-8, 0));

    /** The front door (an empty doorway: the door lies on the porch). */
    public static final Spot DOOR = new Spot(1, HALF_Z);

    public static final List<Window> WINDOWS = List.of(
            new Window(-6, -HALF_Z, 0, 1, 2 * HALF_Z - 1), new Window(-1, -HALF_Z, 0, 1, 2 * HALF_Z - 1),
            new Window(7, -HALF_Z, 0, 1, 2 * HALF_Z - 1), new Window(-6, HALF_Z, 0, -1, 2 * HALF_Z - 1),
            new Window(-2, HALF_Z, 0, -1, 2 * HALF_Z - 1), new Window(7, HALF_Z, 0, -1, 2 * HALF_Z - 1),
            new Window(-HALF_X, -3, 1, 0, 2 * HALF_X - 1), new Window(-HALF_X, 3, 1, 0, 2 * HALF_X - 1),
            new Window(HALF_X, 0, -1, 0, 2 * HALF_X - 1));

    private HouseLayout() {
    }

    // --- the rings ------------------------------------------------------------------------------------------------------

    /** The cells of ring {@code index}: the border of a 5 × 5 square round its centre (so fire lies in all eight directions). */
    public static List<Spot> ringCells(int index) {
        Spot c = RINGS.get(index);
        List<Spot> out = new ArrayList<>();
        for (int x = -RING_HALF; x <= RING_HALF; x++) {
            for (int z = -RING_HALF; z <= RING_HALF; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) == RING_HALF) out.add(new Spot(c.dx() + x, c.dz() + z));
            }
        }
        return out;
    }

    /** The ring whose oil (or the floor inside it) covers column ({@code dx}, {@code dz}), or -1. */
    public static int ringAt(int dx, int dz) {
        for (int i = 0; i < RINGS.size(); i++) {
            Spot c = RINGS.get(i);
            if (Math.abs(dx - c.dx()) <= RING_HALF && Math.abs(dz - c.dz()) <= RING_HALF) return i;
        }
        return -1;
    }

    /** Whether column ({@code dx}, {@code dz}) holds oil (a ring's border, not the floor inside it). */
    public static boolean isOil(int dx, int dz) {
        int i = ringAt(dx, dz);
        if (i < 0) return false;
        Spot c = RINGS.get(i);
        return Math.max(Math.abs(dx - c.dx()), Math.abs(dz - c.dz())) == RING_HALF;
    }

    // --- regions --------------------------------------------------------------------------------------------------------

    public static boolean inRitual(int dx, int dz) {
        return Math.abs(dx) <= RITUAL && Math.abs(dz) <= RITUAL;
    }

    /** Inside the outer walls (walls included). */
    public static boolean inFootprint(int dx, int dz) {
        return Math.abs(dx) <= HALF_X && Math.abs(dz) <= HALF_Z;
    }

    /** A floor cell inside the rooms (walls excluded). */
    public static boolean indoors(int dx, int dz) {
        return Math.abs(dx) < HALF_X && Math.abs(dz) < HALF_Z && dx != DIVIDER_X;
    }

    public static boolean onPorch(int dx, int dz) {
        return Math.abs(dx) <= PORCH_HALF_X && dz > HALF_Z && dz <= HALF_Z + PORCH_DEPTH;
    }

    static boolean isOuterWall(int dx, int dz) {
        return inFootprint(dx, dz) && (Math.abs(dx) == HALF_X || Math.abs(dz) == HALF_Z);
    }

    static boolean isWall(int dx, int dz) {
        return isOuterWall(dx, dz) || (dx == DIVIDER_X && Math.abs(dz) < HALF_Z);
    }

    /** The doorway between the rooms: three wide, two high. */
    static boolean inDoorway(int dx, int dz, int dy) {
        return dx == DIVIDER_X && Math.abs(dz) <= 1 && dy >= 1 && dy <= 2;
    }

    static boolean inWindow(int dx, int dz, int dy) {
        if (dy < 2 || dy > 3) return false;
        for (Window w : WINDOWS) if (w.dx() == dx && w.dz() == dz) return true;
        return false;
    }

    // --- the plan -------------------------------------------------------------------------------------------------------

    /** The whole house, roof included (the body first, then {@link #roof()}; no cell twice). */
    public static List<ArenaCell> plan() {
        List<ArenaCell> out = new ArrayList<>(body());
        out.addAll(roof());
        return out;
    }

    /** Everything but the roof: foundation, floor, walls, the rooms' air and the furniture. */
    public static List<ArenaCell> body() {
        Map<String, ArenaCell> cells = new LinkedHashMap<>();
        for (int dx = -HALF_X; dx <= HALF_X; dx++) {
            for (int dz = -HALF_Z; dz <= HALF_Z + PORCH_DEPTH; dz++) {
                boolean house = inFootprint(dx, dz), porch = onPorch(dx, dz);
                if (!house && !porch) continue;
                // Foundation and floor.
                put(cells, dx, -2, dz, "minecraft:cobblestone");
                put(cells, dx, -1, dz, isWall(dx, dz) || porch ? "minecraft:cobblestone" : "minecraft:dirt");
                put(cells, dx, 0, dz, porch ? "minecraft:spruce_planks" : isWall(dx, dz) ? "minecraft:cobblestone" : floor(dx, dz));
                if (porch) continue;
                for (int dy = 1; dy <= WALL_TOP; dy++) {
                    if (isWall(dx, dz)) {
                        boolean open = inDoorway(dx, dz, dy) || inWindow(dx, dz, dy) || (dx == DOOR.dx() && dz == DOOR.dz() && dy <= 2);
                        put(cells, dx, dy, dz, open ? AIR : wall(dx, dy, dz));
                    } else if (!inRitual(dx, dz)) {
                        put(cells, dx, dy, dz, AIR);
                    }
                }
            }
        }
        furniture(cells);
        return new ArrayList<>(cells.values());
    }

    /**
     * The roof group: the ceiling over the walls, the attic's air, a half-pitch gable of dark oak slabs (the ridge runs east to
     * west, a block of overhang all round), the gable ends and the porch's roof.
     */
    public static List<ArenaCell> roof() {
        Map<String, ArenaCell> cells = new LinkedHashMap<>();
        for (int dx = -HALF_X - 1; dx <= HALF_X + 1; dx++) {
            for (int dz = -HALF_Z - 1; dz <= HALF_Z + 1; dz++) {
                boolean house = inFootprint(dx, dz);
                if (house) put(cells, dx, CEILING, dz, (hash(dx, CEILING, dz) % 23 == 0) ? AIR : "minecraft:spruce_planks");
                int k = HALF_Z + 1 - Math.abs(dz);
                int level = CEILING + 1 + k / 2;
                boolean gable = house && Math.abs(dx) == HALF_X;
                for (int dy = CEILING + 1; dy < level; dy++) {
                    if (house) put(cells, dx, dy, dz, gable ? "minecraft:spruce_planks" : AIR);
                }
                put(cells, dx, level, dz, "minecraft:dark_oak_slab[type=" + (k % 2 == 0 ? "bottom" : "top") + "]");
            }
        }
        for (int dx = -PORCH_HALF_X; dx <= PORCH_HALF_X; dx++) {
            for (int dz = HALF_Z + 2; dz <= HALF_Z + PORCH_DEPTH; dz++) {
                put(cells, dx, WALL_TOP, dz, "minecraft:spruce_slab[type=top]");
            }
        }
        return new ArrayList<>(cells.values());
    }

    /** The parlour's and the kitchen's furniture, all of it knocked over. */
    private static void furniture(Map<String, ArenaCell> cells) {
        // The parlour: an overturned table and its chairs, north of the rite.
        put(cells, -1, 1, -6, "minecraft:oak_trapdoor[facing=north,half=bottom,open=true]");
        put(cells, 0, 1, -6, "minecraft:oak_trapdoor[facing=north,half=bottom,open=true]");
        put(cells, 1, 1, -5, "minecraft:oak_stairs[facing=east,half=top]");
        put(cells, -2, 1, -7, "minecraft:oak_stairs[facing=south,half=bottom]");
        // A toppled bookshelf and scattered books (pages).
        put(cells, 2, 1, -7, "minecraft:bookshelf");
        put(cells, 3, 1, -7, "minecraft:bookshelf");
        put(cells, 3, 2, -7, "minecraft:bookshelf");
        put(cells, 1, 1, -7, "minecraft:white_carpet");
        // A couch against the south wall, a torn rug before it.
        put(cells, -3, 1, 7, "minecraft:dark_oak_slab[type=bottom]");
        put(cells, -2, 1, 7, "minecraft:dark_oak_stairs[facing=south,half=bottom]");
        put(cells, -1, 1, 7, "minecraft:dark_oak_stairs[facing=south,half=bottom]");
        put(cells, 0, 1, 7, "minecraft:dark_oak_slab[type=bottom]");
        put(cells, -2, 1, 5, "minecraft:brown_carpet");
        put(cells, -1, 1, 5, "minecraft:brown_carpet");
        put(cells, -2, 1, 4, "minecraft:brown_carpet");
        // A cold fireplace in the west wall's middle.
        for (int dz : new int[]{-1, 1}) for (int dy = 1; dy <= WALL_TOP; dy++) put(cells, -HALF_X + 1, dy, dz, "minecraft:cobblestone");
        put(cells, -HALF_X + 1, 2, 0, "minecraft:cobblestone");
        put(cells, -HALF_X + 1, 3, 0, "minecraft:cobblestone");
        put(cells, -HALF_X + 1, 4, 0, "minecraft:cobblestone");
        put(cells, -HALF_X + 1, 1, 0, "minecraft:iron_bars");
        // The kitchen: a counter along the north wall, a sink (a cauldron), a fallen cupboard by the south wall.
        for (int dx = DIVIDER_X + 1; dx <= HALF_X - 2; dx++) put(cells, dx, 1, -HALF_Z + 1, "minecraft:spruce_slab[type=top]");
        put(cells, HALF_X - 1, 1, -HALF_Z + 1, "minecraft:cauldron");
        put(cells, DIVIDER_X + 2, 1, HALF_Z - 1, "minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]");
        put(cells, DIVIDER_X + 3, 1, HALF_Z - 1, "minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]");
        put(cells, HALF_X - 1, 1, HALF_Z - 1, "minecraft:spruce_planks");
        put(cells, HALF_X - 1, 2, HALF_Z - 1, "minecraft:spruce_slab[type=bottom]");
        // Cobwebs up in the corners, lanterns hanging where the storm sways them.
        for (int[] c : new int[][]{{-HALF_X + 1, -HALF_Z + 1}, {-HALF_X + 1, HALF_Z - 1}, {DIVIDER_X - 1, -HALF_Z + 1},
                {HALF_X - 1, HALF_Z - 1}, {DIVIDER_X + 1, -HALF_Z + 1}}) {
            put(cells, c[0], WALL_TOP, c[1], "minecraft:cobweb");
        }
        put(cells, -5, WALL_TOP, 0, "minecraft:lantern[hanging=true]");
        put(cells, 7, WALL_TOP, 0, "minecraft:lantern[hanging=true]");
        // The porch's posts (its roof is the roof's).
        for (int dx : new int[]{-PORCH_HALF_X, PORCH_HALF_X}) {
            for (int dy = 1; dy < WALL_TOP; dy++) put(cells, dx, dy, HALF_Z + PORCH_DEPTH, "minecraft:spruce_fence");
        }
        // The front door, torn off, lies on the porch.
        put(cells, DOOR.dx(), 1, HALF_Z + 1, "minecraft:oak_trapdoor[facing=north,half=bottom,open=false]");
    }

    private static String floor(int dx, int dz) {
        int h = hash(dx, 0, dz);
        if (!isOil(dx, dz) && !inRitual(dx, dz) && h % 17 == 0) return "minecraft:coarse_dirt";
        return h % 7 == 0 ? "minecraft:spruce_planks" : FLOOR;
    }

    private static String wall(int dx, int dy, int dz) {
        boolean corner = Math.abs(dx) == HALF_X && Math.abs(dz) == HALF_Z || dx == DIVIDER_X && Math.abs(dz) == HALF_Z;
        if (corner) return "minecraft:stripped_spruce_log[axis=y]";
        if (dy == 1 && isOuterWall(dx, dz) && hash(dx, dy, dz) % 3 == 0) return "minecraft:mossy_cobblestone";
        int h = hash(dx, dy, dz) % 11;
        return h == 0 ? "minecraft:stripped_spruce_wood[axis=y]" : h == 1 ? "minecraft:oak_planks" : "minecraft:spruce_planks";
    }

    private static void put(Map<String, ArenaCell> cells, int dx, int dy, int dz, String block) {
        cells.put(dx + "," + dy + "," + dz, new ArenaCell(dx, dy, dz, block));
    }

    static int hash(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    /** Packs a cell's offsets in one key (for lookups by position). */
    public static long key(int dx, int dy, int dz) {
        return ((long) (dx + 512) << 20) | ((long) (dy + 512) << 10) | (dz + 512);
    }

    /** Every window's lane of floor columns, from the window inward (for the lightning that comes through it). */
    public static List<Spot> lane(Window w) {
        List<Spot> out = new ArrayList<>();
        for (int i = 1; i <= w.length(); i++) out.add(new Spot(w.dx() + w.inX() * i, w.dz() + w.inZ() * i));
        return Collections.unmodifiableList(out);
    }
}
