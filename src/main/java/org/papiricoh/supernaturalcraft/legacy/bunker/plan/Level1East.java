package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;
import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level1.*;

/**
 * East of the war room (pure): the records room on the floor and Henry's study above it at gallery level.
 *
 * <p>The <b>records room</b> is in the Lebanon finish (concrete, green tile, cream). A runner of steel tiles leads from the door to
 * the clerk's desk at the far wall, with its typewriter, service bell and pin board. Ten stacks stand either side of it,
 * back to back, in segments: grey filing drawers, files, books and boxes. Their end panels are labelled ("Case Files" by decade on
 * one side, the bestiary on the other). Map drawers and filing cabinets line the west wall, and lanterns hang over every aisle.
 *
 * <p><b>Henry's study</b> has dark oak panelling under green wallpaper and a beamed spruce ceiling. His desk faces the door, with
 * a leather chair and two visitors' chairs, and the order's banner hangs behind it. There is a brick fireplace with two oxblood
 * armchairs, a rug, a globe, a record player, the drinks sideboard, a long-case clock, and photographs on the walls.
 */
final class Level1East {

    private static final int F = Zones.LEVEL1, M = Zones.GALLERY, CZ = Zones.ROTUNDA_Z;
    /** The records room's inside. */
    static final int X0 = 19, X1 = 44, Z0 = -46, Z1 = -14, H = 6, CEIL = F + H + 1;
    /** The stacks: five pairs from x 25, every 4; north of the central aisle z -44..-32, south z -28..-16 (end panels inward). */
    static final int SX = 25, STACKS = 5, N0 = -44, N1 = -32, S0 = -28, S1 = -16;
    /** Henry's study's inside (floor at the gallery's). */
    static final int SX0 = 19, SX1 = 31, SZ0 = -37, SZ1 = -23, SH = 6;

    private static final String DROPPER = "minecraft:dropper", TABLE_SLAB = "minecraft:spruce_slab";
    private static final String[] DECADES = {"1910 - 1919", "1920 - 1929", "1930 - 1939", "1940 - 1949", "1950 - 1958"};
    private static final String[] BESTIARY = {"Demons", "Spirits", "Shifters", "Vampires", "Witches"};

    private Level1East() {
    }

    static void build(Plan p) {
        records(p);
        study(p);
        p.view("archive", 21, F + 1, CZ, 44, -16.5, CZ);
        p.view("archive_stacks", 27, F + 1, -44, 28, -16, -32);
        p.view("henrys_study", 20, M + 1, -35, 29, -8.5, -26);
    }

    // --- the records room -----------------------------------------------------------------------------------------------------

    private static void records(Plan p) {
        Pattern concrete = concreteFloor();
        Pattern floor = (x, y, z) -> x >= X0 && x <= X1 && z == CZ ? STEEL_TILES : x >= X0 && x <= X1 && Math.abs(z - CZ) == 1 ? BLACK
                : concrete.at(x, y, z);
        Pattern ceiling = (x, y, z) -> x >= SX && x < SX + 4 * STACKS && Math.floorMod(x - SX, 4) < 2 ? STEEL : "minecraft:smooth_stone";
        room(p, new Room(X0, X1, F, H, Z0, Z1), new Style(floor, lebanonWall(F), ceiling, CREAM_STAIRS, null, 0, null, 0));
        doorway(p, X0 - 2, F, CZ, "east", 2, 3, 4, BLACK, BLACK_STAIRS, null);
        for (int x = X0 - 2; x <= X0 - 1; x++) for (int z = CZ - 1; z <= CZ + 1; z++) p.set(x, F, z, STEEL_TILES);
        stacks(p);
        westWall(p);
        clerksDesk(p);
        recordsLights(p);
    }

