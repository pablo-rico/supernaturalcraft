package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;

/**
 * A temple buried in the desert (v0.42), keeper of the Rod of Aaron, where the Ark of the Covenant was laid and later broken.
 * Only the tops of its two pylons stand out of the sand, and the trench the Society dug down to its door, shored with timber,
 * their camp still by it. Inside, a hall of columns under a ceiling of stone, gold at every capital, painted bands along the
 * walls; through a second door the sanctuary, the Ark broken open on the floor, its lid thrown aside, and before it the altar
 * where the Rod lies. Plan space: the door faces -z; the temple's floor is ten blocks down.
 */
final class TemplePlan {

    static final int SEED = 42_400;
    static final int X0 = -9, X1 = 9, Z0 = -6, Z1 = 18, FLOOR_Y = -10, CEIL_Y = -3;
    static final int FEET = FLOOR_Y + 1;

    static final String BAKED = "supernaturalcraft:sun_baked_bricks", GOLD = "supernaturalcraft:ark_gold_trim";
    static final Brush WALL = Palette.mix(SEED + 1, BAKED, 4, "minecraft:sandstone", 3, "minecraft:cut_sandstone", 2,
            "minecraft:smooth_sandstone", 1, "minecraft:chiseled_sandstone", 1);
    static final Brush FLOOR = Palette.mix(SEED + 2, "minecraft:smooth_sandstone", 4, "minecraft:cut_sandstone", 3, BAKED, 2,
            "minecraft:sandstone", 2);
    static final Brush CEILING = Palette.mix(SEED + 3, "minecraft:cut_sandstone", 4, BAKED, 3, "minecraft:smooth_sandstone", 2,
            "minecraft:sandstone", 2);
    static final Brush SAND = Palette.patches2(SEED + 4, 3, "minecraft:sand", 8, "minecraft:sandstone", 1, "minecraft:smooth_sandstone", 1);
    static final Brush SHORING = Palette.mix(SEED + 5, "minecraft:sandstone", 4, "minecraft:smooth_sandstone", 2, "minecraft:spruce_planks", 2);

    private TemplePlan() {
    }

    /** The trench's floor row at {@code z} (the stair's step there), for z between the door and the camp. */
    static int stepRow(int z) {
        return FEET + (-8 - z);
    }

    static Canvas drawn() {
        Canvas c = new Canvas("reliquary_ark_temple_north");
        temple(c);
        hall(c);
        sanctum(c);
        pylons(c);
        trench(c);
        camp(c);
        c.anchor(ReliquaryAnchors.ENTRY, 0, 0, -19);
        return c;
    }

