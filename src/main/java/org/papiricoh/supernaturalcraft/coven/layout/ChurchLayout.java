package org.papiricoh.supernaturalcraft.coven.layout;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Ground;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Openings;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Paths;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.Scatter;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.List;

/**
 * An abandoned church (v0.21, pure): a ruined stone chapel in open country. Origin = the middle of the nave on its floor
 * ({@code y = 0} the first air above it); drawn for {@code dir = 0} with the entrance (under the bell tower) to the south, +Z, and
 * turned by {@code dir} quarter turns ({@link Canvas#turn}; points with {@link #at}).
 * <p>The build (buildkit): a levelled churchyard with a ragged edge and an old path to the door; a nave of weathered stone bricks
 * on a cobbled plinth with buttresses, lancet windows of stained glass (some smashed), a steep slate roof fallen in over one bay;
 * pews either side of an aisle, the stair down to the crypt behind a railing; a narrower chancel through an arch, its altar broken
 * and still candle-lit, a stained window over it; a square bell tower over the door with a bell in its belfry and a pyramid roof;
 * below the nave the crypt with stone coffins, skulls and cobwebs; graves, a dead oak and a spruce in the churchyard.
 * <b>Contract</b>: {@link #ALTAR} is free floor with a free 3 × 3 ring round it (a ritual altar and its chalk circle go there);
 * {@link #CHESTS} hold {@code supernaturalcraft:chests/abandoned_church}; everything fits in {@link #RADIUS}, {@link #HEIGHT}
 * and {@link #DEPTH}.
 */
public final class ChurchLayout {

    public static final int RADIUS = 20, HEIGHT = 22, DEPTH = 7;
    /** Outside the tower door, on the path. */
    public static final LayoutPoint ENTRY = new LayoutPoint(0, 0, 12);
    /** In the chancel before the broken altar: where a ritual altar is set down (the ring round it is free floor). */
    public static final LayoutPoint ALTAR = new LayoutPoint(0, 0, -10);
    /** The middle of the crypt. */
    public static final LayoutPoint CRYPT = new LayoutPoint(0, -5, -4);
    /** Where the restless dead (ghosts, hexed villagers) stand: the aisle and the crypt. */
    public static final List<LayoutPoint> SPAWNS = List.of(new LayoutPoint(0, 0, -5), new LayoutPoint(0, 0, 2),
            new LayoutPoint(-1, -5, -2), new LayoutPoint(1, -5, -6));
    /** The loot chests: one in the chancel, one in the crypt. */
    public static final List<LayoutPoint> CHESTS = List.of(new LayoutPoint(3, 0, -13), new LayoutPoint(-4, -5, -7));
    public static final String LOOT = "supernaturalcraft:chests/abandoned_church";
    /** The crypt's chest (v0.25): the church's loot, and a nun's bones — the bone of a righteous mortal. */
    public static final String CRYPT_LOOT = "supernaturalcraft:chests/abandoned_church_crypt";

    /** The loot table of chest {@code i} of {@link #CHESTS}. */
    public static String loot(int i) {
        return i == 1 ? CRYPT_LOOT : LOOT;
    }

    /** The nave's and the chancel's walls (outlines), the tower's. */
    static final Box NAVE = new Box(-5, 0, -8, 5, 0, 6), CHANCEL = new Box(-4, 0, -14, 4, 0, -8), TOWER = new Box(-2, 0, 6, 2, 0, 10);
    static final int NAVE_TOP = 6, CHANCEL_TOP = 5, TOWER_TOP = 16, BELFRY_FLOOR = 12;
    /** The crypt's shell (walls, floor and ceiling). */
    static final Box CRYPT_SHELL = new Box(-5, -7, -8, 5, -2, 1);
    static final int PATCH = 16;

    private ChurchLayout() {
    }

    /** {@code p} (drawn for dir 0) turned to {@code dir}. */
    public static LayoutPoint at(LayoutPoint p, int dir) {
        int[] r = Canvas.turn(p.x(), p.z(), dir);
        return new LayoutPoint(r[0], p.y(), r[1]);
    }

