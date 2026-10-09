package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;
import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level2Props.*;

/**
 * The dormitories (pure): eight numbered rooms north of the corridor, each a variation on the bunker's bedroom as the series
 * shows it: plain concrete walls with one wall of bare brick, a grid of concrete beams under a dark ceiling with lights set
 * between them, a low ledge along the back wall, a wooden bed in the middle of it with a blanket chest at its foot, a
 * nightstand and a radio, a desk with its lamp and chair, a chest of drawers, a leather settee with wooden arms against the
 * brick, framed landscapes and a floor lamp. Who sleeps there changes the rest: Dean's guns and records, Sam's books, Mary's
 * flowers, Castiel's bees and coat, Charlie's computer, Jack's stars, Bobby's den, and the hunters' bunk room.
 *
 * <p>Each room is 7 × 8 inside, 5 high (the beams take the top course), its door in the middle of the corridor side.
 */
final class Level2Quarters {

    private static final int F = Level2.F, Y = F + 1, Z0 = -52, Z1 = -45;

    private Level2Quarters() {
    }

    /**
     * A room's frame of reference: {@code rx} 0..6 from its plain side wall to its brick wall (mirrored when {@code flip}),
     * {@code rz} 0..7 from the back wall to the door.
     */
    private record Dorm(int a, boolean flip) {
        int x(int rx) {
            return flip ? a + 6 - rx : a + rx;
        }

        static int z(int rz) {
            return Z0 + rz;
        }

        /** Toward the brick wall. */
        String brick() {
            return flip ? "west" : "east";
        }

        /** Toward the plain wall. */
        String plain() {
            return flip ? "east" : "west";
        }

        int brickWall() {
            return flip ? a - 1 : a + 7;
        }

        int plainWall() {
            return flip ? a + 7 : a - 1;
        }
    }

    /** A room's finish: concrete, skirting, beams, the ceiling between them, the brick, the floor, the wood, bed and settee. */
    private record Look(String concrete, String skirting, String beam, Pattern panel, Pattern brick, Pattern floor, String wood,
                        String bed, String settee) {
    }

    static void build(Plan p) {
        dean(p, new Dorm(8, false));
        jack(p, new Dorm(17, true));
        bobby(p, new Dorm(26, false));
        bunks(p, new Dorm(35, true));
        sam(p, new Dorm(-14, true));
        mary(p, new Dorm(-23, false));
        castiel(p, new Dorm(-32, true));
        charlie(p, new Dorm(-41, false));
    }

    // --- the shell ---------------------------------------------------------------------------------------------------------

    private static void shell(Plan p, Dorm d, Look l, String number, String light) {
        Pattern wall = (x, y, z) -> {
            if (x == d.brickWall() && z >= Z0 && z <= Z1) return l.brick.at(x, y, z);
            return y - F == 1 ? l.skirting : l.concrete;
        };
        room(p, new Room(d.a, d.a + 6, F, 5, Z0, Z1), new Style(l.floor, wall, l.panel, null, null, 0, null, 0));
        // The beam grid: a ring beam round the top, beams across at the third points.
        for (int rx = 0; rx <= 6; rx++) {
            for (int rz = 0; rz <= 7; rz++) {
                if (rx == 0 || rx == 3 || rx == 6 || rz == 0 || rz == 4 || rz == 7) p.set(d.x(rx), F + 5, Dorm.z(rz), l.beam);
            }
        }
        // A light set in the back panels.
        p.set(d.x(1), F + 6, Dorm.z(2), light);
        p.set(d.x(5), F + 6, Dorm.z(5), light);
        // The door off the corridor and its number over it.
        doorway(p, d.a + 3, F, -43, "north", 2, 1, 2, BLACK, null, St.door(OAK_DOOR, "north", d.flip ? "right" : "left", false));
        sign(p, d.a + 3, F + 4, -42, "south", "dark_oak", "yellow", "", number, "", "");
    }

