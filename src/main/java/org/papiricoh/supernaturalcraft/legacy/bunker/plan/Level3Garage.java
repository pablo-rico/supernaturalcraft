package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;
import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level2Props.*;

/**
 * The garage (pure): a hall of red brick on a concrete plinth, a step down from the service corridor and a block taller than
 * the rest of the level, steel beams and strip lights under its ceiling, a polished concrete floor with oil stains and yellow
 * bay lines. In the south bays the black 1967 Impala, a red pickup and a motorbike, a petrol pump; in the north a baby-blue
 * Bel Air up on the lift, the workbench under the tool wall and an engine hanging from its hoist. The roller door in the west
 * wall is sealed.
 */
final class Level3Garage {

    private static final int FG = Zones.LEVEL3 - 1, Y = FG + 1;
    private static final int X0 = -36, X1 = -14, Z0 = -52, Z1 = -28;

    private Level3Garage() {
    }

    /** A car's paint: body, slab, roof, stairs, glass, headlight, tail light. */
    private record Paint(String body, String slab, String roof, String stairs, String glass, String light, String tail) {
    }

    static void build(Plan p) {
        Pattern wall = (x, y, z) -> {
            int off = y - FG;
            if (off == 1) return SKIRTING;
            if (off == 2) return "minecraft:stone_bricks";
            return mix(91, "minecraft:bricks", 14, "minecraft:mud_bricks", 2, "minecraft:granite", 1).at(x, y, z);
        };
        Pattern floor = (x, y, z) -> {
            boolean southLine = z >= -37 && z <= -29 && (x == -35 || x == -29 || x == -23 || x == -17);
            boolean northLine = z >= -51 && z <= -45 && (x == -35 || x == -29);
            if (southLine || northLine) return "minecraft:yellow_concrete";
            double n = Plan.noise(x, 0, z, 92);
            if (n < 0.05) return "minecraft:gray_concrete_powder";
            if (n < 0.08) return "minecraft:gray_concrete";
            return mix(93, "minecraft:smooth_stone", 10, "minecraft:polished_andesite", 3, "minecraft:light_gray_concrete", 2).at(x, y, z);
        };
        room(p, new Room(X0, X1, FG, 6, Z0, Z1), new Style(floor, wall, solid("minecraft:smooth_stone"), null, "minecraft:polished_andesite", 6, null, 0));
        // Steel beams across, strip lights between them.
        for (int z : new int[]{-49, -43, -37, -31}) for (int x = X0; x <= X1; x++) p.set(x, FG + 6, z, STEEL);
        for (int z : new int[]{-46, -40, -34}) {
            for (int x0 : new int[]{-34, -27, -20}) for (int x = x0; x <= x0 + 2; x++) p.set(x, FG + 6, z, endRod("east"));
        }
        rollerDoor(p);
        wallLight(p, X1 + 1, Y + 3, -50, "west", HIDDEN_LIGHT);
        wallLight(p, X1 + 1, Y + 3, -30, "west", HIDDEN_LIGHT);
        impala(p);
        pickup(p);
        motorbike(p, -20, -34);
        lift(p);
        workshop(p);
        // The petrol pump and oil drums in the south-east bay.
        p.set(-15, Y, -30, "minecraft:red_concrete");
        p.set(-15, Y + 1, -30, "minecraft:red_concrete");
        p.set(-15, Y + 2, -30, "minecraft:white_stained_glass");
        p.set(-16, Y + 1, -30, hook("west"));
        p.set(-16, Y, -30, St.chain("x"));
        p.set(-15, Y, -32, barrel("up"));
        p.set(-15, Y, -33, barrel("up"));
        p.set(-16, Y, -33, "minecraft:cauldron");
        p.view("garage", -35, Y, -40, -31, Y, -33);
        p.view("garage_hall", -16, Y, -39, -30, Y + 1.5, -41);
        p.view("garage_lift", -24, Y, -42, -32, Y + 3, -48);
    }

    /** The doorway from the corridor's west end: a lintel, a step down into the garage, the sign over it. */
    static void door(Plan p) {
        int f = Level3.F;
        for (int z = -43; z <= -41; z++) {
            p.set(-13, f, z, FLOOR_BORDER);
            for (int y = f + 1; y <= f + 3; y++) p.set(-13, y, z, St.AIR);
            p.set(-13, f + 4, z, BLACK);
            p.set(-14, f, z, St.stairs("minecraft:polished_andesite_stairs", "east", false));
        }
        for (int y = f + 1; y <= f + 4; y++) {
            p.set(-13, y, -44, BLACK);
            p.set(-13, y, -40, BLACK);
        }
        sign(p, -14, f + 5, -42, "west", "dark_oak", "yellow", "", "Garage", "", "");
    }

