package org.papiricoh.supernaturalcraft.apocalypse.seal.garrison;

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
 * Uriel's garrison (v0.40), a pure blueprint for the buildkit: an old walled redoubt the Host kept on Earth, gone to ruin. A
 * square yard inside crenellated stone walls, the gate in the north wall between two tall posts, lamp posts round the yard and
 * scorch marks where angels died, and at the south end a small roofed chapel (pews, an altar with candles, lanterns on chains).
 * No block entities: the whole site is the fight's arena. Contract: {@link GarrisonAnchors}; {@code GarrisonPlanQualityTest} holds
 * it to the kit's bar.
 */
public final class GarrisonPlan {

    static final int SEED = 40_270;
    /** The yard's walls (inclusive outline) and their height. */
    static final int W = 17, WALL_TOP = 3;
    /** The chapel (inclusive outline), its ceiling. */
    static final int CX0 = -6, CX1 = 6, CZ0 = 8, CZ1 = 14, CEIL = 6;

    static final Brush WALL = Palette.mix(SEED + 1, "minecraft:stone_bricks", 5, "minecraft:cracked_stone_bricks", 3,
            "minecraft:mossy_stone_bricks", 3, "minecraft:andesite", 2, "minecraft:cobblestone", 2);
    static final Brush YARD = Palette.mix(SEED + 2, "minecraft:polished_andesite", 4, "minecraft:stone_bricks", 3,
            "minecraft:cracked_stone_bricks", 2, "minecraft:gravel", 2, "minecraft:cobblestone", 2, "minecraft:mossy_cobblestone", 1);
    static final Brush LAWN = Palette.patches2(SEED + 3, 3, "minecraft:grass_block", 10, "minecraft:coarse_dirt", 2, "minecraft:podzol", 2,
            "minecraft:moss_block", 1);
    static final Brush CHAPEL = Palette.mix(SEED + 4, "minecraft:tuff_bricks", 4, "minecraft:stone_bricks", 3, "minecraft:polished_tuff", 2,
            "minecraft:cracked_stone_bricks", 2, "minecraft:chiseled_tuff", 1);
    static final Brush FLOOR = Palette.mix(SEED + 5, "minecraft:polished_deepslate", 3, "minecraft:deepslate_tiles", 3,
            "minecraft:cracked_deepslate_tiles", 2, "minecraft:polished_andesite", 2);
    static final Brush CEILING = Palette.mix(SEED + 6, "minecraft:spruce_planks", 5, "minecraft:dark_oak_planks", 3,
            "minecraft:stripped_spruce_wood[axis=y]", 2);
    static final Brush ROOFING = Palette.mix(SEED + 7, "minecraft:cobbled_deepslate", 3, "minecraft:tuff", 3, "minecraft:andesite", 2,
            "minecraft:gravel", 1);
    static final Brush FOOTING = Palette.mix(SEED + 8, "minecraft:cobblestone", 3, "minecraft:stone", 2, "minecraft:cobbled_deepslate", 2);

