package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;
import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level2Props.*;

/**
 * Level 3 (pure): the depths. The stairwell's door opens on a service corridor of stone and pipes running east and west;
 * off it the garage (west, a step down, see {@link Level3Garage}), the dungeon with its devil's trap and the artifact vault
 * (south), the boiler room (north).
 *
 * <pre>
 *   z -57 .. -45   garage (x -37 .. -13, to z -27)          boiler room (x 8 .. 24)
 *   z -44 .. -40   the corridor, x -13 .. 23 (inside z -43 .. -41)
 *   z -39 .. -23   dungeon (x -10 .. 2)  ·  artifact vault (x 3 .. 23)
 * </pre>
 */
public final class Level3 {

    static final int F = Zones.LEVEL3, Y = F + 1;
    /** The dungeon's devil's trap: its centre (painted by the builder on bare floor). */
    public static final int[] TRAP = {-4, Zones.LEVEL3 + 1, -33};

    private Level3() {
    }

    public static void build(Plan p) {
        p.zone(Zones.LEVEL_3);
        Level3Garage.build(p);
        corridor(p);
        Level3Garage.door(p);
        dungeon(p);
        vault(p);
        boiler(p);
        lobby(p);
        p.zone(null);
    }

    // --- the corridor ----------------------------------------------------------------------------------------------------------