    // --- the bedroom's pieces ---------------------------------------------------------------------------------------------

    /** The low ledge along the back wall. */
    private static void ledge(Plan p, Dorm d, String block) {
        for (int rx = 0; rx <= 6; rx++) p.set(d.x(rx), Y, Dorm.z(0), block);
    }

    /** The bed in the middle of the back wall (head on the ledge, a wooden headboard over it) and the blanket chest at its foot. */
    private static void bed(Plan p, Dorm d, Look l, int rx) {
        Level2Props.bed(p, d.x(rx), Y, Dorm.z(2), l.bed, "north");
        p.set(d.x(rx), Y + 1, Dorm.z(0), St.trapdoor("minecraft:" + l.wood + "_trapdoor", "south", false, true));
    }

    private static void blanketChest(Plan p, Dorm d, int rx) {
        p.set(d.x(rx), Y, Dorm.z(3), chest("south"));
    }

    /** A nightstand of drawers with a lamp on it. */
    private static void nightstand(Plan p, Dorm d, int rx, String onTop) {
        p.set(d.x(rx), Y, Dorm.z(1), books("south", 0));
        p.set(d.x(rx), Y + 1, Dorm.z(1), onTop);
    }

    /** The chest of drawers by the brick wall, two high. */
    private static void dresser(Plan p, Dorm d, String onTop) {
        for (int rz = 1; rz <= 2; rz++) {
            p.set(d.x(6), Y, Dorm.z(rz), books(d.plain(), 0));
            p.set(d.x(6), Y + 1, Dorm.z(rz), books(d.plain(), 0));
        }
        p.set(d.x(6), Y + 2, Dorm.z(1), onTop);
    }

    /** The two-seat settee against the brick, wooden arms. */
    private static void settee(Plan p, Dorm d, Look l) {
        for (int rz = 4; rz <= 5; rz++) chair(p, d.x(6), Y, Dorm.z(rz), d.plain(), "minecraft:" + l.settee, "minecraft:" + l.wood + "_trapdoor");
    }

    /** The desk on the plain wall (a top slab two long), its chair, its lamp. */
    private static void desk(Plan p, Dorm d, Look l, String lamp, String paper) {
        for (int rz = 3; rz <= 4; rz++) p.set(d.x(0), Y, Dorm.z(rz), top("minecraft:" + l.wood + "_slab"));
        chair(p, d.x(1), Y, Dorm.z(3), d.plain(), "minecraft:" + l.wood + "_stairs", null);
        p.set(d.x(0), Y + 1, Dorm.z(4), lamp);
        if (paper != null) p.set(d.x(0), Y + 1, Dorm.z(3), paper);
    }

    /** A brass floor lamp in the corner by the ledge. */
    private static void floorLamp(Plan p, Dorm d, int rx) {
        p.set(d.x(rx), Y, Dorm.z(1), rod("up"));
        p.set(d.x(rx), Y + 1, Dorm.z(1), rod("up"));
        p.set(d.x(rx), Y + 2, Dorm.z(1), St.lantern(false));
    }

    /** A painting on the plain wall over the desk (rows {@code rz0..}, from {@code y}). */
    private static void plainPainting(Plan p, Dorm d, String variant, int rz0, int y) {
        painting(p, d.x(0), 0, d.brick(), variant, Dorm.z(rz0), y);
    }

    /** A painting on the brick wall over the settee (rows {@code rz0..}, from {@code y}). */
    private static void brickPainting(Plan p, Dorm d, String variant, int rz0, int y) {
        painting(p, d.x(6), 0, d.plain(), variant, Dorm.z(rz0), y);
    }

    private static void rugIn(Plan p, Dorm d, int rx0, int rx1, int rz0, int rz1, String border, String fill) {
        rug(p, Math.min(d.x(rx0), d.x(rx1)), Math.max(d.x(rx0), d.x(rx1)), Y, Dorm.z(rz0), Dorm.z(rz1), border, fill);
    }

