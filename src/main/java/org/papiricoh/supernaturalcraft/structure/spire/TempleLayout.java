package org.papiricoh.supernaturalcraft.structure.spire;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Openings;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.chorus.Melody;

/**
 * The Hymnal Spire's temple (pure): a small gothic chapel beside the stair whose east window gives the hymn away. Drawn with
 * its door at the origin facing north (toward the stair, {@code -Z}) and the hall running south; {@link #canvas} turns it.
 *
 * <p>A podium of old stone (footings to the mountain) and a paved forecourt with two lamp posts; white walls on a mossy
 * plinth, stepped buttresses, lancets of white glass, a rose in the front gable and a little bell-cote on its ridge; a steep
 * verdigris roof fallen in over one bay. Inside: a nave arcade of quartz columns under beams with hanging lanterns, pews
 * either side of a worn runner, rubble under the hole.
 *
 * <p><b>Contract</b> (the window and the book): the back wall is row {@link #BACK}; its window holds the hymn's three notes as
 * full stained glass at {@code x = 2, 0, -2} (read left to right from inside, looking south), rows 2–5; the lectern with the
 * hymn book stands at (0, 0, {@link #LECTERN}) facing the door; the chest at {@link #CHEST}.
 */
public final class TempleLayout {

    /** Half width of the hall (outer walls at ±HALF), its back wall row, the lectern's row, the wall top. */
    public static final int HALF = 5, BACK = 12, LECTERN = 10, TOP = 7;
    public static final int[] CHEST = {4, 0, 11};
    /** Footprint for siting (x and z, forecourt and buttresses included): x -8..8, z -5..14. */
    public static final Box FOOTPRINT = new Box(-8, 0, -5, 8, 0, 14);

    private TempleLayout() {
    }

