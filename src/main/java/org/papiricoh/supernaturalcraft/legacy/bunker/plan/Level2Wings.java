package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;
import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level2Props.*;

/**
 * The wings south of the corridor (pure), west to east: the lab, the baths, the kitchen (straight ahead of the stairs), the
 * Dean cave and the armoury. Each 13 deep, 5 high (the kitchen a step lower and 6 high), behind its own wall so every room
 * keeps its finish.
 */
final class Level2Wings {

    private static final int F = Level2.F, Y = F + 1;
    private static final String SHADE = "minecraft:waxed_oxidized_copper_trapdoor";

    private Level2Wings() {
    }

    static void build(Plan p) {
        kitchen(p);
        deanCave(p);
        lab(p);
        baths(p);
        armory(p);
    }

    /** A sign on the corridor's south wall naming a room. */
    private static void plaque(Plan p, int x, String... lines) {
        sign(p, x, F + 3, -40, "north", "dark_oak", "yellow", lines);
    }

    // --- the kitchen ---------------------------------------------------------------------------------------------------------

    /**
     * The kitchen after the series' set: an industrial 1950s kitchen a step down from its door, pale cream tile with a dark
     * band, dark beams under a dark ceiling, a steel range with its hood along the west wall, a long stainless island under a
     * pot rack, green enamel pendants, a steel prep table and shelves of tins, a sink, a walk-in fridge's doors, and the wooden
     * table with its stools under the notice board in the far corner.
     */
    private static void kitchen(Plan p) {
        int fk = F - 1, y = fk + 1;
        Pattern wall = (x, yy, z) -> {
            int off = yy - fk;
            if (off == 1) return SKIRTING;
            if (off == 4) return "minecraft:polished_deepslate";
            return off == 6 ? "minecraft:smooth_sandstone" : "minecraft:end_stone_bricks";
        };
        Pattern floor = checker(3, mix(51, "minecraft:smooth_stone", 10, "minecraft:polished_andesite", 1), solid("minecraft:polished_andesite"));
        room(p, new Room(-8, 8, fk, 6, -37, -25), new Style(floor, wall, solid("minecraft:deepslate_tiles"), null, null, 0, null, 0));
        for (int x : new int[]{-6, -2, 2, 6}) for (int z = -37; z <= -25; z++) p.set(x, fk + 6, z, St.axis("minecraft:stripped_dark_oak_log", "z"));
        // In from the corridor: the threshold, then a broad step down.
        doorway(p, 0, F, -39, "south", 2, 3, 3, BLACK, BLACK_STAIRS, null);
        for (int x = -1; x <= 1; x++) p.set(x, F, -38, FLOOR_BORDER);
        for (int x = -2; x <= 2; x++) p.set(x, y, -37, St.stairs("minecraft:polished_deepslate_stairs", "north", false));
        p.decor(Decor.frame(0, F + 4, -37, "south", "minecraft:clock"));
        plaque(p, 3, "", "Kitchen", "", "");
        // The range: ovens and stoves under a steel hood with a flue to the ceiling.
        String[] range = {"minecraft:furnace", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:furnace"};
        for (int i = 0; i < 5; i++) {
            int z = -33 + i;
            p.set(-8, y, z, oven(range[i], "east"));
            p.set(-8, y + 1, z, i == 1 || i == 3 ? "minecraft:cauldron" : plate("minecraft:heavy_weighted_pressure_plate"));
            p.set(-8, y + 3, z, "minecraft:iron_block");
            p.set(-8, y + 4, z, "minecraft:iron_block");
            p.set(-7, y + 3, z, St.trapdoor("minecraft:iron_trapdoor", "north", true, false));
            p.set(-7, y + 2, z, St.trapdoor("minecraft:iron_trapdoor", "west", false, true));
        }
        p.set(-8, y + 5, -31, "minecraft:iron_block");
        // Shelves of tins on the west wall by the door.
        steelShelf(p, -8, y, -36, -34, "east", true);
        // The prep table and the shelf over it, the sink with its tap, a landscape for a window.
        for (int x = -7; x <= -3; x++) {
            p.set(x, y, -25, top("minecraft:smooth_stone_slab"));
            p.set(x, y + 3, -25, top("minecraft:smooth_stone_slab"));
        }
        p.set(-7, y + 1, -25, pot("north"));
        p.set(-6, y + 1, -25, "minecraft:potted_fern");
        p.set(-4, y + 1, -25, St.candle("white", 2, false));
        String[] tins = {"minecraft:red_candle", "minecraft:flower_pot", "minecraft:yellow_candle", "minecraft:decorated_pot", "minecraft:green_candle"};
        for (int i = 0; i < 5; i++) {
            String t = tins[i];
            p.set(-7 + i, y + 4, -25, t.endsWith("candle") ? St.of(t, "candles", "4", "lit", "false", "waterlogged", "false")
                    : t.endsWith("decorated_pot") ? pot("north") : t);
        }
        p.set(-1, y, -25, "minecraft:iron_block");
        p.set(0, y, -25, water(3));
        p.set(1, y, -25, "minecraft:iron_block");
        p.set(0, y + 1, -25, hook("north"));
        p.set(1, y + 1, -25, "minecraft:potted_red_tulip");
        painting(p, 0, -25, "north", "sea", -1, y + 2);
        // The stainless island under its pot rack, two copper stools.
        for (int x = -2; x <= 2; x++) {
            for (int z = -31; z <= -30; z++) p.set(x, y, z, Math.abs(x) == 2 ? "minecraft:polished_andesite" : "minecraft:iron_block");
            p.set(x, fk + 5, -31, St.bars());
        }
        p.set(-2, y + 1, -31, "minecraft:cauldron");
        frame(p, -1, y + 1, -30, "up", "minecraft:bread");
        frame(p, 0, y + 1, -31, "up", "minecraft:bowl");
        frame(p, 1, y + 1, -30, "up", "minecraft:carrot");
        p.set(2, y + 1, -31, "minecraft:potted_dandelion");
        p.set(0, fk + 4, -31, St.lantern(true));
        for (int x : new int[]{-1, 1}) p.set(x, fk + 4, -31, St.chain("y"));
        for (int x : new int[]{-1, 1}) p.set(x, y, -29, bottom("minecraft:waxed_cut_copper_slab"));
        // The fridge on the east wall, shelves of stores beside it.
        for (int z = -35; z <= -34; z++) {
            for (int yy = y; yy <= y + 2; yy++) p.set(8, yy, z, "minecraft:iron_block");
            door(p, 7, y, z, "minecraft:iron_door", "west", z == -35 ? "left" : "right", false);
            p.set(7, y + 2, z, St.trapdoor("minecraft:iron_trapdoor", "west", false, true));
        }
        steelShelf(p, 8, y, -32, -29, "west", false);
        loot(p, 8, y, -32, barrel("west"), "supernaturalcraft:chests/bunker_kitchen");
        loot(p, 8, y, -30, barrel("west"), "supernaturalcraft:chests/bunker_kitchen");
        // The wooden table and its stools under the notice board.
        for (int x = 4; x <= 6; x++) {
            for (int z = -28; z <= -27; z++) p.set(x, y, z, top("minecraft:spruce_slab"));
            p.set(x, y, -29, bottom("minecraft:waxed_exposed_cut_copper_slab"));
            p.set(x, y, -26, bottom("minecraft:waxed_exposed_cut_copper_slab"));
        }
        p.set(5, y + 1, -28, St.candle("orange", 1, true));
        p.set(4, y + 1, -27, "minecraft:cake[bites=3]");
        for (int x = 3; x <= 7; x++) {
            for (int yy = y + 1; yy <= y + 4; yy++) {
                boolean edge = x == 3 || x == 7 || yy == y + 1 || yy == y + 4;
                p.set(x, yy, -24, edge ? "minecraft:dark_oak_planks" : "minecraft:stripped_oak_wood");
            }
        }
        String[] notes = {"minecraft:paper", "minecraft:map", "minecraft:paper", "minecraft:written_book", "minecraft:paper", "minecraft:paper"};
        for (int i = 0; i < 6; i++) frame(p, 4 + i % 3, y + 2 + i / 3, -25, "north", notes[i]);
        // A bin and a mop bucket by the door; the green pendants.
        p.set(7, y, -37, "minecraft:composter[level=2]");
        p.set(6, y, -37, "minecraft:cauldron");
        p.set(-7, y, -37, barrel("south"));
        p.set(-7, y + 1, -37, St.candle("white", 1, false));
        int ceiling = fk + 7;
        for (int[] c : new int[][]{{-4, -33}, {-4, -28}, {4, -33}, {5, -27}, {0, -34}}) shadedPendant(p, c[0], ceiling, c[1], 3, SHADE);
        p.view("kitchen", 1, F + 1, -38, -4, y + 1, -28);
    }

    /** Shelving of steel: crates below, a shelf board, tins and pots on both levels. */
    private static void steelShelf(Plan p, int x, int y, int z0, int z1, String out, boolean crates) {
        String[] small = {"minecraft:flower_pot", "minecraft:brown_candle", "minecraft:decorated_pot", "minecraft:lime_candle", "minecraft:potted_brown_mushroom"};
        for (int z = z0; z <= z1; z++) {
            int i = Math.floorMod(z * 7 + x, small.length), j = Math.floorMod(z * 3 + x + 2, small.length);
            p.set(x, y, z, crates ? barrel(out) : chest(out));
            p.set(x, y + 1, z, tin(small[i], out));
            p.set(x, y + 2, z, top("minecraft:smooth_stone_slab"));
            p.set(x, y + 3, z, tin(small[j], out));
        }
    }

    private static String tin(String id, String facing) {
        if (id.endsWith("candle")) return St.of(id, "candles", "3", "lit", "false", "waterlogged", "false");
        if (id.endsWith("decorated_pot")) return pot(facing);
        return id;
    }

    // --- the Dean cave -------------------------------------------------------------------------------------------------------

    /**
     * Dean's den: dark wood wainscot under brick, a big screen in the east wall with speakers, three recliners and a black leather
     * sofa behind them, the bar on the west wall with its taps, stools and bottles, a pool table under a green lamp, the
     * dartboard, an arcade cabinet and the popcorn machine, posters.
     */
    private static void deanCave(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            if (off <= 2) return OAK;
            if (off == 3) return "minecraft:stripped_dark_oak_wood";
            return mix(61, "minecraft:bricks", 12, "minecraft:mud_bricks", 2).at(x, y, z);
        };
        room(p, new Room(11, 25, F, 5, -37, -25), new Style(parquet("minecraft:dark_oak_planks", "minecraft:spruce_planks", false), wall,
                solid(OAK), OAK_STAIRS, null, 0, null, 0));
        doorway(p, 20, F, -39, "south", 2, 3, 3, OAK, OAK_STAIRS, null);
        plaque(p, 23, "", "Dean Cave", "", "");
        // The screen in the east wall, its bezel, the console and two tall speakers.
        for (int z = -34; z <= -29; z++) {
            for (int y = F + 1; y <= F + 4; y++) {
                boolean bezel = z == -34 || z == -29 || y == F + 1 || y == F + 4;
                p.set(26, y, z, bezel ? "minecraft:black_concrete" : "minecraft:tinted_glass");
            }
        }
        for (int z = -33; z <= -30; z++) p.set(25, Y, z, top("minecraft:dark_oak_slab"));
        p.set(25, Y + 1, -32, St.candle("red", 1, true));
        for (int z : new int[]{-34, -29}) {
            p.set(25, Y, z, noteBlock());
            p.set(25, Y + 1, z, noteBlock());
        }
        // Three recliners with their footrests up, the sofa behind.
        for (int z : new int[]{-33, -31, -29}) {
            chair(p, 22, Y, z, "east", "minecraft:mud_brick_stairs", "minecraft:spruce_trapdoor");
            p.set(23, Y, z, St.trapdoor("minecraft:spruce_trapdoor", "east", false, false));
        }
        for (int z = -33; z <= -29; z++) chair(p, 19, Y, z, "east", "minecraft:polished_blackstone_stairs", null);
        p.set(19, Y, -34, "minecraft:black_wool");
        p.set(19, Y, -28, "minecraft:black_wool");
        p.set(19, Y + 1, -34, St.lantern(false));
        // The bar: back bar of barrels and a shelf of bottles, the counter with taps and lamps, stools.
        String[] bottles = {"green", "brown", "lime", "yellow", "brown", "orange"};
        for (int i = 0; i < 6; i++) {
            int z = -34 + i;
            p.set(11, Y, z, barrel("east"));
            p.set(11, Y + 1, z, St.candle(bottles[i], 1 + i % 4, false));
            p.set(11, Y + 2, z, top("minecraft:dark_oak_slab"));
            p.set(11, Y + 3, z, St.candle(bottles[5 - i], 1 + (i + 2) % 4, false));
            p.set(13, Y, z, "minecraft:stripped_dark_oak_wood");
        }
        p.set(13, Y + 1, -34, St.lantern(false));
        p.set(13, Y + 1, -29, St.lantern(false));
        p.set(13, Y + 1, -32, lever("floor", "east"));
        p.set(13, Y + 1, -31, lever("floor", "east"));
        p.set(13, Y + 1, -30, St.candle("brown", 2, false));
        for (int z : new int[]{-33, -31, -29}) p.set(14, Y, z, bottom("minecraft:crimson_slab"));
        p.set(13, F + 5, -31, St.of("bell", "attachment", "ceiling", "facing", "east", "powered", "false"));
        // The pool table under its green lamp, the cue rack and a poster.
        for (int x = 13; x <= 15; x++) {
            for (int z = -27; z <= -26; z++) {
                p.set(x, Y, z, "minecraft:stripped_dark_oak_wood");
                p.set(x, Y + 1, z, St.carpet("green"));
            }
            frame(p, x, Y + 1, -25, "north", "minecraft:stick|1");
        }
        shadedPendant(p, 14, F + 6, -27, 3, SHADE);
        shadedPendant(p, 21, F + 6, -31, 3, SHADE);
        pendant(p, 17, F + 6, -35, 2, false);
        painting(p, 0, -25, "north", "fighters", 12, Y + 2);
        // The dartboard and its darts, a western poster, the arcade cabinet and the popcorn machine by the door.
        p.set(15, F + 3, -38, St.of("target", "power", "0"));
        frame(p, 14, F + 3, -37, "south", "minecraft:arrow");
        frame(p, 16, F + 3, -37, "south", "minecraft:arrow");
        painting(p, 17, -37, "south", "prairie_ride", 17, Y + 1);
        p.set(23, Y, -37, "minecraft:black_concrete");
        p.set(23, Y + 1, -37, "minecraft:cyan_stained_glass");
        p.set(23, Y + 2, -37, "minecraft:magenta_glazed_terracotta");
        p.set(25, Y, -37, "minecraft:red_concrete");
        p.set(25, Y + 1, -37, "minecraft:yellow_stained_glass");
        p.set(25, Y + 2, -37, "minecraft:red_terracotta");
        p.set(25, Y, -25, jukebox());
        frame(p, 25, Y + 2, -26, "west", "minecraft:music_disc_13");
        frame(p, 25, Y + 2, -27, "west", "minecraft:music_disc_mellohi");
        rug(p, 17, 24, Y, -35, -27, "red", "brown");
        // Warm lamps in the walls (dim weathered bulbs behind louvres).
        String bulb = St.bulb("minecraft:waxed_weathered_copper_bulb");
        wallLight(p, 10, F + 4, -36, "east", bulb);
        wallLight(p, 10, F + 4, -27, "east", bulb);
        wallLight(p, 26, F + 4, -36, "west", bulb);
        wallLight(p, 26, F + 4, -26, "west", bulb);
        wallLight(p, 20, F + 4, -24, "north", bulb);
        p.view("dean_cave", 15, Y, -36, 25, Y + 1, -31);
    }