    public static LayoutPlan plan(long seed, int dir) {
        return canvas(seed, dir).toPlan();
    }

    /** The church as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(long seed, int dir) {
        Canvas south = drawn(seed);
        Canvas c = new Canvas("coven_church");
        c.zone("church", new Box(-RADIUS, -DEPTH, -RADIUS, RADIUS, HEIGHT, RADIUS));
        c.inZone("church", () -> c.stamp(south, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    /** Whether the sampled ground (tops and floors at the centre and four corners) is dry, flat land. */
    public static boolean suitable(int[] tops, int[] floors, int seaLevel) {
        return Patch.flatAndDry(tops, floors, seaLevel);
    }

    // --- drawing -------------------------------------------------------------------------------------------------------------

    static Canvas drawn(long seed) {
        int s = (int) (seed ^ (seed >>> 32));
        Canvas c = new Canvas("church_south");
        Brush grass = Palette.patches2(s + 1, 4, "minecraft:grass_block", 12, "minecraft:coarse_dirt", 2, "minecraft:podzol", 1,
                "minecraft:moss_block", 1);
        Ground g = Patch.draw(c, s + 2, PATCH, grass, Brush.of("minecraft:dirt"), 15);
        crypt(c, s);
        nave(c, s);
        chancel(c, s);
        roofs(c, s);
        tower(c, s);
        inside(c, s);
        yard(c, g, s);
        c.anchor("entry", ENTRY.x(), ENTRY.y(), ENTRY.z());
        c.anchor("altar", ALTAR.x(), ALTAR.y(), ALTAR.z());
        c.anchor("crypt", CRYPT.x(), CRYPT.y(), CRYPT.z());
        return c;
    }

    static Brush wall(int s) {
        Brush stone = Palette.weathered(Palette.STONE_BRICK_WEAR, Palette.Field.nearGround(-1, 4), 0.3, s + 10);
        return stone.sprinkle(Palette.mix(s + 11, "minecraft:cobblestone", 3, "minecraft:andesite", 2, "minecraft:tuff", 1), 0.12, s + 12);
    }

    static Brush stoneFloor(int s) {
        return Palette.patches(s + 13, 2.5, "minecraft:stone_bricks", 5, "minecraft:cracked_stone_bricks", 3, "minecraft:cobblestone", 2,
                "minecraft:mossy_cobblestone", 2, "minecraft:mossy_stone_bricks", 2, "minecraft:gravel", 1);
    }

    /** Footings under a wall outline, down to the stone. */
    static void footing(Canvas c, Box b) {
        for (Walls.Run r : Walls.around(b)) {
            for (int i = 0; i < r.length(); i++) {
                for (int y = -3; y <= -1; y++) c.set(r.x(i), y, r.z(i), y == -1 ? "minecraft:cobblestone" : "minecraft:cobbled_deepslate");
            }
        }
    }