    private static Pattern brick(int salt, Object... mix) {
        return mix(salt, mix);
    }

    private static void spot(Plan p, Dorm d, String name) {
        p.view(name, d.x(4), Y, Dorm.z(7), d.x(2), Y + 0.5, Dorm.z(0));
    }

    // --- the rooms ---------------------------------------------------------------------------------------------------------

    /** 11: Dean's room. A double bed under the gun wall, the record player on the ledge, Mom's photo over the desk. */
    private static void dean(Plan p, Dorm d) {
        Look l = new Look("minecraft:light_gray_concrete", "minecraft:gray_concrete", "minecraft:polished_andesite",
                solid("minecraft:gray_concrete"), brick(31, "minecraft:bricks", 14, "minecraft:mud_bricks", 1),
                parquet("minecraft:dark_oak_planks", "minecraft:spruce_planks", true), "dark_oak", "gray", "polished_blackstone_stairs");
        shell(p, d, l, "11", HIDDEN_LIGHT);
        ledge(p, d, l.beam);
        bed(p, d, l, 2);
        bed(p, d, l, 3);
        blanketChest(p, d, 3);
        p.set(d.x(2), Y, Dorm.z(3), barrel("up"));
        nightstand(p, d, 1, St.candle("green", 3, false));
        nightstand(p, d, 4, St.lantern(false));
        // The gun wall over the bed.
        String[] upper = {"minecraft:bow", "minecraft:crossbow", "minecraft:crossbow", "minecraft:bow"};
        String[] lower = {"minecraft:iron_sword", "minecraft:crossbow|1", "minecraft:iron_axe", "minecraft:golden_sword"};
        for (int i = 0; i < 4; i++) {
            frame(p, d.x(1 + i), Y + 3, Dorm.z(0), "south", upper[i]);
            frame(p, d.x(1 + i), Y + 2, Dorm.z(0), "south", lower[i]);
        }
        // The record player and two records on the wall; a bottle on the ledge.
        p.set(d.x(5), Y + 1, Dorm.z(0), jukebox());
        frame(p, d.x(5), Y + 2, Dorm.z(0), "south", "minecraft:music_disc_cat");
        frame(p, d.x(6), Y + 2, Dorm.z(0), "south", "minecraft:music_disc_blocks");
        p.set(d.x(6), Y + 1, Dorm.z(0), St.candle("brown", 2, false));
        dresser(p, d, St.lantern(false));
        settee(p, d, l);
        desk(p, d, l, St.lantern(false), "minecraft:white_carpet");
        p.set(d.x(0), Y, Dorm.z(5), barrel(d.brick()));
        painting(p, d.x(0), 0, d.brick(), "alban", Dorm.z(4), Y + 2);
        painting(p, d.x(0), 0, d.brick(), "wasteland", Dorm.z(3), Y + 2);
        brickPainting(p, d, "lowmist", 3, Y + 2);
        rugIn(p, d, 1, 5, 4, 6, "black", "brown");
        p.set(d.x(5), Y, Dorm.z(6), "minecraft:heavy_core[waterlogged=false]");
        spot(p, d, "deans_room");
    }

