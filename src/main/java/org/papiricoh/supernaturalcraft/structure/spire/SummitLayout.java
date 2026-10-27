package org.papiricoh.supernaturalcraft.structure.spire;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.chorus.Melody;

/**
 * The Hymnal Spire's summit (pure): the ruined choir platform over the peak where the Broken Chorus is fought. Origin = the
 * Choir Altar ({@code y = 0}; the platform's top layer is {@code y = -1}).
 *
 * <ul>
 *   <li>A disc of {@link #RADIUS}, three blocks thick: a gold ring, rays of smooth quartz, a chiseled ring under the bells,
 *   weathering toward the rim (calcite, cracks, moss), bites already gone from its edge.</li>
 *   <li>Under the last floor ({@link #INNER}, the part the fight never drops) a pedestal of old stone running down to the
 *   mountain; beyond it the ring hangs on twelve curved ribs over nothing (the fight drops it ring by ring).</li>
 *   <li>The altar on a gold dais with the seven bells round it at {@link #BELL_RING}, each on a pane of its colour.</li>
 *   <li>Six quartz pillars at {@link #INNER_PILLARS} (whole: they shade the last phase) and an outer arcade at
 *   {@link #OUTER_PILLARS}: pillars standing or snapped, arches between some of them, others fallen across the floor.</li>
 *   <li>A broken balustrade round the rim.</li>
 * </ul>
 * The stair arrives from outside (the ascent opens the rim where it lands).
 */
public final class SummitLayout {

    public static final int RADIUS = 22;
    /** The last floor of the Chorus fight ({@code ChorusEntity.FLOOR_RADIUS[4]}): inside it nothing ever falls. */
    public static final int INNER = 10;
    public static final double INNER_PILLARS = 8.5, OUTER_PILLARS = 16.5, BELL_RING = 5, GOLD_RING = 11.5;
    /** Sky cleared over the platform, and how deep the void under the hanging ring is. */
    static final int CLEAR_ABOVE = 13, VOID_BELOW = 16;

    private SummitLayout() {
    }

    /** The angle of the first bell (the bells, the pillars and the arcade all turn with it). */
    public static double turn(long seed) {
        return Noise.hash01(7, 7, 7, (int) (seed ^ seed >>> 32)) * Math.PI * 2;
    }

    /** Where bell {@code i} stands (x, z; its base is at {@code y = -1}, its glass at 0, the bell at 1). */
    public static int[] bell(long seed, int i) {
        double a = turn(seed) + i * Math.PI * 2 / Melody.NOTES;
        return new int[]{(int) Math.round(Math.cos(a) * BELL_RING), (int) Math.round(Math.sin(a) * BELL_RING)};
    }

    public static Canvas canvas(long seed) {
        SpireKit.init();
        int s = (int) (seed ^ seed >>> 32);
        Canvas c = new Canvas("spire_summit");
        int r = RADIUS + 1;
        c.zone("summit", new Box(-r, -VOID_BELOW - 1, -r, r, CLEAR_ABOVE + 2, r));
        c.inZone("summit", () -> {
            platform(c, s);
            undercroft(c, s);
            dais(c, seed);
            innerPillars(c, seed, s);
            arcade(c, seed, s);
            rim(c, s);
            weather(c, s);
            c.anchor("altar", 0, 0, 0);
        });
        return c.finish();
    }

    // --- the platform -----------------------------------------------------------------------------------------------------

    static double dist(int x, int z) {
        return Math.sqrt(x * x + z * z);
    }

    /** A bite out of the rim, already fallen. */
    static boolean lost(int x, int z, int s) {
        double d = dist(x, z);
        if (d <= RADIUS - 3.5) return false;
        double n = Noise.patch2(x, z, 3.5, s + 3);
        return n < 0.2 + (d - (RADIUS - 3.5)) * 0.05;
    }

