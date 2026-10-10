package org.papiricoh.supernaturalcraft.buildkit.demo;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Ground;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Paths;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.Scatter;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Terrain;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;

import java.util.List;

/**
 * A garden island (pure demo): rolling ground on a calcite underside hung with dripstone, a raised terrace behind a mossy
 * retaining wall with a spring that falls into a pond below, a little hip-roofed pavilion, stepping stones and a path with lamp
 * posts, and one of every tree the kit grows.
 */
public final class DemoIsland {

    static final int SEED = 3307;

    private DemoIsland() {
    }

    public static Canvas build() {
        Canvas c = new Canvas("kit_island");
        Ground g = Terrain.island(c, Terrain.island(0, 0, 28, -1, 3, 18, SEED));
        c.anchor("entry", -4, g.top(-4, 22) + 1, 22);
        // The terrace in the north-east, its wall weathered stone.
        Terrain.Mask terraceMask = Terrain.Mask.blob(10, -11, 7.5, 6, 0.18, SEED + 9);
        Box terrace = terraceMask.bounds();
        Brush wall = Palette.weathered(Palette.STONE_BRICK_WEAR, Palette.Field.nearGround(-2, 5), 0.35, SEED + 1);
        Terrain.terrace(c, g, terraceMask, 3, Palette.patches2(SEED + 2, 4, "minecraft:grass_block", 8, "minecraft:moss_block", 2),
                Brush.of("minecraft:dirt"), wall, "minecraft:mossy_stone_bricks", Family.MOSSY_STONE_BRICKS);
        // The spring on the terrace's south lip, falling into a pond.
        int lip = -11;
        while (terraceMask.contains(9, lip + 1)) lip++;
        Terrain.waterfall(c, g, 9, lip, Dir.SOUTH, 3.5, SEED + 3, Palette.patches(SEED + 4, 2, "minecraft:mossy_cobblestone", 2,
                "minecraft:stone", 1));
        Terrain.pond(c, g, -9, 8, 4.5, 3, SEED + 5);

        pavilion(c, g, -8, -8);

        Paths.Style stones = new Paths.Style(2, Brush.of("minecraft:dirt_path"), Brush.of("minecraft:dirt_path"),
                "minecraft:polished_andesite", null, null, 0, null, null);
        Paths.path(c, g, List.of(new int[]{-8, -4}, new int[]{-4, 0}, new int[]{2, 2}, new int[]{8, 4}), stones, SEED + 6);
        Paths.Style walk = Paths.countryPath(3, SEED + 7);
        walk = new Paths.Style(walk.width(), walk.core(), walk.edge(), null, null, "minecraft:dark_oak_fence", 9, "minecraft:mossy_cobblestone",
                walk.border());
        Paths.path(c, g, List.of(new int[]{8, 4}, new int[]{4, 12}, new int[]{-2, 18}, new int[]{-6, 24}), walk, SEED + 8);

        tree(c, g, 12, -10, (x, y, z) -> Trees.oak(c, x, y, z, 3, SEED + 10));
        tree(c, g, -16, 0, (x, y, z) -> Trees.cherry(c, x, y, z, 2, SEED + 11));
        tree(c, g, 2, 20, (x, y, z) -> Trees.cherry(c, x, y, z, 1, SEED + 12));
        tree(c, g, -18, 13, (x, y, z) -> Trees.willow(c, x, y, z, 1, SEED + 13));
        tree(c, g, 18, 6, (x, y, z) -> Trees.birch(c, x, y, z, 2, SEED + 14));
        tree(c, g, 20, 12, (x, y, z) -> Trees.birch(c, x, y, z, 1, SEED + 15));
        tree(c, g, -4, -20, (x, y, z) -> Trees.spruce(c, x, y, z, 2, SEED + 16));
        tree(c, g, 4, -22, (x, y, z) -> Trees.spruce(c, x, y, z, 1, SEED + 17));
        tree(c, g, 12, 18, (x, y, z) -> Trees.deadOak(c, x, y, z, 1, true, SEED + 18));
        tree(c, g, -20, -8, (x, y, z) -> Trees.oak(c, x, y, z, 1, SEED + 19));
        for (int[] b : new int[][]{{-2, -10}, {6, 8}, {-12, 16}, {14, 0}, {0, 8}}) {
            tree(c, g, b[0], b[1], (x, y, z) -> Trees.bush(c, x, y, z, b[0] % 2 == 0 ? "minecraft:flowering_azalea_leaves" : "minecraft:azalea_leaves", 1.3, SEED + b[1]));
        }
        Scatter.flora(c, g, terrace, 0.6, 1, SEED + 20, Scatter.WOODLAND);
        Scatter.flora(c, g, null, 0.5, 1, SEED + 21, Scatter.MEADOW);
        Scatter.prune(c);
        return c.finish();
    }