    /**
     * The stacks, two blocks deep (each half faces its own aisle), four high, in segments of three that cycle drawers (droppers),
     * files (archive shelves), books (chiseled shelves) and boxes (barrels); a dark oak end panel with a label toward the central
     * aisle; boxes and paper stacked on some tops. Three of the boxes hold the archive's loot.
     */
    private static void stacks(Plan p) {
        List<int[]> boxes = new ArrayList<>();
        for (int k = 0; k < STACKS; k++) {
            for (int half = 0; half < 2; half++) {
                int x = SX + 4 * k + half;
                String face = half == 0 ? "west" : "east";
                stackRun(p, x, N0, N1 - 1, face, k, boxes);
                stackRun(p, x, S0 + 1, S1, face, k + 2, boxes);
                for (int y = F + 1; y <= F + 4; y++) {
                    p.set(x, y, N1, OAK);
                    p.set(x, y, S0, OAK);
                }
                p.set(x, F + 5, N1, St.stairs(OAK_STAIRS, "south", false));
                p.set(x, F + 5, S0, St.stairs(OAK_STAIRS, "north", false));
            }
            sign(p, SX + 4 * k, F + 3, N1 + 1, "south", "dark_oak", "black", "", "Case Files", DECADES[k], "");
            sign(p, SX + 4 * k + 1, F + 3, S0 - 1, "north", "dark_oak", "black", "", "Bestiary", BESTIARY[k], "");
        }
        for (int i : new int[]{0, boxes.size() / 2, boxes.size() - 1}) {
            int[] b = boxes.get(i);
            loot(p, b[0], b[1], b[2], p.get(b[0], b[1], b[2]), "supernaturalcraft:chests/bunker_archive");
        }
    }

    private static void stackRun(Plan p, int x, int za, int zb, String face, int salt, List<int[]> boxes) {
        for (int z = za; z <= zb; z++) {
            int type = (salt + (z - za) / 3) % 4;
            for (int y = F + 1; y <= F + 4; y++) {
                int h = y - F;
                String s = switch (type) {
                    case 0 -> h <= 3 ? St.of("dropper", "facing", face, "triggered", "false") : SHELF;
                    case 1 -> SHELF;
                    case 2 -> h <= 2 ? chiseled(face, x, y, z) : SHELF;
                    default -> h <= 2 ? barrel(face) : SHELF;
                };
                p.set(x, y, z, s);
                if (type == 3 && h == 2) boxes.add(new int[]{x, y, z});
            }
            double n = Plan.noise(x, F + 5, z, 61);
            if (n < 0.22) p.set(x, F + 5, z, barrel("up"));
            else if (n < 0.32) p.set(x, F + 5, z, St.carpet("white"));
        }
    }

    /** The west wall either side of the door: filing cabinets three high, map drawers (cartography tables), maps pinned above. */
    private static void westWall(Plan p) {
        for (int z = Z0; z <= Z1; z++) {
            if (Math.abs(z - CZ) <= 2) continue;
            boolean maps = z >= -38 && z <= -35 || z >= -25 && z <= -22;
            for (int y = F + 1; y <= F + 3; y++) {
                p.set(X0, y, z, y == F + 3 ? SHELF : maps ? "minecraft:cartography_table" : St.of("dropper", "facing", "east", "triggered", "false"));
            }
            p.set(X0, F + 4, z, St.stairs(TILE_CAP_STAIRS, "west", true));
            if (maps) p.decor(Decor.frame(X0, F + 5, z, "east", "minecraft:map"));
        }
        sign(p, X0, F + 5, CZ, "east", "dark_oak", "yellow", "", "Records", "& Archive", "");
    }

