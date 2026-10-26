package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;

import java.util.List;

/**
 * The Pit Castle's wings (pure, see {@link Wing} for the frame): the cells, the forge, the torture chamber, the barracks and the
 * ritual hall, and the gallows yard that fills a slot left without a wing.
 */
final class CastleWings {

    private CastleWings() {
    }

    static final List<Wing> POOL = List.of(
            new Wing("cells", CastleWings::cells),
            new Wing("forge", CastleWings::forge),
            new Wing("torture", CastleWings::torture),
            new Wing("barracks", CastleWings::barracks),
            new Wing("ritual", CastleWings::ritual));

    static final Wing YARD = new Wing("gallows", CastleWings::gallows);

    // --- the cells -------------------------------------------------------------------------------------------------------------

    /** Two rows of barred cells either side of a corridor; one cell on each side stands open. */
    private static void cells(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s, st.roof, true);
        int[][] cellZ = {{-11, -9}, {-7, -5}, {-3, -1}};
        for (int side : new int[]{-1, 1}) {
            for (int z : new int[]{-8, -4}) {
                for (int k = 3; k <= 7; k++) for (int y = 0; y <= Wing.WT - 1; y++) c.set(side * k, y, z, st.wall.at(side * k, y, z));
            }
            int open = Noise.pick(side, 0, 0, s + 31, 3);
            for (int z = -11; z <= -1; z++) {
                int x = side * 2;
                for (int y = 0; y <= Wing.WT - 1; y++) {
                    boolean partition = z == -8 || z == -4;
                    if (partition || y >= 4) c.set(x, y, z, st.wall.at(x, y, z));
                    else c.set(x, y, z, St.bars());
                }
            }
            for (int ci = 0; ci < 3; ci++) {
                int z0 = cellZ[ci][0], z1 = cellZ[ci][1], mid = (z0 + z1) / 2;
                for (int x = 3; x <= 7; x++) {
                    for (int z = z0; z <= z1; z++) {
                        int wx = side * x;
                        if (Noise.chance(wx, -1, z, s + 32, 0.45)) c.set(wx, -1, z, Noise.chance(wx, 0, z, s + 33, 0.5) ? "minecraft:soul_soil" : "minecraft:soul_sand");
                    }
                }
                // Shackles on the back wall, bones and cobwebs in the corners.
                c.set(side * 7, 2, mid, St.chain("y"));
                c.set(side * 7, 3, mid, St.chain("y"));
                c.set(side * 7, 0, z0, "minecraft:bone_block[axis=" + (Noise.chance(side, ci, 0, s + 34, 0.5) ? "x" : "z") + "]");
                c.set(side * 3, 3, z1, "minecraft:cobweb");
                Lighting.hanging(c, side * 5, Wing.WT + 1, mid, 2, false);
                if (ci == open) {
                    c.carve(side * 2, 0, mid);
                    c.carve(side * 2, 1, mid);
                    c.set(side * 5, -1, mid, "minecraft:soul_soil");
                    c.set(side * 5, 0, mid, "minecraft:soul_fire");
                }
            }
        }
        Wing.beamLights(c, st, 0);
        int openW = Noise.pick(-1, 0, 0, s + 31, 3), openE = Noise.pick(1, 0, 0, s + 31, 3);
        int[] mids = {-10, -6, -2};
        DungeonKit.guard(c, "cells", 0, 0, 0, -2);
        DungeonKit.guard(c, "cells", 1, 0, 0, -10);
        DungeonKit.guard(c, "cells", 2, -4, 0, mids[openW]);
        DungeonKit.guard(c, "cells", 3, 4, 0, mids[openE]);
    }

    // --- the forge -----------------------------------------------------------------------------------------------------------

    /** A lava hearth under a smoke hood at the back, anvils and grindstones, quenching cauldrons and racks of iron. */
    private static void forge(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s);
        // The hearth: a lava trough behind a grate, a hood of nether brick over it and a stack through the roof.
        for (int x = -3; x <= 3; x++) {
            for (int z = -11; z <= -10; z++) {
                boolean rim = Math.abs(x) == 3;
                c.set(x, -1, z, rim ? Family.NETHER_BRICKS.block() : "minecraft:lava");
                c.set(x, -2, z, Family.NETHER_BRICKS.block());
                if (rim) for (int y = 0; y <= 2; y++) c.set(x, y, z, Family.NETHER_BRICKS.block());
            }
            c.set(x, 0, -9, Math.abs(x) == 3 ? Family.NETHER_BRICKS.block() : St.bars());
            c.set(x, 3, -10, Family.NETHER_BRICKS.stairs(Dir.NORTH, true));
            c.set(x, 3, -11, Family.NETHER_BRICKS.block());
            for (int y = 4; y <= Wing.WT; y++) c.set(x, y, -11, Family.NETHER_BRICKS.block());
            for (int y = 4; y <= Wing.WT; y++) if (Math.abs(x) <= 2) c.set(x, y, -10, Family.NETHER_BRICKS.block());
        }
        Roofs.chimney(c, 0, -11, Wing.WT + 1, st.plinth, Family.NETHER_BRICKS, false);
        Roofs.chimney(c, 0, -10, Wing.WT + 1, st.plinth, Family.NETHER_BRICKS, false);
        // Anvils, a smithing table, grindstones; cauldrons of lava and water to quench.
        c.set(-4, 0, -6, St.of("anvil", "facing", "east"));
        c.set(4, 0, -6, St.of("chipped_anvil", "facing", "west"));
        c.set(-6, 0, -10, "minecraft:smithing_table");
        c.set(6, 0, -10, St.of("grindstone", "face", "floor", "facing", "north"));
        c.set(-6, 0, -2, "minecraft:lava_cauldron");
        c.set(6, 0, -2, St.of("water_cauldron", "level", "3"));
        // Racks of blades and chains along the side walls; iron stock in a corner.
        for (int side : new int[]{-1, 1}) {
            for (int z = -7; z <= -3; z += 2) {
                c.set(side * 7, 1, z, St.trapdoor(st.wood.trapdoorId(), side < 0 ? Dir.EAST : Dir.WEST, true, false));
                c.set(side * 7, 2, z, St.rod("minecraft:lightning_rod", Dir.UP));
            }
            c.set(side * 7, 0, -9, "minecraft:iron_block");
            c.set(side * 6, 0, -9, Family.POLISHED_BLACKSTONE.slabBottom());
        }
        c.set(7, 1, -9, "minecraft:iron_block");
        Wing.beamLights(c, st, -3, 3);
        DungeonKit.guard(c, "forge", 0, 0, 0, -3);
        DungeonKit.guard(c, "forge", 1, -2, 0, -7);
        DungeonKit.guard(c, "forge", 2, 2, 0, -7);
        DungeonKit.guard(c, "forge", 3, -5, 0, -4);
    }

    // --- the torture chamber ------------------------------------------------------------------------------------------------

    /** A rack in the middle, cages hung from the beams, an iron maiden and pillories, a soul fire pit, gore in the floor. */
    private static void torture(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s, Family.RED_NETHER_BRICKS, false);
        for (int x = -6; x <= 6; x++) {
            for (int z = -10; z <= -1; z++) {
                if (Noise.chance(x, -1, z, s + 41, 0.18)) c.set(x, -1, z, Noise.chance(x, 1, z, s + 42, 0.5) ? "minecraft:nether_wart_block" : "minecraft:crimson_nylium");
            }
        }
        // The rack: a stone table with shackles at both ends.
        for (int z = -7; z <= -5; z++) c.set(-4, 0, z, Family.POLISHED_BLACKSTONE.slabTop());
        c.set(-4, 1, -8, St.chain("z"));
        c.set(-4, 0, -8, St.wall(st.post));
        c.set(-4, 0, -4, St.wall(st.post));
        c.set(-4, 1, -4, St.chain("z"));
        // Cages hung from the beams.
        DungeonKit.cage(c, 4, Wing.WT - 1, -8, 2, Family.POLISHED_BLACKSTONE.slab(), 0);
        if (Noise.chance(0, 0, 0, s + 43, 0.5)) DungeonKit.cage(c, 4, Wing.WT - 1, -4, 2, Family.POLISHED_BLACKSTONE.slab(), 0);
        // The iron maiden in the back corner, pillories by the door.
        c.set(-6, 0, -11, "minecraft:iron_block");
        c.set(-6, 1, -11, "minecraft:iron_block");
        c.set(-6, 2, -11, Family.POLISHED_BLACKSTONE.slabBottom());
        c.set(-6, 0, -10, St.trapdoor("minecraft:iron_trapdoor", Dir.NORTH, false, true));
        c.set(-6, 1, -10, St.trapdoor("minecraft:iron_trapdoor", Dir.NORTH, false, true));
        for (int x : new int[]{-5, 5}) {
            c.set(x, 0, -2, St.fence(st.wood.fenceId()));
            c.set(x, 1, -2, St.trapdoor(st.wood.trapdoorId(), Dir.SOUTH, true, false));
        }
        // The soul fire pit in the back.
        for (int x = 3; x <= 5; x++) for (int z = -11; z <= -10; z++) c.set(x, -1, z, "minecraft:soul_soil");
        for (int x = 3; x <= 5; x++) c.set(x, 0, -11, "minecraft:soul_fire");
        for (int x = 3; x <= 5; x++) c.set(x, 0, -10, St.bars());
        c.set(0, 0, -11, "minecraft:cauldron");
        c.set(1, 0, -11, St.of("water_cauldron", "level", "2"));
        for (int z : new int[]{-6, -2}) {
            c.set(7, 2, z, St.chain("y"));
            c.set(-7, 2, z, St.chain("y"));
        }
        Wing.beamLights(c, st, -2);
        DungeonKit.guard(c, "torture", 0, 0, 0, -3);
        DungeonKit.guard(c, "torture", 1, -2, 0, -9);
        DungeonKit.guard(c, "torture", 2, 1, 0, -7);
        DungeonKit.guard(c, "torture", 3, 6, 0, -5);
    }

    // --- the barracks --------------------------------------------------------------------------------------------------------

    /** Two-high bunks of crimson along both walls, a mess table down the middle, a weapon wall at the back. */
    private static void barracks(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s);
        Wing.chimney(c, -5, -11, st.plinth, Family.POLISHED_BLACKSTONE_BRICKS);
        Wing.chimney(c, 5, -11, st.plinth, Family.POLISHED_BLACKSTONE_BRICKS);
        String slab = st.wood.slabId(), fence = st.wood.fenceId();
        for (int side : new int[]{-1, 1}) {
            for (int z : new int[]{-10, -7, -4}) {
                for (int dz = 0; dz <= 1; dz++) {
                    int x = side * 6, xx = side * 7;
                    c.set(xx, 0, z + dz, St.slabBottom(slab));
                    c.set(x, 0, z + dz, St.slabBottom(slab));
                    c.set(xx, 1, z + dz, St.carpet("red"));
                    c.set(x, 1, z + dz, St.carpet(dz == 0 ? "red" : "black"));
                    c.set(xx, 2, z + dz, St.slabTop(slab));
                    c.set(x, 2, z + dz, St.slabTop(slab));
                    c.set(xx, 3, z + dz, St.carpet("black"));
                    c.set(x, 3, z + dz, St.carpet("red"));
                }
                c.set(side * 5, 0, z - 1 < -11 ? z : z - 1, St.fence(fence));
                c.set(side * 5, 1, z - 1 < -11 ? z : z - 1, St.fence(fence));
            }
        }
        // The mess table and its benches.
        Furniture.table(c, -1, -9, 1, -6, 0, slab);
        for (int z = -9; z <= -6; z++) {
            Furniture.chair(c, -2, 0, z, Dir.EAST, st.wood.stairsId(), null);
            Furniture.chair(c, 2, 0, z, Dir.WEST, st.wood.stairsId(), null);
        }
        Lighting.candles(c, 0, 1, -8, "red", 3);
        Lighting.candles(c, -1, 1, -6, "black", 2);
        // Weapon wall at the back.
        for (int x = -3; x <= 3; x++) {
            if (x == 0) continue;
            c.set(x, 2, -11, St.rod("minecraft:lightning_rod", Dir.UP));
            c.set(x, 1, -11, St.trapdoor(st.wood.trapdoorId(), Dir.SOUTH, true, false));
        }
        c.set(0, 1, -11, "minecraft:crying_obsidian");
        c.set(0, 2, -11, "minecraft:crying_obsidian");
        Wing.beamLights(c, st, -3, 3);
        DungeonKit.guard(c, "barracks", 0, 0, 0, -3);
        DungeonKit.guard(c, "barracks", 1, -4, 0, -5);
        DungeonKit.guard(c, "barracks", 2, 4, 0, -8);
        DungeonKit.guard(c, "barracks", 3, 3, 0, -2);
        DungeonKit.guard(c, "barracks", 4, -3, 0, -10);
    }

    // --- the ritual hall ------------------------------------------------------------------------------------------------------

    /** A devil's trap inlaid in the floor, an altar of crying obsidian with black candles, chains, soul fire at the points. */
    private static void ritual(Canvas c, DungeonStyle st, int s) {
        Wing.shell(c, st, s, Family.NETHER_BRICKS, false);
        // A spike of crying obsidian on the ridge.
        int top = Wing.ridge(c, 0, -6);
        for (int y = top + 1; y <= top + 3; y++) c.set(0, y, -6, "minecraft:crying_obsidian");
        c.set(0, top + 4, -6, St.wall("minecraft:polished_blackstone_wall"));
        c.set(0, top + 5, -6, St.rod("minecraft:lightning_rod", Dir.UP));
        int cz = -6;
        for (int x = -6; x <= 6; x++) {
            for (int z = -11; z <= -1; z++) {
                double r = Math.hypot(x, z - cz);
                if (r >= 4.5 && r <= 5.3) c.set(x, -1, z, "minecraft:crying_obsidian");
            }
        }
        int[][] v = new int[5][2];
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / 5;
            v[i] = new int[]{(int) Math.round(Math.cos(a) * 4), cz + (int) Math.round(Math.sin(a) * 4)};
        }
        for (int i = 0; i < 5; i++) {
            int[] a = v[i], b = v[(i + 2) % 5];
            int steps = Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]));
            for (int j = 0; j <= steps; j++) {
                int x = Math.round(a[0] + (b[0] - a[0]) * j / (float) steps), z = Math.round(a[1] + (b[1] - a[1]) * j / (float) steps);
                c.set(x, -1, z, "minecraft:red_nether_bricks");
            }
        }
        // Black candles at the star's points.
        for (int[] p : v) c.set(p[0], 0, p[1], St.candle("black", 1 + Noise.pick(p[0], 0, p[1], s + 51, 3), true));
        // The altar at the back with its tall crying-obsidian reredos.
        for (int x = -2; x <= 2; x++) {
            c.set(x, 0, -11, x == 0 ? "minecraft:crying_obsidian" : Family.RED_NETHER_BRICKS.block());
            c.set(x, 1, -11, Math.abs(x) == 2 ? St.candle("red", 2, true) : St.candle("black", 3, true));
        }
        for (int y = 1; y <= 4; y++) c.set(0, y, -12, "minecraft:crying_obsidian");
        c.set(-1, 3, -12, "minecraft:crying_obsidian");
        c.set(1, 3, -12, "minecraft:crying_obsidian");
        for (int x : new int[]{-6, 6}) {
            c.set(x, -1, -11, "minecraft:soul_soil");
            c.set(x, 0, -11, "minecraft:soul_fire");
            c.set(x, -1, -1, "minecraft:soul_soil");
            c.set(x, 0, -1, "minecraft:soul_fire");
        }
        for (int[] ch : new int[][]{{-3, -8}, {3, -8}, {-3, -4}, {3, -4}}) {
            for (int y = 3; y <= Wing.WT - 1; y++) c.set(ch[0], y, ch[1], St.chain("y"));
        }
        Wing.beamLights(c, st, 0);
        DungeonKit.guard(c, "ritual", 0, -5, 0, -6);
        DungeonKit.guard(c, "ritual", 1, 5, 0, -6);
        DungeonKit.guard(c, "ritual", 2, -2, 0, -2);
        DungeonKit.guard(c, "ritual", 3, 2, 0, -10);
    }

    // --- the gallows yard (an empty slot) -------------------------------------------------------------------------------------

    /** An open yard in the slot's frame: a gallows with hanging cages, spikes, braziers, a heap of bones. */
    private static void gallows(Canvas c, DungeonStyle st, int s) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -12; z <= 0; z++) {
                c.set(x, -3, z, "minecraft:blackstone");
                c.set(x, -2, z, "minecraft:blackstone");
                c.set(x, -1, z, st.court.at(x, -1, z));
                for (int y = 0; y <= 8; y++) c.carve(x, y, z);
            }
        }
        // The gallows: two stem posts and a beam, three cages hung under it.
        String post = st.wood.log("y"), beam = st.wood.log("x");
        for (int x : new int[]{-6, 6}) {
            c.set(x, -1, -6, st.plinth.at(x, -1, -6));
            for (int y = 0; y <= 6; y++) c.set(x, y, -6, post);
        }
        for (int x = -6; x <= 6; x++) c.set(x, 7, -6, beam);
        for (int x : new int[]{-3, 0, 3}) DungeonKit.cage(c, x, 5, -6, 2, Family.POLISHED_BLACKSTONE.slab(), 1);
        // Spikes along the back, bones, braziers by the way in.
        for (int x = -7; x <= 7; x += 2) {
            c.set(x, 0, -11, "minecraft:basalt");
            c.set(x, 1, -11, St.dripstone(Dir.UP, "frustum"));
            c.set(x, 2, -11, St.dripstone(Dir.UP, "tip"));
        }
        for (int[] b : new int[][]{{-7, -2}, {7, -9}, {-6, -10}}) c.set(b[0], 0, b[1], "minecraft:bone_block[axis=y]");
        DungeonKit.brazier(c, -4, 0, -1, st.post, Noise.chance(0, 0, 0, s + 61, 0.5));
        DungeonKit.brazier(c, 4, 0, -1, st.post, true);
        DungeonKit.guard(c, "gallows", 0, -2, 0, -3);
        DungeonKit.guard(c, "gallows", 1, 2, 0, -9);
    }

    static Box slotBox() {
        return Wing.OUTLINE;
    }
}