    /** 10: Sam's room. Low bookcases on the ledge and along the brick, a lectern, a laptop and a research board. */
    private static void sam(Plan p, Dorm d) {
        Look l = new Look("minecraft:polished_andesite", "minecraft:polished_deepslate", "minecraft:smooth_stone",
                solid("minecraft:gray_concrete"), brick(32, "minecraft:mud_bricks", 10, "minecraft:packed_mud", 2),
                mix(33, "minecraft:smooth_stone", 8, "minecraft:polished_andesite", 2), "spruce", "brown", "dark_prismarine_stairs");
        shell(p, d, l, "10", HIDDEN_LIGHT);
        ledge(p, d, l.beam);
        for (int rx = 0; rx <= 1; rx++) p.set(d.x(rx), Y, Dorm.z(0), books("south", rx == 0 ? 0b111011 : 0b011111));
        bed(p, d, l, 3);
        blanketChest(p, d, 3);
        nightstand(p, d, 4, St.lantern(false));
        p.set(d.x(5), Y + 1, Dorm.z(0), noteBlock());
        p.set(d.x(2), Y + 1, Dorm.z(0), "minecraft:potted_bamboo");
        dresser(p, d, pot("south"));
        // Bookcases along the brick instead of a settee, a landscape over them.
        for (int rz = 3; rz <= 6; rz++) {
            p.set(d.x(6), Y, Dorm.z(rz), rz % 2 == 0 ? "minecraft:bookshelf" : books(d.plain(), 0b101101 + rz));
            p.set(d.x(6), Y + 1, Dorm.z(rz), rz % 2 == 1 ? "minecraft:bookshelf" : books(d.plain(), 0b110110 >> (rz - 3)));
        }
        brickPainting(p, d, "passage", 3, Y + 2);
        desk(p, d, l, St.lantern(false), plate("minecraft:polished_blackstone_pressure_plate"));
        p.set(d.x(0), Y, Dorm.z(5), lectern(d.brick(), true));
        frame(p, d.x(0), Y + 2, Dorm.z(3), d.brick(), "minecraft:map");
        frame(p, d.x(0), Y + 2, Dorm.z(4), d.brick(), "minecraft:paper");
        frame(p, d.x(0), Y + 2, Dorm.z(5), d.brick(), "minecraft:writable_book");
        frame(p, d.x(0), Y + 3, Dorm.z(4), d.brick(), "minecraft:compass");
        floorLamp(p, d, 0);
        rugIn(p, d, 2, 5, 4, 6, "blue", "light_blue");
        spot(p, d, "sams_room");
    }

    /** 9: Mary's room. Cherry wood, a white bed, flowers on every ledge, a loom, a bouquet over the settee. */
    private static void mary(Plan p, Dorm d) {
        Look l = new Look("minecraft:white_terracotta", "minecraft:stripped_cherry_wood", "minecraft:calcite",
                solid("minecraft:light_gray_terracotta"), brick(34, "minecraft:bricks", 10, "minecraft:granite", 2),
                parquet("minecraft:cherry_planks", "minecraft:birch_planks", false), "cherry", "white", "polished_granite_stairs");
        shell(p, d, l, "9", HIDDEN_LIGHT);
        ledge(p, d, l.beam);
        bed(p, d, l, 3);
        blanketChest(p, d, 3);
        nightstand(p, d, 4, St.lantern(false));
        nightstand(p, d, 2, "minecraft:potted_lily_of_the_valley");
        p.set(d.x(0), Y + 1, Dorm.z(0), "minecraft:potted_poppy");
        p.set(d.x(1), Y + 1, Dorm.z(0), "minecraft:potted_allium");
        p.set(d.x(5), Y + 1, Dorm.z(0), noteBlock());
        p.set(d.x(6), Y + 1, Dorm.z(0), "minecraft:potted_azure_bluet");
        dresser(p, d, "minecraft:potted_cornflower");
        settee(p, d, l);
        brickPainting(p, d, "bouquet", 3, Y + 1);
        desk(p, d, l, St.lantern(false), St.candle("pink", 1, false));
        p.set(d.x(0), Y, Dorm.z(5), St.facing("minecraft:loom", d.brick()));
        plainPainting(p, d, "sunset", 3, Y + 2);
        painting(p, d.x(0), 0, d.brick(), "kebab", Dorm.z(5), Y + 2);
        rugIn(p, d, 1, 5, 4, 6, "white", "pink");
        spot(p, d, "marys_room");
    }

