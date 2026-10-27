package org.papiricoh.supernaturalcraft.structure.spire;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;

/**
 * The choristers' crypt (pure), cut into the monastery's podium under its south range and cloister. Drawn in the monastery's
 * own coordinates ({@link MonasteryLayout}: gate at the origin, the cloister to the south): a vaulted hall of tuff and calcite,
 * {@link #FLOOR} its feet level. A stair comes down from the chapel along its south wall; burial niches line both long walls,
 * each with a chorister's skull or a candle; an ossuary of bone and skulls fills the north wall behind a low altar; a gilded
 * sarcophagus stands in the middle between two rows of columns that carry the ribbed vault; soul lanterns hang from it.
 * <b>Contract</b>: {@link #CHEST} holds {@link SpireKit#LOOT_CRYPT}.
 */
public final class CryptLayout {

    /** The hall's inside (feet cells): x -6..6, z 8..19, from {@link #FLOOR} up to {@link #CEILING} - 1. */
    public static final Box HALL = new Box(-6, -7, 8, 6, -3, 19);
    public static final int FLOOR = -7, CEILING = -2;
    public static final int[] CHEST = {5, FLOOR, 19};
    /** The stair's bottom step (feet on it) and its top, on the chapel floor. */
    public static final int[] STAIR_FOOT = {-6, FLOOR, 19}, STAIR_HEAD = {2, 0, 19};

    private CryptLayout() {
    }