    private static void nave(Canvas c, int s) {
        Brush stone = wall(s), floor = stoneFloor(s);
        footing(c, NAVE);
        for (int x = NAVE.x0() + 1; x < NAVE.x1(); x++) {
            for (int z = NAVE.z0() + 1; z < NAVE.z1(); z++) {
                c.set(x, -1, z, floor.at(x, -1, z));
                for (int y = 0; y <= NAVE_TOP; y++) c.carve(x, y, z);
            }
        }
        for (Walls.Run r : Walls.around(NAVE)) {
            Walls.body(c, r, 0, NAVE_TOP, stone);
            Walls.plinth(c, r, 0, 1, Family.COBBLESTONE, true);
        }
        // Buttresses down the long walls.
        Walls.Run east = new Walls.Run(5, -8, 5, 6, Dir.EAST), west = new Walls.Run(-5, 6, -5, -8, Dir.WEST);
        for (Walls.Run r : List.of(east, west)) {
            Walls.pillars(c, r, 0, 4, 4, Palette.mix(s + 14, "minecraft:stone_bricks", 3, "minecraft:mossy_stone_bricks", 1),
                    Family.STONE_BRICKS, "minecraft:chiseled_stone_bricks", false);
            Walls.belt(c, r, NAVE_TOP - 1, Family.STONE_BRICKS);
        }
        // Lancets of stained glass, a few smashed.
        String[] glass = {"minecraft:red_stained_glass_pane", "minecraft:blue_stained_glass_pane", "minecraft:yellow_stained_glass_pane",
                "minecraft:purple_stained_glass_pane"};
        for (Walls.Run r : List.of(east, west)) {
            for (int i : new int[]{2, 5, 9, 13}) {
                int pick = Noise.pick(r.x(i), 0, r.z(i), s + 15, glass.length);
                Openings.window(c, r, i, 2, 1, 3, new Openings.WindowStyle(glass[pick], null, Family.STONE_BRICKS, null, null, null, false, null));
                c.set(r.x(i), 5, r.z(i), "minecraft:chiseled_stone_bricks");
                if (Noise.chance(r.x(i), 1, r.z(i), s + 16, 0.35)) {
                    c.carve(r.x(i), 3, r.z(i));
                    c.carve(r.x(i), 4, r.z(i));
                }
            }
        }
        // The chancel arch in the north wall.
        Walls.Run north = new Walls.Run(5, -8, -5, -8, Dir.NORTH);
        Openings.archway(c, north, 3, 0, 5, 5, "minecraft:stone_brick_stairs");
    }

    private static void chancel(Canvas c, int s) {
        Brush stone = wall(s);
        Brush floor = Palette.checker(1, Palette.mix(s + 17, "minecraft:polished_andesite", 4, "minecraft:andesite", 1),
                Palette.mix(s + 18, "minecraft:stone_bricks", 3, "minecraft:cracked_stone_bricks", 2));
        footing(c, CHANCEL);
        for (int x = CHANCEL.x0() + 1; x < CHANCEL.x1(); x++) {
            for (int z = CHANCEL.z0() + 1; z < CHANCEL.z1(); z++) {
                c.set(x, -1, z, floor.at(x, -1, z));
                for (int y = 0; y <= CHANCEL_TOP; y++) c.carve(x, y, z);
            }
        }
        // The arch stays open: the chancel's own south wall is the nave's north wall.
        for (Walls.Run r : Walls.around(CHANCEL)) {
            if (r.out() == Dir.SOUTH) continue;
            Walls.body(c, r, 0, CHANCEL_TOP, stone);
            Walls.plinth(c, r, 0, 1, Family.COBBLESTONE, true);
        }
        Walls.Run ce = new Walls.Run(4, -14, 4, -8, Dir.EAST), cw = new Walls.Run(-4, -8, -4, -14, Dir.WEST);
        Openings.window(c, ce, 3, 2, 1, 2, new Openings.WindowStyle("minecraft:blue_stained_glass_pane", null, Family.STONE_BRICKS, null,
                null, null, false, null));
        Openings.window(c, cw, 3, 2, 1, 2, new Openings.WindowStyle("minecraft:red_stained_glass_pane", null, Family.STONE_BRICKS, null,
                null, null, false, null));
        // The east window over the altar: three lights split by stone mullions.
        Walls.Run back = new Walls.Run(4, -14, -4, -14, Dir.NORTH);
        Openings.window(c, back, 3, 2, 3, 3, new Openings.WindowStyle("minecraft:yellow_stained_glass_pane", null, null,
                Family.STONE_BRICKS, null, null, false, "minecraft:chiseled_stone_bricks"));
        c.set(-1, 3, -14, St.pane("minecraft:red_stained_glass_pane"));
        c.set(1, 3, -14, St.pane("minecraft:blue_stained_glass_pane"));
        Walls.pillars(c, back, 0, 3, 8, Brush.of("minecraft:stone_bricks"), Family.STONE_BRICKS, null, true);
    }