    /** 8: Castiel's room. Made up and never slept in: a coat on its stand, a hive on the ledge, the blade over the bed. */
    private static void castiel(Plan p, Dorm d) {
        Look l = new Look("minecraft:light_gray_concrete", "minecraft:polished_andesite", "minecraft:smooth_stone",
                solid("minecraft:light_gray_concrete"), brick(35, "minecraft:stone_bricks", 10, "minecraft:cracked_stone_bricks", 2),
                mix(36, "minecraft:polished_diorite", 6, "minecraft:smooth_stone", 1), "birch", "light_gray", "dark_prismarine_stairs");
        shell(p, d, l, "8", COOL_LIGHT);
        ledge(p, d, l.beam);
        bed(p, d, l, 3);
        blanketChest(p, d, 3);
        nightstand(p, d, 4, St.candle("white", 1, true));
        nightstand(p, d, 2, St.candle("white", 1, true));
        p.set(d.x(5), Y + 1, Dorm.z(0), St.of("beehive", "facing", "south", "honey_level", "5"));
        p.set(d.x(1), Y + 1, Dorm.z(0), "minecraft:potted_flowering_azalea_bush");
        frame(p, d.x(3), Y + 2, Dorm.z(0), "south", "minecraft:iron_sword|3");
        frame(p, d.x(2), Y + 3, Dorm.z(0), "south", "minecraft:feather");
        frame(p, d.x(4), Y + 3, Dorm.z(0), "south", "minecraft:feather");
        dresser(p, d, St.lantern(false));
        settee(p, d, l);
        brickPainting(p, d, "changing", 3, Y + 2);
        desk(p, d, l, St.lantern(false), null);
        p.set(d.x(0), Y + 1, Dorm.z(3), "minecraft:potted_oxeye_daisy");
        plainPainting(p, d, "sea", 3, Y + 2);
        p.decor(Decor.armorStand(d.x(0), Y, Dorm.z(6), d.brick(), "-", "minecraft:leather_chestplate", "minecraft:leather_leggings",
                "-", "-", "A88B5E"));
        spot(p, d, "castiels_room");
    }

    /** 7: Charlie's room. A dark gaming den: a computer on the desk, a sword and shield, the arcade poster over a purple settee. */
    private static void charlie(Plan p, Dorm d) {
        Look l = new Look("minecraft:gray_concrete", "minecraft:black_concrete", "minecraft:light_gray_concrete",
                solid("minecraft:black_concrete"), brick(37, "minecraft:deepslate_bricks", 10, "minecraft:cracked_deepslate_bricks", 2),
                mix(38, "minecraft:polished_tuff", 6, "minecraft:tuff_bricks", 1), "mangrove", "purple", "purpur_stairs");
        shell(p, d, l, "7", "minecraft:verdant_froglight");
        ledge(p, d, l.beam);
        bed(p, d, l, 3);
        blanketChest(p, d, 3);
        nightstand(p, d, 4, St.lantern(false));
        p.set(d.x(1), Y + 1, Dorm.z(0), amethyst("up"));
        p.set(d.x(5), Y + 1, Dorm.z(0), noteBlock());
        p.set(d.x(6), Y + 1, Dorm.z(0), St.candle("purple", 3, true));
        dresser(p, d, St.candle("cyan", 2, true));
        settee(p, d, l);
        brickPainting(p, d, "donkey_kong", 3, Y + 1);
        // The computer: a monitor, a tower, a gaming chair.
        for (int rz = 3; rz <= 4; rz++) p.set(d.x(0), Y, Dorm.z(rz), top("minecraft:mangrove_slab"));
        chair(p, d.x(1), Y, Dorm.z(3), d.plain(), "minecraft:red_nether_brick_stairs", "minecraft:crimson_trapdoor");
        p.set(d.x(0), Y + 1, Dorm.z(3), "minecraft:tinted_glass");
        p.set(d.x(0), Y + 1, Dorm.z(4), St.of("crafter", "crafting", "false", "orientation", "north_up", "triggered", "false"));
        p.set(d.x(0), Y, Dorm.z(5), chest(d.brick()));
        p.set(d.x(0), Y + 1, Dorm.z(5), St.lantern(false));
        frame(p, d.x(0), Y + 2, Dorm.z(5), d.brick(), "minecraft:iron_sword");
        frame(p, d.x(0), Y + 2, Dorm.z(6), d.brick(), "minecraft:shield");
        frame(p, d.x(0), Y + 3, Dorm.z(4), d.brick(), "minecraft:ender_eye");
        rugIn(p, d, 2, 5, 4, 6, "cyan", "purple");
        spot(p, d, "charlies_room");
    }

