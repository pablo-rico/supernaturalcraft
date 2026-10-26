package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Wood;

import java.util.List;

/**
 * The Monastery's wings (pure, see {@link Wing} for the frame): the scriptorium, the chapel, the dormitory, the refectory and
 * the ossuary, and the herb garden that fills a slot left without a wing.
 */
final class MonasteryWings {

    private MonasteryWings() {
    }

    static final List<Wing> POOL = List.of(
            new Wing("scriptorium", MonasteryWings::scriptorium),
            new Wing("chapel", MonasteryWings::chapel),
            new Wing("dormitory", MonasteryWings::dormitory),
            new Wing("refectory", MonasteryWings::refectory),
            new Wing("ossuary", MonasteryWings::ossuary));

    static final Wing YARD = new Wing("garden", MonasteryWings::garden);

    // --- the scriptorium -----------------------------------------------------------------------------------------------------

    /** Shelves of books along the walls, two rows of copying desks with candles, a reading stand at the back. */
    private static void scriptorium(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s, Family.WEATHERED_CUT_COPPER, false);
        for (int side : new int[]{-1, 1}) {
            Dir front = side < 0 ? Dir.EAST : Dir.WEST;
            Furniture.bookshelves(c, side * 7, -11, Dir.SOUTH, 2, 0, 3, front, false, Wood.SPRUCE.log("y"), 0);
            Furniture.bookshelves(c, side * 7, -7, Dir.SOUTH, 2, 0, 3, front, false, null, 0);
            Furniture.bookshelves(c, side * 7, -3, Dir.SOUTH, 2, 0, 2, front, false, null, 0);
            for (int z = -10; z <= -2; z += 4) {
                int x = side * 4;
                c.set(x, 0, z, St.slabTop(Wood.SPRUCE.slabId()));
                c.set(x, 0, z + 1, St.slabTop(Wood.SPRUCE.slabId()));
                Furniture.chair(c, x - side, 0, z, side < 0 ? Dir.WEST : Dir.EAST, Wood.SPRUCE.stairsId(), null);
                c.set(x, 1, z, St.candle("white", 1 + Noise.pick(x, 1, z, s + 21, 2), true));
                c.set(x, 1, z + 1, Noise.chance(x, 1, z, s + 22, 0.5) ? "minecraft:flower_pot" : St.candle("yellow", 1, true));
            }
        }
        for (int x = -3; x <= 3; x++) Furniture.bookshelves(c, x, -11, Dir.SOUTH, 1, 0, Math.abs(x) <= 1 ? 1 : 3, Dir.SOUTH, false, null, 0);
        c.set(0, 2, -11, "minecraft:cartography_table");
        c.set(0, 3, -11, St.candle("white", 3, true));
        c.set(-1, 2, -11, St.candle("yellow", 2, true));
        c.set(1, 2, -11, St.candle("yellow", 2, true));
        Wing.beamLights(c, st, -3, 3);
        DungeonKit.guard(c, "scriptorium", 0, 0, 0, -3);
        DungeonKit.guard(c, "scriptorium", 1, 0, 0, -8);
        DungeonKit.guard(c, "scriptorium", 2, -2, 0, -5);
        DungeonKit.guard(c, "scriptorium", 3, 2, 0, -9);
    }

    // --- the chapel ----------------------------------------------------------------------------------------------------------

    /** Pews either side of an aisle facing a gilded altar under a golden reredos and a fan of end rods. */
    private static void chapel(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s);
        // A flèche on the ridge: a little quartz lantern under a gilded finial.
        int top = Wing.ridge(c, 0, -6);
        c.set(0, top + 1, -6, "minecraft:chiseled_quartz_block");
        c.set(0, top + 2, -6, "minecraft:quartz_pillar[axis=y]");
        c.set(0, top + 3, -6, "minecraft:quartz_pillar[axis=y]");
        c.set(0, top + 4, -6, "minecraft:gold_block");
        c.set(0, top + 5, -6, St.endRod(Dir.UP));
        for (int z = -7; z <= -2; z += 2) {
            for (int x = -6; x <= 6; x++) {
                if (Math.abs(x) <= 1) continue;
                Furniture.chair(c, x, 0, z, Dir.NORTH, Wood.SPRUCE.stairsId(), null);
            }
        }
        for (int z = -9; z <= -1; z++) c.set(0, 0, z, St.carpet(z % 2 == 0 ? "yellow" : "white"));
        c.set(-1, 0, -9, St.carpet("white"));
        c.set(1, 0, -9, St.carpet("white"));
        // The altar and the reredos behind it (gold used sparingly).
        for (int x = -2; x <= 2; x++) {
            c.set(x, 0, -11, x == 0 ? "minecraft:gold_block" : Family.QUARTZ.block());
            c.set(x, 1, -11, Math.abs(x) == 2 ? St.candle("white", 3, true) : x == 0 ? St.candle("yellow", 1, true) : St.candle("white", 1, true));
        }
        for (int y = 1; y <= 4; y++) c.set(0, y, -12, y == 3 ? "minecraft:gold_block" : "minecraft:chiseled_quartz_block");
        c.set(-1, 3, -12, St.endRod(Dir.WEST));
        c.set(1, 3, -12, St.endRod(Dir.EAST));
        c.set(0, 5, -12, St.endRod(Dir.UP));
        for (int x : new int[]{-5, 5}) {
            c.set(x, 0, -11, "minecraft:quartz_pillar[axis=y]");
            c.set(x, 1, -11, St.endRod(Dir.UP));
        }
        Wing.beamLights(c, st, -4, 4);
        DungeonKit.guard(c, "chapel", 0, 0, 0, -4);
        DungeonKit.guard(c, "chapel", 1, -1, 0, -8);
        DungeonKit.guard(c, "chapel", 2, 1, 0, -10);
        DungeonKit.guard(c, "chapel", 3, -6, 0, -9);
    }

    // --- the dormitory -------------------------------------------------------------------------------------------------------

    /** Plain cots of wool on birch along both walls, a chest-trapdoor at each foot, a side table and candle between pairs. */
    private static void dormitory(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s, Family.EXPOSED_CUT_COPPER, false);
        for (int side : new int[]{-1, 1}) {
            for (int z = -11; z <= -1; z += 3) {
                for (int k = 6; k <= 7; k++) {
                    c.set(side * k, 0, z, St.slabBottom(Wood.BIRCH.slabId()));
                    c.set(side * k, 1, z, St.carpet(k == 7 ? "white" : "light_gray"));
                }
                c.set(side * 5, 0, z, St.trapdoor(Wood.SPRUCE.trapdoorId(), Dir.NORTH, false, false));
                if (z + 1 <= -1) {
                    c.set(side * 7, 0, z + 1, St.fence(Wood.SPRUCE.fenceId()));
                    c.set(side * 7, 1, z + 1, St.candle("white", 1 + Noise.pick(side, 0, z, s + 31, 3), true));
                }
            }
        }
        for (int z = -10; z <= -2; z++) c.set(0, 0, z, St.carpet("light_blue"));
        Wing.beamLights(c, st, -3, 3);
        DungeonKit.guard(c, "dormitory", 0, 0, 0, -3);
        DungeonKit.guard(c, "dormitory", 1, -2, 0, -7);
        DungeonKit.guard(c, "dormitory", 2, 2, 0, -10);
        DungeonKit.guard(c, "dormitory", 3, 3, 0, -5);
    }

    // --- the refectory -------------------------------------------------------------------------------------------------------

    /** A long table with benches, a hearth on the back wall, a water cauldron and a pantry corner. */
    private static void refectory(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s);
        Wing.chimney(c, 0, -11, st.plinth, Family.STONE_BRICKS);
        String slab = Wood.SPRUCE.slabId(), stairs = Wood.SPRUCE.stairsId();
        for (int x = -5; x <= 5; x++) {
            c.set(x, 0, -6, St.slabTop(slab));
            Furniture.chair(c, x, 0, -7, Dir.SOUTH, stairs, null);
            Furniture.chair(c, x, 0, -5, Dir.NORTH, stairs, null);
            if (Math.floorMod(x, 3) == 0) c.set(x, 1, -6, St.candle("white", 2, true));
            else if (Noise.chance(x, 1, -6, s + 41, 0.4)) c.set(x, 1, -6, "minecraft:flower_pot");
        }
        // The hearth: an open fire on netherrack in a stone surround.
        for (int x = -2; x <= 2; x++) {
            c.set(x, 3, -11, Family.STONE_BRICKS.block());
            c.setIfAir(x, 3, -10, Family.STONE_BRICKS.stairs(Dir.NORTH, true));
            if (Math.abs(x) == 2) for (int y = 0; y <= 2; y++) c.set(x, y, -11, Family.STONE_BRICKS.block());
        }
        for (int x = -1; x <= 1; x++) {
            c.set(x, -1, -11, "minecraft:netherrack");
            c.set(x, 0, -11, "minecraft:fire");
            c.set(x, 0, -10, St.fence("minecraft:iron_bars"));
        }
        c.set(-6, 0, -2, St.of("water_cauldron", "level", "3"));
        c.set(6, 0, -10, "minecraft:hay_block[axis=y]");
        c.set(6, 1, -10, "minecraft:hay_block[axis=x]");
        c.set(5, 0, -10, "minecraft:composter[level=0]");
        c.set(6, 0, -9, "minecraft:hay_block[axis=z]");
        c.set(-6, 0, -10, "minecraft:loom[facing=east]");
        Wing.beamLights(c, st, -4, 4);
        DungeonKit.guard(c, "refectory", 0, 0, 0, -3);
        DungeonKit.guard(c, "refectory", 1, -6, 0, -6);
        DungeonKit.guard(c, "refectory", 2, 6, 0, -6);
        DungeonKit.guard(c, "refectory", 3, 0, 0, -9);
    }

    // --- the ossuary ---------------------------------------------------------------------------------------------------------

    /** Walls lined with bone in calcite niches, a reliquary in the middle, soul lanterns and cobwebs. */
    private static void ossuary(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s, Family.DEEPSLATE_TILES, false);
        for (int side : new int[]{-1, 1}) {
            for (int z = -11; z <= -1; z++) {
                if (z == -10 || z == -6 || z == -2) continue;
                for (int y = 0; y <= 3; y++) {
                    double n = Noise.hash01(side, y, z, s + 51);
                    c.set(side * 7, y, z, n < 0.5 ? "minecraft:bone_block[axis=" + (n < 0.25 ? "x" : "z") + "]" : n < 0.8 ? "minecraft:calcite" : "minecraft:bone_block[axis=y]");
                }
                if (Noise.chance(side, 4, z, s + 52, 0.3)) c.set(side * 6, 0, z, St.candle("white", 1 + Noise.pick(side, 0, z, s + 53, 4), true));
            }
        }
        for (int x = -5; x <= 5; x++) {
            if (Math.abs(x) <= 1) continue;
            c.set(x, 0, -11, "minecraft:bone_block[axis=y]");
            c.set(x, 1, -11, Math.abs(x) % 2 == 0 ? "minecraft:bone_block[axis=x]" : "minecraft:calcite");
        }
        // The reliquary.
        c.set(0, 0, -7, "minecraft:chiseled_quartz_block");
        c.set(0, 1, -7, "minecraft:gold_block");
        c.set(0, 2, -7, St.candle("white", 4, true));
        for (Dir d : Dir.HORIZONTAL) c.set(d.dx, 0, -7 + d.dz, Family.QUARTZ.stairs(d.opposite(), false));
        for (int[] w : new int[][]{{-6, 5, -11}, {6, 5, -1}, {-6, 5, -3}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
        for (int z : new int[]{-4, -8}) for (int x : new int[]{-3, 3}) Lighting.hanging(c, x, Wing.WT, z, 2, true);
        DungeonKit.guard(c, "ossuary", 0, 0, 0, -3);
        DungeonKit.guard(c, "ossuary", 1, -3, 0, -9);
        DungeonKit.guard(c, "ossuary", 2, 3, 0, -9);
        DungeonKit.guard(c, "ossuary", 3, 3, 0, -5);
    }

    // --- the herb garden (an empty slot) --------------------------------------------------------------------------------------

    /** A walled herb garden: beds of crops and flowers between gravel walks, azaleas, a wellhead and a bench. */
    private static void garden(Canvas c, DungeonStyle st, int s) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -12; z <= 0; z++) {
                c.set(x, -3, z, "minecraft:dirt");
                c.set(x, -2, z, "minecraft:dirt");
                boolean edge = Math.abs(x) == 8 || z == -12;
                boolean walk = Math.abs(x) <= 1 || z == -6 || z == -1;
                c.set(x, -1, z, edge ? st.plinth.at(x, -1, z) : walk ? st.path.at(x, -1, z) : "minecraft:grass_block");
                for (int y = 0; y <= 8; y++) c.carve(x, y, z);
                if (edge && !(z == 0)) c.set(x, 0, z, St.wall("minecraft:mossy_stone_brick_wall"));
            }
        }
        String[] crops = {"minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots"};
        for (int x = -7; x <= 7; x++) {
            for (int z = -11; z <= -2; z++) {
                if (Math.abs(x) <= 1 || z == -6) continue;
                boolean herbs = (x + 100) / 3 % 2 == 0;
                if (herbs && Math.abs(x) >= 3) {
                    c.set(x, -1, z, "minecraft:farmland[moisture=7]");
                    String crop = crops[Noise.pick(x / 3, 0, z / 3, s + 61, crops.length)];
                    c.set(x, 0, z, St.crop(crop, crop.endsWith("beetroots") ? 3 : 7));
                } else {
                    double n = Noise.hash01(x, 0, z, s + 62);
                    if (n < 0.12) c.set(x, 0, z, "minecraft:flowering_azalea");
                    else if (n < 0.3) c.set(x, 0, z, n < 0.2 ? "minecraft:lily_of_the_valley" : "minecraft:oxeye_daisy");
                    else if (n < 0.4) c.set(x, 0, z, "minecraft:azure_bluet");
                }
            }
        }
        // The wellhead (a covered water cistern) and a bench.
        c.set(0, -1, -9, "minecraft:water");
        c.set(0, -2, -9, "minecraft:stone_bricks");
        for (Dir d : Dir.HORIZONTAL) c.set(d.dx, 0, -9 + d.dz, St.wall("minecraft:stone_brick_wall"));
        for (int x = -1; x <= 1; x += 2) for (int z = -10; z <= -8; z += 2) c.set(x, 0, z, St.wall("minecraft:stone_brick_wall"));
        for (Dir d : Dir.HORIZONTAL) c.set(d.dx, -1, -9 + d.dz, "minecraft:stone_bricks");
        for (int x = -1; x <= 1; x += 2) for (int z = -10; z <= -8; z += 2) c.set(x, -1, z, "minecraft:stone_bricks");
        Furniture.chair(c, -3, 0, -1, Dir.NORTH, Wood.SPRUCE.stairsId(), null);
        Furniture.chair(c, 3, 0, -1, Dir.NORTH, Wood.SPRUCE.stairsId(), null);
        Lighting.lampPost(c, -7, 0, -1, 2, Family.STONE_BRICKS.block(), "minecraft:stone_brick_wall", null, false);
        Lighting.lampPost(c, 7, 0, -1, 2, Family.STONE_BRICKS.block(), "minecraft:stone_brick_wall", null, false);
        DungeonKit.guard(c, "garden", 0, 0, 0, -3);
        DungeonKit.guard(c, "garden", 1, -4, 0, -6);
    }
}