    /** The sealed roller door: iron slats in a frame of hazard stripes, its control lever. */
    private static void rollerDoor(Plan p) {
        int x = X0 - 1;
        for (int z = -45; z <= -37; z++) {
            for (int y = Y; y <= Y + 5; y++) {
                boolean frame = z == -45 || z == -37 || y == Y + 5;
                p.set(x, y, z, frame ? (((y + z) & 1) == 0 ? "minecraft:yellow_concrete" : "minecraft:black_concrete")
                        : St.trapdoor("minecraft:iron_trapdoor", "east", false, true));
            }
        }
        p.set(X0, Y + 1, -46, lever("wall", "east"));
        sign(p, X0, Y + 2, -47, "east", "spruce", "red", "", "KEEP CLEAR", "", "");
    }

    // --- the cars ------------------------------------------------------------------------------------------------------------

    /**
     * A car from its rows, nose first: each row three layers (bottom up) of three cells (west to east). Codes: B body, S slab,
     * R roof, T stairs rising to the tail (windshield), U stairs rising to the nose (rear window), G glass, W wheel, H headlight,
     * I grille, L tail light, F bed floor, Y hay, K a crate, '.' nothing. Chrome bumpers before and behind, hubcaps on the
     * wheels, a plate on the back if {@code plate} is given.
     */
    private static void car(Plan p, int cx, int y0, int noseZ, int dir, Paint c, String[][] rows, String plate) {
        String toTail = dir > 0 ? "south" : "north", toNose = St.opposite(toTail);
        for (int i = 0; i < rows.length; i++) {
            int z = noseZ + dir * i;
            for (int h = 0; h < 3; h++) {
                for (int w = -1; w <= 1; w++) {
                    char ch = rows[i][h].charAt(w + 1);
                    String s = switch (ch) {
                        case 'B' -> c.body;
                        case 'S' -> bottom(c.slab);
                        case 'R' -> c.roof;
                        case 'T' -> St.stairs(c.stairs, toTail, false);
                        case 'U' -> St.stairs(c.stairs, toNose, false);
                        case 'G' -> c.glass;
                        case 'W' -> St.axis("minecraft:polished_basalt", "x");
                        case 'H' -> c.light;
                        case 'I' -> St.bars();
                        case 'L' -> c.tail;
                        case 'F' -> top("minecraft:spruce_slab");
                        case 'Y' -> St.axis("minecraft:hay_block", "z");
                        case 'K' -> barrel("up");
                        default -> null;
                    };
                    if (s != null) p.set(cx + w, y0 + h, z, s);
                }
            }
            if (rows[i][0].charAt(0) == 'W') {
                p.set(cx - 2, y0, z, St.button("minecraft:stone_button", "wall", "west"));
                p.set(cx + 2, y0, z, St.button("minecraft:stone_button", "wall", "east"));
            }
        }
        int front = noseZ - dir, back = noseZ + dir * rows.length;
        for (int w = -1; w <= 1; w++) {
            p.set(cx + w, y0, front, St.trapdoor("minecraft:iron_trapdoor", toNose, false, true));
            if (w != 0 || plate == null) p.set(cx + w, y0, back, St.trapdoor("minecraft:iron_trapdoor", toTail, false, true));
        }
        if (plate != null) sign(p, cx, y0, back, toTail, "birch", "black", "", plate, "", "");
    }

    /** The black 1967 Chevrolet Impala: a full-width chrome grille (its lamps sit in it), long hood, a fastback roof, red tails. */
    private static void impala(Plan p) {
        Paint black = new Paint("minecraft:black_concrete", "minecraft:polished_blackstone_slab", "minecraft:black_concrete",
                "minecraft:polished_blackstone_stairs", "minecraft:tinted_glass", "minecraft:sea_lantern", "minecraft:red_stained_glass");
        car(p, -32, Y, -36, 1, black, new String[][]{
                {"III", "SSS", "..."},
                {"WBW", "SSS", "..."},
                {"BBB", "SSS", "..."},
                {"BBB", "GGG", "TTT"},
                {"BBB", "GGG", "RRR"},
                {"BBB", "GGG", "RRR"},
                {"WBW", "UUU", "..."},
                {"LBL", "SSS", "..."}}, "KAZ 2Y5");
    }

    /** A red pickup, nose in, hay and a crate in its bed. */
    private static void pickup(Plan p) {
        Paint red = new Paint("minecraft:red_concrete", "minecraft:red_nether_brick_slab", "minecraft:red_concrete",
                "minecraft:red_nether_brick_stairs", "minecraft:glass", "minecraft:pearlescent_froglight", "minecraft:orange_stained_glass");
        car(p, -26, Y, -36, 1, red, new String[][]{
                {"HIH", "SSS", "..."},
                {"WBW", "SSS", "..."},
                {"BBB", "GGG", "TTT"},
                {"BBB", "GGG", "RRR"},
                {"BBB", "BBB", "..."},
                {"BFB", "SYS", "..."},
                {"WFW", "SKS", "..."},
                {"LBL", "SSS", "..."}}, null);
    }