    private GarrisonPlan() {
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
        Canvas north = drawn();
        Canvas c = new Canvas("uriels_garrison");
        int r = Math.max(Math.max(-GarrisonAnchors.MIN_X, GarrisonAnchors.MAX_X), Math.max(-GarrisonAnchors.MIN_Z, GarrisonAnchors.MAX_Z)) + 4;
        c.zone("garrison", new Box(-r, GarrisonAnchors.BOTTOM_Y, -r, r, GarrisonAnchors.TOP_Y, r));
        c.inZone("garrison", () -> c.stamp(north, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn() {
        Canvas c = new Canvas("uriels_garrison_north");
        ground(c);
        walls(c);
        yard(c);
        chapel(c);
        points(c);
        return c;
    }

    // --- the ground and the yard ---------------------------------------------------------------------------------------------

    private static void ground(Canvas c) {
        int g = GarrisonAnchors.GROUND_Y;
        for (int x = GarrisonAnchors.MIN_X; x <= GarrisonAnchors.MAX_X; x++) {
            for (int z = GarrisonAnchors.MIN_Z; z <= GarrisonAnchors.MAX_Z; z++) {
                boolean inside = Math.abs(x) < W && Math.abs(z) < W;
                boolean path = Math.abs(x) <= 1 && z < -W;
                c.set(x, g, z, inside ? YARD.at(x, g, z) : path ? Palette.mix(SEED + 20, "minecraft:gravel", 3, "minecraft:dirt_path", 3,
                        "minecraft:coarse_dirt", 2).at(x, g, z) : LAWN.at(x, g, z));
                c.set(x, g - 1, z, inside ? FOOTING.at(x, g - 1, z) : "minecraft:dirt");
            }
        }
    }

    private static void walls(Canvas c) {
        Box outline = new Box(-W, 0, -W, W, 0, W);
        for (Walls.Run r : Walls.around(outline)) {
            Walls.body(c, r, 0, WALL_TOP, WALL);
            // Crenellations: merlons of wall every other cell.
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                c.set(x, WALL_TOP + 1, z, Math.floorMod(i, 2) == 0 ? "minecraft:stone_bricks" : St.wall("minecraft:stone_brick_wall"));
            }
            // Buttresses outside every six cells.
            Walls.Run o = r.offset(1);
            for (int i = 3; i < r.length() - 3; i += 6) {
                for (int y = 0; y <= WALL_TOP - 1; y++) c.set(o.x(i), y, o.z(i), y == WALL_TOP - 1 ? Family.STONE_BRICKS.stairs(r.out().opposite(), false) : "minecraft:stone_bricks");
            }
        }
        // The gate: an open arch in the north wall between two tall posts with lanterns.
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) c.carve(x, y, -W);
        for (int x = -1; x <= 1; x++) c.set(x, 3, -W, "minecraft:chiseled_stone_bricks");
        for (int sx : new int[]{-2, 2}) {
            for (int y = 0; y <= 5; y++) c.set(sx, y, -W, y == 5 ? "minecraft:chiseled_stone_bricks" : "minecraft:polished_andesite");
            c.set(sx, 6, -W, St.lantern(false));
        }
        // A breach in the east wall's top, as if something hit it.
        for (int z = -3; z <= 0; z++) {
            c.carve(W, WALL_TOP + 1, z);
            if (Noise.chance(W, WALL_TOP, z, SEED + 30, 0.5)) c.carve(W, WALL_TOP, z);
        }
    }

    private static void yard(Canvas c) {
        // Lamp posts round the yard.
        for (int[] l : new int[][]{{-13, -6}, {13, -6}, {-13, 4}, {13, 4}, {-7, -13}, {7, -13}}) {
            Lighting.lampPost(c, l[0], 0, l[1], 3, "minecraft:stone_bricks", "minecraft:stone_brick_wall", null, false);
        }
        // Scorch marks where angels died: dark carpets in the shape of wings, away from the clear middle.
        for (int[] s : new int[][]{{-11, 0}, {10, -2}, {-4, -11}, {5, -10}}) {
            for (int dx = -2; dx <= 2; dx++) {
                int dz = Math.abs(dx) == 2 ? 1 : 0;
                c.set(s[0] + dx, 0, s[1] + dz, St.carpet(Math.abs(dx) == 1 ? "gray" : "black"));
            }
        }
        // Rubble along the walls.
        for (int[] r : new int[][]{{-15, -15}, {15, -14}, {-15, 13}, {15, 15}, {-9, 15}, {12, 15}}) {
            c.set(r[0], 0, r[1], "minecraft:mossy_cobblestone");
            c.set(r[0] + (r[0] < 0 ? 1 : -1), 0, r[1], St.slabBottom("minecraft:cobblestone_slab"));
        }
    }

    // --- the chapel ------------------------------------------------------------------------------------------------------------

    private static void chapel(Canvas c) {
        int g = GarrisonAnchors.GROUND_Y;
        Box foot = new Box(CX0, 0, CZ0, CX1, 0, CZ1);
        for (int x = CX0; x <= CX1; x++) {
            for (int z = CZ0; z <= CZ1; z++) {
                boolean edge = x == CX0 || x == CX1 || z == CZ0 || z == CZ1;
                c.set(x, g, z, edge ? "minecraft:stone_bricks" : FLOOR.at(x, g, z));
                if (edge) continue;
                for (int y = 0; y < CEIL; y++) c.carve(x, y, z);
                c.set(x, CEIL, z, CEILING.at(x, CEIL, z));
                c.set(x, CEIL + 1, z, ROOFING.at(x, CEIL + 1, z));
            }
        }
        for (Walls.Run r : Walls.around(foot)) {
            Walls.body(c, r, 0, CEIL + 1, CHAPEL);
            // Pilasters one cell out every three: the walls' relief.
            Walls.Run o = r.offset(1);
            for (int i = 0; i < r.length(); i += 3) {
                int x = o.x(i), z = o.z(i);
                if (r.out() == Dir.NORTH && Math.abs(x) <= 2) continue;
                for (int y = 0; y <= CEIL; y++) c.set(x, y, z, y == CEIL ? "minecraft:chiseled_stone_bricks" : "minecraft:stone_bricks");
            }
            // A parapet.
            for (int i = 0; i < r.length(); i++) c.set(r.x(i), CEIL + 2, r.z(i), Math.floorMod(i, 3) == 0 ? "minecraft:tuff_bricks" : St.wall("minecraft:tuff_brick_wall"));
            // Tall windows between the pilasters.
            for (int i = 1; i < r.length() - 1; i += 3) {
                int x = r.x(i), z = r.z(i);
                if (r.out() == Dir.NORTH && Math.abs(x) <= 2) continue;
                for (int y = 2; y <= 4; y++) c.set(x, y, z, St.pane("minecraft:white_stained_glass_pane"));
            }
        }
        // The doorway (north, facing the yard).
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 3; y++) c.carve(x, y, CZ0);
        for (int x = -2; x <= 2; x++) c.set(x, 4, CZ0, x == 0 ? "minecraft:chiseled_tuff" : "minecraft:polished_tuff");
        // The altar at the south end: a slab of chiseled stone with candles, a step before it.
        for (int x = -1; x <= 1; x++) {
            c.set(x, 0, CZ1 - 1, "minecraft:chiseled_stone_bricks");
            c.set(x, 1, CZ1 - 1, St.candle(x == 0 ? "white" : "yellow", x == 0 ? 3 : 2, true));
        }
        // Pews: two rows of spruce stairs either side of the aisle.
        for (int z = CZ0 + 2; z <= CZ1 - 3; z += 2) {
            for (int x : new int[]{-4, -3, 3, 4}) c.set(x, 0, z, St.stairs("minecraft:spruce_stairs", Dir.SOUTH, false));
        }
        // Lanterns on chains from the ceiling.
        for (int[] l : new int[][]{{-3, 10}, {3, 10}, {0, 12}}) Lighting.hanging(c, l[0], CEIL, l[1], 2, false);
    }

    // --- the points ------------------------------------------------------------------------------------------------------------

    private static void points(Canvas c) {
        c.anchor(GarrisonAnchors.ENTRY, 0, 0, -W - 4);
        c.anchor(GarrisonAnchors.YARD, 0, 0, 0);
        c.anchor(GarrisonAnchors.ARENA_CENTRE, 0, 0, 0);
        c.anchor(GarrisonAnchors.URIEL, 0, 0, 1);
        c.anchor(GarrisonAnchors.CASTIEL, 0, 0, -8);
        c.anchor(GarrisonAnchors.gateMin(GarrisonAnchors.GATE), -1, 0, -W);
        c.anchor(GarrisonAnchors.gateMax(GarrisonAnchors.GATE), 1, 2, -W);
        int[][] spawns = {{-10, 0, -9}, {10, 0, -9}, {-12, 0, 0}, {12, 0, 0}, {-10, 0, 8}, {10, 0, 8}};
        for (int n = 0; n < GarrisonAnchors.MIN_SPAWNS; n++) c.anchor(GarrisonAnchors.spawn(n), spawns[n][0], spawns[n][1], spawns[n][2]);
    }
}
