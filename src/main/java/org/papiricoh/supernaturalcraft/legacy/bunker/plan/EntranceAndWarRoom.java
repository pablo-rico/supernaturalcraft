package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;

/**
 * The way in and the war room (pure): the façade set in the hillside with the armoured door, the vestibule, the stair tunnel
 * down to the rotunda's gallery, and the rotunda itself after the series' set: an octagon two storeys high, the gallery on
 * eight iron columns with its art-deco railing, a grated ceiling lit from behind, the lighting rig over the lit map table, a
 * flight of iron stairs down from the balcony and another up at the back, and back-lit glass screens either side of the
 * passage north to the stairwell. The reference room of the kit: the other parts follow its finish.
 */
public final class EntranceAndWarRoom {

    /** The door's lower half, the spot outside it, the map table Henry points at and his spot, a spot in the war room. */
    public static final int[] DOOR = {0, 1, 5}, OUTSIDE = {0, 1, 8}, MAP_TABLE = {0, -18, -30}, HENRY = {-3, -18, -30},
            WAR_ROOM = {0, -18, -22}, EMBLEM = {0, -19, -25};
    /** The map table's blocks (3×2). */
    public static final int[][] MAP_TABLES = {{-1, -18, -30}, {0, -18, -30}, {1, -18, -30}, {-1, -18, -29}, {0, -18, -29}, {1, -18, -29}};

    static final String DOOR_ID = "supernaturalcraft:bunker_door", TABLE = "supernaturalcraft:map_table",
            EMBLEM_ID = "supernaturalcraft:men_of_letters_emblem";

    private static final int CZ = Zones.ROTUNDA_Z, F1 = Zones.LEVEL1, FG = Zones.GALLERY;
    /** The lower storey's air: F1+1 .. FG-1; the upper: FG+1 .. GRATE_Y-1; the grate, the light plenum, the ceiling. */
    private static final int GRATE_Y = -5, PLENUM = -4, CEILING = -3;

    private EntranceAndWarRoom() {
    }

    public static void build(Plan p) {
        p.zone(Zones.ENTRANCE);
        facade(p);
        vestibule(p);
        tunnel(p);
        p.zone(Zones.ROTUNDA);
        rotunda(p);
        p.zone(null);
    }

    // --- the hillside façade ------------------------------------------------------------------------------------------------