    /** 12: Jack's room. Stars in the ceiling, a glowing lamp, a map table, a cake on the desk, the End on the wall. */
    private static void jack(Plan p, Dorm d) {
        Look l = new Look("minecraft:light_gray_concrete", "minecraft:blue_terracotta", "minecraft:polished_andesite",
                solid("minecraft:black_concrete"), brick(39, "minecraft:tuff_bricks", 10, "minecraft:polished_tuff", 1),
                mix(40, "minecraft:polished_andesite", 6, "minecraft:smooth_stone", 2), "oak", "yellow", "warped_stairs");
        shell(p, d, l, "12", "minecraft:glowstone");
        for (int[] s : new int[][]{{1, 6}, {2, 1}, {4, 3}, {5, 1}, {4, 6}}) p.set(d.x(s[0]), F + 6, Dorm.z(s[1]), "minecraft:glowstone");
        ledge(p, d, l.beam);
        bed(p, d, l, 3);
        blanketChest(p, d, 3);
        nightstand(p, d, 4, "minecraft:beacon");
        p.set(d.x(5), Y + 1, Dorm.z(0), noteBlock());
        p.set(d.x(1), Y + 1, Dorm.z(0), "minecraft:potted_cactus");
        p.set(d.x(6), Y, Dorm.z(1), "minecraft:cartography_table");
        p.set(d.x(6), Y, Dorm.z(2), books(d.plain(), 0b000111));
        p.set(d.x(6), Y + 1, Dorm.z(1), St.lantern(false));
        settee(p, d, l);
        brickPainting(p, d, "endboss", 3, Y + 1);
        desk(p, d, l, St.lantern(false), "minecraft:cake[bites=1]");
        frame(p, d.x(0), Y + 2, Dorm.z(3), d.brick(), "minecraft:spyglass");
        frame(p, d.x(0), Y + 2, Dorm.z(4), d.brick(), "minecraft:filled_map");
        painting(p, d.x(0), 0, d.brick(), "aztec2", Dorm.z(5), Y + 2);
        rugIn(p, d, 1, 5, 4, 6, "light_blue", "white");
        spot(p, d, "jacks_room");
    }

    /** 13: Bobby's room. Panelled in spruce: a red bed, books on the ledge, a bottle and a radio, horns on the wall. */
    private static void bobby(Plan p, Dorm d) {
        Look l = new Look("minecraft:spruce_planks", "minecraft:stripped_spruce_wood", "minecraft:stripped_spruce_wood",
                solid("minecraft:spruce_planks"), brick(41, "minecraft:bricks", 12, "minecraft:mud_bricks", 3),
                parquet("minecraft:spruce_planks", "minecraft:oak_planks", true), "spruce", "red", "mud_brick_stairs");
        shell(p, d, l, "13", HIDDEN_LIGHT);
        ledge(p, d, "minecraft:polished_andesite");
        for (int rx = 0; rx <= 1; rx++) p.set(d.x(rx), Y, Dorm.z(0), "minecraft:bookshelf");
        bed(p, d, l, 3);
        blanketChest(p, d, 3);
        nightstand(p, d, 4, St.lantern(false));
        p.set(d.x(5), Y + 1, Dorm.z(0), noteBlock());
        p.set(d.x(6), Y + 1, Dorm.z(0), St.candle("brown", 3, false));
        dresser(p, d, St.candle("brown", 1, false));
        settee(p, d, l);
        brickPainting(p, d, "finding", 3, Y + 2);
        desk(p, d, l, St.lantern(false), "minecraft:white_carpet");
        p.set(d.x(0), Y, Dorm.z(5), lectern(d.brick(), true));
        frame(p, d.x(0), Y + 3, Dorm.z(3), d.brick(), "minecraft:goat_horn");
        frame(p, d.x(0), Y + 3, Dorm.z(5), d.brick(), "minecraft:goat_horn");
        plainPainting(p, d, "courbet", 3, Y + 2);
        floorLamp(p, d, 0);
        rugIn(p, d, 1, 5, 4, 6, "black", "red");
        spot(p, d, "bobbys_room");
    }