    private static void platform(Canvas c, int s) {
        Brush under = Palette.patches(s + 4, 2.5, SpireKit.QUARTZ_BRICKS, 4, SpireKit.CALCITE, 3, "minecraft:diorite", 1);
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = dist(x, z);
                if (d > RADIUS + 0.5) continue;
                for (int y = 0; y <= CLEAR_ABOVE; y++) c.carve(x, y, z);
                if (lost(x, z, s)) {
                    // The bite goes through all three layers at the very edge, only the top a little further in.
                    boolean through = d > RADIUS - 1.5 || Noise.chance(x, 0, z, s + 5, 0.5);
                    for (int y = -3; y <= -1; y++) c.carve(x, y, z);
                    if (through) continue;
                    c.set(x, -2, z, under.at(x, -2, z));
                    c.set(x, -3, z, under.at(x, -3, z));
                    if (Noise.chance(x, 1, z, s + 6, 0.5)) c.set(x, -1, z, St.slabBottom("minecraft:smooth_quartz_slab"));
                    continue;
                }
                c.set(x, -1, z, top(x, z, d, s));
                c.set(x, -2, z, under.at(x, -2, z));
                c.set(x, -3, z, d > RADIUS - 0.5 ? SpireKit.CHISELED : under.at(x, -3, z));
            }
        }
    }

    /** The top layer: gold ring, chiseled bell ring, rays of smooth quartz, white paving weathering toward the rim. */
    private static String top(int x, int z, double d, int s) {
        if (Math.abs(d - BELL_RING) < 0.7) return SpireKit.CHISELED;
        if (Math.abs(d - GOLD_RING) < 0.55) return Noise.chance(x, 2, z, s + 7, 0.12) ? "minecraft:raw_gold_block" : SpireKit.GOLD;
        double a = Math.atan2(z, x);
        if (d > 2 && d < RADIUS - 2 && Math.abs(Math.sin(a * 4)) < 0.11) return Noise.chance(x, 3, z, s + 8, 0.15) ? SpireKit.CALCITE : SpireKit.SMOOTH;
        if (d <= INNER) {
            double n = Noise.hash01(x, 4, z, s + 9);
            return n < 0.62 ? SpireKit.SMOOTH : n < 0.86 ? SpireKit.QUARTZ_BRICKS : SpireKit.CALCITE;
        }
        double wear = (d - INNER) / (RADIUS - INNER) * 0.7 + (Noise.patch2(x, z, 3, s + 10) - 0.5) * 0.8;
        double n = Noise.hash01(x, 5, z, s + 11);
        if (wear > 0.55 && n < 0.35) return n < 0.12 ? "minecraft:mossy_stone_bricks" : n < 0.24 ? "minecraft:cracked_stone_bricks" : "minecraft:tuff";
        if (wear > 0.25 && n < 0.3) return "minecraft:diorite";
        return n < 0.55 ? SpireKit.CALCITE : n < 0.8 ? SpireKit.QUARTZ_BRICKS : SpireKit.SMOOTH;
    }

    /**
     * Below the platform: the pedestal under the last floor (footing stone down to the mountain, a chiseled band at its head)
     * and, under the hanging ring, the void and the twelve ribs that carry it, deep at the pedestal and thinning to the rim.
     */
    private static void undercroft(Canvas c, int s) {
        Brush foot = SpireKit.footing(s + 12);
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = dist(x, z);
                if (d > RADIUS + 0.5) continue;
                if (d <= INNER + 0.5) {
                    c.set(x, -4, z, d > INNER - 0.8 ? SpireKit.CHISELED : SpireKit.CALCITE);
                    c.set(x, -5, z, foot.at(x, -5, z));
                } else {
                    for (int y = -4; y >= -VOID_BELOW; y--) c.carve(x, y, z);
                }
            }
        }
        double turn = Noise.hash01(1, 2, 3, s) * Math.PI * 2;
        for (int k = 0; k < 12; k++) {
            double a = turn + k * Math.PI / 6;
            double cos = Math.cos(a), sin = Math.sin(a);
            for (double rho = INNER + 0.5; rho <= RADIUS - 0.5; rho += 0.5) {
                int x = (int) Math.round(cos * rho), z = (int) Math.round(sin * rho);
                if (lost(x, z, s) && rho > RADIUS - 2) break;
                int depth = (int) Math.round(Math.pow((RADIUS - rho) / (RADIUS - INNER), 1.6) * 9);
                Dir out = Dir.toward((int) Math.round(cos * 2), (int) Math.round(sin * 2));
                for (int y = -4; y >= -4 - depth; y--) {
                    boolean bottom = y == -4 - depth;
                    c.set(x, y, z, bottom && depth > 0 ? SpireKit.TRIM.stairs(out, true) : y == -4 ? SpireKit.CHISELED : SpireKit.QUARTZ_BRICKS);
                }
            }
        }
    }

    // --- altar and bells ---------------------------------------------------------------------------------------------------

    private static void dais(Canvas c, long seed) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) c.set(x, -1, z, SpireKit.GOLD);
        for (Dir d : Dir.HORIZONTAL) {
            for (int k = -1; k <= 1; k++) {
                int x = d.dx * 2 + (d.dx == 0 ? k : 0), z = d.dz * 2 + (d.dz == 0 ? k : 0);
                c.set(x, -1, z, k == 0 ? SpireKit.CHISELED : SpireKit.SMOOTH);
            }
        }
        c.set(0, 0, 0, SpireKit.ALTAR);
        for (int i = 0; i < Melody.NOTES; i++) {
            int[] b = bell(seed, i);
            c.set(b[0], -1, b[1], SpireKit.CHISELED);
            c.set(b[0], 0, b[1], SpireKit.noteGlass(i));
            c.set(b[0], 1, b[1], SpireKit.bell(i));
        }
    }

    // --- pillars -------------------------------------------------------------------------------------------------------------

    /** A 2 × 2 quartz pillar with a flared base and a capital, its lower corner at (bx, bz). */
    static void pillar(Canvas c, int bx, int bz, int height, boolean capped, boolean rod) {
        for (int ox = 0; ox <= 1; ox++) {
            for (int oz = 0; oz <= 1; oz++) {
                c.set(bx + ox, 0, bz + oz, SpireKit.QUARTZ);
                for (int y = 1; y < height; y++) c.set(bx + ox, y, bz + oz, SpireKit.pillar("y"));
                if (capped) c.set(bx + ox, height, bz + oz, SpireKit.CHISELED);
            }
        }
        // Stairs flaring at the foot (and under the capital) on the four faces.
        for (Dir d : Dir.HORIZONTAL) {
            for (int k = 0; k <= 1; k++) {
                int x = bx + (d.dx > 0 ? 2 : d.dx < 0 ? -1 : k), z = bz + (d.dz > 0 ? 2 : d.dz < 0 ? -1 : k);
                if (!c.has(x, 0, z) || c.isAir(x, 0, z)) c.set(x, 0, z, SpireKit.TRIM.stairs(d.opposite(), false));
                if (capped && (!c.has(x, height, z) || c.isAir(x, height, z))) c.set(x, height, z, SpireKit.QUARTZ_TRIM.stairs(d.opposite(), true));
            }
        }
        if (capped && rod) c.set(bx, height + 1, bz, St.endRod(Dir.UP));
    }

    private static void innerPillars(Canvas c, long seed, int s) {
        double turn = turn(seed);
        for (int k = 0; k < 6; k++) {
            double a = turn + Math.PI / 6 + k * Math.PI / 3;
            int h = 6 + Noise.pick(k, 1, 0, s + 20, 3);
            pillar(c, (int) Math.floor(Math.cos(a) * INNER_PILLARS), (int) Math.floor(Math.sin(a) * INNER_PILLARS), h, true, true);
        }
    }

    /** The outer arcade: twelve pillars, some whole and joined by arches, some snapped short, some fallen across the floor. */
    private static void arcade(Canvas c, long seed, int s) {
        double turn = turn(seed);
        int[] height = new int[12];
        int[][] at = new int[12][];
        for (int k = 0; k < 12; k++) {
            double a = turn + k * Math.PI / 6;
            at[k] = new int[]{(int) Math.floor(Math.cos(a) * OUTER_PILLARS), (int) Math.floor(Math.sin(a) * OUTER_PILLARS)};
            double roll = Noise.hash01(k, 2, 0, s + 21);
            if (roll < 0.2) {
                height[k] = 0;
                fallen(c, at[k][0], at[k][1], a + (roll < 0.12 ? Math.PI / 2 : -Math.PI / 2), 4 + Noise.pick(k, 3, 0, s + 22, 3), s);
            } else {
                height[k] = roll > 0.42 ? 8 : 3 + (int) (roll * 6);
                pillar(c, at[k][0], at[k][1], height[k], height[k] == 8, false);
                if (height[k] < 8) rubbleAround(c, at[k][0], at[k][1], s + k);
            }
        }
        // An arch springs between two whole neighbours: a lintel over the chord and stairs easing into each capital.
        for (int k = 0; k < 12; k++) {
            int n = (k + 1) % 12;
            if (height[k] != 8 || height[n] != 8) continue;
            int x0 = at[k][0], z0 = at[k][1], x1 = at[n][0], z1 = at[n][1];
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
            if (Noise.chance(k, 4, 0, s + 23, 0.3)) {
                // This one fell: its lintel lies broken on the floor below the gap, nearer the middle.
                for (int i = 2; i < steps - 1; i++) {
                    int x = x0 + Math.round((x1 - x0) * i / (float) steps), z = z0 + Math.round((z1 - z0) * i / (float) steps);
                    int fx = x - Integer.signum(x) * (i % 3 == 0 ? 2 : 1), fz = z - Integer.signum(z) * (i % 3 == 0 ? 2 : 1);
                    if (!c.isAir(fx, 0, fz) || dist(fx, fz) > RADIUS - 2) continue;
                    c.set(fx, 0, fz, i == 2 || i == steps - 2 ? SpireKit.QUARTZ_TRIM.stairs(Dir.toward(x1 - x0, z1 - z0), false) : SpireKit.SMOOTH);
                }
                continue;
            }
            for (int i = 1; i < steps; i++) {
                int x = x0 + Math.round((x1 - x0) * i / (float) steps), z = z0 + Math.round((z1 - z0) * i / (float) steps);
                for (int ox = 0; ox <= 1; ox++) {
                    for (int oz = 0; oz <= 1; oz++) {
                        int cx = x + ox, cz = z + oz;
                        c.set(cx, 9, cz, Noise.chance(cx, 9, cz, s + 24, 0.2) ? SpireKit.CALCITE : SpireKit.SMOOTH);
                        if (c.get(cx, 8, cz) != null && !c.isAir(cx, 8, cz)) continue;
                        boolean end = i <= 1 || i >= steps - 1;
                        if (end) c.set(cx, 8, cz, SpireKit.QUARTZ_TRIM.stairs(Dir.toward(i <= 1 ? x0 - x : x1 - x, i <= 1 ? z0 - z : z1 - z), true));
                    }
                }
            }
        }
    }

    /** A pillar lying where it fell, broken in two, its capital rolled off the end. */
    private static void fallen(Canvas c, int x, int z, double along, int length, int s) {
        double dx = Math.cos(along), dz = Math.sin(along);
        String axis = Math.abs(dx) > Math.abs(dz) ? "x" : "z";
        c.set(x, 0, z, SpireKit.QUARTZ);
        c.set(x + 1, 0, z, SpireKit.TRIM.slabBottom());
        for (int i = 1; i <= length; i++) {
            int px = (int) Math.round(x + dx * (i + (i > length / 2 ? 1 : 0))), pz = (int) Math.round(z + dz * (i + (i > length / 2 ? 1 : 0)));
            if (dist(px, pz) > RADIUS - 1 || dist(px, pz) < INNER + 1) break;
            c.set(px, 0, pz, SpireKit.pillar(axis));
            if (i == length) {
                int ex = (int) Math.round(px + dx), ez = (int) Math.round(pz + dz);
                if (dist(ex, ez) <= RADIUS - 1) c.set(ex, 0, ez, SpireKit.CHISELED);
            }
        }
        rubbleAround(c, x, z, s + x * 31 + z);
    }

    /** Broken stone round a pillar's foot. */
    private static void rubbleAround(Canvas c, int x, int z, int s) {
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 3; dz++) {
                int px = x + dx, pz = z + dz;
                if (!c.isAir(px, 0, pz) || !Kinds.fullCube(c.world(px, -1, pz)) || !Noise.chance(px, 0, pz, s + 25, 0.22)) continue;
                double n = Noise.hash01(px, 1, pz, s + 26);
                c.set(px, 0, pz, n < 0.4 ? SpireKit.TRIM.slabBottom() : n < 0.6 ? "minecraft:calcite"
                        : n < 0.8 ? SpireKit.QUARTZ_TRIM.stairs(Dir.HORIZONTAL[Noise.pick(px, 2, pz, s, 4)], false) : St.mossCarpet());
            }
        }
    }

    // --- the rim ------------------------------------------------------------------------------------------------------------

    /** A balustrade of wall posts round the rim, broken where the rim is and wherever it has fallen; lanterns on a few posts. */
    private static void rim(Canvas c, int s) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = dist(x, z);
                if (d <= RADIUS - 0.5 || d > RADIUS + 0.5 || lost(x, z, s) || c.isAir(x, -1, z)) continue;
                double gap = Noise.patch2(x, z, 2.5, s + 30);
                if (gap < 0.3) continue;
                c.set(x, 0, z, SpireKit.baluster(x, 0, z, s + 31));
                if (gap > 0.85 && Noise.chance(x, 1, z, s + 32, 0.35)) c.set(x, 1, z, St.lantern(false));
            }
        }
    }

    private static void weather(Canvas c, int s) {
        // Moss creeping in over the outer ring, the odd tuft of grass in the cracks.
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = dist(x, z);
                if (d <= INNER + 1 || d > RADIUS || !c.isAir(x, 0, z) || c.isAir(x, -1, z)) continue;
                String below = c.get(x, -1, z);
                if (below == null || !Kinds.fullCube(below)) continue;
                double n = Noise.patch2(x, z, 3, s + 40) + (d - INNER) / RADIUS * 0.25;
                if (n > 0.78 && Noise.chance(x, 0, z, s + 41, 0.7)) c.set(x, 0, z, St.mossCarpet());
            }
        }
        SpireKit.ivy(c, new Box(-RADIUS - 1, -12, -RADIUS - 1, RADIUS + 1, -4, RADIUS + 1), 0.08, s + 42);
    }
}