    private static void facade(Plan p) {
        Pattern brick = mix(21, "minecraft:bricks", 14, "minecraft:mud_bricks", 2, "minecraft:granite", 0);
        // The brick face (z 6) over a stone plinth, backed (z 5) by brick round the door.
        for (int x = -5; x <= 5; x++) {
            for (int y = 0; y <= 7; y++) {
                p.set(x, y, 6, y <= 1 ? "minecraft:stone_bricks" : brick.at(x, y, 6));
                if (Math.abs(x) >= 3) p.set(x, y, 5, ROCK.at(x, y, 5));
            }
        }
        // Concrete piers at the corners, a cut band at the top, a corniced coping.
        for (int y = 1; y <= 6; y++) {
            for (int x : new int[]{-5, 5}) p.set(x, y, 6, y % 3 == 0 ? "minecraft:chiseled_stone_bricks" : "minecraft:polished_andesite");
        }
        for (int x = -5; x <= 5; x++) {
            p.set(x, 6, 6, "minecraft:polished_andesite");
            p.set(x, 7, 6, St.slab("minecraft:stone_brick_slab", false));
            p.set(x, 6, 7, St.stairs("minecraft:stone_brick_stairs", "north", true));
        }
        // The steel door frame, recessed one block, the emblem over it between two carved stones.
        for (int y = 1; y <= 3; y++) {
            p.set(-1, y, 6, STEEL);
            p.set(1, y, 6, STEEL);
        }
        p.set(0, 3, 6, STEEL);
        p.air(0, 0, 1, 2, 6, 6);
        p.set(0, 0, 6, STEEL_TILES);
        p.set(0, 4, 6, EMBLEM_ID);
        p.set(-1, 4, 6, "minecraft:chiseled_stone_bricks");
        p.set(1, 4, 6, "minecraft:chiseled_stone_bricks");
        p.set(0, 5, 7, St.stairs("minecraft:stone_brick_stairs", "north", true));
        String[] door = St.door(DOOR_ID, "north", "left", false);
        p.set(DOOR[0], DOOR[1], DOOR[2], door[0]);
        p.set(DOOR[0], DOOR[1] + 1, DOOR[2], door[1]);
        // Vents either side of the door, a weathered copper pipe down the east pier.
        for (int x : new int[]{-3, 3}) {
            p.set(x, 3, 6, St.bars());
            p.set(x, 3, 5, "minecraft:stone");
        }
        for (int y = 1; y <= 5; y++) p.set(4, y, 7, St.of("minecraft:lightning_rod", "facing", "up", "powered", "false", "waterlogged", "false"));
        p.set(4, 6, 7, St.of("minecraft:waxed_weathered_copper_grate"));
        // A sconce either side of the door: a lantern on a black bracket.
        for (int x : new int[]{-2, 2}) {
            p.set(x, 3, 7, St.trapdoor("minecraft:iron_trapdoor", "south", true, false));
            p.set(x, 4, 7, St.lantern(false));
        }
        // Retaining wing walls stepping down into the hill either side, mossy, with a slab cap.
        Pattern stone = mix(23, "minecraft:stone_bricks", 6, "minecraft:mossy_stone_bricks", 3, "minecraft:cracked_stone_bricks", 1);
        int[] height = {6, 5, 4, 3, 2};
        for (int i = 0; i < height.length; i++) {
            for (int sx : new int[]{-1, 1}) {
                int x = sx * (6 + i);
                for (int y = 0; y < height[i]; y++) p.set(x, y, 6, stone.at(x, y, 6));
                p.set(x, height[i], 6, St.slab("minecraft:mossy_stone_brick_slab", false));
                for (int y = 0; y <= height[i]; y++) p.set(x, y, 5, ROCK.at(x, y, 5));
            }
        }
        // Ivy on the brick.
        for (int x = -5; x <= 5; x++) {
            for (int y = 2; y <= 6; y++) {
                if (Math.abs(x) <= 2 && y <= 5 || Math.abs(x) == 4 && y <= 6 || x == 4 || Math.abs(x) == 3 && y == 3) continue;
                if (p.isAir(x, y, 7) || p.get(x, y, 7) == null) {
                    if (Plan.noise(x, y, 7, 25) < (y >= 5 ? 0.55 : 0.25)) p.set(x, y, 7, St.of("minecraft:vine", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false"));
                }
            }
        }
        // The forecourt: a worn path to the door, cleared of grass; a lamp post.
        for (int z = 7; z <= 14; z++) {
            for (int x = -2; x <= 2; x++) {
                double n = Plan.noise(x, 0, z, 27);
                boolean edge = Math.abs(x) == 2;
                if (edge && n < 0.5) continue;
                p.set(x, 0, z, edge ? (n < 0.8 ? "minecraft:coarse_dirt" : "minecraft:gravel")
                        : n < 0.7 ? "minecraft:dirt_path" : n < 0.88 ? "minecraft:gravel" : "minecraft:cobblestone");
                for (int y = 1; y <= 2; y++) if (p.get(x, y, z) == null) p.set(x, y, z, St.AIR);
            }
        }
        for (int y = 1; y <= 3; y++) p.set(3, y, 9, St.wall("minecraft:polished_blackstone_wall"));
        p.set(3, 0, 9, "minecraft:polished_andesite");
        p.set(3, 4, 9, St.lantern(false));
        p.view("entrance", 2, 1, 12, 0, 3, 6);
    }

    // --- the vestibule -------------------------------------------------------------------------------------------------------

    private static void vestibule(Plan p) {
        Room r = new Room(-2, 2, 0, 4, 0, 4);
        room(p, r, new Style(concreteFloor(), lebanonWall(0), solid(CREAM), CREAM_STAIRS, null, 0, null, 0));
        // The door back into the plan's front wall (the room drew its finish there), and the brick behind the brick face.
        String[] door = St.door(DOOR_ID, "north", "left", false);
        p.set(DOOR[0], DOOR[1], DOOR[2], door[0]);
        p.set(DOOR[0], DOOR[1] + 1, DOOR[2], door[1]);
        for (int x = -1; x <= 1; x += 2) for (int y = 1; y <= 3; y++) p.set(x, y, 5, STEEL);
        p.set(0, 3, 5, STEEL);
        p.set(0, 4, 5, St.stairs(CREAM_STAIRS, "south", true));
        // A mat, a bench with arms, a coat stand, an umbrella pot, a hanging lamp; the map and the clock on the walls.
        p.set(0, 1, 4, St.carpet("brown"));
        p.set(0, 1, 3, St.carpet("brown"));
        chair(p, 2, 1, 2, "west", OAK_STAIRS, null);
        chair(p, 2, 1, 3, "west", OAK_STAIRS, null);
        p.set(2, 1, 1, St.trapdoor(OAK_TRAPDOOR, "north", false, true));
        p.set(2, 1, 4, St.trapdoor(OAK_TRAPDOOR, "south", false, true));
        p.set(-2, 1, 4, St.fence(OAK_FENCE));
        p.set(-2, 2, 4, St.fence(OAK_FENCE));
        p.set(-2, 1, 3, "minecraft:decorated_pot[cracked=false,facing=east,waterlogged=false]");
        pendant(p, 0, 5, 2, 1, false);
        p.decor(Decor.frame(-2, 2, 1, "east", "minecraft:map"));
        p.decor(Decor.frame(2, 3, 2, "west", "minecraft:clock"));
        sign(p, 0, 4, 0, "south", "dark_oak", "yellow", "", "Men of Letters", "Chapter House", "");
        // The north wall opens on the stair tunnel under a cream lintel (the sign hangs on it).
        p.air(-1, 1, 1, 3, -1, -1);
        for (int x = -1; x <= 1; x++) p.set(x, 4, -1, CREAM);
        p.view("vestibule", 0, 1, 3, 0, -6, -11);
    }

    // --- the stair tunnel ---------------------------------------------------------------------------------------------------

    private static void tunnel(Plan p) {
        // Eleven steps down north from the vestibule (z -1 … -11), then a landing at the gallery's floor (z -12, -13).
        int steps = 11;
        for (int i = 0; i < steps; i++) {
            int z = -1 - i, y = -i;
            for (int x = -3; x <= 3; x++) {
                int ax = Math.abs(x);
                if (ax == 3) {
                    for (int yy = y - 1; yy <= y + 5; yy++) p.fill(x, yy, z, ROCK.at(x, yy, z), false);
                } else if (ax == 2) {
                    Pattern wall = lebanonWall(y - 1);
                    for (int yy = y - 1; yy <= y + 4; yy++) p.set(x, yy, z, wall.at(x, yy, z));
                    p.set(x, y + 5, z, ROCK.at(x, y + 5, z));
                } else {
                    p.set(x, y - 1, z, ROCK.at(x, y - 1, z));
                    p.set(x, y + 4, z, i == 0 ? CREAM : St.stairs(CREAM_STAIRS, "north", true));
                    p.set(x, y + 5, z, CREAM);
                }
            }
        }
        flight(p, -1, -1, 0, "north", "east", 3, steps, STEEL_STAIRS, null);
        // A handrail course of oak along both walls, lamps in alternate walls.
        for (int i = 0; i < steps; i++) {
            int z = -1 - i, y = -i;
            p.set(-2, y + 2, z, St.axis(OAK_LOG, "z"));
            p.set(2, y + 2, z, St.axis(OAK_LOG, "z"));
            if (i % 4 == 2) {
                int side = (i / 4 & 1) == 0 ? -2 : 2;
                p.set(side, y + 3, z, St.bulb("minecraft:waxed_exposed_copper_bulb"));
            }
        }
        // The landing.
        for (int z = -13; z <= -12; z++) {
            for (int x = -3; x <= 3; x++) {
                int ax = Math.abs(x);
                for (int y = -12; y <= -6; y++) {
                    if (ax == 3 || y == -12 || y == -6) p.fill(x, y, z, ROCK.at(x, y, z), false);
                    else if (ax == 2) p.set(x, y, z, lebanonWall(-11).at(x, y, z));
                    else if (y == -11) p.set(x, y, z, concreteFloor().at(x, y, z));
                    else if (y == -7) p.set(x, y, z, CREAM);
                    else p.set(x, y, z, St.AIR);
                }
            }
        }
        wallLight(p, -2, -9, -13, "east", HIDDEN_LIGHT);
    }

    // --- the rotunda ------------------------------------------------------------------------------------------------------

    /** The octagon's measure: inside the rotunda's open floor while {@code ≤ ROTUNDA_R} (14), the gallery's edge at 10. */
    public static double m(int x, int z) {
        int dx = Math.abs(x), dz = Math.abs(z - CZ);
        return Math.max(Math.max(dx, dz), (dx + dz) * 7 / 10.0);
    }

    private static boolean gallery(int x, int z) {
        return m(x, z) > 9.5 && m(x, z) <= 14;
    }

    private static boolean voidCell(int x, int z) {
        return m(x, z) <= 9.5;
    }

    /** A gallery cell touching the open middle (also corner to corner, so the chamfers' edges join up): railing, fascia, columns. */
    private static boolean edge(int x, int z) {
        if (!gallery(x, z)) return false;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (voidCell(x + dx, z + dz)) return true;
        return false;
    }

    /** The eight columns, at the inner octagon's corners. */
    private static final int[][] COLUMNS = {{10, CZ - 4}, {10, CZ + 4}, {-10, CZ - 4}, {-10, CZ + 4}, {4, CZ - 10}, {-4, CZ - 10}, {4, CZ + 10}, {-4, CZ + 10}};

    /** The flights' stairwells in the gallery floor: x -1..6, three lanes from the wall side to the open side. */
    private static final int FLIGHT_X0 = -1, FLIGHT_STEPS = 8;

    private static boolean stairwell(int x, int z) {
        if (x < FLIGHT_X0 || x > FLIGHT_X0 + FLIGHT_STEPS - 1) return false;
        int dz = z - CZ;
        return dz >= 10 && dz <= 12 || dz <= -10 && dz >= -12;
    }

    private static void rotunda(Plan p) {
        Pattern floor = concreteFloor(), lower = lebanonWall(F1), upper = creamWall(FG);
        Pattern plenum = (x, y, z) -> Math.floorMod(x, 3) == 1 && Math.floorMod(z, 3) == 1 ? HIDDEN_LIGHT : "minecraft:smooth_stone";
        for (int x = -16; x <= 16; x++) {
            for (int z = -46; z <= -14; z++) {
                double m = m(x, z);
                if (m > 16) continue;
                for (int y = -21; y <= -1; y++) {
                    String s;
                    if (m > 15 || y <= F1 - 1 || y >= CEILING + 1) s = ROCK.at(x, y, z);
                    else if (m > 14) {
                        // The wall's finish: tile and cream below the gallery, cream above, the ceiling's edge.
                        if (y == F1) s = FLOOR_BORDER;
                        else if (y < FG) s = lower.at(x, y, z);
                        else if (y == FG) s = TILE_CAP;
                        else if (y < GRATE_Y) s = upper.at(x, y, z);
                        else s = CREAM;
                    } else if (y == F1) s = m > 13 ? FLOOR_BORDER : floor.at(x, y, z);
                    else if (y < FG) s = St.AIR;
                    else if (y == FG) s = gallery(x, z) ? floor.at(x, y, z) : St.AIR;
                    else if (y < GRATE_Y) s = St.AIR;
                    else if (y == GRATE_Y) s = gallery(x, z) ? GRATE : St.AIR;
                    else if (y == PLENUM) s = gallery(x, z) ? plenum.at(x, y, z) : St.AIR;
                    else s = gallery(x, z) ? "minecraft:smooth_stone" : (Math.floorMod(x, 4) == 0 || Math.floorMod(z - CZ, 4) == 0 ? BLACK : CREAM);
                    p.set(x, y, z, s);
                }
            }
        }
        galleryEdge(p);
        gratingBeams(p);
        flights(p);
        balcony(p);
        for (int[] c : COLUMNS) {
            column(p, c[0], c[1], F1 + 1, GRATE_Y - 1, IRON_COLUMN, BLACK, BLACK_CHISELED, BLACK_STAIRS);
            p.set(c[0], FG - 1, c[1], BLACK_CHISELED);
        }
        lightingRig(p);
        mapTable(p);
        doorways(p);
        screens(p);
        lowerWalls(p);
        upperWalls(p);
        p.view("war_room", 0, F1 + 1, -22, 0, -13, -44);
        p.view("balcony", -2, FG + 1, -22, 0, -17, -30);
        p.view("map_table", 3, F1 + 1, -26, 0, -17.5, -29.5);
        p.view("gallery", 12, FG + 1, -36, -6, -12, -24);
    }

    /** The gallery's edge: a cream fascia with a moulding under it, the railing on it (posts every few bars). */
    private static void galleryEdge(Plan p) {
        for (int x = -14; x <= 14; x++) {
            for (int z = CZ - 14; z <= CZ + 14; z++) {
                if (!edge(x, z) || stairwell(x, z)) continue;
                p.set(x, FG, z, CREAM);
                p.set(x, FG - 1, z, CREAM_COURSE);
                p.set(x, FG + 1, z, (x + z & 3) == 0 ? St.wall(BLACK_WALL) : St.bars());
                for (String d : St.HORIZONTAL) {
                    int vx = x + St.dx(d), vz = z + St.dz(d);
                    if (voidCell(vx, vz) && p.isAir(vx, FG - 1, vz)) p.set(vx, FG - 1, vz, St.stairs(CREAM_STAIRS, St.opposite(d), true));
                }
            }
        }
    }

    /** Black beams in the grate: a ring over the edge and spokes from each column to the wall; the hanging lamps under the gallery. */
    private static void gratingBeams(Plan p) {
        for (int x = -14; x <= 14; x++) {
            for (int z = CZ - 14; z <= CZ + 14; z++) {
                if (edge(x, z)) p.set(x, GRATE_Y, z, BLACK);
            }
        }
        for (int[] c : COLUMNS) {
            int sx = Integer.signum(c[0]), sz = Integer.signum(c[1] - CZ);
            boolean alongX = Math.abs(c[0]) == 10;
            for (int k = 0; k <= 5; k++) {
                int x = alongX ? c[0] + sx * k : c[0], z = alongX ? c[1] : c[1] + sz * k;
                if (gallery(x, z)) p.set(x, GRATE_Y, z, BLACK);
            }
        }
        // Lamps hung under the gallery floor, round the lower storey.
        for (int x = -13; x <= 13; x++) {
            for (int z = CZ - 13; z <= CZ + 13; z++) {
                if (!gallery(x, z) || edge(x, z) || stairwell(x, z) || m(x, z) > 12.6) continue;
                if (Math.floorMod(x + 2 * (z - CZ), 7) != 0) continue;
                p.set(x, FG - 1, z, St.lantern(true));
            }
        }
    }

    /**
     * The two flights: the entrance stairs down from the balcony (south) and the stairs up at the back (north), each 8 steps
     * east from x -1, three lanes wide (the open lane carries chain balusters), with a smooth soffit; the gallery round their
     * wells railed.
     */
    private static void flights(Plan p) {
        for (int side : new int[]{1, -1}) {
            int wallLane = CZ + side * 12, openLane = CZ + side * 10;
            String across = side > 0 ? "north" : "south";
            for (int x = FLIGHT_X0; x < FLIGHT_X0 + FLIGHT_STEPS; x++) {
                for (int z = Math.min(wallLane, openLane); z <= Math.max(wallLane, openLane); z++) p.set(x, FG, z, St.AIR);
            }
            flight(p, FLIGHT_X0, wallLane, FG, "east", across, 3, FLIGHT_STEPS, STEEL_STAIRS, STEEL_STAIRS);
            balusters(p, FLIGHT_X0, openLane, FG, "east", FLIGHT_STEPS);
            // Railing round the well on the gallery: along the wall side and across its far end.
            List<int[]> run = new ArrayList<>();
            for (int x = FLIGHT_X0; x <= FLIGHT_X0 + FLIGHT_STEPS; x++) run.add(new int[]{x, wallLane + side});
            for (int k = 0; k <= 2; k++) run.add(new int[]{FLIGHT_X0 + FLIGHT_STEPS, wallLane - side * k});
            railing(p, FG + 1, run, BLACK_WALL, 4);
            // Newel posts at the stair's head and foot.
            p.set(FLIGHT_X0 - 1, FG + 1, openLane, St.wall(BLACK_WALL));
            p.set(FLIGHT_X0 + FLIGHT_STEPS, F1 + 1, openLane, St.wall(BLACK_WALL));
            p.set(FLIGHT_X0 + FLIGHT_STEPS, F1 + 2, openLane, St.lantern(false));
        }
    }

    /** The balcony the tunnel opens on: a bay pushed out over the room west of the entrance stairs, railed on three sides. */
    private static void balcony(Plan p) {
        int z0 = CZ + 7, z1 = CZ + 9;
        for (int x = -6; x <= -1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, FG, z, STEEL_TILES);
                p.set(x, FG - 1, z, St.stairs(STEEL_STAIRS, "south", true));
            }
        }
        for (int x = -5; x <= -2; x++) {
            p.set(x, FG + 1, CZ + 10, St.AIR);
            p.set(x, FG, CZ + 10, STEEL_TILES);
        }
        List<int[]> run = new ArrayList<>();
        run.add(new int[]{-6, z1});
        run.add(new int[]{-6, z1 - 1});
        for (int x = -6; x <= -1; x++) run.add(new int[]{x, z0});
        run.add(new int[]{-1, z0 + 1});
        run.add(new int[]{-1, z1});
        railing(p, FG + 1, run, BLACK_WALL, 0);
        p.set(-6, FG + 2, z0, St.lantern(false));
        p.set(-1, FG + 2, z0, St.lantern(false));
    }

