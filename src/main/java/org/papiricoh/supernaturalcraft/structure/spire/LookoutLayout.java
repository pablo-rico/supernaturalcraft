package org.papiricoh.supernaturalcraft.structure.spire;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;

/**
 * A lookout off the Hymnal Spire's stair (pure): a rounded balcony thrown out over the drop on corbels, a pier under its middle
 * running down to the rock, a balustrade round it. Drawn with its mouth at the origin (the stair's outer edge) facing north and
 * the balcony out to the south; {@link #canvas} turns it. Three kinds ({@code variant}): a bench and a lamp to rest at; a
 * gazebo of four columns under a copper cap; a wayside shrine with a bell on a pedestal among candles.
 */
public final class LookoutLayout {

    public static final int VARIANTS = 3;
    static final double R = 3.6;
    static final int CZ = 3;
    /** Footprint for siting (x and z). */
    public static final Box FOOTPRINT = new Box(-5, 0, -1, 5, 0, 8);

    private LookoutLayout() {
    }

    public static Canvas canvas(long seed, int variant, int dir) {
        Canvas south = drawn(seed, variant);
        Canvas c = new Canvas("spire_lookout");
        c.zone("lookout", new Box(-16, -64, -16, 16, 24, 16));
        c.inZone("lookout", () -> c.stamp(south, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static boolean on(int x, int z) {
        return z >= 0 && Math.hypot(x, z - CZ) <= R;
    }

    static boolean rim(int x, int z) {
        if (!on(x, z) || z == 0 && Math.abs(x) <= 1) return false;
        for (Dir d : Dir.HORIZONTAL) if (!on(x + d.dx, z + d.dz) && !(z + d.dz < 0)) return true;
        return Math.abs(x) >= 2 && z == 0;
    }

    static Canvas drawn(long seed, int variant) {
        SpireKit.init();
        int s = (int) (seed ^ seed >>> 32) + 404;
        Canvas c = new Canvas("lookout_drawn");
        Brush foot = SpireKit.footing(s);
        for (int x = -4; x <= 4; x++) {
            for (int z = 0; z <= 7; z++) {
                if (!on(x, z)) continue;
                double d = Math.hypot(x, z - CZ);
                c.set(x, -1, z, d < 1 ? SpireKit.GOLD : d < 2 ? SpireKit.CHISELED : Noise.chance(x, -1, z, s, 0.3) ? SpireKit.CALCITE : SpireKit.SMOOTH);
                for (int y = 0; y <= 4; y++) c.carve(x, y, z);
                // Corbels: the underside steps in toward the pier.
                if (d > R - 1.2) {
                    Dir out = Dir.toward((int) Math.round(x * 2.0), (int) Math.round((z - CZ) * 2.0));
                    c.set(x, -2, z, SpireKit.QUARTZ_TRIM.stairs(out, true));
                } else if (d > 1.6) {
                    c.set(x, -2, z, SpireKit.QUARTZ_BRICKS);
                    Dir out = Dir.toward((int) Math.round(x * 2.0), (int) Math.round((z - CZ) * 2.0));
                    c.set(x, -3, z, SpireKit.QUARTZ_TRIM.stairs(out, true));
                } else {
                    SpireKit.pier(c, x, z, -2, -4, foot);
                }
                if (rim(x, z)) c.set(x, 0, z, SpireKit.baluster(x, 0, z, s + 1));
            }
        }
        // The mouth onto the stair.
        for (int x = -1; x <= 1; x++) {
            c.set(x, -1, 0, SpireKit.DIORITE);
            SpireKit.pier(c, x, 0, -2, -2, foot);
        }
        switch (Math.floorMod(variant, VARIANTS)) {
            case 0 -> bench(c, s);
            case 1 -> gazebo(c, s);
            default -> shrine(c, s);
        }
        c.anchor("entry", 0, 0, 0);
        c.anchor("view", 0, 0, CZ);
        return c;
    }

    private static void bench(Canvas c, int s) {
        for (int x = -1; x <= 1; x++) c.set(x, 0, CZ + 1, St.stairs("minecraft:smooth_quartz_stairs", Dir.NORTH, false));
        c.set(-2, 0, CZ + 1, SpireKit.TRIM.slabBottom());
        c.set(2, 0, CZ + 1, SpireKit.TRIM.slabBottom());
        Lighting.lampPost(c, -2, 0, 1, 2, SpireKit.CHISELED, SpireKit.BALUSTER, null, false);
        c.set(2, 0, 2, St.potted("azure_bluet"));
    }

    private static void gazebo(Canvas c, int s) {
        int[][] cols = {{-2, 1}, {2, 1}, {-2, 5}, {2, 5}};
        for (int[] p : cols) {
            c.set(p[0], 0, p[1], SpireKit.QUARTZ);
            for (int y = 1; y <= 2; y++) c.set(p[0], y, p[1], SpireKit.pillar("y"));
            c.set(p[0], 3, p[1], SpireKit.CHISELED);
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = 1; z <= 5; z++) {
                boolean ring = Math.abs(x) == 2 || z == 1 || z == 5;
                if (ring && c.isAir(x, 3, z)) c.set(x, 3, z, SpireKit.SMOOTH);
            }
        }
        Roofs.hip(c, new Box(-2, 0, 1, 2, 0, 5), 3, SpireKit.roof(s + 2, Brush.of(SpireKit.SMOOTH)).withOverhang(1));
        for (int x = -1; x <= 1; x++) for (int z = 2; z <= 4; z++) c.setIfAir(x, 4, z, SpireKit.SMOOTH);
        int top = c.topAt(0, CZ, 16, 3);
        c.set(0, top + 1, CZ, St.rod("minecraft:lightning_rod", Dir.UP));
        Lighting.hanging(c, 0, 4, CZ, 1, false);
        c.set(-1, 0, CZ + 1, St.stairs("minecraft:smooth_quartz_stairs", Dir.NORTH, false));
        c.set(1, 0, CZ + 1, St.stairs("minecraft:smooth_quartz_stairs", Dir.NORTH, false));
    }

    private static void shrine(Canvas c, int s) {
        c.set(0, 0, CZ + 2, SpireKit.CHISELED);
        c.set(0, 1, CZ + 2, SpireKit.GOLD);
        c.set(0, 2, CZ + 2, St.of("bell", "attachment", "floor", "facing", "north", "powered", "false"));
        for (int x : new int[]{-1, 1}) {
            c.set(x, 0, CZ + 2, SpireKit.QUARTZ_TRIM.stairs(x < 0 ? Dir.EAST : Dir.WEST, false));
            c.set(x, 1, CZ + 2, St.candle("white", 2 + Noise.pick(x, 1, CZ, s, 3), true));
        }
        c.set(-2, 0, CZ, St.candle("yellow", 3, true));
        c.set(2, 0, CZ, St.candle("white", 2, true));
        c.set(0, 0, CZ, St.carpet("yellow"));
        c.set(1, 0, 1, St.mossCarpet());
    }
}