    /** The temple turned {@code dir} quarter turns clockwise (0: door facing north). */
    public static Canvas canvas(long seed, byte[] melody, int dir) {
        Canvas south = drawn(seed, melody);
        Canvas c = new Canvas("spire_temple");
        c.zone("temple", new Box(-24, -12, -24, 24, 24, 24));
        c.inZone("temple", () -> c.stamp(south, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn(long seed, byte[] melody) {
        SpireKit.init();
        int s = (int) (seed ^ seed >>> 32) + 101;
        Canvas c = new Canvas("temple_drawn");
        Box hall = new Box(-HALF, 0, 0, HALF, 0, BACK);
        podium(c, s);
        walls(c, hall, s);
        roof(c, hall, s);
        windows(c, melody, s);
        inside(c, s);
        c.anchor("entry", 0, 0, -3);
        c.anchor("lectern", 0, 0, LECTERN);
        return c;
    }

    private static void podium(Canvas c, int s) {
        Brush foot = SpireKit.footing(s);
        Brush floor = SpireKit.floor(s + 1);
        for (int x = -7; x <= 7; x++) {
            for (int z = -1; z <= BACK + 1; z++) {
                boolean rim = Math.abs(x) >= HALF + 1 || z == BACK + 1;
                c.set(x, -1, z, rim ? (Noise.chance(x, -1, z, s + 2, 0.3) ? "minecraft:mossy_stone_bricks" : SpireKit.DIORITE) : floor.at(x, -1, z));
                SpireKit.pier(c, x, z, -2, -3, foot);
                for (int y = 0; y <= 4; y++) c.carve(x, y, z);
            }
        }
        // The forecourt toward the stair: paving, clear air, two lamp posts.
        for (int x = -3; x <= 3; x++) {
            for (int z = -5; z <= -2; z++) {
                c.set(x, -1, z, Math.abs(x) == 3 ? SpireKit.DIORITE : floor.at(x, -1, z));
                SpireKit.pier(c, x, z, -2, -2, foot);
                for (int y = 0; y <= 4; y++) c.carve(x, y, z);
            }
        }
        Lighting.lampPost(c, -3, 0, -2, 2, SpireKit.CHISELED, SpireKit.BALUSTER, null, false);
        Lighting.lampPost(c, 3, 0, -2, 2, SpireKit.CHISELED, SpireKit.BALUSTER, null, false);
    }

    private static void walls(Canvas c, Box hall, int s) {
        Brush stone = SpireKit.wall(s + 3, -1);
        for (Walls.Run r : Walls.around(hall)) {
            Walls.body(c, r, 0, TOP, stone);
            Walls.plinth(c, r, 0, 1, Family.MOSSY_STONE_BRICKS, true);
        }
        for (int x = -HALF + 1; x < HALF; x++) for (int z = 1; z < BACK; z++) for (int y = 0; y <= TOP; y++) c.carve(x, y, z);
        // Stepped buttresses down both long walls and at the back corners.
        Walls.Run east = new Walls.Run(HALF, 0, HALF, BACK, Dir.EAST), west = new Walls.Run(-HALF, BACK, -HALF, 0, Dir.WEST);
        for (Walls.Run r : new Walls.Run[]{east, west}) {
            for (int i : Walls.pillarPositions(r, 4)) {
                int x = r.x(i) + r.out().dx, z = r.z(i) + r.out().dz;
                for (int y = 0; y <= 5; y++) c.set(x, y, z, y == 0 ? SpireKit.QUARTZ : y == 5 ? SpireKit.CHISELED : stone.at(x, y, z));
                c.set(x, 6, z, SpireKit.QUARTZ_TRIM.stairs(r.out().opposite(), false));
                int ox = x + r.out().dx, oz = z + r.out().dz;
                for (int y = 0; y <= 2; y++) c.set(ox, y, oz, y == 0 ? SpireKit.QUARTZ : stone.at(ox, y, oz));
                c.set(ox, 3, oz, SpireKit.QUARTZ_TRIM.stairs(r.out().opposite(), false));
            }
            Walls.belt(c, r, TOP - 1, SpireKit.TRIM);
        }
        // The door: a pointed arch between quartz jambs, a chiseled head.
        Walls.Run front = new Walls.Run(-HALF, 0, HALF, 0, Dir.NORTH);
        Openings.archway(c, front, HALF - 1, 0, 3, 4, "minecraft:quartz_stairs");
        for (int x : new int[]{-2, 2}) {
            for (int y = 0; y <= 3; y++) c.set(x, y, 0, SpireKit.pillar("y"));
            c.set(x, 4, 0, SpireKit.CHISELED);
            c.set(x, 0, -1, SpireKit.QUARTZ_TRIM.stairs(Dir.SOUTH, false));
        }
        for (int x = -1; x <= 1; x++) c.set(x, 4, 0, x == 0 ? SpireKit.GOLD : SpireKit.CHISELED);
    }

    private static void roof(Canvas c, Box hall, int s) {
        Roofs.gable(c, hall, TOP, false, SpireKit.roof(s + 4, SpireKit.wall(s + 5, -1)));
        SpireKit.weatherRoof(c, new Box(-8, TOP, -2, 8, 20, BACK + 2), s + 6);
        // One bay fallen in on the east slope.
        for (int x = 1; x <= 7; x++) {
            for (int z = 5; z <= 8; z++) {
                double edge = Math.abs(z - 6.5) / 2.5 + (x - 1) / 9.0;
                if (Noise.hash01(x, 0, z, s + 7) < 0.95 - edge * 0.55) for (int y = TOP + 1; y <= 16; y++) c.carve(x, y, z);
            }
        }
        // A bell-cote on the front gable's peak.
        int top = c.topAt(0, 0, 24, TOP);
        c.set(0, top + 1, 0, SpireKit.CHISELED);
        for (int x : new int[]{-1, 1}) {
            c.set(x, top + 1, 0, SpireKit.TRIM.slabBottom());
            c.set(x, top + 2, 0, St.wall(SpireKit.BALUSTER));
            c.set(x, top + 3, 0, SpireKit.TRIM.slabBottom());
        }
        c.set(0, top + 3, 0, SpireKit.SMOOTH);
        c.set(0, top + 2, 0, St.of("bell", "attachment", "ceiling", "facing", "east", "powered", "false"));
        c.set(0, top + 4, 0, St.rod("minecraft:lightning_rod", Dir.UP));
    }

    private static void windows(Canvas c, byte[] melody, int s) {
        // Lancets of white glass between the buttresses, each under a chiseled keystone.
        for (int x : new int[]{-HALF, HALF}) {
            for (int z : new int[]{2, 6, 10}) {
                for (int y = 2; y <= 4; y++) c.set(x, y, z, St.pane("minecraft:white_stained_glass_pane"));
                c.set(x, 5, z, SpireKit.CHISELED);
                c.set(x, 1, z, SpireKit.CHISELED);
            }
        }
        // The hymn window: three lights of the notes split by quartz mullions, a pointed head above.
        for (int i = 0; i < Melody.LENGTH; i++) {
            int x = 2 - i * 2;
            for (int y = 2; y <= 5; y++) c.set(x, y, BACK, SpireKit.noteGlass(melody[i]));
        }
        for (int x : new int[]{-1, 1}) for (int y = 2; y <= 5; y++) c.set(x, y, BACK, SpireKit.pillar("y"));
        for (int x : new int[]{-3, 3}) for (int y = 1; y <= 6; y++) c.set(x, y, BACK, y == 1 || y == 6 ? SpireKit.CHISELED : SpireKit.pillar("y"));
        for (int x = -2; x <= 2; x++) c.set(x, 1, BACK, SpireKit.GOLD);
        c.set(-2, 6, BACK, SpireKit.QUARTZ_TRIM.stairs(Dir.WEST, true));
        c.set(2, 6, BACK, SpireKit.QUARTZ_TRIM.stairs(Dir.EAST, true));
        for (int x = -1; x <= 1; x++) c.set(x, 6, BACK, St.pane("minecraft:white_stained_glass_pane"));
        c.set(-1, 7, BACK, SpireKit.QUARTZ_TRIM.stairs(Dir.WEST, true));
        c.set(1, 7, BACK, SpireKit.QUARTZ_TRIM.stairs(Dir.EAST, true));
        c.set(0, 7, BACK, St.pane("minecraft:yellow_stained_glass_pane"));
        c.set(0, 8, BACK, SpireKit.CHISELED);
        // A rose over the door.
        int[][] rose = {{0, 10}, {-1, 10}, {1, 10}, {0, 9}, {0, 11}};
        for (int[] p : rose) c.set(p[0], p[1], 0, St.pane(p[0] == 0 && p[1] == 10 ? "minecraft:yellow_stained_glass_pane" : "minecraft:white_stained_glass_pane"));
        for (int[] p : new int[][]{{-1, 9}, {1, 9}, {-1, 11}, {1, 11}, {-2, 10}, {2, 10}, {0, 8}, {0, 12}}) c.set(p[0], p[1], 0, SpireKit.CHISELED);
    }

    private static void inside(Canvas c, int s) {
        // The aisle: a worn yellow runner over chiseled quartz with gold studs.
        for (int z = 1; z < BACK; z++) {
            c.set(0, -1, z, z % 3 == 2 ? SpireKit.GOLD : SpireKit.CHISELED);
            if (z <= 8 && Noise.chance(0, 0, z, s + 10, 0.75)) c.set(0, 0, z, St.carpet("yellow"));
        }
        // The nave arcade: columns under beams, lanterns hanging from the beams.
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{4, 8}) {
                c.set(x, 0, z, SpireKit.QUARTZ);
                for (int y = 1; y <= 6; y++) c.set(x, y, z, SpireKit.pillar("y"));
                c.set(x, 7, z, SpireKit.CHISELED);
            }
            for (int z = 1; z < BACK; z++) if (z != 4 && z != 8) c.set(x, 7, z, SpireKit.pillar("z"));
            for (int z : new int[]{2, 6, 10}) Lighting.hanging(c, x, 7, z, 2, false);
        }
        // Pews either side of the runner, facing the window; a few broken or gone.
        for (int z : new int[]{2, 5, 6}) {
            for (int x = -4; x <= 4; x++) {
                if (x == 0 || Math.abs(x) == 3 && (z == 4 || z == 8)) continue;
                if (x >= 1 && z >= 5 && Noise.chance(x, 0, z, s + 11, 0.6)) continue;
                if (Noise.chance(x, 1, z, s + 12, 0.12)) continue;
                c.set(x, 0, z, St.stairs("minecraft:birch_stairs", Dir.NORTH, false));
            }
        }
        // Rubble under the fallen bay.
        for (int x = 1; x <= 4; x++) {
            for (int z = 5; z <= 8; z++) {
                if (!c.isAir(x, 0, z) || (x == 3 && z == 8)) continue;
                double n = Noise.hash01(x, 0, z, s + 13);
                if (n < 0.2) c.set(x, 0, z, "minecraft:calcite");
                else if (n < 0.45) c.set(x, 0, z, SpireKit.ROOF.slabBottom());
                else if (n < 0.6) c.set(x, 0, z, SpireKit.TRIM.slabBottom());
                else if (n < 0.75) c.set(x, 0, z, St.mossCarpet());
            }
        }
        // The sanctuary: the lectern facing the door, candles on gold either side, the chest in the corner.
        c.set(0, 0, LECTERN, St.lectern(Dir.NORTH));
        c.decor(Decor.lecternBook(0, 0, LECTERN, "book.supernaturalcraft.hymn"));
        for (int x : new int[]{-2, 2}) {
            c.set(x, 0, LECTERN, SpireKit.GOLD);
            c.set(x, 1, LECTERN, St.candle("white", 3, true));
            c.set(x, 0, BACK - 1, SpireKit.TRIM.slabTop());
            c.set(x, 1, BACK - 1, St.candle("yellow", 1 + Noise.pick(x, 1, BACK - 1, s + 14, 3), true));
        }
        c.set(CHEST[0], CHEST[1], CHEST[2], St.of("chest", "facing", "north", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(CHEST[0], CHEST[1], CHEST[2], SpireKit.LOOT_TEMPLE));
        c.set(-4, 0, BACK - 1, St.decoratedPot(Dir.NORTH));
        c.set(-4, 0, 1, St.candle("white", 2, true));
        Lighting.sconce(c, -4, 2, 3, Dir.EAST, "minecraft:birch_trapdoor", false);
        Lighting.sconce(c, 4, 2, 9, Dir.WEST, "minecraft:birch_trapdoor", false);
        // Cobwebs high in the corners, moss creeping in from the hole.
        for (int[] w : new int[][]{{-4, 6, 1}, {4, 6, 1}, {-4, 6, BACK - 1}, {-2, 6, 3}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
        for (int x = -4; x <= 4; x++) {
            for (int z = 1; z < BACK; z++) {
                if (c.isAir(x, 0, z) && Kinds.fullCube(c.world(x, -1, z)) && Noise.chance(x, 0, z, s + 15, x > 0 && z > 3 && z < 10 ? 0.25 : 0.05)) {
                    c.set(x, 0, z, St.mossCarpet());
                }
            }
        }
        SpireKit.ivy(c, new Box(-HALF - 3, 0, -1, HALF + 3, TOP, BACK + 2), new Box(-HALF, 0, 0, HALF, 0, BACK), 0.18, s + 16);
    }
}