    /** The rig of studio lamps over the table, and a long pendant down to it. */
    private static void lightingRig(Plan p) {
        for (int x = -4; x <= 4; x++) {
            for (int z = CZ - 4; z <= CZ + 4; z++) {
                if (Math.max(Math.abs(x), Math.abs(z - CZ)) == 4) p.set(x, GRATE_Y, z, St.bars());
            }
        }
        for (int[] c : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) p.set(c[0], PLENUM, CZ + c[1], St.chain("y"));
        for (int[] c : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}, {0, -4}, {0, 4}, {-4, 0}, {4, 0}}) {
            p.set(c[0], GRATE_Y - 1, CZ + c[1], St.bulb("minecraft:waxed_copper_bulb"));
            p.set(c[0], GRATE_Y - 2, CZ + c[1], St.trapdoor("minecraft:iron_trapdoor", "north", true, false));
        }
        pendant(p, 0, CEILING, CZ, 8, false);
    }

    /** The lit map table, its six chairs, Henry's place at its west end, the order's emblem set in the floor before it. */
    private static void mapTable(Plan p) {
        for (int[] t : MAP_TABLES) p.set(t[0], t[1], t[2], TABLE);
        for (int x : new int[]{-1, 1}) {
            chair(p, x, F1 + 1, CZ - 1, "south", STEEL_STAIRS, null);
            chair(p, x, F1 + 1, CZ + 2, "north", STEEL_STAIRS, null);
        }
        chair(p, 2, F1 + 1, CZ, "west", STEEL_STAIRS, null);
        chair(p, 2, F1 + 1, CZ + 1, "west", STEEL_STAIRS, null);
        int ex = EMBLEM[0], ez = EMBLEM[2];
        p.set(ex, F1, ez, EMBLEM_ID);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) p.set(ex + dx, F1, ez + dz, BLACK);
        for (String d : St.HORIZONTAL) p.set(ex + 2 * St.dx(d), F1, ez + 2 * St.dz(d), "minecraft:chiseled_tuff_bricks");
    }

    /** The doorways: to the library and the archive on both storeys, and north to the stairwell. */
    private static void doorways(Plan p) {
        for (int side : new int[]{-1, 1}) {
            String out = side < 0 ? "west" : "east";
            doorway(p, side * 15, F1, CZ, out, 2, 3, 4, BLACK, BLACK_STAIRS, null);
            doorway(p, side * 15, FG, CZ, out, 2, 3, 3, BLACK, BLACK_STAIRS, null);
            p.air(side * 14, side * 14, F1 + 1, F1 + 4, CZ - 1, CZ + 1);
            p.air(side * 14, side * 14, FG + 1, FG + 3, CZ - 1, CZ + 1);
            for (int z = CZ - 1; z <= CZ + 1; z++) p.set(side * 14, FG, z, floorAt(side * 14, z));
            sign(p, side * 14, F1 + 5, CZ - 2, St.opposite(out), "dark_oak", "yellow", "", side < 0 ? "Library" : "Archive", "", "");
        }
        doorway(p, 0, F1, CZ - 15, "north", 2, 3, 4, BLACK, BLACK_STAIRS, null);
        sign(p, 0, F1 + 6, CZ - 14, "south", "dark_oak", "yellow", "", "Stairs", "Levels 2 - 3", "");
        // The tunnel's mouth on the south gallery.
        doorway(p, 0, FG, CZ + 15, "south", 2, 3, 3, BLACK, BLACK_STAIRS, null);
    }

    private static String floorAt(int x, int z) {
        return concreteFloor().at(x, FG, z);
    }

    /** Back-lit art-deco glass either side of the passage north (as the screens behind the set's back stairs). */
    private static void screens(Plan p) {
        for (int sx : new int[]{-1, 1}) {
            for (int i = 2; i <= 6; i++) {
                int x = sx * i;
                for (int y = F1 + 1; y <= F1 + 6; y++) {
                    boolean frame = i == 2 || i == 6 || y == F1 + 1 || y == F1 + 6;
                    if (frame) {
                        p.set(x, y, CZ - 15, BLACK);
                    } else {
                        p.set(x, y, CZ - 15, (i == 4 && (y - F1) % 2 == 0) ? St.bars() : St.pane("minecraft:gray_stained_glass_pane"));
                        p.set(x, y, CZ - 16, COOL_LIGHT);
                    }
                }
            }
        }
    }

    /** The lower storey's walls: lamps in the dado, the gauges and filing cabinets of the set. */
    private static void lowerWalls(Plan p) {
        int y = F1 + 4;
        for (int z : new int[]{CZ - 5, CZ + 5}) {
            wallLight(p, -15, y, z, "east", HIDDEN_LIGHT);
            wallLight(p, 15, y, z, "west", HIDDEN_LIGHT);
        }
        wallLight(p, -5, y, CZ + 15, "north", HIDDEN_LIGHT);
        // Gauges on the tile by the west door: clocks and compasses in frames.
        String[] gauges = {"minecraft:clock", "minecraft:compass", "minecraft:clock", "minecraft:recovery_compass", "minecraft:compass", "minecraft:clock"};
        for (int i = 0; i < 6; i++) {
            int z = CZ + 3 + i % 3, yy = F1 + 2 + i / 3;
            p.decor(Decor.frame(-14, yy, z, "east", gauges[i]));
        }
        for (int i = 0; i < 4; i++) p.decor(Decor.frame(14, F1 + 2 + i / 2, CZ - 4 - i % 2, "west", i % 2 == 0 ? "minecraft:clock" : "minecraft:compass"));
        // Filing cabinets (droppers: grey drawers) against the walls either side of the east door; a desk with a typewriter-lamp.
        for (int z : new int[]{CZ + 3, CZ + 4, CZ + 6}) {
            p.set(14, F1 + 1, z, St.of("minecraft:dropper", "facing", "west", "triggered", "false"));
            p.set(14, F1 + 2, z, St.of("minecraft:dropper", "facing", "west", "triggered", "false"));
        }
        loot(p, 14, F1 + 1, CZ + 5, St.of("minecraft:barrel", "facing", "west", "open", "false"), "supernaturalcraft:chests/bunker_archive");
        p.set(14, F1 + 2, CZ + 5, St.candle("white", 3, true));
        for (int z : new int[]{CZ - 3, CZ - 4, CZ - 6}) {
            p.set(-14, F1 + 1, z, St.of("minecraft:dropper", "facing", "east", "triggered", "false"));
            p.set(-14, F1 + 2, z, St.of("minecraft:dropper", "facing", "east", "triggered", "false"));
        }
        // A wall phone and a fire bucket by the north passage.
        p.set(-3, F1 + 1, CZ - 14, "minecraft:cauldron");
        p.set(3, F1 + 1, CZ - 14, St.of("minecraft:lectern", "facing", "south", "has_book", "true", "powered", "false"));
    }

    /** The gallery's walls: the order's banners over the four doors' sides, sconces, two armchairs and a reading lamp. */
    private static void upperWalls(Plan p) {
        int y = FG + 4;
        banner(p, -14, y, CZ - 3, "east", "black", ORDER_BANNER);
        banner(p, -14, y, CZ + 3, "east", "black", ORDER_BANNER);
        banner(p, 14, y, CZ - 3, "west", "black", ORDER_BANNER);
        banner(p, 14, y, CZ + 3, "west", "black", ORDER_BANNER);
        banner(p, -3, y, CZ - 14, "south", "black", ORDER_BANNER);
        banner(p, 3, y, CZ - 14, "south", "black", ORDER_BANNER);
        for (int sx : new int[]{-6, 6}) {
            wallLight(p, sx, FG + 3, CZ - 15, "south", HIDDEN_LIGHT);
            wallLight(p, sx, FG + 3, CZ + 15, "north", HIDDEN_LIGHT);
        }
        // A reading corner on the north-west gallery.
        chair(p, -9, FG + 1, CZ - 8, "south", OAK_STAIRS, OAK_TRAPDOOR);
        chair(p, -7, FG + 1, CZ - 10, "east", OAK_STAIRS, null);
        p.set(-9, FG + 1, CZ - 10, St.fence(OAK_FENCE));
        p.set(-9, FG + 2, CZ - 10, St.lantern(false));
        p.decor(Decor.painting(14, FG + 2, CZ - 5, "west", "wanderer"));
        p.decor(Decor.painting(-14, FG + 2, CZ + 5, "east", "graham"));
    }
}