    /** A motorbike: two wheels, the engine, a red tank, the seat and the bars. */
    private static void motorbike(Plan p, int x, int z) {
        p.set(x, Y, z, St.axis("minecraft:polished_basalt", "x"));
        p.set(x, Y + 1, z, rod("east"));
        p.set(x, Y, z + 1, "minecraft:iron_block");
        p.set(x, Y + 1, z + 1, "minecraft:red_concrete");
        p.set(x, Y, z + 2, St.axis("minecraft:polished_basalt", "x"));
        p.set(x, Y + 1, z + 2, St.carpet("black"));
        frame(p, x + 1, Y, z + 1, "east", "minecraft:leather_helmet");
    }

    /** The lift: four posts, arms under the car, a baby-blue two-tone Bel Air raised on it, a drip tray under. */
    private static void lift(Plan p) {
        int cx = -32, y0 = FG + 3;
        for (int x : new int[]{cx - 2, cx + 2}) {
            for (int z : new int[]{-50, -46}) {
                for (int y = Y; y <= Y + 1; y++) p.set(x, y, z, y == Y + 1 ? "minecraft:red_concrete" : "minecraft:iron_block");
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z : new int[]{-50, -46}) p.set(x, Y + 1, z, St.trapdoor("minecraft:iron_trapdoor", "north", true, false));
        }
        Paint blue = new Paint("minecraft:light_blue_concrete", "minecraft:smooth_quartz_slab", "minecraft:smooth_quartz",
                "minecraft:smooth_quartz_stairs", "minecraft:glass", "minecraft:sea_lantern", "minecraft:red_stained_glass");
        car(p, cx, y0, -45, -1, blue, new String[][]{
                {"HIH", "SSS", "..."},
                {"WBW", "SSS", "..."},
                {"BBB", "GGG", "TTT"},
                {"BBB", "GGG", "RRR"},
                {"BBB", "GGG", "UUU"},
                {"WBW", "SSS", "..."},
                {"LBL", "SSS", "..."}}, null);
        p.set(cx, Y, -48, "minecraft:cauldron");
        p.set(cx + 2, Y + 1, -45, lever("wall", "south"));
    }

    /** The workbench under the tool wall on the north wall, tyres and drums in the corner, the engine on its hoist. */
    private static void workshop(Plan p) {
        String[] bench = {"minecraft:crafting_table", barrel("south"), chest("south"), "minecraft:damaged_anvil[facing=east]",
                St.of("grindstone", "face", "floor", "facing", "south"), "minecraft:smithing_table", "minecraft:cauldron",
                oven("minecraft:blast_furnace", "south"), barrel("up"), "minecraft:composter[level=1]"};
        String[] tools = {"minecraft:iron_pickaxe", "minecraft:iron_shovel", "minecraft:shears", "minecraft:flint_and_steel", "minecraft:brush",
                "minecraft:iron_axe", "minecraft:fishing_rod", "minecraft:lead", "minecraft:bucket", "minecraft:clock", "minecraft:compass",
                "minecraft:spyglass", "minecraft:iron_hoe", "minecraft:name_tag", "minecraft:saddle", "minecraft:chain", "minecraft:tripwire_hook",
                "minecraft:iron_ingot", "minecraft:lava_bucket", "minecraft:carrot_on_a_stick"};
        for (int i = 0; i < bench.length; i++) {
            int x = -27 + i;
            p.set(x, Y, Z0, bench[i]);
            for (int y = Y + 1; y <= Y + 3; y++) {
                p.set(x, y, Z0 - 1, "minecraft:spruce_planks");
                frame(p, x, y, Z0, "south", tools[Math.floorMod(i * 3 + y, tools.length)]);
            }
        }
        // The engine on its stand, hanging from the hoist's chain.
        p.set(-22, Y, -48, St.wall("minecraft:polished_blackstone_wall"));
        p.set(-22, Y + 1, -48, oven("minecraft:blast_furnace", "south"));
        for (int y = Y + 2; y <= FG + 6; y++) p.set(-22, y, -48, St.chain("y"));
        // Tyres and oil drums.
        for (int y = Y; y <= Y + 2; y++) p.set(-15, y, -51, St.axis("minecraft:polished_basalt", "y"));
        for (int y = Y; y <= Y + 1; y++) p.set(-16, y, -51, St.axis("minecraft:polished_basalt", "y"));
        p.set(-15, Y, -49, barrel("up"));
        p.set(-15, Y, -48, barrel("up"));
        p.set(-16, Y, -49, "minecraft:cauldron");
    }
}
