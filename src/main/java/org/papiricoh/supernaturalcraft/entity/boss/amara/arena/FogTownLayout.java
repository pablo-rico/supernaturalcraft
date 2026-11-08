package org.papiricoh.supernaturalcraft.entity.boss.amara.arena;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The town under the black fog (v0.42): the crossroads of a small American town the Darkness swallowed, laid round the rite that
 * calls Amara (pure, tested in JUnit: {@code FogTownLayoutTest}). Every cell is relative to the rite's column, {@code dy} from the
 * floor (0 is the floor block, 1 the rite's own level):
 * <ul>
 *   <li>two roads of cracked asphalt cross at the rite ({@link #ROAD_HALF} wide each side), with kerbs and sidewalks;</li>
 *   <li>four small clapboard houses, one per quadrant ({@link #HOUSES}), their windows broken, their porches sagging, their roofs
 *   half caved in; and dead lawns between them;</li>
 *   <li>dead street lamps at the crossroads' corners (iron posts, unlit lamps: the power is out); the four Light Wells that the
 *   fight places stand on the roads ({@code AmaraSummoning.raiseWells}), so the plan keeps those spots ({@link #wellSpot}) clear;</li>
 *   <li>the middle ({@link #CLEAR}) is open road: she rises there.</li>
 * </ul>
 * Only vanilla blocks without block entities, so the arena can write and restore all of it.
 */
public final class FogTownLayout {

    /** The town's reach: every cell is within this many blocks (flat) of the rite. */
    public static final int RADIUS = 26;
    /** The smallest arena the town fits in, with its margin. */
    public static final int MIN_RADIUS = 28;
    /** Half the road's width (the road spans -ROAD_HALF..ROAD_HALF), then a kerb, then the sidewalk. */
    public static final int ROAD_HALF = 3, SIDEWALK = 2;
    /** The open middle, where she rises: nothing above the floor within this many blocks. */
    public static final int CLEAR = 5;
    /** How far below the floor the plan reaches, and how high its roofs go. */
    public static final int BOTTOM = -1, TOP = 9;

    public static final String AIR = "minecraft:air";
    static final String ASPHALT = "minecraft:polished_blackstone", ASPHALT_CRACKED = "minecraft:cracked_polished_blackstone_bricks",
            ASPHALT_PATCH = "minecraft:blackstone", LINE = "minecraft:yellow_terracotta", KERB = "minecraft:smooth_stone",
            SIDEWALK_BLOCK = "minecraft:light_gray_concrete", SIDEWALK_CRACK = "minecraft:andesite", LAWN = "minecraft:coarse_dirt",
            LAWN_DEAD = "minecraft:podzol", BUSH = "minecraft:dead_bush", WALL = "minecraft:birch_planks", WALL_ROT = "minecraft:stripped_birch_wood",
            CORNER = "minecraft:stripped_dark_oak_log", FOUNDATION = "minecraft:cobblestone", FLOOR = "minecraft:spruce_planks",
            ROOF = "minecraft:dark_oak_slab[type=bottom]", ROOF_TOP = "minecraft:dark_oak_slab[type=top]", PANE = "minecraft:glass_pane",
            POST = "minecraft:iron_bars", LAMP = "minecraft:redstone_lamp[lit=false]", COBWEB = "minecraft:cobweb",
            FENCE = "minecraft:spruce_fence", PORCH = "minecraft:spruce_slab[type=bottom]";

    /** A house: its centre, half sizes and which way its front (porch) faces (+1 / -1 along x or z). */
    public record House(int cx, int cz, int halfX, int halfZ, int frontX, int frontZ) {
        boolean inside(int x, int z) {
            return Math.abs(x - cx) <= halfX && Math.abs(z - cz) <= halfZ;
        }

        boolean wall(int x, int z) {
            return inside(x, z) && (Math.abs(x - cx) == halfX || Math.abs(z - cz) == halfZ);
        }
    }

    /** One per quadrant, each facing the road nearest it. */
    public static final List<House> HOUSES = List.of(
            new House(13, 13, 5, 4, 0, -1), new House(-13, 13, 5, 4, 0, -1),
            new House(13, -13, 5, 4, 0, 1), new House(-13, -13, 4, 5, 1, 0));

    private FogTownLayout() {
    }

    /** Where Light Well {@code i} (0 E, 1 S, 2 W, 3 N) stands for an arena of {@code radius}: on a road, three fifths out. */
    public static int[] wellSpot(int i, int radius) {
        int d = Math.round(radius * 0.6f);
        return switch (i) {
            case 0 -> new int[]{d, 0};
            case 1 -> new int[]{0, d};
            case 2 -> new int[]{-d, 0};
            default -> new int[]{0, -d};
        };
    }

    /** Whether column ({@code x}, {@code z}) is road (either of the two). */
    public static boolean road(int x, int z) {
        return Math.abs(x) <= ROAD_HALF || Math.abs(z) <= ROAD_HALF;
    }

    static boolean kerb(int x, int z) {
        return !road(x, z) && (Math.abs(x) == ROAD_HALF + 1 || Math.abs(z) == ROAD_HALF + 1);
    }

    static boolean sidewalk(int x, int z) {
        return !road(x, z) && !kerb(x, z) && (Math.abs(x) <= ROAD_HALF + 1 + SIDEWALK || Math.abs(z) <= ROAD_HALF + 1 + SIDEWALK);
    }

    /** A stable 0..1 noise per cell (the plan is the same every time). */
    static double hash(int x, int y, int z) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L;
        h ^= (h >>> 13);
        h *= 0x5bd1e995L;
        h ^= (h >>> 15);
        return ((h & 0xFFFF) / 65535.0);
    }

    /** The whole town. */
    public static List<ArenaCell> plan() {
        Map<Long, ArenaCell> out = new HashMap<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (x * x + z * z > RADIUS * RADIUS) continue;
                ground(out, x, z);
            }
        }
        for (House h : HOUSES) house(out, h);
        lamps(out);
        return new ArrayList<>(out.values());
    }

    private static void put(Map<Long, ArenaCell> out, int x, int y, int z, String block) {
        if (x * x + z * z > RADIUS * RADIUS || y < BOTTOM || y > TOP) return;
        if (y >= 1 && Math.abs(x) <= CLEAR && Math.abs(z) <= CLEAR) return;
        out.put(((long) (x + 512) << 20) | ((long) (y + 64) << 10) | (z + 512), new ArenaCell(x, y, z, block));
    }

    private static void ground(Map<Long, ArenaCell> out, int x, int z) {
        double n = hash(x, 0, z);
        String top;
        if (road(x, z)) {
            boolean centreLine = (x == 0 && Math.abs(z) > ROAD_HALF + 2 && Math.floorMod(z, 4) < 2)
                    || (z == 0 && Math.abs(x) > ROAD_HALF + 2 && Math.floorMod(x, 4) < 2);
            top = centreLine ? (n < 0.3 ? ASPHALT_CRACKED : LINE) : n < 0.18 ? ASPHALT_CRACKED : n < 0.3 ? ASPHALT_PATCH : ASPHALT;
        } else if (kerb(x, z)) {
            top = KERB;
        } else if (sidewalk(x, z)) {
            top = n < 0.15 ? SIDEWALK_CRACK : SIDEWALK_BLOCK;
        } else {
            top = n < 0.4 ? LAWN_DEAD : LAWN;
            if (n > 0.93) put(out, x, 1, z, BUSH);
        }
        put(out, x, 0, z, top);
        put(out, x, -1, z, road(x, z) ? "minecraft:gravel" : "minecraft:dirt");
    }

    private static void house(Map<Long, ArenaCell> out, House h) {
        // Foundation and floor.
        for (int x = h.cx - h.halfX; x <= h.cx + h.halfX; x++) {
            for (int z = h.cz - h.halfZ; z <= h.cz + h.halfZ; z++) {
                put(out, x, 0, z, h.wall(x, z) ? FOUNDATION : FLOOR);
                for (int y = 1; y <= 4; y++) put(out, x, y, z, AIR);
            }
        }
        // Walls with corner posts, a door in the front and windows (most broken).
        for (int x = h.cx - h.halfX; x <= h.cx + h.halfX; x++) {
            for (int z = h.cz - h.halfZ; z <= h.cz + h.halfZ; z++) {
                if (!h.wall(x, z)) continue;
                boolean corner = Math.abs(x - h.cx) == h.halfX && Math.abs(z - h.cz) == h.halfZ;
                boolean front = h.frontZ != 0 ? z - h.cz == h.frontZ * h.halfZ : x - h.cx == h.frontX * h.halfX;
                boolean door = front && (h.frontZ != 0 ? x == h.cx : z == h.cz);
                for (int y = 1; y <= 4; y++) {
                    String b;
                    if (corner) b = CORNER;
                    else if (door && y <= 2) b = AIR;
                    else if (y == 2 && !corner && Math.floorMod(h.frontZ != 0 ? x : z, 3) == 0) b = hash(x, y, z) < 0.65 ? AIR : PANE;
                    else b = hash(x, y, z) < 0.12 ? WALL_ROT : WALL;
                    // The upper corners of the walls are torn away in places.
                    if (y == 4 && !corner && hash(x, 7, z) < 0.25) b = AIR;
                    put(out, x, y, z, b);
                }
            }
        }
        // A roof of slabs, caved in over a third of the house.
        for (int x = h.cx - h.halfX - 1; x <= h.cx + h.halfX + 1; x++) {
            for (int z = h.cz - h.halfZ - 1; z <= h.cz + h.halfZ + 1; z++) {
                int dx = Math.abs(x - h.cx), dz = Math.abs(z - h.cz);
                int rise = Math.min(h.halfX + 1 - dx, h.halfZ + 1 - dz);
                if (hash(x, 9, z) < 0.33 && dx < h.halfX && dz < h.halfZ) continue;
                put(out, x, 5 + Math.min(2, rise / 2), z, rise % 2 == 0 ? ROOF : ROOF_TOP);
            }
        }
        // Cobwebs in the corners and the porch out front.
        put(out, h.cx - h.halfX + 1, 4, h.cz - h.halfZ + 1, COBWEB);
        put(out, h.cx + h.halfX - 1, 4, h.cz + h.halfZ - 1, COBWEB);
        int px = h.cx + h.frontX * (h.halfX + 1), pz = h.cz + h.frontZ * (h.halfZ + 1);
        for (int i = -2; i <= 2; i++) {
            int x = h.frontZ != 0 ? h.cx + i : px, z = h.frontZ != 0 ? pz : h.cz + i;
            put(out, x, 1, z, PORCH);
            if (Math.abs(i) == 2) put(out, x, 2, z, FENCE);
        }
    }

    private static void lamps(Map<Long, ArenaCell> out) {
        int d = ROAD_HALF + 2;
        int[][] spots = {{d, d}, {-d, d}, {d, -d}, {-d, -d}, {d, 16}, {-d, -16}, {16, -d}, {-16, d}};
        for (int[] s : spots) {
            if (Math.abs(s[0]) <= CLEAR && Math.abs(s[1]) <= CLEAR) continue;
            for (int y = 1; y <= 3; y++) put(out, s[0], y, s[1], POST);
            put(out, s[0], 4, s[1], LAMP);
        }
    }

    /** Whether nothing is planned above the floor at ({@code x}, {@code z}) (the middle, where she rises). */
    public static boolean inClear(int x, int z) {
        return Math.abs(x) <= CLEAR && Math.abs(z) <= CLEAR;
    }
}