    /** 14: the hunters' bunk room. Two bunks either side, a row of steel lockers on the back wall, a card table. */
    private static void bunks(Plan p, Dorm d) {
        Look l = new Look("minecraft:light_gray_concrete", SKIRTING, "minecraft:polished_andesite",
                solid("minecraft:gray_concrete"), brick(42, "minecraft:bricks", 10, "minecraft:mud_bricks", 3),
                concreteFloor(), "spruce", "green", "dark_prismarine_stairs");
        shell(p, d, l, "14", HIDDEN_LIGHT);
        for (int rx = 2; rx <= 4; rx++) door(p, d.x(rx), Y, Dorm.z(0), "minecraft:iron_door", "south", rx % 2 == 0 ? "left" : "right", false);
        for (int rx = 2; rx <= 4; rx++) p.set(d.x(rx), Y + 2, Dorm.z(0), St.trapdoor("minecraft:iron_trapdoor", "south", false, true));
        for (int side : new int[]{0, 6}) {
            int post = side == 0 ? 1 : 5;
            for (int rz : new int[]{0, 3, 6}) for (int y = Y; y <= Y + 2; y++) p.set(d.x(post), y, Dorm.z(rz), St.wall(BLACK_WALL));
            for (int rz : new int[]{2, 5}) {
                Level2Props.bed(p, d.x(side), Y, Dorm.z(rz), side == 0 ? "green" : "brown", "north");
                Level2Props.bed(p, d.x(side), Y + 2, Dorm.z(rz), side == 0 ? "brown" : "green", "north");
                p.set(d.x(side), Y + 1, Dorm.z(rz - 1), top("minecraft:spruce_slab"));
                p.set(d.x(side), Y + 1, Dorm.z(rz), top("minecraft:spruce_slab"));
            }
            p.set(d.x(side), Y, Dorm.z(0), chest("south"));
            p.set(d.x(side), Y + 1, Dorm.z(0), St.lantern(false));
            p.set(d.x(side), Y, Dorm.z(6), chest(side == 0 ? d.brick() : d.plain()));
        }
        // The card table.
        p.set(d.x(3), Y, Dorm.z(4), top("minecraft:spruce_slab"));
        p.set(d.x(3), Y, Dorm.z(3), top("minecraft:spruce_slab"));
        p.set(d.x(3), Y + 1, Dorm.z(3), St.lantern(false));
        p.set(d.x(3), Y + 1, Dorm.z(4), St.carpet("white"));
        chair(p, d.x(2), Y, Dorm.z(4), d.brick(), "minecraft:spruce_stairs", null);
        chair(p, d.x(4), Y, Dorm.z(3), d.plain(), "minecraft:spruce_stairs", null);
        brickPainting(p, d, "creebet", 6, Y + 3);
        p.view("dormitory", d.x(3), Y, Dorm.z(7), d.x(3), Y + 1, Dorm.z(0));
    }
}
