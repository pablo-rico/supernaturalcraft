package org.papiricoh.supernaturalcraft.pact.layout;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/**
 * Crowley's lab (v0.39), a pure blueprint for the buildkit: an old brick pump house at the edge of a reservoir, the waterworks
 * nobody has run for decades, which the King of Hell has made his workshop. Inside, one tall hall: cages along the west wall
 * (where he kept the Alphas), benches and tackle along the east, chains from the trusses, a clear floor in the middle for the
 * great circle. The big doors open south onto a concrete apron and the reservoir, with a deck out over the water. No block
 * entities: the whole site is the fight's arena. Contract: {@link LabAnchors}; {@code LabPlanQualityTest} holds it to the kit's
 * bar.
 */
public final class LabPlan {

    static final int SEED = 39_600;
    /** The pump house's walls (inclusive), the roof's level, the belt course. */
    static final int WX0 = -12, WX1 = 12, WZ0 = -10, WZ1 = 10, ROOF = 10, BELT = 5;
    /** The reservoir's rim and its water (inclusive); the water's floor and top. */
    static final int RX0 = -18, RX1 = 18, RZ0 = 14, RZ1 = 42, FLOOR_Y = -6, WATER_TOP = -2;
    /** The deck out over the water. */
    static final int DX0 = -7, DX1 = 7, DZ0 = 15, DZ1 = 28;

    static final Brush BRICK = Palette.mix(SEED + 1, "minecraft:bricks", 10, "minecraft:granite", 2, "minecraft:mud_bricks", 2,
            "minecraft:packed_mud", 1, "minecraft:red_terracotta", 1);
    static final Brush STONE = Palette.mix(SEED + 2, "minecraft:stone_bricks", 6, "minecraft:cracked_stone_bricks", 2,
            "minecraft:andesite", 2, "minecraft:mossy_stone_bricks", 1);
    static final Brush FLOOR = Palette.mix(SEED + 3, "minecraft:polished_andesite", 5, "minecraft:smooth_stone", 3, "minecraft:stone", 2,
            "minecraft:tuff", 2, "minecraft:cracked_stone_bricks", 1);
    static final Brush CONCRETE = Palette.mix(SEED + 4, "minecraft:light_gray_concrete", 5, "minecraft:stone", 3, "minecraft:andesite", 3,
            "minecraft:gravel", 1, "minecraft:cobblestone", 1);
    static final Brush CEILING = Palette.mix(SEED + 5, "minecraft:spruce_planks", 5, "minecraft:dark_oak_planks", 3,
            "minecraft:stripped_spruce_wood[axis=y]", 2);
    static final Brush ROOFING = Palette.mix(SEED + 6, "minecraft:tuff", 3, "minecraft:cobbled_deepslate", 2, "minecraft:andesite", 3,
            "minecraft:gravel", 1);
    static final Brush BASIN = Palette.mix(SEED + 7, "minecraft:stone", 4, "minecraft:andesite", 3, "minecraft:gravel", 2, "minecraft:clay", 1);
    static final Brush LAWN = Palette.patches2(SEED + 8, 3, "minecraft:grass_block", 10, "minecraft:moss_block", 2, "minecraft:podzol", 2,
            "minecraft:coarse_dirt", 2, "minecraft:rooted_dirt", 1);
    static final Brush FOOTING = Palette.mix(SEED + 9, "minecraft:cobblestone", 3, "minecraft:cobbled_deepslate", 2, "minecraft:stone", 2);

    private LabPlan() {
    }

    public static int[] rotate(int x, int z, int q) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    public static LayoutPlan plan(int q) {
        return canvas(q).toPlan();
    }

