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
import org.papiricoh.supernaturalcraft.buildkit.Terrain;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.List;

/**
 * A witch coven (v0.21, pure): a clearing in the swamp or the dark woods where the hedge witches meet. Origin = the middle of
 * the ring of standing stones on the ground ({@code y = 0} the first air above it); drawn for {@code dir = 0} with the way in from
 * the south, +Z, and turned by {@code dir} quarter turns ({@link Canvas#turn}; points with {@link #at}).
 * <p>The build (buildkit): a levelled clearing of podzol, moss, mud and coarse dirt with a ragged edge; a ring of eight standing
 * stones (four trilithons with lintels at the quarters, the southern one the gate; four lone menhirs between); in the middle the
 * bowl altar, a block of polished deepslate under the spell bowl with candle-lit stair wings, a ring of trodden mud round it and
 * skulls on stakes; three crooked huts outside the ring (mud brick and dark oak under steep, patched roofs; inside a cauldron, a
 * brewing stand, barrels, herbs hanging from the rafters and a chest); drying racks hung with roots, a campfire with log seats,
 * bone piles, a stagnant pond, willows and dead oaks at the rim. <b>Contract</b>: {@link #BOWL} is the
 * {@code supernaturalcraft:spell_bowl}; {@link #CHESTS} hold {@code supernaturalcraft:chests/witch_coven}; everything fits in
 * {@link #RADIUS} and {@link #HEIGHT}. {@link #suitable} keeps the dry rule (no standing water under the samples), like the
 * crossroads.
 */
public final class CovenLayout {

    public static final int RADIUS = 22, HEIGHT = 16;
    /** At the clearing's southern edge, on the path in. */
    public static final LayoutPoint ENTRY = new LayoutPoint(0, 0, 15);
    /** The spell bowl on its altar in the middle of the ring. */
    public static final LayoutPoint BOWL = new LayoutPoint(0, 1, 0);
    /** Before the altar, facing the bowl. */
    public static final LayoutPoint ALTAR = new LayoutPoint(0, 0, 2);
    public static final List<LayoutPoint> WITCH_SPAWNS = List.of(new LayoutPoint(-4, 0, 2), new LayoutPoint(4, 0, -2),
            new LayoutPoint(-2, 0, -4), new LayoutPoint(3, 0, 4));
    public static final List<LayoutPoint> FAMILIAR_SPAWNS = List.of(new LayoutPoint(-3, 0, -1), new LayoutPoint(2, 0, -3));
    public static final LayoutPoint COVEN_WITCH_SPAWN = new LayoutPoint(0, 0, -3);
    public static final List<LayoutPoint> VICTIM_SPAWNS = List.of(new LayoutPoint(-3, 0, 10), new LayoutPoint(7, 0, 8));
    /** The loot chests, one in each hut. */
    public static final List<LayoutPoint> CHESTS = List.of(new LayoutPoint(-13, 0, -11), new LayoutPoint(12, 0, -13), new LayoutPoint(14, 0, 4));
    public static final String LOOT = "supernaturalcraft:chests/witch_coven";

    /** The huts' wall outlines and the side their door is on. */
    static final Box HUT_NW = new Box(-14, 0, -12, -9, 0, -7), HUT_NE = new Box(8, 0, -14, 13, 0, -9), HUT_E = new Box(11, 0, 0, 16, 0, 5);
    static final int HUT_TOP = 3, RING = 7, PATCH = 19;

    private CovenLayout() {
    }

    public static LayoutPoint at(LayoutPoint p, int dir) {
        int[] r = Canvas.turn(p.x(), p.z(), dir);
        return new LayoutPoint(r[0], p.y(), r[1]);
    }

    public static LayoutPlan plan(long seed, int dir) {
        return canvas(seed, dir).toPlan();
    }

    /** The coven as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(long seed, int dir) {
        Canvas south = drawn(seed);
        Canvas c = new Canvas("coven_witch_coven");
        c.zone("coven", new Box(-RADIUS, -6, -RADIUS, RADIUS, HEIGHT, RADIUS));
        c.inZone("coven", () -> c.stamp(south, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    /** Whether the sampled ground is dry land within 3 blocks of level (no standing water: the clearing is drained, not flooded). */
    public static boolean suitable(int[] tops, int[] floors, int seaLevel) {
        return Patch.flatAndDry(tops, floors, seaLevel);
    }