    private static void corridor(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            if (off == 1) return SKIRTING;
            if (off == 4) return "minecraft:polished_andesite";
            return mix(81, "minecraft:stone_bricks", 12, "minecraft:cracked_stone_bricks", 1, "minecraft:mossy_stone_bricks", 1).at(x, y, z);
        };
        room(p, new Room(-12, 22, F, 4, -43, -41), new Style(concreteFloor(), wall, solid("minecraft:smooth_stone"), null, null, 0, null, 0));
        // The pipe along the north wall (copper joints every eight blocks) and a cable along the south.
        for (int x = -12; x <= 22; x++) {
            p.set(x, F + 4, -43, Math.floorMod(x, 8) == 0 ? "minecraft:waxed_copper_grate" : rod("east"));
            p.set(x, F + 4, -41, St.chain("x"));
        }
        for (int x : new int[]{-9, -2, 5, 11, 19}) p.set(x, F + 3, -43, St.lantern(true));
        // The east end: a fuse panel of drawers and levers, a warning.
        for (int z = -43; z <= -41; z++) {
            p.set(23, F + 2, z, St.of("dropper", "facing", "west", "triggered", "false"));
            p.set(22, F + 3, z, lever("wall", "west"));
        }
        sign(p, 22, F + 1, -42, "west", "spruce", "red", "", "DANGER", "High Voltage", "");
        p.set(22, F + 1, -43, "minecraft:cauldron");
        sign(p, -6, F + 3, -41, "north", "dark_oak", "yellow", "", "Dungeon", "", "");
        sign(p, 10, F + 3, -41, "north", "dark_oak", "yellow", "", "Storage", "Keep Out", "");
        sign(p, 18, F + 3, -43, "south", "dark_oak", "yellow", "", "Boiler Room", "", "");
        p.view("level3_hall", 20, Y, -42, -12, Y + 0.5, -42);
    }

    /** The lobby from the stairwell's door (z -46) to the corridor. */
    private static void lobby(Plan p) {
        enclose(p, -3, 3, F - 1, F + 6, -46, -45);
        Pattern wall = lebanonWall(F);
        for (int z = -46; z <= -45; z++) {
            for (int x = -2; x <= 2; x++) {
                boolean edge = Math.abs(x) == 2;
                p.set(x, F, z, edge ? FLOOR_BORDER : concreteFloor().at(x, F, z));
                for (int y = F + 1; y <= F + 4; y++) p.set(x, y, z, edge ? wall.at(x, y, z) : St.AIR);
                p.set(x, F + 5, z, CREAM);
            }
        }
        doorway(p, 0, F, -45, "south", 2, 3, 4, BLACK, BLACK_STAIRS, null);
    }

    // --- the dungeon ---------------------------------------------------------------------------------------------------------

    /**
     * The dungeon: riveted iron walls on a black base, a floor of dark tile with a red circle round the devil's trap, the same
     * trap painted on the ceiling over it, one bare bulb on a chain, shackles hanging, the chair at the trap's edge, the holy
     * water and the book, shelves of jars and salt. A heavy iron door in a recess, a lever either side.
     */
    private static void dungeon(Plan p) {
        int cx = TRAP[0], cz = TRAP[2];
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            if (off == 1) return SKIRTING;
            if (off == 5) return BLACK;
            return Math.floorMod(x + z, 3) == 0 ? St.axis("minecraft:polished_basalt", "y") : "minecraft:iron_block";
        };
        Pattern floor = (x, y, z) -> {
            if (Math.max(Math.abs(x - cx), Math.abs(z - cz)) == 2) return "minecraft:polished_deepslate";
            return mix(82, "minecraft:deepslate_tiles", 10, "minecraft:cracked_deepslate_tiles", 2, "minecraft:polished_deepslate", 3).at(x, y, z);
        };
        room(p, new Room(-9, 1, F, 5, -38, -28), new Style(floor, wall, solid("minecraft:smooth_stone"), null, null, 0, null, 0));
        // The sigil on the ceiling: a circle and a pentagram.
        int ceiling = F + 6;
        double[][] pt = new double[5][];
        for (int k = 0; k < 5; k++) pt[k] = new double[]{cx + 4.3 * Math.sin(2 * Math.PI * k / 5), cz - 4.3 * Math.cos(2 * Math.PI * k / 5)};
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int z = cz - 5; z <= cz + 5; z++) {
                boolean on = Math.abs(Math.hypot(x - cx, z - cz) - 4.5) < 0.4;
                for (int k = 0; k < 5 && !on; k++) on = segment(x, z, pt[k], pt[(k + 2) % 5]) < 0.3;
                if (on) p.set(x, ceiling, z, "minecraft:red_terracotta");
            }
        }
        // The bulb, the shackles.
        pendant(p, cx, ceiling, cz, 2, false);
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            p.set(cx + c[0], F + 5, cz + c[1], St.chain("y"));
            p.set(cx + c[0], F + 4, cz + c[1], St.chain("y"));
        }
        // The chair at the trap's south edge, facing the door.
        chair(p, cx, Y, cz + 2, "north", "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        p.set(cx, Y, cz + 3, St.chain("x"));
        // The door: a recess in the corridor's wall, the iron door, levers on the lintel both sides.
        for (int z = -40; z <= -39; z++) {
            for (int y = F + 1; y <= F + 3; y++) {
                p.set(cx - 1, y, z, BLACK);
                p.set(cx + 1, y, z, BLACK);
            }
        }
        for (int y = F + 1; y <= F + 3; y++) p.set(cx, y, -40, St.AIR);
        p.set(cx, F + 4, -40, BLACK);
        p.set(cx, F + 3, -39, BLACK);
        door(p, cx, Y, -39, "minecraft:iron_door", "south", "left", false);
        p.set(cx, F + 3, -40, lever("wall", "north"));
        p.set(cx, F + 3, -38, lever("wall", "south"));
        for (int x = cx - 1; x <= cx + 1; x++) p.set(x, Y, -37, St.carpet("white"));
        // West wall: iron shelving of jars and salt, wall chains.
        for (int z = -37; z <= -35; z++) {
            p.set(-9, Y, z, barrel("east"));
            p.set(-9, Y + 1, z, z == -36 ? "minecraft:glass" : St.candle(z == -37 ? "white" : "red", 2, z == -37));
            p.set(-9, Y + 2, z, top("minecraft:polished_blackstone_slab"));
            p.set(-9, Y + 3, z, z == -35 ? skull("minecraft:skeleton_skull", 4) : z == -36 ? "minecraft:potted_wither_rose" : "minecraft:glass");
        }
        for (int z : new int[]{-32, -30}) for (int y = Y + 1; y <= Y + 3; y++) p.set(-9, y, z, St.chain("y"));
        // East wall: the instrument table, the holy water, the book open on its lectern.
        for (int z = -37; z <= -35; z++) p.set(1, Y, z, "minecraft:polished_blackstone");
        frame(p, 1, Y + 1, -37, "up", "minecraft:iron_sword|2");
        frame(p, 1, Y + 1, -36, "up", "minecraft:shears|1");
        frame(p, 1, Y + 1, -35, "up", "minecraft:flint_and_steel");
        p.set(1, Y, -34, water(3));
        p.set(1, Y, -33, lectern("west", true));
        p.set(1, Y, -31, barrel("west"));
        p.set(1, Y + 1, -31, St.candle("black", 3, true));
        // South wall: a bench, a bucket, more chains.
        for (int x = -8; x <= -6; x++) p.set(x, Y, -28, top("minecraft:polished_blackstone_slab"));
        p.set(-7, Y + 1, -28, St.candle("white", 1, false));
        p.set(0, Y, -28, "minecraft:cauldron");
        for (int y = Y + 1; y <= Y + 3; y++) p.set(-2, y, -28, St.chain("y"));
        p.view("dungeon", -8, Y, -37, cx, Y, cz);
    }

    /** The distance from a cell's centre to a segment. */
    private static double segment(int x, int z, double[] a, double[] b) {
        double dx = b[0] - a[0], dz = b[1] - a[1];
        double t = Math.max(0, Math.min(1, ((x - a[0]) * dx + (z - a[1]) * dz) / (dx * dx + dz * dz)));
        return Math.hypot(x - (a[0] + t * dx), z - (a[1] + t * dz));
    }

    // --- the artifact vault ----------------------------------------------------------------------------------------------------

    /**
     * The artifact vault: glass-fronted cases of cursed curios either side of the door, two locked iron cages, rows of shelves of
     * boxes, jars, skulls and oddities, and in the middle, raised on a stepped dais under a cold lamp, the vault itself.
     */
    private static void vault(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            if (off == 1) return SKIRTING;
            if (off == 5) return "minecraft:smooth_stone";
            if (off == 4) return "minecraft:chiseled_stone_bricks";
            return mix(83, "minecraft:stone_bricks", 10, "minecraft:cracked_stone_bricks", 1).at(x, y, z);
        };
        Pattern floor = (x, y, z) -> {
            int d = Math.max(Math.abs(x - 13), Math.abs(z + 31));
            if (d == 3) return "minecraft:polished_blackstone";
            return mix(84, "minecraft:polished_andesite", 10, "minecraft:andesite", 1).at(x, y, z);
        };
        room(p, new Room(4, 22, F, 5, -38, -24), new Style(floor, wall, solid("minecraft:smooth_stone"), null, null, 0, null, 0));
        doorway(p, 13, F, -40, "south", 2, 3, 3, BLACK, BLACK_STAIRS, null);
        // The cases along the north wall.
        String[] curios = {"minecraft:totem_of_undying", "minecraft:ender_eye", "minecraft:nether_star", "minecraft:heart_of_the_sea",
                "minecraft:echo_shard", "minecraft:skull_banner_pattern", "minecraft:goat_horn", "minecraft:recovery_compass", "minecraft:trial_key",
                "minecraft:ominous_trial_key", "minecraft:ominous_bottle", "minecraft:amethyst_shard", "minecraft:golden_apple", "minecraft:music_disc_11",
                "minecraft:rabbit_foot", "minecraft:nautilus_shell", "minecraft:breeze_rod", "minecraft:wind_charge", "minecraft:blaze_rod",
                "minecraft:ender_pearl", "minecraft:enchanted_book", "minecraft:clock", "minecraft:prismarine_crystals", "minecraft:ghast_tear"};
        int n = 0;
        for (int[] run : new int[][]{{5, 10}, {16, 21}}) {
            for (int z = -38; z <= -37; z++) {
                for (int y = Y; y <= Y + 3; y++) {
                    p.set(run[0] - 1, y, z, St.axis("minecraft:stripped_dark_oak_log", "y"));
                    p.set(run[1] + 1, y, z, St.axis("minecraft:stripped_dark_oak_log", "y"));
                }
            }
            for (int x = run[0]; x <= run[1]; x++) {
                for (int z = -38; z <= -37; z++) {
                    p.set(x, Y, z, "minecraft:stripped_dark_oak_wood");
                    p.set(x, Y + 3, z, top("minecraft:dark_oak_slab"));
                }
                for (int y = Y + 1; y <= Y + 2; y++) {
                    p.set(x, y, -38, St.AIR);
                    p.set(x, y, -37, St.pane("minecraft:glass_pane"));
                    glow(p, x, y, -38, "south", curios[n++ % curios.length]);
                }
            }
        }
        // The dais and the vault on it, candles of light at its corners, the cold lamp over it.
        int vx = 13, vz = -31;
        for (int x = vx - 2; x <= vx + 2; x++) {
            for (int z = vz - 2; z <= vz + 2; z++) {
                int dx = x - vx, dz = z - vz;
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                    String toward = Math.abs(dz) == 2 ? (dz < 0 ? "south" : "north") : (dx < 0 ? "east" : "west");
                    p.set(x, Y, z, St.stairs("minecraft:polished_tuff_stairs", toward, false));
                } else {
                    p.set(x, Y, z, dx == 0 && dz == 0 ? "minecraft:chiseled_tuff" : "minecraft:polished_tuff");
                    if (Math.abs(dx) == 1 && Math.abs(dz) == 1) p.set(x, Y + 1, z, endRod("up"));
                }
            }
        }
        p.set(vx, Y + 1, vz, St.of("vault", "facing", "north", "ominous", "false", "vault_state", "inactive"));
        p.decor(Decor.vault(vx, Y + 1, vz, "supernaturalcraft:chests/bunker_vault", "supernaturalcraft:bunker_key"));
        pendant(p, vx, F + 6, vz, 2, true);
        // Two locked cages on the west side: a wither skull on its plinth, a scarecrow's suit.
        for (int cz : new int[]{-33, -29}) {
            for (int x = 5; x <= 7; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    boolean centre = x == 6 && z == cz;
                    for (int y = Y; y <= Y + 2; y++) if (!centre) p.set(x, y, z, St.bars());
                    p.set(x, Y + 3, z, bottom("minecraft:polished_blackstone_slab"));
                }
            }
            door(p, 7, Y, cz, "minecraft:iron_door", "east", "left", false);
        }
        p.set(6, Y, -33, "minecraft:polished_blackstone");
        p.set(6, Y + 1, -33, skull("minecraft:wither_skeleton_skull", 4));
        p.decor(Decor.armorStand(6, Y, -29, "east", "minecraft:carved_pumpkin", "minecraft:leather_chestplate", "minecraft:leather_leggings",
                "minecraft:leather_boots", "minecraft:iron_hoe", "6B5A3A"));
        // Rows of shelves: a double row and the east wall, the south wall.
        for (int x = 18; x <= 19; x++) {
            for (int y = Y; y <= Y + 3; y++) {
                p.set(x, y, -35, St.axis("minecraft:stripped_dark_oak_log", "y"));
                p.set(x, y, -26, St.axis("minecraft:stripped_dark_oak_log", "y"));
            }
            for (int z = -34; z <= -27; z++) shelf(p, x, z, x == 18 ? "west" : "east");
        }
        for (int z = -35; z <= -25; z++) shelf(p, 22, z, "west");
        for (int x = 4; x <= 15; x++) shelf(p, x, -24, "north");
        // The ledger on its lectern by the door; lamps over the aisles.
        p.set(16, Y, -36, lectern("west", true));
        for (int[] c : new int[][]{{20, -30}, {16, -27}, {9, -31}, {10, -26}, {20, -36}}) pendant(p, c[0], F + 6, c[1], 2, false);
        p.view("artifact_vault", 13, Y, -37, 13, Y + 1, -29);
    }

    private static final String[] BOXES = {"barrel", "chest", "trapped_chest", "dropper", "barrel", "lodestone", "chiseled_bookshelf", "barrel",
            "respawn_anchor", "chest"};
    private static final String[] CURIOS = {"pot", "-", "skull", "candle", "-", "flower_pot", "amethyst", "pot", "-", "head", "potted_wither_rose",
            "ender_chest", "candle", "-", "heavy_core", "lantern", "pot", "brewing", "-", "head"};

    /** A shelf: a box on the floor, a curio on it, a board, a curio on the board. */
    private static void shelf(Plan p, int x, int z, String out) {
        int a = (int) (Plan.noise(x, 0, z, 85) * BOXES.length), b = (int) (Plan.noise(x, 1, z, 86) * CURIOS.length),
                c = (int) (Plan.noise(x, 2, z, 87) * CURIOS.length);
        String box = switch (BOXES[a]) {
            case "barrel" -> barrel(out);
            case "chest" -> chest(out);
            case "trapped_chest" -> St.of("trapped_chest", "facing", out, "type", "single", "waterlogged", "false");
            case "dropper" -> St.of("dropper", "facing", out, "triggered", "false");
            case "chiseled_bookshelf" -> books(out, (x * 7 + z) & 63);
            case "respawn_anchor" -> St.of("respawn_anchor", "charges", "0");
            default -> "minecraft:" + BOXES[a];
        };
        p.set(x, Y, z, box);
        curio(p, x, Y + 1, z, CURIOS[b], out);
        p.set(x, Y + 2, z, top("minecraft:dark_oak_slab"));
        curio(p, x, Y + 3, z, CURIOS[c], out);
    }

    private static void curio(Plan p, int x, int y, int z, String what, String out) {
        int rot = Math.floorMod(x * 5 + z * 3, 16);
        String[] heads = {"minecraft:zombie_head", "minecraft:creeper_head", "minecraft:piglin_head", "minecraft:player_head"};
        String s = switch (what) {
            case "-" -> null;
            case "pot" -> pot(out);
            case "skull" -> skull("minecraft:skeleton_skull", rot);
            case "head" -> skull(heads[Math.floorMod(x + z, heads.length)], rot);
            case "candle" -> St.candle(Math.floorMod(x + z, 2) == 0 ? "black" : "white", 1 + Math.floorMod(x - z, 3), false);
            case "flower_pot" -> "minecraft:flower_pot";
            case "amethyst" -> amethyst("up");
            case "potted_wither_rose" -> "minecraft:potted_wither_rose";
            case "ender_chest" -> St.of("ender_chest", "facing", out, "waterlogged", "false");
            case "heavy_core" -> "minecraft:heavy_core[waterlogged=false]";
            case "lantern" -> St.lantern(false);
            case "brewing" -> brewing(true, false, false);
            default -> null;
        };
        if (s != null) p.set(x, y, z, s);
    }

    // --- the boiler room -------------------------------------------------------------------------------------------------------

    /**
     * The boiler room: two horizontal copper boilers on black saddles, their fireboxes glowing under them, gauges on their ends,
     * valves on their fronts; pipes from their domes to a main run along the back wall, a hot line glowing along the front, a
     * green copper water tank, a control desk and a heap of coal by the door.
     */
    private static void boiler(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - F;
            return off == 1 || off == 5 ? "minecraft:polished_tuff" : "minecraft:tuff_bricks";
        };
        room(p, new Room(9, 23, F, 5, -56, -46), new Style(mix(88, "minecraft:smooth_basalt", 10, "minecraft:deepslate_tiles", 3, "minecraft:cracked_deepslate_tiles", 1),
                wall, solid("minecraft:polished_tuff"), null, null, 0, null, 0));
        doorway(p, 16, F, -44, "north", 2, 1, 2, BLACK, null, St.door("minecraft:waxed_exposed_copper_door", "north", "left", false));
        boilerBody(p, 10, "minecraft:waxed_copper_block", "minecraft:waxed_cut_copper", "minecraft:waxed_chiseled_copper", "minecraft:waxed_cut_copper_stairs", -1);
        boilerBody(p, 18, "minecraft:waxed_weathered_copper", "minecraft:waxed_weathered_cut_copper", "minecraft:waxed_weathered_chiseled_copper",
                "minecraft:waxed_weathered_cut_copper_stairs", 1);
        // Gauges on the inner ends, valves and wheels on the fronts.
        frame(p, 15, Y + 2, -52, "east", "minecraft:clock");
        frame(p, 15, Y + 3, -52, "east", "minecraft:compass");
        frame(p, 17, Y + 2, -52, "west", "minecraft:clock");
        frame(p, 17, Y + 3, -52, "west", "minecraft:recovery_compass");
        for (int x0 : new int[]{10, 18}) {
            p.set(x0 + 1, Y + 2, -50, St.button("minecraft:polished_blackstone_button", "wall", "south"));
            p.set(x0 + 3, Y + 2, -50, lever("wall", "south"));
            frame(p, x0 + 2, Y + 2, -50, "south", "minecraft:clock");
        }
        // The pipes: domes up to the main run at the back, the run along it, a riser in the corner, the hot line in front.
        for (int x = 9; x <= 23; x++) p.set(x, Y + 4, -55, Math.floorMod(x, 5) == 1 ? "minecraft:waxed_exposed_copper_grate" : rod("east"));
        for (int x : new int[]{12, 20}) {
            p.set(x, Y + 4, -52, "minecraft:waxed_copper_grate");
            p.set(x, Y + 4, -53, rod("north"));
            p.set(x, Y + 4, -54, rod("north"));
        }
        for (int y = Y; y <= Y + 3; y++) p.set(9, y, -55, rod("up"));
        for (int x = 10; x <= 22; x++) p.set(x, Y + 4, -47, x == 16 ? "minecraft:waxed_copper_grate" : endRod("east"));
        for (int y = Y + 1; y <= Y + 3; y++) p.set(23, y, -47, St.chain("y"));
        for (int x : new int[]{11, 16, 19}) p.set(x, Y + 3, -55, St.lantern(true));
        // The water tank in the north-east corner.
        for (int x = 21; x <= 23; x++) {
            for (int z = -56; z <= -55; z++) {
                for (int y = Y; y <= Y + 2; y++) p.set(x, y, z, x == 22 && y == Y + 1 ? "minecraft:waxed_oxidized_cut_copper" : "minecraft:waxed_oxidized_copper");
                p.set(x, Y + 3, z, bottom("minecraft:waxed_oxidized_cut_copper_slab"));
            }
        }
        // The control desk on the south wall.
        for (int x = 20; x <= 21; x++) p.set(x, Y, -46, top("minecraft:polished_blackstone_slab"));
        p.set(20, Y + 1, -46, St.lantern(false));
        for (int x = 19; x <= 22; x++) frame(p, x, Y + 2, -46, "north", x % 2 == 0 ? "minecraft:clock" : "minecraft:compass");
        p.set(19, Y + 1, -46, lever("wall", "north"));
        p.set(22, Y + 1, -46, lever("wall", "north"));
        // The coal heap by the door, a shovel stuck in it, an ash bin.
        for (int x = 9; x <= 11; x++) for (int z = -48; z <= -46; z++) if (!(x == 11 && z == -48)) p.set(x, Y, z, "minecraft:coal_block");
        for (int[] c : new int[][]{{9, -47}, {9, -46}, {10, -46}}) p.set(c[0], Y + 1, c[1], "minecraft:coal_block");
        p.set(9, Y + 2, -46, "minecraft:deepslate_coal_ore");
        p.set(10, Y + 1, -48, "minecraft:coal_ore");
        frame(p, 10, Y + 1, -47, "up", "minecraft:iron_shovel");
        p.set(12, Y, -46, "minecraft:cauldron");
        String bulb = St.bulb("minecraft:waxed_exposed_copper_bulb");
        wallLight(p, 8, Y + 2, -49, "east", bulb);
        wallLight(p, 24, Y + 2, -49, "west", bulb);
        p.view("boiler_room", 16, Y, -47, 16, Y + 1.5, -55);
    }

    /**
     * A horizontal boiler along X from {@code x0} (five long), its axis at z -52, y {@code Y + 2}: a round section of blocks and
     * stairs, banded, capped with chiseled copper and domed at its outer end ({@code outer}: -1 west, 1 east), on two saddles, a
     * firebox glowing under it between them.
     */
    private static void boilerBody(Plan p, int x0, String body, String band, String cap, String stairs, int outer) {
        int zc = -52, yc = Y + 2;
        // The domed outer end: stairs tapering away round a riveted boss.
        int ex = outer < 0 ? x0 - 1 : x0 + 5;
        String in = outer < 0 ? "east" : "west";
        p.set(ex, yc, zc, cap);
        p.set(ex, yc + 1, zc, St.stairs(stairs, in, false));
        p.set(ex, yc - 1, zc, St.stairs(stairs, in, true));
        p.set(ex, yc, zc - 1, St.stairs(stairs, in, false));
        p.set(ex, yc, zc + 1, St.stairs(stairs, in, false));
        for (int x = x0; x <= x0 + 4; x++) {
            boolean end = x == x0 || x == x0 + 4;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    String s;
                    if (dz == 0 || dy == 0) s = end && dz == 0 && dy == 0 ? cap : (x - x0) % 2 == 0 ? band : body;
                    else s = St.stairs(stairs, dz < 0 ? "south" : "north", dy < 0);
                    p.set(x, yc + dy, zc + dz, s);
                }
            }
        }
        for (int x : new int[]{x0 + 1, x0 + 3}) for (int z = zc - 1; z <= zc + 1; z++) p.set(x, Y, z, "minecraft:polished_blackstone_bricks");
        p.set(x0 + 2, Y, zc, "minecraft:magma_block");
        p.set(x0 + 2, Y, zc - 1, St.bars());
        p.set(x0 + 2, Y, zc + 1, St.bars());
    }
}