    public static Canvas canvas(int q) {
        Canvas south = drawn();
        Canvas c = new Canvas("crowleys_lab");
        int r = Math.max(Math.max(-LabAnchors.MIN_X, LabAnchors.MAX_X), Math.max(-LabAnchors.MIN_Z, LabAnchors.MAX_Z)) + 4;
        c.zone("lab", new Box(-r, LabAnchors.BOTTOM_Y, -r, r, LabAnchors.TOP_Y, r));
        c.inZone("lab", () -> c.stamp(south, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn() {
        Canvas c = new Canvas("crowleys_lab_south");
        grounds(c);
        reservoir(c);
        house(c);
        hall(c);
        points(c);
        return c;
    }

    // --- the ground --------------------------------------------------------------------------------------------------------------

    private static void grounds(Canvas c) {
        for (int x = LabAnchors.MIN_X; x <= LabAnchors.MAX_X; x++) {
            for (int z = LabAnchors.MIN_Z; z <= LabAnchors.MAX_Z; z++) {
                boolean drive = Math.abs(x) <= 1 && z < WZ0;
                c.set(x, LabAnchors.GROUND_Y, z, drive ? Palette.mix(SEED + 20, "minecraft:gravel", 3, "minecraft:dirt_path", 3,
                        "minecraft:coarse_dirt", 2).at(x, LabAnchors.GROUND_Y, z) : LAWN.at(x, LabAnchors.GROUND_Y, z));
                c.set(x, LabAnchors.GROUND_Y - 1, z, "minecraft:dirt");
            }
        }
        // The apron between the doors and the water.
        for (int x = -15; x <= 15; x++) for (int z = WZ1 + 1; z < RZ0; z++) c.set(x, LabAnchors.GROUND_Y, z, CONCRETE.at(x, LabAnchors.GROUND_Y, z));
    }

    // --- the reservoir -----------------------------------------------------------------------------------------------------------

    private static void reservoir(Canvas c) {
        for (int x = RX0; x <= RX1; x++) {
            for (int z = RZ0; z <= RZ1; z++) {
                boolean rim = x == RX0 || x == RX1 || z == RZ0 || z == RZ1;
                if (rim) {
                    for (int y = FLOOR_Y; y <= LabAnchors.GROUND_Y; y++) c.set(x, y, z, CONCRETE.at(x, y, z));
                    // A low kerb of slabs, gapped where the deck meets it.
                    if (!(z == RZ0 && x >= DX0 && x <= DX1)) c.set(x, 0, z, St.slabBottom("minecraft:smooth_stone_slab"));
                    continue;
                }
                c.set(x, FLOOR_Y, z, BASIN.at(x, FLOOR_Y, z));
                for (int y = FLOOR_Y + 1; y <= WATER_TOP; y++) c.set(x, y, z, St.water());
                c.carve(x, WATER_TOP + 1, z);
            }
        }
        // The deck: a concrete slab on piers, iron rails round its three open sides, two lamps.
        for (int x = DX0; x <= DX1; x++) {
            for (int z = DZ0; z <= DZ1; z++) {
                c.set(x, LabAnchors.GROUND_Y, z, CONCRETE.at(x, LabAnchors.GROUND_Y, z));
                boolean pier = Math.floorMod(x - DX0, 7) == 0 && Math.floorMod(z - DZ0, 4) == 1;
                if (pier) for (int y = FLOOR_Y + 1; y < LabAnchors.GROUND_Y; y++) c.set(x, y, z, STONE.at(x, y, z));
            }
        }
        for (int z = DZ0; z <= DZ1; z++) {
            c.set(DX0 - 1, 0, z, St.bars());
            c.set(DX1 + 1, 0, z, St.bars());
            c.set(DX0 - 1, LabAnchors.GROUND_Y, z, "minecraft:stone_bricks");
            c.set(DX1 + 1, LabAnchors.GROUND_Y, z, "minecraft:stone_bricks");
            for (int y = FLOOR_Y + 1; y <= WATER_TOP + 1; y++) {
                if (Math.floorMod(z - DZ0, 4) == 1) {
                    c.set(DX0 - 1, y, z, STONE.at(DX0 - 1, y, z));
                    c.set(DX1 + 1, y, z, STONE.at(DX1 + 1, y, z));
                }
            }
        }
        for (int x = DX0 - 1; x <= DX1 + 1; x++) {
            c.set(x, LabAnchors.GROUND_Y, DZ1 + 1, "minecraft:stone_bricks");
            c.set(x, 0, DZ1 + 1, St.bars());
            for (int y = FLOOR_Y + 1; y <= WATER_TOP + 1; y++) if (Math.floorMod(x - DX0, 7) == 0) c.set(x, y, DZ1 + 1, STONE.at(x, y, DZ1 + 1));
        }
        Lighting.lampPost(c, DX0 - 1, 0, DZ1 + 1, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.EAST, false);
        Lighting.lampPost(c, DX1 + 1, 0, DZ1 + 1, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.WEST, false);
        // The intake tower at the far end: a stone stack with chains down into the water.
        for (int x = -2; x <= 2; x++) {
            for (int y = FLOOR_Y + 1; y <= 4; y++) {
                for (int z = RZ1 - 4; z <= RZ1 - 1; z++) {
                    boolean wall = x == -2 || x == 2 || z == RZ1 - 4 || z == RZ1 - 1;
                    if (wall) c.set(x, y, z, y >= 2 ? BRICK.at(x, y, z) : STONE.at(x, y, z));
                    else if (y > WATER_TOP) c.carve(x, y, z);
                }
            }
            for (int z = RZ1 - 5; z <= RZ1; z++) c.set(x, 5, z, St.slabBottom("minecraft:dark_oak_slab"));
        }
        for (int y = WATER_TOP + 1; y <= 4; y++) c.set(0, y, RZ1 - 5, St.chain("y"));
        c.set(0, 3, RZ1 - 5, St.lantern(true));
        // The rim lamps.
        for (int z = RZ0 + 6; z < RZ1; z += 10) {
            Lighting.lampPost(c, RX0, 0, z, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.EAST, false);
            Lighting.lampPost(c, RX1, 0, z, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.WEST, false);
        }
    }

    // --- the pump house ----------------------------------------------------------------------------------------------------------

    static final Box FOOT = new Box(WX0, 0, WZ0, WX1, 0, WZ1);

    private static void house(Canvas c) {
        for (int x = WX0; x <= WX1; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                c.set(x, LabAnchors.GROUND_Y - 1, z, FOOTING.at(x, LabAnchors.GROUND_Y - 1, z));
                boolean edge = x == WX0 || x == WX1 || z == WZ0 || z == WZ1;
                c.set(x, LabAnchors.GROUND_Y, z, edge ? STONE.at(x, LabAnchors.GROUND_Y, z) : FLOOR.at(x, LabAnchors.GROUND_Y, z));
                if (edge) continue;
                for (int y = 0; y < ROOF; y++) c.carve(x, y, z);
                c.set(x, ROOF, z, CEILING.at(x, ROOF, z));
                c.set(x, ROOF + 1, z, ROOFING.at(x, ROOF + 1, z));
            }
        }
        for (Walls.Run r : Walls.around(FOOT)) {
            Walls.body(c, r, 0, 1, STONE);
            Walls.body(c, r, 2, ROOF + 1, BRICK);
            Walls.plinth(c, r, 0, 1, Family.STONE_BRICKS, true);
            Walls.belt(c, r, BELT, Family.STONE_BRICKS);
            Walls.cornice(c, r, ROOF + 1, Family.STONE_BRICKS, null);
            pilasters(c, r);
            windows(c, r);
            // The parapet.
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                c.set(x, ROOF + 2, z, Math.floorMod(i, 4) == 0 ? "minecraft:stone_bricks" : St.wall("minecraft:stone_brick_wall"));
            }
        }
        // The front door (north) and the big doors (south): both open archways, the south one barred by the fight.
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) c.carve(x, y, WZ0);
        c.set(-2, 3, WZ0, "minecraft:chiseled_stone_bricks");
        c.set(2, 3, WZ0, "minecraft:chiseled_stone_bricks");
        for (int x = -1; x <= 1; x++) c.set(x, 3, WZ0, "minecraft:polished_andesite");
        for (int x = -3; x <= 3; x++) for (int y = 0; y <= 4; y++) c.carve(x, y, WZ1);
        for (int x = -4; x <= 4; x++) c.set(x, 5, WZ1, x == -4 || x == 4 ? "minecraft:chiseled_stone_bricks" : "minecraft:polished_andesite");
        // A painted name over the big doors, long faded: a row of white terracotta.
        for (int x = -3; x <= 3; x++) c.set(x, 7, WZ1, Noise.chance(x, 7, WZ1, SEED + 30, 0.35) ? "minecraft:light_gray_terracotta" : "minecraft:white_terracotta");
        // The roof walk's skylight vents.
        for (int[] v : new int[][]{{-6, -4}, {6, -4}, {-6, 4}, {6, 4}}) {
            c.set(v[0], ROOF + 2, v[1], "minecraft:stone_bricks");
            c.set(v[0], ROOF + 3, v[1], St.slabBottom("minecraft:stone_brick_slab"));
        }
    }