    private static void roofs(Canvas c, int s) {
        Roofs.Style slate = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), wall(s), 1, Roofs.Pitch.STEEP, 0, true,
                null, true);
        new Roofs.Plan().gable(NAVE, NAVE_TOP, false, slate).gable(CHANCEL, CHANCEL_TOP, false, slate).draw(c);
        // Barge boards only at the true gable ends: where the nave's north rake runs down over the chancel's roof it would read as a
        // loose rib across the slope, so there it is roofed in slate like the rest.
        for (int x = CHANCEL.x0() - 3; x <= CHANCEL.x1() + 3; x++) {
            for (int y = CHANCEL_TOP; y <= NAVE_TOP + 8; y++) {
                for (int z = NAVE.z0() - 2; z <= NAVE.z0() + 1; z++) {
                    String b = c.get(x, y, z);
                    if (b == null || !b.startsWith("minecraft:dark_oak_")) continue;
                    String slateRoof = b.replace("minecraft:dark_oak_stairs", "minecraft:deepslate_tile_stairs")
                            .replace("minecraft:dark_oak_slab", "minecraft:deepslate_tile_slab")
                            .replace("minecraft:dark_oak_planks", "minecraft:deepslate_tiles");
                    if (!slateRoof.equals(b)) c.set(x, y, z, slateRoof);
                }
            }
        }
        Palette.swapFamily(c, new Box(-7, NAVE_TOP, -16, 7, 16, 8), Family.DEEPSLATE_TILES, Family.COBBLED_DEEPSLATE, 0.2, 2.5, s + 20);
        Palette.swapFamily(c, new Box(-7, NAVE_TOP, -16, 7, 16, 8), Family.DEEPSLATE_TILES, Family.MOSSY_COBBLESTONE, 0.1, 2, s + 21);
        // One bay fallen in on the east slope: a ragged hole through roof and ceiling.
        for (int x = 1; x <= 6; x++) {
            for (int z = -3; z <= 1; z++) {
                double edge = (Math.abs(z + 1) / 3.0) + (x - 1) / 12.0;
                if (Noise.hash01(x, 0, z, s + 22) < 0.85 - edge * 0.6) {
                    for (int y = NAVE_TOP + 1; y <= 13; y++) c.carve(x, y, z);
                }
            }
        }
        // A bare rafter still spans the hole.
        for (int x = 1; x <= 5; x++) c.set(x, NAVE_TOP + 6 - x, -1, Wood.DARK_OAK.stairs(Dir.WEST, false));
    }

    private static void tower(Canvas c, int s) {
        Brush stone = wall(s);
        footing(c, TOWER);
        for (int x = TOWER.x0() + 1; x < TOWER.x1(); x++) {
            for (int z = TOWER.z0() + 1; z < TOWER.z1(); z++) {
                c.set(x, -1, z, stoneFloor(s).at(x, -1, z));
                for (int y = 0; y < TOWER_TOP; y++) c.carve(x, y, z);
                c.set(x, BELFRY_FLOOR, z, Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:dark_oak_planks"), 3)
                        .at(x, BELFRY_FLOOR, z));
                c.set(x, TOWER_TOP, z, "minecraft:dark_oak_planks");
            }
        }
        for (Walls.Run r : Walls.around(TOWER)) {
            Walls.body(c, r, 0, TOWER_TOP, stone);
            Walls.plinth(c, r, 0, 1, Family.COBBLESTONE, true);
        }
        Walls.quoins(c, TOWER, 0, TOWER_TOP, Palette.mix(s + 23, "minecraft:stone_bricks", 3, "minecraft:mossy_stone_bricks", 1));
        for (Walls.Run r : Walls.around(TOWER)) Walls.belt(c, r, BELFRY_FLOOR, Family.STONE_BRICKS);
        // Belfry openings on all four sides, and the bell hung from the ceiling.
        for (Walls.Run r : Walls.around(TOWER)) {
            if (r.out() == Dir.NORTH) {
                for (int y = BELFRY_FLOOR + 1; y < TOWER_TOP; y++) c.carve(r.x(2), y, r.z(2));
                continue;
            }
            for (int y = BELFRY_FLOOR + 1; y < TOWER_TOP; y++) c.carve(r.x(2), y, r.z(2));
            c.set(r.x(2), BELFRY_FLOOR + 1, r.z(2), St.fence("minecraft:dark_oak_fence"));
        }
        c.set(0, TOWER_TOP - 1, 8, St.of("bell", "attachment", "ceiling", "facing", "north", "powered", "false"));
        c.set(1, BELFRY_FLOOR + 1, 9, St.lantern(false));
        c.set(-1, BELFRY_FLOOR + 1, 7, "minecraft:cobweb");
        Lighting.hanging(c, 0, BELFRY_FLOOR, 8, 3, false);
        // The door south, the arch north into the nave.
        Walls.Run front = new Walls.Run(2, 10, -2, 10, Dir.SOUTH);
        Openings.door(c, front, 2, 0, 1, new Openings.DoorStyle("minecraft:dark_oak_door", Brush.of("minecraft:chiseled_stone_bricks"),
                null, null, null, "minecraft:dark_oak_trapdoor"));
        Walls.Run toNave = new Walls.Run(2, 6, -2, 6, Dir.NORTH);
        Openings.archway(c, toNave, 1, 0, 3, 4, "minecraft:stone_brick_stairs");
        // A pyramid roof and a cross of iron over it.
        Roofs.Style spire = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), stone, 1, Roofs.Pitch.STEEP, 0, false, null, false);
        Roofs.hip(c, TOWER, TOWER_TOP, spire);
        int top = c.topAt(0, 8, HEIGHT, TOWER_TOP);
        c.set(0, top + 1, 8, St.fence("minecraft:dark_oak_fence"));
        c.set(0, top + 2, 8, St.rod("minecraft:lightning_rod", Dir.UP));
    }

    private static void inside(Canvas c, int s) {
        // The stair down to the crypt along the east wall, behind a railing.
        org.papiricoh.supernaturalcraft.buildkit.Furniture.flight(c, 4, -5, -6, Dir.SOUTH, 5, 1, "minecraft:stone_brick_stairs", true);
        for (int z = -5; z <= -2; z++) c.set(3, 0, z, St.fence("minecraft:dark_oak_fence"));
        // Pews either side of the aisle, facing the altar; some broken or gone.
        String pew = "minecraft:spruce_stairs";
        for (int z : new int[]{-5, -3, -1, 1, 3}) {
            for (int x = -4; x <= 4; x++) {
                if (x == 0) continue;
                if (x >= 3 && z <= -2) continue;
                if (x == 4 && z == -1) continue;
                boolean rubble = x >= 1 && z >= -2 && z <= 1;
                if (rubble || Noise.chance(x, 0, z, s + 30, 0.15)) continue;
                c.set(x, 0, z, St.stairs(Noise.chance(x, 1, z, s + 31, 0.25) ? "minecraft:dark_oak_stairs" : pew, Dir.SOUTH, false));
            }
            if (!c.isAir(-4, 0, z)) c.set(-4, 1, z, St.candle("white", 1 + Noise.pick(-4, 1, z, s + 32, 3), true));
        }
        // Rubble under the fallen bay.
        for (int x = 1; x <= 4; x++) {
            for (int z = -2; z <= 1; z++) {
                if (x >= 3 && z <= 0) continue;
                double n = Noise.hash01(x, 0, z, s + 33);
                if (n < 0.3) c.set(x, 0, z, n < 0.15 ? "minecraft:cobblestone" : "minecraft:mossy_cobblestone");
                else if (n < 0.55) c.set(x, 0, z, Family.COBBLED_DEEPSLATE.slabBottom());
                else if (n < 0.65) c.set(x, 0, z, St.stairs("minecraft:deepslate_tile_stairs", Dir.of(new String[]{"north", "east", "south", "west"}[Noise.pick(x, 2, z, s, 4)]), false));
            }
        }
        // Sconces on the walls between the windows.
        for (int z : new int[]{-5, -1, 3}) {
            Lighting.sconce(c, -4, 2, z, Dir.EAST, "minecraft:dark_oak_trapdoor", false);
            if (z != -5) Lighting.sconce(c, 4, 2, z, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        }
        Lighting.sconce(c, 4, 2, -7, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        // Cobwebs in the corners, moss creeping over the floor.
        for (int[] w : new int[][]{{-4, 5, -7}, {4, 5, 5}, {-4, 5, 5}, {-3, 4, -13}, {3, 4, -9}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
        for (int x = -4; x <= 4; x++) {
            for (int z = -7; z <= 5; z++) {
                if (c.isAir(x, 0, z) && Noise.chance(x, 0, z, s + 34, 0.1) && org.papiricoh.supernaturalcraft.buildkit.Kinds.fullCube(c.get(x, -1, z))) c.set(x, 0, z, St.mossCarpet());
            }
        }
        // The broken altar, its candles still lit; a lectern-less reading desk; the chancel's chest.
        for (int x = -2; x <= 2; x++) {
            boolean gone = x == 2 || x == -1 && Noise.chance(x, 0, -13, s + 35, 0.5);
            c.set(x, 0, -13, gone ? Family.STONE_BRICKS.slabBottom() : x == 0 ? "minecraft:chiseled_stone_bricks" : "minecraft:cracked_stone_bricks");
            if (!gone) c.set(x, 1, -13, x == 0 ? St.candle("white", 4, true) : x == 1 ? St.candle("red", 2, true) : St.candle("white", 1 + Noise.pick(x, 1, -13, s + 36, 3), x != -2));
        }
        c.set(2, 0, -12, Family.STONE_BRICKS.slabBottom());
        c.set(-3, 0, -12, St.candle("white", 2, true));
        c.set(3, 0, -10, St.candle("white", 2, true));
        LayoutPoint chest = CHESTS.get(0);
        c.set(chest.x(), chest.y(), chest.z(), St.of("chest", "facing", "west", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(chest.x(), chest.y(), chest.z(), LOOT));
        Lighting.sconce(c, -3, 2, -10, Dir.EAST, "minecraft:dark_oak_trapdoor", true);
        Lighting.sconce(c, 3, 2, -12, Dir.WEST, "minecraft:dark_oak_trapdoor", true);
    }

    private static void crypt(Canvas c, int s) {
        Brush shell = Palette.patches(s + 40, 2, "minecraft:deepslate_bricks", 4, "minecraft:cracked_deepslate_bricks", 2,
                "minecraft:cobbled_deepslate", 2, "minecraft:mossy_stone_bricks", 1, "minecraft:stone_bricks", 1);
        Box b = CRYPT_SHELL;
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int y = b.y0(); y <= b.y1(); y++) {
                for (int z = b.z0(); z <= b.z1(); z++) {
                    boolean in = x > b.x0() && x < b.x1() && z > b.z0() && z < b.z1() && y > b.y0() + 1 && y < b.y1();
                    if (in) c.carve(x, y, z);
                    else c.set(x, y, z, y == b.y0() ? "minecraft:deepslate" : shell.at(x, y, z));
                }
            }
        }
        // Coffins along both long walls, a skull on some, candles at their heads.
        for (int[] k : new int[][]{{-4, -5}, {-4, -2}, {4, -1}, {-4, 0}}) {
            int x = k[0], z = k[1];
            c.set(x, -5, z, "minecraft:polished_deepslate");
            c.set(x, -4, z, Family.DEEPSLATE_TILES.slabBottom());
            if (Noise.chance(x, 0, z, s + 41, 0.6)) c.set(x, -3, z, St.of("skeleton_skull", "rotation", x < 0 ? "4" : "12"));
        }
        c.set(-4, -5, -3, "minecraft:polished_deepslate");
        c.set(-4, -4, -3, St.candle("white", 3, true));
        c.set(4, -5, 0, "minecraft:bone_block[axis=y]");
        c.set(4, -4, 0, St.candle("black", 2, true));
        for (int[] w : new int[][]{{-4, -3, -7}, {4, -3, 0}, {-4, -3, 0}, {0, -3, 0}, {2, -3, -7}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
        Lighting.hanging(c, 0, -2, -4, 1, true);
        Lighting.hanging(c, -2, -2, -1, 1, true);
        Lighting.hanging(c, 2, -2, -6, 1, true);
        LayoutPoint chest = CHESTS.get(1);
        c.set(chest.x(), chest.y(), chest.z(), St.of("chest", "facing", "south", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(chest.x(), chest.y(), chest.z(), CRYPT_LOOT));
    }

    private static void yard(Canvas c, Ground g, int s) {
        Paths.path(c, g, List.of(new int[]{0, 11}, new int[]{1, 14}, new int[]{0, 17}, new int[]{-1, PATCH + 4}), Paths.countryPath(3, s + 50), s + 51);
        // Graves either side of the nave: a mossy headstone, a sunken plot, now and then a slab over it.
        for (int[] gr : new int[][]{{8, -6}, {10, -3}, {8, 0}, {10, 3}, {-8, -5}, {-10, -1}, {-8, 2}, {-9, 7}}) {
            int x = gr[0], z = gr[1];
            if (!g.has(x, z) || !g.has(x, z + 2)) continue;
            c.set(x, 0, z, Noise.chance(x, 0, z, s + 52, 0.5) ? St.wall("minecraft:mossy_cobblestone_wall") : St.wall("minecraft:mossy_stone_brick_wall"));
            for (int k = 1; k <= 2; k++) {
                c.set(x, -1, z + k, Noise.chance(x, k, z, s + 53, 0.5) ? "minecraft:coarse_dirt" : "minecraft:podzol");
                c.carve(x, 0, z + k);
                if (Noise.chance(x, 2, z, s + 54, 0.4)) c.set(x, 0, z + k, Family.STONE.slabBottom());
            }
        }
        // A thin, crooked dead tree well clear of the walls (and two old graves at its foot).
        if (g.has(11, 9)) deadSnag(c, g, 11, 9, s + 55);
        if (g.has(-10, -9)) Trees.spruce(c, -10, 0, -9, 1, s + 56);
        if (g.has(9, -10)) Trees.spruce(c, 9, 0, -10, 0, s + 57);
        // Ivy up the walls.
        for (int z = -7; z <= 5; z++) {
            for (int y = 1; y <= 5; y++) {
                if (Noise.chance(6, y, z, s + 58, 0.25) && c.isAir(6, y, z) && !c.isAir(5, y, z)) c.set(6, y, z, St.vine(Dir.WEST));
                if (Noise.chance(-6, y, z, s + 59, 0.2) && c.isAir(-6, y, z) && !c.isAir(-5, y, z)) c.set(-6, y, z, St.vine(Dir.EAST));
            }
        }
        Scatter.flora(c, g, null, 0.5, 1, s + 60, Scatter.MEADOW);
        Scatter.prune(c);
    }

    /**
     * A thin dead tree: a one-log trunk that leans one way as it rises, one short crooked limb low on the other side and a twig
     * at the top — asymmetric, nothing longer than two blocks, with two sunken graves at its foot.
     */
    static void deadSnag(Canvas c, Ground g, int x, int z, int s) {
        Wood w = Wood.DARK_OAK;
        int lean = Noise.chance(x, 0, z, s, 0.5) ? 1 : -1;
        for (int y = 0; y <= 2; y++) c.set(x, y, z, w.log("y"));
        Trees.line(c, x, 2, z, x + lean, 5, z, w);
        Trees.line(c, x, 1, z, x - lean * 2, 3, z + 1, w);
        Trees.line(c, x + lean, 5, z, x + lean * 2, 6, z - 1, w);
        c.set(x, -1, z, "minecraft:rooted_dirt");
        for (int[] gr : new int[][]{{x - 2, z + 2}, {x + 2, z + 2}}) {
            if (!g.has(gr[0], gr[1]) || !g.has(gr[0], gr[1] + 1) || !c.isAir(gr[0], 0, gr[1])) continue;
            c.set(gr[0], 0, gr[1], St.wall("minecraft:mossy_cobblestone_wall"));
            c.set(gr[0], -1, gr[1] + 1, "minecraft:coarse_dirt");
        }
    }
}