    /** The clerk's desk across the far end of the runner: typewriter, service bell, lamp, chair, and a pin board on the wall. */
    private static void clerksDesk(Plan p) {
        int x = X1 - 1;
        table(p, x, x, F + 1, CZ - 1, CZ + 1, TABLE_SLAB);
        p.set(x, F + 2, CZ - 1, St.of("bell", "attachment", "floor", "facing", "west", "powered", "false"));
        p.set(x, F + 2, CZ, St.of("heavy_core", "waterlogged", "false"));
        p.set(x, F + 2, CZ + 1, St.lantern(false));
        chair(p, X1, F + 1, CZ, "west", "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        String[] pins = {"minecraft:map", "minecraft:paper", "minecraft:writable_book", "minecraft:compass", "minecraft:clock", "minecraft:paper"};
        for (int i = 0; i < 6; i++) p.decor(Decor.frame(X1, F + 4 + i / 3, CZ - 1 + i % 3, "west", pins[i]));
        // A trolley of boxes and a step stool by the desk.
        p.set(X1, F + 1, CZ - 4, barrel("up"));
        p.set(X1, F + 2, CZ - 4, St.carpet("white"));
        p.set(X1, F + 1, CZ + 4, St.of("composter", "level", "0"));
    }

    /** Lanterns hung over every aisle, the central one and the ends; lamps behind louvres in the north and south walls. */
    private static void recordsLights(Plan p) {
        int y = CEIL;
        for (int k = 0; k < STACKS - 1; k++) {
            int x = SX + 4 * k + 2 + (k & 1);
            for (int z : new int[]{-41, -35, -25, -19}) pendant(p, x, y, z, 1, false);
        }
        for (int z : new int[]{-41, -35, -25, -19}) {
            pendant(p, 23, y, z, 1, false);
            pendant(p, X1 - 1, y, z, 1, false);
        }
        for (int x : new int[]{22, 28, 32, 36, 40}) pendant(p, x, y, CZ, 1, false);
        for (int x = 22; x <= 42; x += 5) {
            wallLight(p, x, F + 4, Z0 - 1, "south", HIDDEN_LIGHT);
            wallLight(p, x, F + 4, Z1 + 1, "north", HIDDEN_LIGHT);
        }
    }

    // --- Henry's study ----------------------------------------------------------------------------------------------------------

    private static void study(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - M;
            return off <= 2 ? OAK : off == 3 ? "minecraft:stripped_dark_oak_wood" : "minecraft:green_terracotta";
        };
        Pattern ceiling = (x, y, z) -> Math.floorMod(x, 3) == 0 ? St.axis("minecraft:stripped_spruce_log", "z") : "minecraft:spruce_planks";
        room(p, new Room(SX0, SX1, M, SH, SZ0, SZ1), new Style(parquet("minecraft:oak_planks", "minecraft:spruce_planks", false), wall, ceiling,
                OAK_STAIRS, null, 0, null, 0));
        doorway(p, SX0 - 2, M, CZ, "east", 2, 3, 3, OAK, OAK_STAIRS, null);
        for (int x = SX0 - 2; x <= SX0 - 1; x++) for (int z = CZ - 1; z <= CZ + 1; z++) p.set(x, M, z, St.axis(OAK_LOG, "x"));
        rug(p);
        desk(p);
        fireplace(p);
        northWall(p);
        sideWalls(p);
        for (int x = 21; x <= 30; x += 3) for (int z = SZ0; z <= SZ1; z++) p.set(x, M + SH, z, St.axis(OAK_LOG, "z"));
        pendant(p, 25, M + SH + 1, CZ, 2, false);
    }

    /** A rug in the middle: a black border, a red field with an orange line inset. */
    private static void rug(Plan p) {
        int xa = 21, xb = 27, za = -34, zb = -26;
        for (int x = xa; x <= xb; x++) {
            for (int z = za; z <= zb; z++) {
                int d = Math.min(Math.min(x - xa, xb - x), Math.min(z - za, zb - z));
                p.set(x, M + 1, z, St.carpet(d == 0 ? "black" : d == 2 ? "orange" : "red"));
            }
        }
    }