    private static void pilasters(Canvas c, Walls.Run r) {
        Walls.Run o = r.offset(1);
        for (int i = 0; i < r.length(); i++) {
            if (Math.floorMod(i, 6) != 0 && i != r.length() - 1) continue;
            int x = o.x(i), z = o.z(i);
            if ((r.out() == Dir.SOUTH && Math.abs(x) <= 4) || (r.out() == Dir.NORTH && Math.abs(x) <= 2)) continue;
            for (int y = 0; y <= ROOF; y++) c.set(x, y, z, y == BELT ? "minecraft:chiseled_stone_bricks" : y <= 1 ? STONE.at(x, y, z) : BRICK.at(x, y, z));
            c.set(x, ROOF + 1, z, "minecraft:stone_bricks");
        }
    }

    /** Tall arched bays between the pilasters: grimy glass, some panes out, bars on the lower half. */
    private static void windows(Canvas c, Walls.Run r) {
        for (int i = 0; i < r.length(); i++) {
            int m = Math.floorMod(i, 6);
            if (m != 2 || i + 2 >= r.length()) continue;
            int x0 = r.x(i);
            if (r.out() == Dir.SOUTH && Math.abs(x0) <= 5) continue;
            if (r.out() == Dir.NORTH && Math.abs(x0) <= 3) continue;
            for (int k = 0; k < 2; k++) {
                int x = r.x(i + k), z = r.z(i + k);
                for (int y = 2; y <= 8; y++) {
                    String pane = y <= 3 ? St.bars() : Noise.chance(x, y, z, SEED + 40, 0.15) ? St.AIR : St.pane("minecraft:gray_stained_glass_pane");
                    c.set(x, y, z, pane);
                }
                c.set(x, 9, z, "minecraft:chiseled_stone_bricks");
                Walls.Run o = r.offset(1);
                c.setIfAir(o.x(i + k), 1, o.z(i + k), St.stairs("minecraft:stone_brick_stairs", r.out().opposite(), true));
            }
        }
    }