    static void draw(Canvas c, int s) {
        Box shell = HALL.grow(1);
        Brush wall = Palette.patches(s, 2.2, "minecraft:tuff_bricks", 5, "minecraft:polished_tuff", 2, "minecraft:chiseled_tuff_bricks", 1,
                "minecraft:calcite", 1, "minecraft:mossy_stone_bricks", 1);
        Brush floor = Palette.checker(1, Palette.mix(s + 1, "minecraft:polished_tuff", 4, "minecraft:tuff", 1),
                Palette.mix(s + 2, SpireKit.CALCITE, 4, "minecraft:polished_diorite", 1));
        for (int x = shell.x0(); x <= shell.x1(); x++) {
            for (int z = shell.z0(); z <= shell.z1(); z++) {
                for (int y = shell.y0(); y <= shell.y1(); y++) {
                    if (HALL.contains(x, y, z)) c.carve(x, y, z);
                    else if (y == shell.y0()) c.set(x, y, z, floor.at(x, y, z));
                    else if (y == shell.y1()) c.set(x, y, z, "minecraft:tuff_bricks");
                    else c.set(x, y, z, wall.at(x, y, z));
                }
            }
        }
        vault(c, s);
        niches(c, s);
        ossuary(c, s);
        sarcophagus(c, s);
        stair(c);
        // The chest at the stair's foot, a pot beside it; cobwebs in the corners.
        c.set(CHEST[0], CHEST[1], CHEST[2], St.of("chest", "facing", "west", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(CHEST[0], CHEST[1], CHEST[2], SpireKit.LOOT_CRYPT));
        c.set(6, FLOOR, 19, St.decoratedPot(Dir.WEST));
        for (int[] w : new int[][]{{-6, -3, 8}, {6, -3, 8}, {6, -3, 19}, {-5, -3, 12}, {4, -4, 15}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
    }

    /** Two rows of columns and the ribs of the vault between them and the walls. */
    private static void vault(Canvas c, int s) {
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{11, 15}) {
                c.set(x, FLOOR, z, "minecraft:chiseled_tuff_bricks");
                for (int y = FLOOR + 1; y < CEILING - 1; y++) c.set(x, y, z, SpireKit.pillar("y"));
                c.set(x, CEILING - 1, z, "minecraft:chiseled_tuff");
                // Ribs: upside-down stairs out from each capital along both axes.
                for (Dir d : Dir.HORIZONTAL) c.setIfAir(x + d.dx, CEILING - 1, z + d.dz, Family.TUFF_BRICKS.stairs(d.opposite(), true));
            }
        }
        // The vault's springing all round the walls.
        for (int x = HALL.x0(); x <= HALL.x1(); x++) {
            for (int z = HALL.z0(); z <= HALL.z1(); z++) {
                Dir wall = x == HALL.x0() ? Dir.WEST : x == HALL.x1() ? Dir.EAST : z == HALL.z0() ? Dir.NORTH : z == HALL.z1() ? Dir.SOUTH : null;
                if (wall == null || z == HALL.z1() && x <= 2) continue;
                if ((x + z) % 4 == 0) c.setIfAir(x, CEILING - 1, z, Family.TUFF_BRICKS.stairs(wall, true));
            }
        }
        for (int[] l : new int[][]{{0, 10}, {0, 16}, {-4, 13}, {4, 13}, {4, 18}, {-4, 17}}) Lighting.hanging(c, l[0], CEILING, l[1], 1, true);
    }

    /** Burial niches in both long walls: a skull or a candle in each, a bone lintel over it. */
    private static void niches(Canvas c, int s) {
        for (int x : new int[]{HALL.x0() - 1, HALL.x1() + 1}) {
            Dir in = x < 0 ? Dir.EAST : Dir.WEST;
            for (int z = 9; z <= 17; z += 2) {
                c.carve(x, FLOOR + 1, z);
                c.set(x, FLOOR, z, SpireKit.CALCITE);
                c.set(x, FLOOR + 2, z, St.axis("minecraft:bone_block", "z"));
                double n = Noise.hash01(x, 0, z, s + 10);
                if (n < 0.55) c.set(x, FLOOR + 1, z, St.of("skeleton_skull", "rotation", in == Dir.EAST ? "12" : "4"));
                else c.set(x, FLOOR + 1, z, St.candle(n < 0.8 ? "white" : "black", 1 + Noise.pick(x, 1, z, s + 11, 3), true));
                // Pilasters of calcite between the niches.
                c.set(x, FLOOR + 1, z + 1, "minecraft:polished_tuff");
            }
        }
    }

    /** The north wall: an ossuary of bone blocks with skulls set in it, a low altar before it with candles. */
    private static void ossuary(Canvas c, int s) {
        int z = HALL.z0() - 1;
        for (int x = -5; x <= 5; x++) {
            for (int y = FLOOR; y <= CEILING - 1; y++) {
                boolean skull = (x + y) % 2 == 0 && y > FLOOR && y < CEILING - 1 && Math.abs(x) < 5;
                if (skull) {
                    c.set(x, y, z, "minecraft:bone_block[axis=z]");
                    c.set(x, y, z + 1, St.wallHead("minecraft:skeleton_skull", Dir.SOUTH));
                } else {
                    c.set(x, y, z, St.axis("minecraft:bone_block", Noise.chance(x, y, z, s + 20, 0.5) ? "x" : "y"));
                }
            }
        }
        // Clear the skulls off the altar's row and set the altar.
        for (int x = -2; x <= 2; x++) {
            c.set(x, FLOOR, HALL.z0() + 1, x == 0 ? SpireKit.CHISELED : SpireKit.TRIM.slabTop());
            if (x != 0) c.carve(x, FLOOR + 1, HALL.z0() + 1);
        }
        c.set(0, FLOOR + 1, HALL.z0() + 1, St.candle("white", 4, true));
        c.set(-2, FLOOR + 1, HALL.z0() + 1, St.candle("white", 2, true));
        c.set(2, FLOOR + 1, HALL.z0() + 1, St.candle("white", 1, true));
    }

    /** The first chorister's tomb: calcite and tuff, a gold boss at its head, a skull on the lid. */
    private static void sarcophagus(Canvas c, int s) {
        for (int z = 12; z <= 14; z++) {
            c.set(0, FLOOR, z, z == 12 ? SpireKit.GOLD : "minecraft:chiseled_tuff_bricks");
            c.set(0, FLOOR + 1, z, SpireKit.TRIM.slabBottom());
            for (int x : new int[]{-1, 1}) c.set(x, FLOOR, z, Family.TUFF_BRICKS.stairs(x < 0 ? Dir.EAST : Dir.WEST, false));
        }
        c.set(0, FLOOR + 2, 12, St.of("skeleton_skull", "rotation", "8"));
        c.set(0, FLOOR, 11, St.candle("white", 3, true));
        c.set(0, FLOOR, 15, St.candle("white", 2, true));
    }

    /** The stair down from the chapel, one wide along the south wall, a stepped stringer and parapet on its open side. */
    private static void stair(Canvas c) {
        int steps = -FLOOR, x0 = STAIR_FOOT[0] + 1;
        Furniture.flight(c, x0, FLOOR, STAIR_FOOT[2], Dir.EAST, steps, 1, "minecraft:tuff_brick_stairs", true);
        for (int k = 1; k < steps; k++) {
            int x = x0 + k;
            for (int y = FLOOR; y < FLOOR + k; y++) c.set(x, y, STAIR_FOOT[2] - 1, "minecraft:tuff_bricks");
            // Solid under the step (no dark pocket behind the stringer).
            for (int y = FLOOR; y < FLOOR + k - 1; y++) c.set(x, y, STAIR_FOOT[2], "minecraft:tuff_bricks");
            c.set(x, FLOOR + k, STAIR_FOOT[2] - 1, St.wall("minecraft:tuff_brick_wall"));
        }
    }
}
