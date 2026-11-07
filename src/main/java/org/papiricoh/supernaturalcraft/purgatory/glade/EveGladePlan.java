package org.papiricoh.supernaturalcraft.purgatory.glade;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Ground;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Scatter;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Wood;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Eve's glade (v0.39), a pure buildkit plan: a round clearing in Purgatory's forest, ringed by gnarled roots that arch out of the
 * ground and back into it, a root throne on a mossy dais at the north with the Mother's tree behind it, standing stones round the
 * open ground, and a way in through the ring at the south. Origin = the clearing's centre; {@code y = 0} the first air layer over
 * its ground ({@code y = -1}). The fight is held over the clearing ({@link #RADIUS}); the rift home opens at {@link #RIFT}.
 */
public final class EveGladePlan {

    /** The clearing's radius; the root ring stands just outside it, the ground is laid to {@link #REACH}. */
    public static final int RADIUS = 22, REACH = RADIUS + 6, DEPTH = 5, HEIGHT = 24;
    /** How far anything of it reaches (the Mother's tree's crown overhangs the ground at the north). */
    public static final int EXTENT = REACH + 9;
    /** Anchors (named points, {@link #at}). */
    public static final String CENTRE = "centre", THRONE = "throne", SEAT = "seat", RIFT = "rift", ENTRY = "entry";
    /** Where her children climb out of the ground (a ring round the centre). */
    public static final int BIRTHS = 8, BIRTH_RING = 11;
    /** Purgatory's own wood, for the roots and the Mother's tree. */
    public static final Wood WOOD = new Wood("purgatory", "supernaturalcraft:purgatory_log", "supernaturalcraft:purgatory_wood",
            "supernaturalcraft:purgatory_log", "supernaturalcraft:purgatory_wood", "supernaturalcraft:purgatory_leaves");
    /** The mod's blocks the plan uses (the rest is vanilla). */
    public static final List<String> MOD_BLOCKS = List.of("supernaturalcraft:purgatory_log", "supernaturalcraft:purgatory_wood",
            "supernaturalcraft:purgatory_leaves", "supernaturalcraft:purgatory_stone", "supernaturalcraft:purgatory_stone_bricks",
            "supernaturalcraft:purgatory_cobblestone");

    private static final int DAIS_Z = -(RADIUS - 5);

    private EveGladePlan() {
    }

    /** A fixed point of the glade (the same for every seed). */
    public static LayoutPoint at(String anchor) {
        return switch (anchor) {
            case CENTRE -> new LayoutPoint(0, 0, 0);
            case THRONE -> new LayoutPoint(0, 1, DAIS_Z + 2);
            case SEAT -> new LayoutPoint(0, 2, DAIS_Z - 1);
            case RIFT -> new LayoutPoint(0, 0, 9);
            case ENTRY -> new LayoutPoint(0, 0, RADIUS + 3);
            default -> throw new IllegalArgumentException("no glade anchor " + anchor);
        };
    }

    /** Where her {@code i}th child climbs out. */
    public static LayoutPoint birth(int i) {
        double a = Math.PI * 2 * i / BIRTHS + Math.PI / BIRTHS;
        return new LayoutPoint((int) Math.round(Math.cos(a) * BIRTH_RING), 0, (int) Math.round(Math.sin(a) * BIRTH_RING));
    }

    public static LayoutPlan plan(long seed) {
        return canvas(seed).toPlan();
    }

    public static Canvas canvas(long seed) {
        int s = (int) (seed ^ (seed >>> 32)) * 31 + 7;
        Canvas c = new Canvas("eve_glade");
        c.zone("ground", new Box(-EXTENT, -DEPTH, -EXTENT, EXTENT, HEIGHT, EXTENT));
        c.zone("roots", new Box(-EXTENT, -DEPTH, -EXTENT, EXTENT, HEIGHT, EXTENT));
        c.zone("throne", new Box(-EXTENT, -DEPTH, -EXTENT, EXTENT, HEIGHT, EXTENT));
        c.zone("stones", new Box(-EXTENT, -DEPTH, -EXTENT, EXTENT, HEIGHT, EXTENT));
        c.zone("flora", new Box(-EXTENT, -DEPTH, -EXTENT, EXTENT, HEIGHT, EXTENT));
        Ground g = new Ground();
        c.inZone("ground", () -> ground(c, g, s));
        c.inZone("roots", () -> roots(c, s));
        c.inZone("throne", () -> throne(c, s));
        c.inZone("stones", () -> stones(c, s));
        c.inZone("flora", () -> Scatter.flora(c, g, new Box(-RADIUS, -1, -RADIUS, RADIUS, -1, RADIUS), 0.3, 1, s + 9,
                "minecraft:short_grass", 50, "minecraft:fern", 26, "minecraft:large_fern", 8, "minecraft:dead_bush", 3));
        for (String a : List.of(CENTRE, THRONE, SEAT, RIFT, ENTRY)) {
            LayoutPoint p = at(a);
            c.anchor(a, p.x(), p.y(), p.z());
        }
        for (int i = 0; i < BIRTHS; i++) {
            LayoutPoint p = birth(i);
            c.anchor("birth_" + i, p.x(), p.y(), p.z());
        }
        return c.finish();
    }

    // --- the ground ------------------------------------------------------------------------------------------------------------

    private static void ground(Canvas c, Ground g, int s) {
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > REACH + 0.5) continue;
                c.set(x, -1, z, topSoil(x, z, r, s));
                for (int y = -2; y >= -DEPTH; y--) c.set(x, y, z, y >= -3 && Noise.chance(x, y, z, s + 3, 0.35) ? "minecraft:rooted_dirt" : "minecraft:dirt");
                g.set(x, z, -1, Math.max(0, 1 - r / REACH));
                if (r <= RADIUS + 2.5) for (int y = 0; y <= 14; y++) c.set(x, y, z, "minecraft:air");
            }
        }
    }

    private static String topSoil(int x, int z, double r, int s) {
        // Moss and roots thicken toward the throne; a worn path of coarse dirt runs from the way in to the centre.
        if (Math.abs(x) <= 1 && z > 2 && z <= REACH && Noise.chance(x, 0, z, s + 1, 0.7)) return "minecraft:coarse_dirt";
        double nearThrone = Math.sqrt(x * x + (z - DAIS_Z) * (double) (z - DAIS_Z));
        if (nearThrone < 7 && Noise.chance(x, 0, z, s + 2, 0.6)) return "minecraft:moss_block";
        return Scatter.pick(x, z, s + 4, "minecraft:grass_block", 42, "minecraft:podzol", 24, "minecraft:moss_block", 18,
                "minecraft:coarse_dirt", 10, "minecraft:rooted_dirt", 6);
    }

    // --- the ring of roots ------------------------------------------------------------------------------------------------------

    private static void roots(Canvas c, int s) {
        int arches = 18;
        for (int k = 0; k < arches; k++) {
            double a = Math.PI * 2 * k / arches + (Noise.hash01(k, 1, 0, s) - 0.5) * 0.2;
            // The way in, at the south (+z), is left open.
            if (Math.abs(Math.atan2(Math.sin(a - Math.PI / 2), Math.cos(a - Math.PI / 2))) < 0.3) continue;
            double out = RADIUS + 4.5 + Noise.hash01(k, 2, 0, s) * 1.5, in = RADIUS - 1.5 - Noise.hash01(k, 3, 0, s) * 1.5;
            double top = 4.5 + Noise.hash01(k, 4, 0, s) * 4.0, mid = (out + in) / 2 + (Noise.hash01(k, 5, 0, s) - 0.5);
            double cos = Math.cos(a), sin = Math.sin(a);
            Trees.line(c, cos * out, 0, sin * out, cos * mid, top, sin * mid, WOOD);
            Trees.line(c, cos * mid, top, sin * mid, cos * in, 0, sin * in, WOOD);
            // A thinner root twisting off some arches.
            if (Noise.chance(k, 6, 0, s, 0.5)) {
                double b = a + (Noise.hash01(k, 7, 0, s) - 0.5) * 0.25;
                Trees.line(c, cos * mid, top - 1, sin * mid, Math.cos(b) * (in - 1.5), 0, Math.sin(b) * (in - 1.5), WOOD);
            }
        }
    }

    // --- the throne and the Mother's tree ---------------------------------------------------------------------------------------

    private static void throne(Canvas c, int s) {
        int z0 = DAIS_Z;
        // The dais: a low mound of moss and roots.
        for (int x = -4; x <= 4; x++) {
            for (int z = z0 - 3; z <= z0 + 2; z++) {
                if (Math.abs(x) == 4 && (z == z0 - 3 || z == z0 + 2)) continue;
                c.set(x, 0, z, Noise.chance(x, 1, z, s + 11, 0.3) ? "minecraft:rooted_dirt" : "minecraft:moss_block");
            }
        }
        // The seat, its arms and its back: roots grown into a chair.
        c.set(0, 1, z0 - 1, St.axis(WOOD.log(), "x"));
        for (int sx : new int[]{-1, 1}) {
            c.set(sx, 1, z0 - 1, WOOD.wood("y"));
            c.set(sx, 2, z0 - 1, WOOD.wood("z"));
        }
        for (int x = -3; x <= 3; x++) {
            int h = 7 - Math.abs(x) - (Math.abs(x) == 3 ? 1 : 0);
            for (int y = 1; y <= h; y++) c.set(x, y, z0 - 2, WOOD.wood("y"));
        }
        // Twisted spires off the back, curling outward like a crown of roots.
        for (int sx : new int[]{-1, 1}) {
            Trees.line(c, 3 * sx, 3, z0 - 2, 4.5 * sx, 8, z0 - 2.5, WOOD);
            Trees.line(c, 4.5 * sx, 8, z0 - 2.5, 3.5 * sx, 11, z0 - 3, WOOD);
            Trees.line(c, 3.5 * sx, 11, z0 - 3, 2.5 * sx, 12, z0 - 2.5, WOOD);
            Trees.line(c, 1 * sx, 6, z0 - 2, 1.5 * sx, 10.5, z0 - 2, WOOD);
        }
        Trees.line(c, 0, 7, z0 - 2, 0, 12.5, z0 - 2.5, WOOD);
        // Roots running off the dais into the ground.
        for (int k = 0; k < 6; k++) {
            double a = Math.PI + Math.PI * k / 5;
            Trees.line(c, Math.cos(a) * 3, 0, z0 - 1 + Math.sin(a) * 1.5, Math.cos(a) * 6.5, 0, z0 - 1 + Math.sin(a) * 4, WOOD);
        }
        // The Mother's tree behind it: a thick trunk, splayed roots, branches and a dark crown.
        int tz = z0 - 9;
        // A thick trunk: a 3×3 cross at the foot, 2×2 up to the fork, leaning a little as it rises.
        for (int y = 0; y <= 13; y++) {
            int lean = y > 8 ? 1 : 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                boolean core = dx >= 0 && dz >= 0;
                boolean flare = y <= 3 && (dx == 0 || dz == 0);
                if (core || flare) c.set(dx + lean, y, tz + dz, WOOD.wood("y"));
            }
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * k / 8 + Noise.hash01(k, 21, 0, s) * 0.5;
            double len = 4 + Noise.hash01(k, 25, 0, s) * 2.5;
            Trees.line(c, 0.5, 2, tz + 0.5, 0.5 + Math.cos(a) * len, -0.4, tz + 0.5 + Math.sin(a) * len, WOOD);
        }
        for (int k = 0; k < 6; k++) {
            double a = Math.PI * 2 * k / 6 + Noise.hash01(k, 22, 0, s) * 0.6;
            double len = 5 + Noise.hash01(k, 23, 0, s) * 2.5;
            double ex = 1.5 + Math.cos(a) * len, ez = tz + 0.5 + Math.sin(a) * len, ey = 12 + Noise.hash01(k, 24, 0, s) * 4;
            Trees.line(c, 1.5, 8 + k % 4, tz + 0.5, ex, ey, ez, WOOD);
            Trees.crown(c, ex, ey + 1, ez, 3.6, 0.65, WOOD.leavesState(), s + 31 * k);
        }
        Trees.crown(c, 1.5, 16, tz + 0.5, 5.5, 0.6, WOOD.leavesState(), s + 77);
    }

    // --- the standing stones ----------------------------------------------------------------------------------------------------

    private static void stones(Canvas c, int s) {
        String[] kinds = {"minecraft:mossy_cobblestone", "minecraft:cobbled_deepslate", "supernaturalcraft:purgatory_stone_bricks",
                "minecraft:mossy_stone_bricks", "supernaturalcraft:purgatory_cobblestone", "minecraft:deepslate_bricks"};
        // Degrees round the centre (0 = east, 90 = the way in): none before the throne or across the path.
        int[] at = {-35, 0, 38, 142, 180, 215};
        for (int i = 0; i < at.length; i++) {
            double a = Math.toRadians(at[i]);
            int x = (int) Math.round(Math.cos(a) * (RADIUS - 5)), z = (int) Math.round(Math.sin(a) * (RADIUS - 5));
            int h = 4 + Noise.pick(i, 0, 0, s + 41, 3);
            // A menhir: two blocks thick (along the ring) at its foot, one at its head, leaning stones of mixed kinds.
            boolean alongX = Math.abs(Math.sin(a)) > 0.7;
            int sx = alongX ? 1 : 0, sz = alongX ? 0 : 1;
            for (int y = 0; y < h; y++) {
                c.set(x, y, z, kinds[(i + y) % kinds.length]);
                if (y < h - 2) c.set(x + sx, y, z + sz, kinds[(i + y + 2) % kinds.length]);
                if (y == 0) c.set(x - sx, y, z - sz, kinds[(i + 3) % kinds.length]);
            }
            if (Noise.chance(i, 2, 0, s + 43, 0.6)) c.set(x, h, z, St.mossCarpet());
            if (Noise.chance(i, 3, 0, s + 44, 0.7)) c.set(x + sx, h - 2, z + sz, St.mossCarpet());
        }
        // One stone has fallen, long ago, and lies in the grass.
        double a = Math.toRadians(110);
        int fx = (int) Math.round(Math.cos(a) * (RADIUS - 6)), fz = (int) Math.round(Math.sin(a) * (RADIUS - 6));
        for (int k = 0; k < 4; k++) c.set(fx + k, 0, fz, kinds[k % kinds.length]);
        c.set(fx + 1, 1, fz, St.mossCarpet());
    }
}