    // --- the hall ----------------------------------------------------------------------------------------------------------------

    private static void hall(Canvas c) {
        // The trusses: spruce beams across the hall under the ceiling, chains hanging from some.
        for (int z = WZ0 + 3; z < WZ1; z += 5) {
            for (int x = WX0 + 1; x < WX1; x++) c.set(x, ROOF - 1, z, St.axis("minecraft:stripped_spruce_log", "x"));
        }
        // The lights: lanterns on chains in a grid (the middle kept high for the door), sconces between the windows.
        for (int x = -8; x <= 8; x += 8) {
            for (int z = -6; z <= 9; z += 5) {
                if (x == 0 && Math.abs(z + 4) <= 3) continue;
                Lighting.hanging(c, x, ROOF, z, x == 0 ? 2 : 3, false);
            }
        }
        for (int z = -8; z <= 8; z += 4) {
            c.set(WX0 + 1, 5, z, St.lantern(false));                  // on the cages' roof
            if (Math.abs(z) > 1) c.set(WX1 - 1, 4, z, St.lantern(false));   // on the shelves
        }
        // The west wall: three cages of iron bars, a bed of straw and a chained post in each (the Alphas' cells).
        for (int n = 0; n < 3; n++) {
            int z0 = -8 + n * 6, z1 = z0 + 4;
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y <= 3; y++) c.set(-8, y, z, St.bars());
                c.set(-8, 4, z, "minecraft:polished_andesite");
                for (int x = WX0 + 1; x < -8; x++) c.set(x, 4, z, "minecraft:polished_andesite");
            }
            for (int x = WX0 + 1; x <= -8; x++) {
                if (n == 0) for (int y = 0; y <= 4; y++) c.set(x, y, z0 - 1, y == 4 ? "minecraft:polished_andesite" : St.bars());
                for (int y = 0; y <= 4; y++) c.set(x, y, z1 + 1, y == 4 ? "minecraft:polished_andesite" : St.bars());
            }
            c.set(-10, 0, z0 + 1, "minecraft:hay_block");
            c.set(-10, 0, z0 + 2, St.slabBottom("minecraft:spruce_slab"));
            for (int y = 0; y <= 3; y++) c.set(-11, y, z0 + 3, y == 3 ? St.chain("y") : "minecraft:stripped_dark_oak_log[axis=y]");
            c.set(-9, 0, z0 + 3, St.chain("x"));
            c.set(-10, 3, z0 + 2, St.lantern(true));
            c.set(-10, 3, z0 + 3, St.chain("y"));
        }
        // The east wall: the workbenches, an anvil, cauldrons, the old pump's flywheel housing, shelves of jars (glass, bottles).
        for (int z = -8; z <= 8; z++) {
            boolean gap = Math.abs(z) <= 1;
            if (gap) continue;
            c.set(WX1 - 1, 0, z, Math.floorMod(z, 3) == 0 ? "minecraft:smithing_table" : "minecraft:stripped_spruce_wood[axis=y]");
            c.set(WX1 - 1, 1, z, Math.floorMod(z, 4) == 0 ? St.candle("white", 2, true) : St.slabBottom("minecraft:spruce_slab"));
            c.set(WX1 - 1, 3, z, "minecraft:bookshelf");
        }
        c.set(WX1 - 3, 0, -6, St.facing("minecraft:anvil", Dir.NORTH));
        c.set(WX1 - 3, 0, 6, "minecraft:cauldron");
        c.set(WX1 - 4, 0, 6, "minecraft:cauldron");
        c.set(WX1 - 3, 0, -3, "minecraft:crafting_table");
        c.set(WX1 - 3, 0, 3, St.facing("minecraft:grindstone", Dir.NORTH).replace("]", ",face=floor]"));
        // The old pump: a deepslate housing with copper pipes up the wall in the middle of the east side.
        for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++) c.set(WX1 - 1, y, z, y == 2 ? "minecraft:polished_deepslate" : "minecraft:waxed_cut_copper");
        for (int y = 3; y <= ROOF - 1; y++) c.set(WX1 - 1, y, 0, "minecraft:waxed_copper_grate");
        c.set(WX1 - 2, 1, 0, St.facing("minecraft:lightning_rod", Dir.WEST).replace("]", ",powered=false,waterlogged=false]"));
        // Sigils chalked round the floor's middle, a ring of candles for the circle that is not yet drawn.
        for (int[] k : new int[][]{{-5, -9}, {5, -9}}) c.set(k[0], 0, k[1], St.candle("black", 3, false));
        // Sacks and crates by the front door.
        c.set(-9, 0, -9, "minecraft:hay_block");
        c.set(9, 0, -9, "minecraft:target");
        c.set(8, 0, -9, St.slabBottom("minecraft:spruce_slab"));
    }

    // --- the points --------------------------------------------------------------------------------------------------------------

    private static void points(Canvas c) {
        c.anchor(LabAnchors.ENTRY, 0, 0, WZ0 - 3);
        c.anchor(LabAnchors.RITE, 0, 0, -4);
        c.anchor(LabAnchors.HALL, 0, 0, 3);
        c.anchor(LabAnchors.DOOR, 0, 5, -4);
        c.anchor(LabAnchors.CROWLEY, -6, 0, -7);
        c.anchor(LabAnchors.DECK, 0, 0, 22);
        c.anchor(LabAnchors.ARENA_CENTRE, 0, 0, 12);
        c.anchor(LabAnchors.WATER_MIN, RX0 + 1, FLOOR_Y + 1, RZ0 + 1);
        c.anchor(LabAnchors.WATER_MAX, RX1 - 1, WATER_TOP, RZ1 - 1);
        c.anchor(LabAnchors.gateMin(LabAnchors.GATE_DOORS), -3, 0, WZ1);
        c.anchor(LabAnchors.gateMax(LabAnchors.GATE_DOORS), 3, 4, WZ1);
        int[][] hall = {{-6, 0, -8}, {6, 0, -8}, {-6, 0, 8}, {6, 0, 8}};
        int[][] deck = {{-5, 0, 16}, {5, 0, 16}, {-5, 0, 27}, {5, 0, 27}};
        for (int n = 0; n < LabAnchors.MIN_SPAWNS; n++) {
            c.anchor(LabAnchors.spawn("hall", n), hall[n][0], hall[n][1], hall[n][2]);
            c.anchor(LabAnchors.spawn("deck", n), deck[n][0], deck[n][1], deck[n][2]);
        }
    }
}
