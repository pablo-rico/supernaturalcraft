package org.papiricoh.supernaturalcraft.crossroads.wild;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Ground;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Paths;
import org.papiricoh.supernaturalcraft.buildkit.Scatter;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.List;

/**
 * A natural crossroads (v0.18, pure): two old dirt roads crossing in open country. Origin = the crossing on the ground
 * ({@code y = 0} first air above it). <b>Contract</b> fixed by the foundations: {@link #CENTRE} is the {@code crossroads_soil}
 * block (in the ground, {@code y = -1}); everything fits in {@link #RADIUS} and {@link #HEIGHT}; {@code suitable} has the
 * grave's shape so {@code CrossroadsStructure} can test a spot cheaply.
 * <p>The build (buildkit): a levelled patch of meadow with a ragged edge; the main road (along {@code dir}) and a narrower lane
 * that bends across it, both rutted dirt fraying into coarse dirt and gravel; the bare patch of soil at the crossing; an old
 * signpost with two boards; a lightning-struck dead oak; the leaning remains of a split-rail fence along the main road; a
 * moss-grown cairn; grass, ferns and dead bushes along the verges. Drawn for {@code dir = 0} (main road north-south) and turned.
 */
public final class CrossroadsLayout {

    public static final LayoutPoint CENTRE = new LayoutPoint(0, -1, 0);
    public static final int RADIUS = 24, HEIGHT = 12;
    /** The meadow's mean radius (its edge wanders a couple of blocks). */
    static final int PATCH = 16;

    private CrossroadsLayout() {
    }

    /** @param dir 0-3, which way the main road runs */
    public static LayoutPlan plan(long seed, int dir) {
        return canvas(seed, dir).toPlan();
    }