    // --- the lab -------------------------------------------------------------------------------------------------------------

    /**
     * The lab: white tile under cool light, black benches along the south wall and in an island, brewing stands, flasks and a
     * microscope, a fume hood set in the west wall behind its glass sash, the specimen shelf, a chalkboard, an autopsy table
     * under its lamp, an infirmary bed and a fridge for samples.
     */
    private static void lab(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            if (off == 1) return "minecraft:light_gray_concrete";
            return off == 5 ? "minecraft:smooth_stone" : "minecraft:white_concrete";
        };
        room(p, new Room(-41, -25, F, 5, -37, -25), new Style(checker(1, solid("minecraft:white_concrete"), solid("minecraft:light_gray_concrete")),
                wall, solid("minecraft:smooth_stone"), null, null, 0, COOL_LIGHT, 4));
        doorway(p, -29, F, -39, "south", 2, 1, 2, BLACK, null, St.door("minecraft:waxed_copper_door", "south", "left", false));
        plaque(p, -31, "", "Laboratory", "", "");
        String bench = top("minecraft:polished_blackstone_slab");
        // The south bench with the sink, the shelf over it.
        for (int x = -40; x <= -27; x++) {
            p.set(x, Y, -25, x == -33 ? water(3) : bench);
            p.set(x, Y + 3, -25, top("minecraft:smooth_stone_slab"));
        }
        p.set(-33, Y + 1, -25, hook("north"));
        String[] onBench = {"brew", "minecraft:flower_pot", "-", "brew", "rod", "candle", "-", "-", "minecraft:hopper[enabled=true,facing=down]",
                "pot", "amethyst", "-", "minecraft:potted_red_mushroom", "brew"};
        for (int i = 0; i < onBench.length; i++) placeSmall(p, -40 + i, Y + 1, -25, onBench[i]);
        String[] onShelf = {"minecraft:potted_brown_mushroom", "candle", "minecraft:flower_pot", "pot", "minecraft:potted_dead_bush", "-",
                "minecraft:potted_wither_rose", "candle", "minecraft:flower_pot", "-", "minecraft:potted_crimson_fungus", "pot",
                "minecraft:potted_warped_fungus", "candle"};
        for (int i = 0; i < onShelf.length; i++) placeSmall(p, -40 + i, Y + 4, -25, onShelf[i]);
        // The island bench and its stools.
        for (int x = -37; x <= -31; x++) for (int z = -31; z <= -30; z++) p.set(x, Y, z, bench);
        placeSmall(p, -36, Y + 1, -31, "brew");
        placeSmall(p, -35, Y + 1, -30, "minecraft:flower_pot");
        placeSmall(p, -34, Y + 1, -31, "rod");
        placeSmall(p, -33, Y + 1, -30, "brew");
        placeSmall(p, -32, Y + 1, -31, "candle");
        p.set(-31, Y + 1, -30, St.of("crafter", "crafting", "false", "orientation", "up_north", "triggered", "false"));
        for (int x : new int[]{-36, -34, -32}) {
            p.set(x, Y, -32, bottom("minecraft:smooth_stone_slab"));
            p.set(x, Y, -29, bottom("minecraft:smooth_stone_slab"));
        }
        // The fume hood in the west wall: a steel frame, the work surface inside, the glass sash, a grate to the duct.
        for (int z = -36; z <= -32; z++) {
            for (int y = Y; y <= Y + 3; y++) {
                boolean side = z == -36 || z == -32;
                if (y == Y + 3 || side) {
                    p.set(-41, y, z, "minecraft:iron_block");
                    p.set(-42, y, z, "minecraft:iron_block");
                } else if (y == Y) {
                    p.set(-41, y, z, "minecraft:light_gray_concrete");
                    p.set(-42, y, z, "minecraft:smooth_stone");
                } else {
                    p.set(-41, y, z, St.pane("minecraft:glass_pane"));
                    p.set(-42, y, z, St.AIR);
                }
            }
        }
        p.set(-42, Y + 1, -34, brewing(true, false, true));
        p.set(-42, Y + 1, -33, St.candle("lime", 1, false));
        p.set(-42, Y + 4, -34, "minecraft:waxed_oxidized_copper_grate");
        p.set(-41, Y + 4, -34, "minecraft:waxed_oxidized_copper_grate");
        // The specimen shelf on the north wall: drawers and loot below, two rows of jars.
        for (int x = -40; x <= -35; x++) {
            p.set(x, Y, -37, x == -38 || x == -37 ? St.of("dispenser", "facing", "south", "triggered", "false") : "minecraft:smooth_stone");
            p.set(x, Y + 2, -37, top("minecraft:smooth_stone_slab"));
        }
        loot(p, -40, Y, -37, barrel("south"), "supernaturalcraft:chests/bunker_lab");
        loot(p, -35, Y, -37, barrel("south"), "supernaturalcraft:chests/bunker_lab");
        String[] jars = {"minecraft:potted_red_mushroom", "skull", "minecraft:potted_crimson_roots", "pot", "minecraft:potted_warped_roots", "amethyst"};
        String[] jars2 = {"candle", "minecraft:potted_cactus", "minecraft:flower_pot", "brew", "minecraft:potted_azure_bluet", "candle"};
        for (int i = 0; i < 6; i++) {
            placeSmall(p, -40 + i, Y + 1, -37, jars[i]);
            placeSmall(p, -40 + i, Y + 3, -37, jars2[i]);
        }
        // The chalkboard by the door, notes pinned to it.
        for (int x = -34; x <= -31; x++) for (int y = Y + 1; y <= Y + 3; y++) p.set(x, y, -38, x == -34 || x == -31 ? "minecraft:spruce_planks" : "minecraft:black_concrete");
        frame(p, -33, Y + 2, -37, "south", "minecraft:paper");
        frame(p, -32, Y + 2, -37, "south", "minecraft:map");
        // The autopsy table under its lamp, specimens on the east wall, the infirmary bed and the sample fridge.
        for (int x = -28; x <= -27; x++) p.set(x, Y, -33, "minecraft:iron_block");
        p.set(-28, Y + 1, -33, St.carpet("white"));
        p.set(-27, Y + 1, -33, skull("minecraft:skeleton_skull", 12));
        pendant(p, -28, F + 6, -33, 2, false);
        String[] specimens = {"minecraft:spider_eye", "minecraft:fermented_spider_eye", "minecraft:ghast_tear", "minecraft:phantom_membrane"};
        for (int i = 0; i < 4; i++) frame(p, -25, Y + 2, -28 + i % 2 * 2 - (i / 2), "west", specimens[i]);
        Level2Props.bed(p, -26, Y, -35, "white", "north");
        p.set(-27, Y, -36, rod("up"));
        p.set(-27, Y + 1, -36, rod("up"));
        p.set(-27, Y + 2, -36, "minecraft:glass");
        for (int z = -31; z <= -30; z++) {
            for (int y = Y; y <= Y + 2; y++) p.set(-25, y, z, "minecraft:iron_block");
            door(p, -26, Y, z, "minecraft:iron_door", "west", z == -31 ? "left" : "right", false);
        }
        p.view("lab", -29, Y, -37, -36, Y + 0.5, -28);
    }

    /** A small thing on a bench or shelf, by name: a brewing stand, a microscope, a candle, a pot, a skull, a crystal, or a state. */
    private static void placeSmall(Plan p, int x, int y, int z, String what) {
        String s = switch (what) {
            case "-" -> null;
            case "brew" -> brewing((x & 1) == 0, true, (z & 1) == 0);
            case "rod" -> rod("up");
            case "candle" -> St.candle(Math.floorMod(x, 2) == 0 ? "lime" : "white", 1 + Math.floorMod(x, 3), false);
            case "pot" -> pot("south");
            case "skull" -> skull("minecraft:skeleton_skull", 8);
            case "amethyst" -> amethyst("up");
            default -> what;
        };
        if (s != null) p.set(x, y, z, s);
    }

    // --- the baths -----------------------------------------------------------------------------------------------------------

    /**
     * The baths: aqua tile over a dark base, three glass-fronted showers along the south wall, two toilet stalls on the east, a
     * row of basins with taps under a long mirror on the west, a bench with towels, a hamper and the towel shelf.
     */
    private static void baths(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            if (off == 1) return "minecraft:dark_prismarine";
            if (off == 4) return "minecraft:polished_diorite";
            return off == 5 ? "minecraft:calcite" : "minecraft:prismarine_bricks";
        };
        room(p, new Room(-22, -12, F, 5, -37, -25), new Style(checker(1, solid("minecraft:calcite"), solid("minecraft:prismarine_bricks")), wall,
                solid("minecraft:calcite"), null, null, 0, "minecraft:sea_lantern", 4));
        doorway(p, -20, F, -39, "south", 2, 1, 2, BLACK, null, St.door("minecraft:birch_door", "south", "right", false));
        plaque(p, -18, "", "Baths", "", "");
        // Showers.
        for (int x : new int[]{-22, -19, -16, -13}) for (int z = -27; z <= -25; z++) for (int y = Y; y <= Y + 2; y++) p.set(x, y, z, "minecraft:prismarine_bricks");
        for (int sx : new int[]{-21, -18, -15}) {
            for (int x = sx; x <= sx + 1; x++) {
                for (int y = Y; y <= Y + 1; y++) p.set(x, y, -27, St.pane("minecraft:white_stained_glass_pane"));
                for (int z = -26; z <= -25; z++) p.set(x, F, z, "minecraft:dark_prismarine");
            }
            p.set(sx, Y + 2, -25, rod("north"));
            p.set(sx + 1, Y + 1, -25, lever("wall", "north"));
        }
        p.set(-12, Y, -26, barrel("west"));
        p.set(-12, Y, -25, barrel("west"));
        p.set(-12, Y + 1, -26, St.carpet("white"));
        p.set(-12, Y + 1, -25, St.carpet("light_blue"));
        // Toilet stalls.
        for (int z : new int[]{-35, -32}) for (int x = -14; x <= -12; x++) for (int y = Y; y <= Y + 2; y++) p.set(x, y, z, "minecraft:quartz_bricks");
        for (int z : new int[]{-37, -34}) for (int y = Y; y <= Y + 2; y++) p.set(-14, y, z, "minecraft:quartz_bricks");
        for (int z : new int[]{-36, -33}) {
            door(p, -14, Y, z, "minecraft:birch_door", "west", "left", false);
            chair(p, -12, Y, z, "west", "minecraft:quartz_stairs", null);
            p.set(-12, Y + 1, z, St.button("minecraft:stone_button", "wall", "west"));
            frame(p, -13, Y + 1, z == -36 ? -37 : -34, z == -36 ? "south" : "south", "minecraft:paper");
        }
        // Basins and the mirror.
        for (int z = -36; z <= -30; z++) {
            boolean basin = z == -35 || z == -33 || z == -31;
            p.set(-22, Y, z, basin ? water(3) : "minecraft:smooth_quartz");
            if (basin) p.set(-22, Y + 1, z, hook("east"));
            for (int y = Y + 2; y <= Y + 3; y++) p.set(-23, y, z, "minecraft:glass");
        }
        p.set(-22, Y + 1, -36, "minecraft:potted_lily_of_the_valley");
        p.set(-22, Y + 1, -30, St.candle("white", 1, false));
        wallLight(p, -23, Y + 3, -37, "east", "minecraft:sea_lantern");
        // The bench with towels, a hamper.
        for (int x = -18; x <= -16; x++) p.set(x, Y, -31, top("minecraft:birch_slab"));
        p.set(-18, Y + 1, -31, St.carpet("white"));
        p.set(-17, Y + 1, -31, St.carpet("light_blue"));
        p.set(-12, Y, -29, "minecraft:composter[level=4]");
        p.set(-12, Y, -30, "minecraft:cauldron");
        p.view("baths", -20, Y, -37, -16, Y + 0.5, -26);
    }

    // --- the armoury ---------------------------------------------------------------------------------------------------------

    /**
     * The armoury: dark steel tile, five suits on stands under racks of arms, a pegboard wall of weapons over the ammunition
     * crates, a workbench of anvil, grindstone and smithing table, a table laid with arms under a lamp, and the locked cage.
     */
    private static void armory(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            if (off == 1) return SKIRTING;
            if (off == 4) return "minecraft:polished_deepslate";
            return off == 5 ? "minecraft:deepslate_bricks" : "minecraft:deepslate_tiles";
        };
        room(p, new Room(28, 41, F, 5, -37, -25), new Style(checker(2, solid("minecraft:polished_deepslate"), solid("minecraft:smooth_basalt")), wall,
                solid("minecraft:deepslate_tiles"), null, null, 0, St.bulb("minecraft:waxed_copper_bulb"), 5));
        doorway(p, 29, F, -39, "south", 2, 1, 2, BLACK, null, St.door("minecraft:spruce_door", "south", "right", false));
        plaque(p, 31, "", "Armory", "", "");
        // Suits on stands along the north wall, arms racked over them and between.
        String[][] suits = {
                {"minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:crossbow", ""},
                {"-", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:iron_sword", "3B2A1E"},
                {"minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots", "minecraft:iron_axe", ""},
                {"minecraft:turtle_helmet", "minecraft:iron_chestplate", "-", "minecraft:iron_boots", "minecraft:trident", ""},
                {"minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:bow", "4A5A3A"}};
        for (int i = 0; i < 5; i++) {
            int x = 32 + 2 * i;
            String[] s = suits[i];
            p.decor(Decor.armorStand(x, Y, -37, "south", s[0], s[1], s[2], s[3], s[4], s[5]));
            frame(p, x, Y + 3, -37, "south", i % 2 == 0 ? "minecraft:crossbow" : "minecraft:bow");
            if (i < 4) {
                frame(p, x + 1, Y + 1, -37, "south", "minecraft:iron_sword|1");
                frame(p, x + 1, Y + 2, -37, "south", "minecraft:crossbow|1");
                frame(p, x + 1, Y + 3, -37, "south", "minecraft:iron_axe|1");
            }
        }
        // The pegboard of arms over the crates on the east wall.
        String[] arms = {"minecraft:crossbow", "minecraft:bow", "minecraft:iron_sword", "minecraft:iron_axe", "minecraft:trident", "minecraft:mace",
                "minecraft:shield", "minecraft:arrow", "minecraft:spectral_arrow"};
        for (int z = -35; z <= -27; z++) {
            for (int y = Y + 1; y <= Y + 3; y++) {
                p.set(42, y, z, "minecraft:spruce_planks");
                frame(p, 41, y, z, "west", arms[Math.floorMod(z * 3 + y, arms.length)]);
            }
            p.set(41, Y, z, z % 4 == 1 ? chest("west") : barrel("west"));
        }
        loot(p, 41, Y, -35, barrel("west"), "supernaturalcraft:chests/bunker_armory");
        loot(p, 41, Y, -31, barrel("west"), "supernaturalcraft:chests/bunker_armory");
        loot(p, 41, Y, -28, barrel("west"), "supernaturalcraft:chests/bunker_armory");
        // The workbench along the south wall, tools over it.
        String[] work = {"minecraft:crafting_table", "minecraft:smithing_table", St.facing("minecraft:anvil", "east"),
                St.of("grindstone", "face", "floor", "facing", "north"), St.facing("minecraft:stonecutter", "north"), "minecraft:fletching_table",
                oven("minecraft:blast_furnace", "north"), barrel("north"), "minecraft:chipped_anvil[facing=east]", chest("north")};
        String[] tools = {"minecraft:iron_pickaxe", "minecraft:shears", "minecraft:flint_and_steel", "minecraft:brush", "minecraft:spyglass",
                "minecraft:arrow", "minecraft:iron_shovel", "minecraft:lead", "minecraft:clock", "minecraft:compass"};
        for (int i = 0; i < work.length; i++) {
            p.set(30 + i, Y, -25, work[i]);
            frame(p, 30 + i, Y + 2, -25, "north", tools[i]);
        }
        p.set(37, Y + 1, -25, St.lantern(false));
        // The table laid with arms, under a lamp.
        for (int x = 33; x <= 37; x++) for (int z = -31; z <= -30; z++) p.set(x, Y, z, "minecraft:stripped_spruce_wood");
        String[] laid = {"minecraft:crossbow", "minecraft:iron_sword", "minecraft:bow", "minecraft:arrow", "minecraft:iron_axe", "minecraft:spyglass",
                "minecraft:arrow", "minecraft:crossbow", "minecraft:shears"};
        for (int i = 0; i < laid.length; i++) {
            int x = 33 + i % 5, z = i < 5 ? -31 : -30;
            if (x == 35 && z == -30) continue;
            frame(p, x, Y + 1, z, "up", laid[i] + "|" + (i % 4));
        }
        p.set(35, Y + 1, -30, St.lantern(false));
        shadedPendant(p, 35, F + 6, -31, 3, "minecraft:waxed_weathered_copper_trapdoor");
        // The cage on the west wall: bars to the ceiling, a door, chests and crossbows inside.
        for (int z = -34; z <= -26; z++) {
            for (int y = Y; y <= Y + 4; y++) {
                if (z == -30 && y <= Y + 1) continue;
                p.set(30, y, z, St.bars());
                if (z == -34 || z == -26) for (int x = 28; x <= 29; x++) p.set(x, y, z, St.bars());
            }
        }
        door(p, 30, Y, -30, "minecraft:iron_door", "east", "left", false);
        for (int z = -33; z <= -31; z++) p.set(28, Y, z, chest("east"));
        p.set(28, Y, -28, barrel("east"));
        p.set(28, Y, -27, St.of("target", "power", "0"));
        for (int z = -32; z <= -28; z++) frame(p, 28, Y + 2, z, "east", "minecraft:crossbow");
        p.decor(Decor.armorStand(29, Y, -28, "east", "minecraft:netherite_helmet", "minecraft:netherite_chestplate", "-", "-", "-", ""));
        p.view("armory", 30, Y, -36, 38, Y + 0.5, -29);
    }
}