    /** A small open pavilion: a raised stone platform, four log posts with fence balustrades, a hip roof with a lantern. */
    static void pavilion(Canvas c, Ground g, int cx, int cz) {
        Box base = new Box(cx - 3, 0, cz - 3, cx + 3, 0, cz + 3);
        Terrain.level(c, g, base.growXZ(1), -1, Brush.of("minecraft:grass_block"), Brush.of("minecraft:dirt"), 2);
        c.zone("pavilion", new Box(cx - 6, -2, cz - 6, cx + 6, 10, cz + 6));
        c.inZone("pavilion", () -> {
            for (int x = base.x0(); x <= base.x1(); x++) {
                for (int z = base.z0(); z <= base.z1(); z++) {
                    boolean edge = x == base.x0() || x == base.x1() || z == base.z0() || z == base.z1();
                    c.set(x, 0, z, edge ? "minecraft:polished_andesite" : Palette.checker(1, Brush.of("minecraft:calcite"),
                            Brush.of("minecraft:polished_diorite")).at(x, 0, z));
                    c.set(x, -1, z, "minecraft:stone_bricks");
                }
            }
            for (Walls.Run r : Walls.around(base)) Walls.plinth(c, r, 0, 0, Family.POLISHED_ANDESITE.withBlock("minecraft:polished_andesite"), true);
            // Steps on the south side.
            c.set(cx, 0, cz + 4, Family.POLISHED_ANDESITE.stairs(Dir.NORTH, false));
            Box posts = new Box(cx - 2, 0, cz - 2, cx + 2, 0, cz + 2);
            Walls.cornerPosts(c, posts, 1, 4, St.log("minecraft:stripped_birch_log"));
            for (Walls.Run r : Walls.around(posts)) {
                for (int i = 1; i < r.length() - 1; i++) {
                    if (r.out() == Dir.SOUTH && i == 2) continue;
                    c.set(r.x(i), 1, r.z(i), St.fence("minecraft:birch_fence"));
                }
                Walls.beam(c, r, 4, Wood.BIRCH, true);
                for (int i = 1; i < r.length() - 1; i++) c.set(r.x(i), 3, r.z(i), St.trapdoor("minecraft:birch_trapdoor", r.out(), true, false));
            }
            Roofs.Style roof = new Roofs.Style(Family.EXPOSED_CUT_COPPER, Family.CUT_COPPER, Brush.of("minecraft:birch_planks"), 1,
                    Roofs.Pitch.STEEP, 0, true, "minecraft:birch_trapdoor", true);
            Roofs.hip(c, posts, 4, roof);
            Lighting.hanging(c, cx, 7, cz, 3, false);
            c.set(cx, 1, cz, St.fence("minecraft:birch_fence"));
            c.set(cx, 2, cz, St.carpet("white"));
            c.set(cx - 1, 1, cz, St.stairs("minecraft:birch_stairs", Dir.WEST, false));
            c.set(cx + 1, 1, cz, St.stairs("minecraft:birch_stairs", Dir.EAST, false));
        });
    }

    interface Grow {
        void at(int x, int y, int z);
    }

    static void tree(Canvas c, Ground g, int x, int z, Grow grow) {
        if (!g.has(x, z) || g.water(x, z)) return;
        grow.at(x, g.top(x, z) + 1, z);
    }
}