    /** The crossroads as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(long seed, int dir) {
        Canvas north = drawn(seed);
        Canvas c = new Canvas("crossroads_wild");
        c.zone("roads", new Box(-RADIUS, -5, -RADIUS, RADIUS, HEIGHT, RADIUS));
        c.inZone("roads", () -> c.stamp(north, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    private static Canvas drawn(long seed) {
        int s = (int) (seed ^ (seed >>> 32));
        Canvas c = new Canvas("crossroads_north");
        c.anchor("centre", CENTRE.x(), CENTRE.y(), CENTRE.z());
        // The meadow: levelled ground with a ragged edge, a little fill under it, the air above it cleared.
        Ground g = new Ground();
        Brush grass = Palette.patches2(s + 1, 4, "minecraft:grass_block", 14, "minecraft:coarse_dirt", 1, "minecraft:podzol", 1);
        for (int x = -PATCH - 3; x <= PATCH + 3; x++) {
            for (int z = -PATCH - 3; z <= PATCH + 3; z++) {
                double r = PATCH * (0.85 + 0.3 * Noise.value2(x, z, 6, s + 2));
                double d = Math.hypot(x, z);
                if (d > r) continue;
                c.set(x, -1, z, grass.at(x, -1, z));
                for (int y = -4; y <= -2; y++) c.set(x, y, z, y == -4 ? "minecraft:stone" : "minecraft:dirt");
                int clear = d < r - 2 ? 6 : 3;
                for (int y = 0; y < clear; y++) c.carve(x, y, z);
                g.set(x, z, -1, 1 - d / r);
            }
        }
        // The roads: the main one straight north-south, the lane bending east-west across it.
        Paths.Style main = new Paths.Style(3.4,
                Palette.patches2(s + 3, 1.5, "minecraft:dirt_path", 6, "minecraft:coarse_dirt", 2, "minecraft:packed_mud", 1),
                Palette.patches2(s + 4, 1.5, "minecraft:coarse_dirt", 4, "minecraft:gravel", 2, "minecraft:dirt_path", 2, "minecraft:rooted_dirt", 1),
                null, null, null, 0, null, new Object[]{"minecraft:short_grass", 6, "minecraft:dead_bush", 1, "minecraft:fern", 2, "minecraft:tall_grass", 2});
        Paths.path(c, g, List.of(new int[]{1, -PATCH - 3}, new int[]{0, -6}, new int[]{0, 6}, new int[]{-1, PATCH + 3}), main, s + 5);
        Paths.Style lane = new Paths.Style(2.4, main.core(), main.edge(), null, null, null, 0, null, main.border());
        Paths.path(c, g, List.of(new int[]{-PATCH - 3, 2}, new int[]{-7, 1}, new int[]{0, 0}, new int[]{7, -1}, new int[]{PATCH + 3, -3}), lane, s + 6);
        // Ruts down the main road's middle.
        for (int z = -PATCH; z <= PATCH; z++) {
            for (int x : new int[]{-1, 1}) {
                String cur = c.get(x, -1, z);
                if (cur != null && (St.path(cur).equals("dirt_path") || St.path(cur).equals("coarse_dirt")) && Noise.chance(x, 0, z, s + 7, 0.7)) {
                    c.set(x, -1, z, "minecraft:packed_mud");
                }
            }
        }
        // The crossing itself: bare, trodden soil round the buried box's spot.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) + Math.abs(z) > 3) continue;
                c.set(x, -1, z, x == 0 && z == 0 ? "supernaturalcraft:crossroads_soil"
                        : Math.abs(x) + Math.abs(z) <= 1 ? "minecraft:coarse_dirt" : Noise.chance(x, 1, z, s, 0.5) ? "minecraft:packed_mud" : "minecraft:dirt_path");
                c.carve(x, 0, z);
                c.carve(x, 1, z);
            }
        }
        // The signpost in the north-east corner: a stripped log post, a fence finial, two boards (wall signs) on its sides.
        int px = 3, pz = -3;
        for (int y = 0; y <= 2; y++) c.set(px, y, pz, St.log("minecraft:stripped_spruce_log"));
        c.set(px, 3, pz, St.fence("minecraft:spruce_fence"));
        c.set(px, -1, pz, "minecraft:coarse_dirt");
        c.set(px - 1, 2, pz, St.wallSign("spruce", Dir.WEST));
        c.decor(Decor.sign(px - 1, 2, pz, "", "<- Lawrence", "12 mi"));
        c.set(px, 1, pz + 1, St.wallSign("spruce", Dir.SOUTH));
        c.decor(Decor.sign(px, 1, pz + 1, "", "Sioux Falls", "40 mi ->"));
        // The struck oak in the south-west, a cairn in the north-west, a split-rail fence leaning along the main road's east.
        Trees.deadOak(c, -9, 0, 8, 1, true, s + 8);
        cairn(c, -6, -9, s);
        for (int z = 4; z <= 13; z++) {
            if (Noise.chance(4, 0, z, s + 9, 0.25)) continue;
            String here = c.get(4, 0, z);
            if (here != null && !Kinds.air(here) && !Kinds.soilPlant(here)) continue;
            if (here != null && Scatter.tall(St.id(here))) c.carve(4, 1, z);
            boolean post = z % 3 == 1;
            c.set(4, 0, z, post ? St.fence("minecraft:spruce_fence") : St.fence("minecraft:dark_oak_fence"));
            c.set(4, -1, z, "minecraft:coarse_dirt");
        }
        c.set(5, 0, 9, St.axis("minecraft:hay_block", "z"));
        c.set(5, -1, 9, "minecraft:coarse_dirt");
        Scatter.flora(c, g, null, 0.55, 1, s + 10, "minecraft:short_grass", 30, "minecraft:tall_grass", 10, "minecraft:fern", 6,
                "minecraft:large_fern", 2, "minecraft:dead_bush", 3, "minecraft:poppy", 1, "minecraft:dandelion", 1, "minecraft:cornflower", 1);
        Scatter.prune(c);
        return c;
    }

    /** A low cairn of mossy stones with a stair skirt. */
    private static void cairn(Canvas c, int x, int z, int s) {
        c.set(x, 0, z, "minecraft:mossy_cobblestone");
        c.set(x, 1, z, St.wall("minecraft:mossy_cobblestone_wall"));
        c.set(x, 2, z, "minecraft:mossy_cobblestone_slab[type=bottom,waterlogged=false]");
        for (Dir d : Dir.HORIZONTAL) {
            if (Noise.chance(x, d.ordinal(), z, s + 11, 0.2)) continue;
            c.set(x + d.dx, 0, z + d.dz, Family.MOSSY_COBBLESTONE.stairs(d.opposite(), false));
            c.set(x + d.dx, -1, z + d.dz, "minecraft:coarse_dirt");
        }
    }

    /** Whether the sampled ground (tops and floors at the centre and four corners) is dry, flat land. */
    public static boolean suitable(int[] tops, int[] floors, int seaLevel) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < tops.length; i++) {
            if (floors[i] < tops[i] || tops[i] <= seaLevel) return false;
            min = Math.min(min, tops[i]);
            max = Math.max(max, tops[i]);
        }
        return max - min <= 3;
    }
}