    // --- drawing -------------------------------------------------------------------------------------------------------------

    static Canvas drawn(long seed) {
        int s = (int) (seed ^ (seed >>> 32)) * 31 + 7;
        Canvas c = new Canvas("coven_south");
        Brush ground = Palette.patches2(s + 1, 3.5, "minecraft:podzol", 6, "minecraft:grass_block", 5, "minecraft:moss_block", 3,
                "minecraft:coarse_dirt", 3, "minecraft:mud", 2, "minecraft:rooted_dirt", 1);
        Ground g = Patch.draw(c, s + 2, PATCH, ground, Palette.mix(s + 3, "minecraft:dirt", 9, "minecraft:mud", 1), 9);
        for (Box h : List.of(HUT_NW, HUT_NE, HUT_E)) Patch.pad(c, g, h.growXZ(2), ground);
        Terrain.pond(c, g, -9, 10, 2.4, 2, s + 4);
        paths(c, g, s);
        ring(c, s);
        altar(c, s);
        hut(c, HUT_NW, Dir.SOUTH, CHESTS.get(0), s + 10, false);
        hut(c, HUT_NE, Dir.SOUTH, CHESTS.get(1), s + 20, true);
        hut(c, HUT_E, Dir.WEST, CHESTS.get(2), s + 30, false);
        camp(c, g, s);
        rim(c, g, s);
        c.anchor("entry", ENTRY.x(), ENTRY.y(), ENTRY.z());
        c.anchor("bowl", BOWL.x(), BOWL.y(), BOWL.z());
        return c;
    }

