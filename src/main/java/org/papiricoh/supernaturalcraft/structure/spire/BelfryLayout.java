package org.papiricoh.supernaturalcraft.structure.spire;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Openings;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Walls;

/**
 * The fallen belfry (pure): the stump of the choir's bell tower on a terrace beside the stair, its upper part lying broken
 * down the terrace where it fell, and the great bell in the rubble beside it, on its side and cracked through. Drawn with its
 * door at the origin facing north (toward the stair), the terrace running south; {@link #canvas} turns it.
 *
 * <p>The stump: a square tower of white masonry on a mossy plinth, quartz quoins, its walls snapped off in jagged teeth (high
 * at the back, low where it gave way), a rotten floor and a bell rope hanging inside. The fallen part: a hollow shaft of the
 * same masonry lying along the terrace, holed and broken at both ends, the belfry's arch lying past it. The bell: gold gone
 * green where it touches the ground, a crack across its waist, its clapper and headstock thrown clear.
 */
public final class BelfryLayout {

    /** Footprint for siting (x and z, forecourt included). */
    public static final Box FOOTPRINT = new Box(-9, 0, -4, 10, 0, 20);
    static final Box TOWER = new Box(-3, 0, 1, 3, 0, 7);
    /** The bell: its axis along z at {@link #BELL_X}, crown at {@link #BELL_Z0}, lip at {@link #BELL_Z1}. */
    static final int BELL_X = 4, BELL_Z0 = 10, BELL_Z1 = 15;

    private BelfryLayout() {
    }