    /** Henry's desk facing the door: a top between two pedestals, lamp and candle, his chair, two visitors' chairs, the globe. */
    private static void desk(Plan p) {
        int dx = 28, y = M + 1;
        p.set(dx, y, CZ - 1, St.stairs(OAK_STAIRS, "north", true));
        p.set(dx, y, CZ, St.slab(OAK_SLAB, true));
        p.set(dx, y, CZ + 1, St.stairs(OAK_STAIRS, "south", true));
        p.set(dx, y + 1, CZ - 1, St.lantern(false));
        p.set(dx, y + 1, CZ + 1, St.candle("green", 1, true));
        chair(p, dx + 1, y, CZ, "west", "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        chair(p, dx - 2, y, CZ - 1, "east", "minecraft:red_nether_brick_stairs", null);
        chair(p, dx - 2, y, CZ + 1, "east", "minecraft:red_nether_brick_stairs", null);
        p.set(29, y, -35, St.of("lightning_rod", "facing", "up", "powered", "false", "waterlogged", "false"));
        p.set(29, y + 1, -35, St.of("light_blue_glazed_terracotta", "facing", "north"));
        // Behind him: bookcases either side of a low credenza, the order's banner over it, photographs above the shelves.
        bookcases(p, SX1, SZ0 + 1, SX1, -33, y, 3, "west", OAK_STAIRS);
        bookcases(p, SX1, -27, SX1, SZ1 - 1, y, 3, "west", OAK_STAIRS);
        for (int z = CZ - 2; z <= CZ + 2; z++) p.set(SX1, y, z, z == CZ ? barrel("west") : OAK);
        loot(p, SX1, y, CZ, barrel("west"), "supernaturalcraft:chests/bunker_library");
        p.set(SX1, y + 1, CZ - 2, pot("west"));
        p.set(SX1, y + 1, CZ + 2, St.candle("white", 3, true));
        p.set(SX1, y + 1, CZ - 1, "minecraft:potted_dead_bush");
        banner(p, SX1, M + 4, CZ, "west", "black", ORDER_BANNER);
        String[] photos = {"kebab", "aztec", "alban", "bomb"};
        int[] zs = {-35, -33, -27, -25};
        for (int i = 0; i < 4; i++) p.decor(Decor.painting(SX1, M + 5, zs[i], "west", photos[i]));
    }

    /** The fireplace on the south wall: a brick chimney breast, a fire in its opening, a hearth, the mantel; two armchairs before it. */
    private static void fireplace(Plan p) {
        int fx = 25, z = SZ1, y = M + 1;
        for (int x = fx - 2; x <= fx + 2; x++) {
            for (int yy = y; yy <= M + SH; yy++) p.set(x, yy, z, "minecraft:bricks");
            p.set(x, M, z - 1, BLACK);
            p.set(x, M + 3, z - 1, St.stairs(OAK_STAIRS, "south", true));
        }
        for (int x = fx - 1; x <= fx + 1; x++) {
            p.set(x, M, z, BLACK);
            for (int yy = y; yy <= y + 1; yy++) p.set(x, yy, z, St.AIR);
            p.set(x, y, z + 1, SKIRTING);
            p.set(x, y + 1, z + 1, SKIRTING);
        }
        p.set(fx, y, z, St.of("campfire", "facing", "north", "lit", "true", "signal_fire", "false", "waterlogged", "false"));
        p.set(fx - 2, M + 4, z - 1, St.candle("white", 2, true));
        p.set(fx + 2, M + 4, z - 1, St.candle("white", 2, true));
        p.set(fx, M + 4, z - 1, pot("north"));
        p.decor(Decor.frame(fx, M + 5, z - 1, "north", "minecraft:clock"));
        chair(p, fx - 2, y, z - 3, "south", "minecraft:red_nether_brick_stairs", "minecraft:mangrove_trapdoor");
        chair(p, fx + 2, y, z - 3, "south", "minecraft:red_nether_brick_stairs", "minecraft:mangrove_trapdoor");
        p.set(fx, y, z - 3, St.slab(OAK_SLAB, true));
        p.set(fx, y + 1, z - 3, "minecraft:potted_fern");
    }

    /** The north wall: the record player with a disc framed over it, the drinks sideboard with bottles above, the long-case clock. */
    private static void northWall(Plan p) {
        int z = SZ0, y = M + 1;
        p.set(21, y, z, St.of("jukebox", "has_record", "false"));
        p.decor(Decor.frame(21, y + 1, z, "south", "minecraft:music_disc_cat"));
        for (int x = 24; x <= 27; x++) p.set(x, y, z, x == 24 || x == 27 ? OAK : barrel("south"));
        p.set(24, y + 1, z, St.candle("white", 1, true));
        p.set(25, y + 1, z, St.of("brewing_stand", "has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "false"));
        p.set(26, y + 1, z, pot("south"));
        p.set(27, y + 1, z, "minecraft:potted_red_tulip");
        String[] bottles = {"minecraft:potion", "minecraft:honey_bottle", "minecraft:glass_bottle", "minecraft:experience_bottle"};
        for (int i = 0; i < 4; i++) p.decor(Decor.frame(24 + i, y + 2, z, "south", bottles[i]));
        for (int yy = y; yy <= y + 2; yy++) p.set(29, yy, z, OAK);
        p.set(29, y + 3, z, St.stairs(OAK_STAIRS, "north", true));
        p.decor(Decor.frame(29, y + 2, z + 1, "south", "minecraft:clock"));
    }

    /** The west wall either side of the door: low bookcases with lanterns on them, sconces, pictures over the fireplace's sides. */
    private static void sideWalls(Plan p) {
        int y = M + 1;
        bookcases(p, SX0, SZ0, SX0, -33, y, 2, "east", OAK_STAIRS);
        bookcases(p, SX0, -27, SX0, SZ1, y, 2, "east", OAK_STAIRS);
        p.set(SX0, y + 3, -35, St.lantern(false));
        p.set(SX0, y + 3, -25, St.lantern(false));
        p.decor(Decor.painting(SX0, M + 4, -31, "east", "wanderer"));
        p.decor(Decor.painting(21, M + 2, SZ1, "north", "graham"));
        p.decor(Decor.painting(29, M + 2, SZ1, "north", "prairie_ride"));
    }
}