    private static void paths(Canvas c, Ground g, int s) {
        Paths.Style trail = new Paths.Style(2.4, Palette.patches2(s + 5, 1.5, "minecraft:dirt_path", 5, "minecraft:coarse_dirt", 2, "minecraft:mud", 1),
                Palette.patches2(s + 6, 1.5, "minecraft:coarse_dirt", 3, "minecraft:podzol", 2, "minecraft:packed_mud", 1), null, null, null, 0, null,
                new Object[]{"minecraft:fern", 4, "minecraft:short_grass", 4, "minecraft:brown_mushroom", 1, "minecraft:red_mushroom", 1});
        Paths.path(c, g, List.of(new int[]{0, PATCH}, new int[]{1, 13}, new int[]{0, 8}), trail, s + 7);
        Paths.path(c, g, List.of(new int[]{-3, -7}, new int[]{-7, -8}, new int[]{-11, -6}), trail, s + 8);
        Paths.path(c, g, List.of(new int[]{4, -6}, new int[]{8, -8}, new int[]{10, -8}), trail, s + 9);
        Paths.path(c, g, List.of(new int[]{7, 1}, new int[]{9, 2}, new int[]{10, 2}), trail, s + 10);
        // Trodden mud round the altar, inside the stones.
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double d = Math.hypot(x, z);
                if (d > 4.5 || d < 1) continue;
                if (d > 2.5 && d < 3.5) c.set(x, -1, z, Noise.chance(x, 0, z, s + 11, 0.3) ? "minecraft:soul_soil" : "minecraft:packed_mud");
                else if (Noise.chance(x, 1, z, s + 12, 0.5)) c.set(x, -1, z, "minecraft:coarse_dirt");
                c.carve(x, 0, z);
                c.carve(x, 1, z);
            }
        }
    }

    private static void ring(Canvas c, int s) {
        Brush stone = Palette.patches(s + 13, 1.8, "minecraft:mossy_cobblestone", 3, "minecraft:cobblestone", 2, "minecraft:tuff", 2,
                "minecraft:mossy_stone_bricks", 2, "minecraft:andesite", 1);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + Math.PI / 2;
            int x = (int) Math.round(Math.cos(a) * RING), z = (int) Math.round(Math.sin(a) * RING);
            if (k % 2 == 0) {
                // A trilithon across the ring's line: two uprights, a lintel.
                Dir across = Math.abs(x) > Math.abs(z) ? Dir.SOUTH : Dir.EAST;
                int h = 3;
                for (int side : new int[]{-1, 1}) {
                    int ux = x + across.dx * side, uz = z + across.dz * side;
                    c.set(ux, -1, uz, "minecraft:cobblestone");
                    for (int y = 0; y < h; y++) c.set(ux, y, uz, stone.at(ux, y, uz));
                }
                for (int i = -1; i <= 1; i++) {
                    int lx = x + across.dx * i, lz = z + across.dz * i;
                    c.set(lx, h, lz, i == 0 ? "minecraft:chiseled_tuff" : "minecraft:tuff_bricks");
                }
                c.set(x + across.dx * 2, h, z + across.dz * 2, Family.TUFF.stairs(across.opposite(), true));
                c.set(x - across.dx * 2, h, z - across.dz * 2, Family.TUFF.stairs(across, true));
                for (int y = 0; y < h; y++) c.carve(x, y, z);
            } else {
                int h = 2 + Noise.pick(x, 0, z, s + 14, 3);
                c.set(x, -1, z, "minecraft:cobblestone");
                for (int y = 0; y < h; y++) c.set(x, y, z, stone.at(x, y, z));
                c.set(x, h, z, Noise.chance(x, 1, z, s + 15, 0.5) ? St.wall("minecraft:mossy_cobblestone_wall") : Family.MOSSY_COBBLESTONE.slabBottom());
                // A stub fallen at its foot.
                Dir out = Dir.toward((int) Math.signum(x), (int) Math.signum(z));
                if (out == null) out = Dir.NORTH;
                c.setIfAir(x + out.dx, 0, z + out.dz, Family.MOSSY_COBBLESTONE.slabBottom());
            }
        }
    }

    private static void altar(Canvas c, int s) {
        c.set(0, -1, 0, "minecraft:polished_deepslate");
        c.set(0, 0, 0, "minecraft:chiseled_polished_blackstone");
        c.set(BOWL.x(), BOWL.y(), BOWL.z(), "supernaturalcraft:spell_bowl");
        c.set(-1, 0, 0, Family.POLISHED_DEEPSLATE.stairs(Dir.EAST, false));
        c.set(1, 0, 0, Family.POLISHED_DEEPSLATE.stairs(Dir.WEST, false));
        c.set(0, 0, -1, Family.POLISHED_DEEPSLATE.stairs(Dir.SOUTH, false));
        c.set(-1, 1, 0, St.candle("purple", 3, true));
        c.set(1, 1, 0, St.candle("black", 2, true));
        c.set(0, 1, -1, St.candle("white", 4, true));
        // Skulls on stakes at the four diagonals.
        for (int[] k : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            c.set(k[0], 0, k[1], St.fence("minecraft:dark_oak_fence"));
            c.set(k[0], 1, k[1], St.fence("minecraft:dark_oak_fence"));
            boolean wither = Noise.chance(k[0], 0, k[1], s + 16, 0.25);
            c.set(k[0], 2, k[1], St.of(wither ? "wither_skeleton_skull" : "skeleton_skull", "rotation", String.valueOf(Noise.pick(k[0], 1, k[1], s + 17, 16))));
        }
    }

    /** A crooked hut on {@code b}, its door in the middle of the {@code door} side, a chest at {@code chest}. */
    private static void hut(Canvas c, Box b, Dir door, LayoutPoint chest, int s, boolean shed) {
        Brush mud = Palette.patches(s, 1.5, "minecraft:mud_bricks", 4, "minecraft:packed_mud", 2, "minecraft:cobblestone", 1);
        Brush boards = Palette.mix(s + 1, "minecraft:dark_oak_planks", 4, "minecraft:spruce_planks", 2, "minecraft:stripped_dark_oak_wood[axis=y]", 1);
        Brush floor = Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:dark_oak_planks"), Brush.of("minecraft:packed_mud"), 3);
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                boolean edge = x == b.x0() || x == b.x1() || z == b.z0() || z == b.z1();
                c.set(x, -1, z, edge ? "minecraft:cobblestone" : floor.at(x, -1, z));
                c.set(x, -2, z, "minecraft:cobblestone");
                c.set(x, -3, z, "minecraft:dirt");
                if (!edge) for (int y = 0; y <= HUT_TOP + 4; y++) c.carve(x, y, z);
            }
        }
        for (Walls.Run r : Walls.around(b)) {
            Walls.body(c, r, 0, 1, mud);
            Walls.body(c, r, 2, HUT_TOP, boards);
            Walls.beam(c, r, HUT_TOP, Wood.DARK_OAK, false);
        }
        Walls.cornerPosts(c, b, 0, HUT_TOP, St.log("minecraft:dark_oak_log"));
        Walls.Run front = null;
        for (Walls.Run r : Walls.around(b)) if (r.out() == door) front = r;
        int mid = front.length() / 2;
        Openings.door(c, front, mid, 0, 1, new Openings.DoorStyle("minecraft:spruce_door", null, null, null, null, null));
        Openings.WindowStyle win = new Openings.WindowStyle("minecraft:brown_stained_glass_pane", null, Wood.SPRUCE.family(), null,
                "minecraft:dark_oak_trapdoor", null, false, null);
        for (Walls.Run r : Walls.around(b)) {
            if (r.out() == door || r.out() == door.opposite()) continue;
            Openings.window(c, r, r.length() / 2, 1, 1, 2, win);
        }
        Roofs.Style roof = new Roofs.Style(Wood.DARK_OAK.family(), Wood.SPRUCE.family(), boards, 1,
                shed ? Roofs.Pitch.HALF : Roofs.Pitch.STEEP, 0, false, null, false);
        if (shed) Roofs.shed(c, b, HUT_TOP, door.opposite(), roof);
        else Roofs.gable(c, b, HUT_TOP, door.axis().equals("z"), roof);
        // A clean eave line: nothing of the roof past its one-block overhang (no ridge slabs poking out of the gables), and the
        // patching of spruce boards kept off the eave row.
        Box eaves = b.growXZ(1);
        for (int x = b.x0() - 3; x <= b.x1() + 3; x++) {
            for (int z = b.z0() - 3; z <= b.z1() + 3; z++) {
                if (eaves.containsColumn(x, z)) continue;
                for (int y = HUT_TOP; y <= HUT_TOP + 8; y++) {
                    String at = c.get(x, y, z);
                    if (at != null && (at.startsWith("minecraft:dark_oak_") || at.startsWith("minecraft:spruce_"))
                            && (at.contains("_stairs") || at.contains("_slab") || at.contains("_planks"))) c.carve(x, y, z);
                }
            }
        }
        Palette.swapFamily(c, b.growXZ(1).withY(HUT_TOP + 2, HUT_TOP + 8), Wood.DARK_OAK.family(), Wood.SPRUCE.family(), 0.3, 1.5, s + 2);
        // Inside: the cauldron, a brewing stand, barrels, herbs hung from the beams, a lantern, the chest.
        int x0 = b.x0() + 1, x1 = b.x1() - 1, z0 = b.z0() + 1, z1 = b.z1() - 1;
        Dir back = door.opposite();
        int bx = back == Dir.NORTH || back == Dir.SOUTH ? (x0 + x1) / 2 : back == Dir.EAST ? x1 : x0;
        int bz = back == Dir.EAST || back == Dir.WEST ? (z0 + z1) / 2 : back == Dir.SOUTH ? z1 : z0;
        c.set(bx, 0, bz, St.of("water_cauldron", "level", "3"));
        Dir side = back.cw();
        c.set(bx + side.dx, 0, bz + side.dz, "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]");
        c.set(bx - side.dx, 0, bz - side.dz, St.barrel(Dir.UP, false));
        c.set(bx - side.dx, 1, bz - side.dz, St.candle("green", 2, true));
        c.set(chest.x(), chest.y(), chest.z(), St.of("chest", "facing", door.id(), "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(chest.x(), chest.y(), chest.z(), LOOT));
        // Rafters across the room under the roof, herbs hanging from them.
        boolean alongX = door.axis().equals("z");
        int ry = HUT_TOP + 1;
        int from = alongX ? x0 : z0, to = alongX ? x1 : z1, lamp = (from + to) / 2;
        for (int i = from; i <= to; i++) {
            int rx = alongX ? i : (x0 + x1) / 2, rz = alongX ? (z0 + z1) / 2 : i;
            c.set(rx, ry, rz, St.axis("minecraft:stripped_dark_oak_log", alongX ? "x" : "z"));
            if (i == lamp) Lighting.hanging(c, rx, ry, rz, 1, false);
            else if (Noise.chance(rx, 0, rz, s + 4, 0.6)) c.set(rx, ry - 1, rz, "minecraft:hanging_roots");
        }
        c.setIfAir(x0, 2, z0, "minecraft:cobweb");
        c.setIfAir(x1, 2, z1, "minecraft:cobweb");
    }

    private static void camp(Canvas c, Ground g, int s) {
        // The campfire west of the ring, log seats round it.
        int fx = -11, fz = 1;
        c.set(fx, -1, fz, "minecraft:coarse_dirt");
        c.set(fx, 0, fz, St.campfire(true, false, Dir.NORTH));
        c.set(fx - 2, 0, fz, St.axis("minecraft:dark_oak_log", "z"));
        c.set(fx + 2, 0, fz, St.axis("minecraft:dark_oak_log", "z"));
        c.set(fx, 0, fz - 2, St.axis("minecraft:spruce_log", "x"));
        c.set(fx - 1, 0, fz - 2, St.axis("minecraft:spruce_log", "x"));
        // Drying racks hung with roots.
        rack(c, -9, -3, true);
        rack(c, 9, 5, false);
        // Bone piles and a cauldron left out in the rain.
        for (int[] k : new int[][]{{-6, -10}, {5, 12}, {-13, 4}}) {
            if (!g.has(k[0], k[1])) continue;
            c.set(k[0], 0, k[1], St.axis("minecraft:bone_block", Noise.chance(k[0], 0, k[1], s + 40, 0.5) ? "x" : "y"));
            c.setIfAir(k[0] + 1, 0, k[1], St.of("skeleton_skull", "rotation", "6"));
        }
        c.set(6, 0, -5, "minecraft:cauldron");
        c.set(-6, 0, 5, St.of("water_cauldron", "level", "2"));
    }

    private static void rack(Canvas c, int x, int z, boolean alongX) {
        Dir a = alongX ? Dir.EAST : Dir.SOUTH;
        for (int i = 0; i <= 2; i++) {
            int rx = x + a.dx * i, rz = z + a.dz * i;
            c.set(rx, 2, rz, St.axis("minecraft:stripped_spruce_log", alongX ? "x" : "z"));
            if (i == 1) c.set(rx, 1, rz, "minecraft:hanging_roots");
        }
        for (int i : new int[]{0, 2}) {
            int rx = x + a.dx * i, rz = z + a.dz * i;
            c.set(rx, 0, rz, St.fence("minecraft:spruce_fence"));
            c.set(rx, 1, rz, St.fence("minecraft:spruce_fence"));
        }
    }

    private static void rim(Canvas c, Ground g, int s) {
        for (int[] t : new int[][]{{-15, 11}, {14, 12}, {-4, -16}, {16, -4}}) {
            if (!g.has(t[0], t[1]) || g.water(t[0], t[1])) continue;
            int i = Math.floorMod(t[0] * 3 + t[1], 3);
            if (i == 0) Trees.willow(c, t[0], 0, t[1], 0, s + 50 + t[0]);
            else if (i == 1) Trees.deadOak(c, t[0], 0, t[1], 0, false, s + 51 + t[0]);
            else Trees.spruce(c, t[0], 0, t[1], 0, s + 52 + t[0]);
        }
        Scatter.flora(c, g, null, 0.45, 1, s + 53, "minecraft:fern", 8, "minecraft:short_grass", 6, "minecraft:large_fern", 2,
                "minecraft:brown_mushroom", 2, "minecraft:red_mushroom", 1, "minecraft:dead_bush", 1, "minecraft:blue_orchid", 1);
        Scatter.prune(c);
        // Cobwebs strung between the ring's stones and the rafters of the racks.
        for (int[] w : new int[][]{{-8, 1, -3}, {10, 1, 5}}) if (c.isAir(w[0], w[1], w[2])) c.set(w[0], w[1], w[2], "minecraft:cobweb");
        // Moss over the stones' feet.
        for (int x = -RING - 1; x <= RING + 1; x++) {
            for (int z = -RING - 1; z <= RING + 1; z++) {
                String below = c.get(x, -1, z);
                if (below != null && St.path(below).equals("moss_block") && c.isAir(x, 0, z) && Noise.chance(x, 0, z, s + 54, 0.5)) {
                    c.set(x, 0, z, St.mossCarpet());
                }
            }
        }
    }
}