    public static Canvas canvas(long seed, int dir) {
        Canvas south = drawn(seed);
        Canvas c = new Canvas("spire_belfry");
        c.zone("belfry", new Box(-40, -64, -40, 40, 40, 40));
        c.inZone("belfry", () -> c.stamp(south, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn(long seed) {
        SpireKit.init();
        int s = (int) (seed ^ seed >>> 32) + 303;
        Canvas c = new Canvas("belfry_drawn");
        terrace(c, s);
        stump(c, s);
        fallenShaft(c, s);
        rubble(c, s);
        bell(c, s);
        c.anchor("entry", 0, 0, -3);
        c.anchor("bell", BELL_X, 0, BELL_Z1 + 1);
        return c;
    }

    /** Whether column (x, z) is on the terrace (its south and side edges ragged). */
    static boolean onTerrace(int x, int z, int s) {
        if (z < 0) return Math.abs(x) <= 2 && z >= -4;
        int edgeX = 7 + Noise.jitter(0, z, s + 1, 1), edgeZ = 18 + Noise.jitter(x, 0, s + 2, 1);
        return x >= -edgeX && x <= edgeX + 1 && z <= edgeZ;
    }

    private static void terrace(Canvas c, int s) {
        Brush foot = SpireKit.footing(s);
        Brush floor = org.papiricoh.supernaturalcraft.buildkit.Palette.patches2(s + 3, 2.5, SpireKit.CALCITE, 4, "minecraft:cobblestone", 2,
                "minecraft:mossy_cobblestone", 2, "minecraft:gravel", 1, SpireKit.QUARTZ_BRICKS, 2, "minecraft:moss_block", 1);
        for (int x = -9; x <= 10; x++) {
            for (int z = -4; z <= 20; z++) {
                if (!onTerrace(x, z, s)) continue;
                c.set(x, -1, z, z < 0 ? SpireKit.floor(s + 4).at(x, -1, z) : floor.at(x, -1, z));
                SpireKit.pier(c, x, z, -2, -3, foot);
                for (int y = 0; y <= 5; y++) c.carve(x, y, z);
            }
        }
        // A low broken wall along the terrace's open edges.
        for (int x = -9; x <= 10; x++) {
            for (int z = 0; z <= 20; z++) {
                if (!onTerrace(x, z, s)) continue;
                boolean edge = !onTerrace(x - 1, z, s) || !onTerrace(x + 1, z, s) || !onTerrace(x, z + 1, s);
                if (edge && Noise.patch2(x, z, 2.5, s + 5) > 0.4) c.set(x, 0, z, SpireKit.baluster(x, 0, z, s + 6));
            }
        }
    }

    /** The tower's stump: walls snapped off in teeth, quoins, the door toward the stair, a rotten floor, a rope. */
    private static void stump(Canvas c, int s) {
        Brush stone = SpireKit.wall(s + 7, -1);
        for (Walls.Run r : Walls.around(TOWER)) {
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                int h = height(x, z, s);
                for (int y = 0; y <= h; y++) c.set(x, y, z, y == 0 ? "minecraft:mossy_stone_bricks" : stone.at(x, y, z));
                if (Noise.chance(x, h, z, s + 8, 0.4)) c.set(x, h + 1, z, SpireKit.TRIM.slabBottom());
            }
            Walls.plinth(c, r, 0, 1, org.papiricoh.supernaturalcraft.buildkit.Family.MOSSY_STONE_BRICKS, true);
        }
        for (int[] q : new int[][]{{-3, 1}, {3, 1}, {-3, 7}, {3, 7}}) {
            for (int y = 0; y <= height(q[0], q[1], s); y++) c.set(q[0], y, q[1], y % 4 == 3 ? SpireKit.CHISELED : SpireKit.pillar("y"));
        }
        Walls.Run front = new Walls.Run(-3, 1, 3, 1, Dir.NORTH);
        Openings.archway(c, front, 2, 0, 3, 4, "minecraft:quartz_stairs");
        for (int x = -1; x <= 1; x++) c.set(x, 4, 1, x == 0 ? SpireKit.GOLD : SpireKit.CHISELED);
        for (Walls.Run r : Walls.around(TOWER)) {
            if (r.out() == Dir.NORTH) continue;
            for (int y : new int[]{2, 3, 6, 7}) if (y < height(r.x(3), r.z(3), s)) c.set(r.x(3), y, r.z(3), St.bars());
        }
        // Inside: a heap of fallen stone, the joists of the ringing floor (most boards gone), the rope.
        for (int x = -2; x <= 2; x++) {
            for (int z = 2; z <= 6; z++) {
                double heap = 2.2 - Math.hypot(x - 0.5, z - 5);
                if (heap > 0 && Noise.chance(x, 0, z, s + 9, 0.8)) c.set(x, 0, z, heap > 1.4 ? "minecraft:cobblestone" : SpireKit.TRIM.slabBottom());
                if (heap > 1.6) c.set(x, 1, z, "minecraft:mossy_cobblestone");
                if (z == 3 || z == 5) c.set(x, 6, z, St.axis("minecraft:stripped_dark_oak_log", "x"));
                else if (Noise.chance(x, 6, z, s + 10, 0.25)) c.set(x, 6, z, St.slabTop("minecraft:dark_oak_slab"));
            }
        }
        for (int y = 2; y <= 5; y++) c.set(-1, y, 3, St.chain("y"));
        c.set(-1, 1, 3, St.lantern(true));
        c.set(2, 5, 2, "minecraft:cobweb");
        c.set(-2, 5, 6, "minecraft:cobweb");
    }

    /** The stump's height at a wall cell: high at the back, low and ragged at the front where it broke. */
    static int height(int x, int z, int s) {
        double base = 8.5 + (z - 1) * 0.55 - Math.abs(x) * 0.15;
        return (int) Math.round(base + (Noise.hash01(x, 0, z, s + 11) - 0.5) * 3.5);
    }

    /** The upper shaft lying along the terrace: a hollow square of masonry, holed, broken at both ends. */
    private static void fallenShaft(Canvas c, int s) {
        Brush stone = SpireKit.wall(s + 12, -1);
        int x0 = -6, x1 = -2;
        for (int z = 9; z <= 17; z++) {
            for (int x = x0; x <= x1; x++) {
                for (int y = 0; y <= 4; y++) {
                    boolean rim = x == x0 || x == x1 || y == 0 || y == 4;
                    if (!rim) continue;
                    double broken = (z == 9 || z == 17 ? 0.45 : 0) + (z == 10 || z == 16 ? 0.15 : 0) + (y == 4 ? 0.12 : 0);
                    if (Noise.patch3(x, y, z, 2, s + 13) < 0.25 + broken) continue;
                    boolean quoin = (x == x0 || x == x1) && (y == 0 || y == 4);
                    c.set(x, y, z, quoin ? SpireKit.pillar("z") : stone.at(x, y, z));
                }
            }
        }
        // The belfry's arch, lying past the shaft's broken end.
        for (int z = 18; z <= 19; z++) {
            if (!onTerrace(-5, z, s)) continue;
            c.set(-5, 0, z, SpireKit.pillar("z"));
            c.set(-3, 0, z, SpireKit.pillar("z"));
        }
        if (onTerrace(-4, 19, s)) c.set(-4, 0, 19, SpireKit.QUARTZ_TRIM.stairs(Dir.SOUTH, false));
    }

    /** The great bell on its side, mouth to the south: a gold shell, verdigris where it lies, cracked across its waist. */
    private static void bell(Canvas c, int s) {
        for (int z = BELL_Z0; z <= BELL_Z1; z++) {
            // Lying on its side, sunk a little into the rubble.
            double r = radius(z), yc = r - 0.9;
            for (int x = BELL_X - 4; x <= BELL_X + 4; x++) {
                for (int y = 0; y <= (int) Math.ceil(yc + r) + 1; y++) {
                    double d = Math.hypot(x - BELL_X, y - yc);
                    boolean shell = z == BELL_Z0 ? d <= r + 0.45 : Math.abs(d - r) <= 0.55;
                    if (!shell) continue;
                    if (crack(x, y, z)) {
                        c.carve(x, y, z);
                        continue;
                    }
                    c.set(x, y, z, bellMetal(x, y, z, s));
                }
            }
        }
        // The crown's loop, the clapper thrown out of the mouth, the broken headstock.
        c.set(BELL_X, 0, BELL_Z0 - 1, SpireKit.GOLD);
        c.set(BELL_X, 1, BELL_Z0 - 1, "minecraft:raw_gold_block");
        c.set(BELL_X + 1, 0, BELL_Z1 + 2, "minecraft:iron_block");
        c.set(BELL_X, 0, BELL_Z1 + 2, St.chain("x"));
        c.set(BELL_X - 1, 0, BELL_Z1 + 3, St.chain("x"));
        for (int x = 1; x <= 6; x++) {
            if (x == 3) continue;
            c.set(x, 0, BELL_Z0 - 2, St.axis(x % 4 == 0 ? "minecraft:stripped_dark_oak_log" : "minecraft:dark_oak_log", "x"));
        }
        c.set(7, 1, BELL_Z0 - 2, St.axis("minecraft:dark_oak_log", "y"));
        c.set(7, 0, BELL_Z0 - 2, St.axis("minecraft:dark_oak_log", "y"));
    }

    /** The bell's radius from crown to lip. */
    static double radius(int z) {
        double t = (z - BELL_Z0) / (double) (BELL_Z1 - BELL_Z0);
        return 1.3 + t * 1.1 + t * t * 0.8;
    }

    /** A crack zig-zagging across the bell's waist. */
    static boolean crack(int x, int y, int z) {
        int line = 12 + ((x + y) % 3 == 0 ? 1 : 0) - (y > 3 ? 1 : 0);
        return z == line && x >= BELL_X - 1 && y >= 1;
    }

    private static String bellMetal(int x, int y, int z, int s) {
        double green = (1.4 - y) * 0.6 + Noise.patch3(x, y, z, 2, s + 14) - 0.5;
        if (green > 0.45) return Noise.chance(x, y, z, s + 15, 0.5) ? "minecraft:waxed_weathered_copper" : "minecraft:waxed_exposed_copper";
        if (Noise.chance(x, y, z, s + 16, 0.14)) return "minecraft:raw_gold_block";
        return SpireKit.GOLD;
    }

    private static void rubble(Canvas c, int s) {
        for (int x = -7; x <= 9; x++) {
            for (int z = 8; z <= 20; z++) {
                if (!onTerrace(x, z, s) || !c.isAir(x, 0, z) || !Kinds.fullCube(c.world(x, -1, z))) continue;
                double near = Noise.patch2(x, z, 3, s + 17);
                if (near < 0.55 || !Noise.chance(x, 0, z, s + 18, 0.6)) continue;
                double n = Noise.hash01(x, 1, z, s + 19);
                c.set(x, 0, z, n < 0.25 ? "minecraft:cobblestone" : n < 0.45 ? SpireKit.TRIM.slabBottom() : n < 0.6 ? "minecraft:calcite"
                        : n < 0.75 ? SpireKit.QUARTZ_TRIM.stairs(Dir.HORIZONTAL[Noise.pick(x, 2, z, s, 4)], false) : St.mossCarpet());
            }
        }
        SpireKit.ivy(c, new Box(-9, 0, 0, 10, 14, 20), new Box(-2, 0, 2, 2, 0, 6), 0.15, s + 20);
    }
}