    private static void temple(Canvas c) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean edge = x == X0 || x == X1 || z == Z0 || z == Z1;
                c.set(x, FLOOR_Y, z, FLOOR.at(x, FLOOR_Y, z));
                for (int y = FEET; y < CEIL_Y; y++) {
                    if (edge) c.set(x, y, z, WALL.at(x, y, z));
                    else c.carve(x, y, z);
                }
                c.set(x, CEIL_Y, z, CEILING.at(x, CEIL_Y, z));
                c.set(x, CEIL_Y + 1, z, "minecraft:sandstone");
                c.set(x, -1, z, SAND.at(x, -1, z));
            }
        }
        // The sand over everything round about.
        for (int x = -12; x <= 12; x++) for (int z = -20; z <= 20; z++) c.setIfEmpty(x, -1, z, SAND.at(x, -1, z));
        // Painted bands round the walls inside.
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean edge = x == X0 || x == X1 || z == Z0 || z == Z1;
                if (!edge) continue;
                int i = x + z;
                c.set(x, FEET + 3, z, Math.floorMod(i, 4) == 0 ? "minecraft:blue_terracotta" : Math.floorMod(i, 2) == 0 ? "minecraft:orange_terracotta"
                        : "minecraft:chiseled_sandstone");
            }
        }
    }

    private static void hall(Canvas c) {
        // The door.
        for (int x = -1; x <= 1; x++) for (int y = FEET; y <= FEET + 3; y++) c.carve(x, y, Z0);
        c.set(0, FEET + 4, Z0, GOLD);
        // The columns.
        for (int x : new int[]{-6, -3, 3, 6}) {
            for (int z : new int[]{-3, 0, 3, 6}) {
                for (int y = FEET; y < CEIL_Y; y++) {
                    String s = y == FEET ? "minecraft:chiseled_sandstone" : y == CEIL_Y - 1 ? GOLD
                            : Math.floorMod(y, 2) == 0 ? "minecraft:cut_sandstone" : "minecraft:smooth_sandstone";
                    c.set(x, y, z, s);
                }
            }
        }
        // Lamps hung from the ceiling down the hall, torches the Society left on the walls.
        for (int[] p : new int[][]{{0, -2}, {0, 4}, {-5, 7}, {5, 7}, {-5, -4}, {5, -4}}) Lighting.hanging(c, p[0], CEIL_Y, p[1], 1, false);
        for (int z : new int[]{-1, 5}) {
            c.set(X0 + 1, FEET + 2, z, St.wallTorch(Dir.EAST));
            c.set(X1 - 1, FEET + 2, z, St.wallTorch(Dir.WEST));
        }
        // The Society's leavings: a ladder up a column, picks and crates, a chest.
        c.set(-2, FEET, 7, "minecraft:spruce_planks");
        c.set(-2, FEET + 1, 7, St.slabBottom("minecraft:spruce_slab"));
        c.set(-1, FEET, 7, St.axis("minecraft:stripped_spruce_log", "x"));
        c.carve(7, FEET, -5);
        c.anchor(ReliquaryAnchors.chest(0), 7, FEET, -5);
        // The dividing wall and the sanctuary's door.
        for (int x = X0; x <= X1; x++) {
            for (int y = FEET; y < CEIL_Y; y++) {
                if (Math.abs(x) <= 1 && y <= FEET + 3) c.carve(x, y, 9);
                else c.set(x, y, 9, WALL.at(x, y, 9));
            }
        }
        c.set(-1, FEET + 4, 9, GOLD);
        c.set(0, FEET + 4, 9, GOLD);
        c.set(1, FEET + 4, 9, GOLD);
    }

    private static void sanctum(Canvas c) {
        // The broken Ark: its chest split open, the lid thrown down beside it, the poles fallen.
        for (int x = -1; x <= 1; x++) for (int z = 15; z <= 16; z++) c.set(x, FEET, z, GOLD);
        c.set(-1, FEET + 1, 16, "minecraft:gold_block");
        c.set(1, FEET + 1, 16, GOLD);
        c.set(3, FEET, 14, GOLD);
        c.set(3, FEET, 13, St.slabBottom("minecraft:smooth_sandstone_slab"));
        c.set(-3, FEET, 15, St.axis("minecraft:stripped_dark_oak_log", "z"));
        c.set(-3, FEET, 16, St.axis("minecraft:stripped_dark_oak_log", "z"));
        // A dais of gold-trimmed stone for the altar.
        for (int x = -2; x <= 2; x++) c.set(x, FLOOR_Y, 12, Math.abs(x) == 2 ? GOLD : "minecraft:chiseled_sandstone");
        c.carve(0, FEET, 12);
        c.anchor(ReliquaryAnchors.ALTAR, 0, FEET, 12);
        c.anchor(ReliquaryAnchors.KNEEL, 0, FEET, 10);
        for (int s : new int[]{-1, 1}) {
            c.set(s * 5, FEET, 11, "minecraft:chiseled_sandstone");
            c.set(s * 5, FEET + 1, 11, St.candle("yellow", 4, true));
            Lighting.hanging(c, s * 4, CEIL_Y, 15, 1, true);
        }
        c.carve(-7, FEET, 17);
        c.anchor(ReliquaryAnchors.chest(1), -7, FEET, 17);
    }

    /** The two pylons either side of the door, rising out of the sand, their tops broken. */
    private static void pylons(Canvas c) {
        for (int s : new int[]{-1, 1}) {
            for (int ax = 3; ax <= 6; ax++) {
                int x = s * ax;
                for (int z = -8; z <= -7; z++) {
                    int top = 2 - (int) Math.round(Noise.hash01(x, z, SEED + 6) * 2) - (ax == 6 ? 1 : 0);
                    for (int y = FLOOR_Y; y <= top; y++) {
                        String b = y == FEET + 4 ? GOLD : y == top ? "minecraft:smooth_sandstone" : WALL.at(x, y, z);
                        c.set(x, y, z, b);
                    }
                }
            }
            // A bevel where each pylon meets the facade.
            c.set(s * 3, 0, -6, St.stairs("minecraft:sandstone_stairs", Dir.SOUTH, false));
        }
    }

    /** The Society's trench: a stair down from the sand to the door, its sides shored up. */
    private static void trench(Canvas c) {
        Furniture.flight(c, -1, FEET, -8, Dir.NORTH, -FEET, 3, "minecraft:sandstone_stairs", false);
        for (int z = -17; z <= -7; z++) {
            int row = z >= -16 && z <= -8 ? stepRow(z) : z == -7 ? FEET - 1 : -1;
            for (int x = -2; x <= 2; x++) {
                if (z == -7) c.set(x, FLOOR_Y, z, FLOOR.at(x, FLOOR_Y, z));
                for (int y = row + 1; y <= 3; y++) {
                    if (Math.abs(x) <= 1 && y <= row + 3 && z <= -8) continue; // the flight's own headroom
                    c.carve(x, y, z);
                }
                if (Math.abs(x) == 2 && z <= -8) c.set(x, row, z, SHORING.at(x, row, z));
            }
            for (int s : new int[]{-1, 1}) {
                int x = s * 3;
                if (z >= -8 && z <= -7) continue;
                for (int y = Math.max(row - 1, FLOOR_Y); y <= -1; y++) {
                    boolean post = Math.floorMod(z, 3) == 0;
                    c.set(x, y, z, post ? St.axis("minecraft:spruce_log", "y") : SHORING.at(x, y, z));
                }
            }
        }
        for (int x = -2; x <= 2; x++) for (int y = FEET; y <= FEET + 3; y++) c.carve(x, y, -7);
        // Cross-braces over the trench.
        for (int z : new int[]{-15, -12}) for (int x = -2; x <= 2; x++) c.set(x, 2, z, St.axis("minecraft:spruce_log", "x"));
    }

    /** Their camp on the sand: a canvas lean-to, crates, a lamp on a post. */
    private static void camp(Canvas c) {
        for (int x = 6; x <= 9; x++) {
            for (int z = -17; z <= -15; z++) c.set(x, 2, z, z == -16 ? "minecraft:white_wool" : St.slabBottom("minecraft:birch_slab"));
        }
        for (int x : new int[]{6, 9}) for (int z : new int[]{-17, -15}) for (int y = 0; y <= 1; y++) c.set(x, y, z, St.fence("minecraft:spruce_fence"));
        c.set(7, 0, -16, "minecraft:spruce_planks");
        c.set(8, 0, -16, St.axis("minecraft:stripped_spruce_log", "x"));
        c.set(7, 1, -16, St.slabBottom("minecraft:spruce_slab"));
        c.set(-7, 0, -13, "minecraft:spruce_planks");
        c.set(-8, 0, -13, "minecraft:spruce_planks");
        c.set(-7, 1, -13, St.axis("minecraft:stripped_spruce_log", "z"));
        Lighting.lampPost(c, 5, 0, -11, 2, "minecraft:smooth_sandstone", "minecraft:spruce_fence", Dir.WEST, false);
    }
}
