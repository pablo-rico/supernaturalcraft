package org.papiricoh.supernaturalcraft.structure.spire;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stair up the Hymnal Spire (pure): drawn along the planned path in the altar's coordinates (origin = the Choir Altar, so
 * the summit platform's top is {@code y = -1}). Each node is a feet cell; {@code ground*} hold the mountain's top (same
 * coordinates) sampled beside the node (in: toward the peak, mid: under it, out: away from it) when the spire was planned.
 *
 * <ul>
 *   <li>A walkway three wide paved in white with a darker kerb, a sub-floor of footing stone that runs down to the rock; every
 *   rise of a block is a stair, edged where it turns.</li>
 *   <li>Over a drop on either side a balustrade; against a bank higher than the stair a retaining wall with pilasters;
 *   through a cutting, arches across the stair now and then.</li>
 *   <li>Where it crosses a hollow, a bridge: piers to the rock every few nodes, arches springing from them, a flat soffit
 *   between.</li>
 *   <li>A landing every so often on the outer side (a bench looking out, a lamp), lanterns along the way.</li>
 *   <li>At the top a ruined gate onto the platform; just outside the fight's reach a wayside shrine holding
 *   {@link SpireKit#LOOT_SUMMIT}.</li>
 * </ul>
 */
public final class AscentLayout {

    /** The summit chest stays beyond this distance from the altar (outside the Chorus arena, which keeps no block entities). */
    public static final int SHRINE_MIN_DISTANCE = 29;
    static final int LANDING_EVERY = 16, LANTERN_EVERY = 7, PIER_EVERY = 5;

    /** The planned stair, in the altar's coordinates. */
    public record Input(List<int[]> nodes, int[] groundIn, int[] groundMid, int[] groundOut, long seed) {
    }

    private AscentLayout() {
    }

    /** One walkway column: its feet level and the node that owns it. */
    private record Col(int feet, int node, double w) {
    }

    static long col(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    static int cx(long k) {
        return (int) (k >> 32);
    }

    static int cz(long k) {
        return (int) k;
    }

    /** Tangent (unit, downhill along the stair) and outward (unit, away from the peak) at node i. */
    static double[] frame(List<int[]> n, int i) {
        int[] a = n.get(Math.max(0, i - 1)), b = n.get(Math.min(n.size() - 1, i + 1));
        double tx = b[0] - a[0], tz = b[2] - a[2], len = Math.max(1e-6, Math.hypot(tx, tz));
        tx /= len;
        tz /= len;
        double ox = -tz, oz = tx;
        int[] p = n.get(i);
        if (p[0] * ox + p[2] * oz < 0) {
            ox = -ox;
            oz = -oz;
        }
        return new double[]{tx, tz, ox, oz};
    }

    /** The node whose outer side holds the wayside shrine (the first far enough from the altar), or -1. */
    public static int shrineNode(List<int[]> n) {
        for (int i = 2; i < n.size() - 2; i++) {
            int[] p = n.get(i);
            if (Math.hypot(p[0], p[2]) >= SHRINE_MIN_DISTANCE - 2) return i;
        }
        return -1;
    }

    public static Canvas canvas(Input in) {
        SpireKit.init();
        int s = (int) (in.seed() ^ in.seed() >>> 32) + 505;
        Canvas c = new Canvas("spire_ascent");
        List<int[]> n = in.nodes();
        Map<Long, Col> walk = raster(n);
        int shrine = shrineNode(n);
        Set<Integer> landings = landings(in, shrine);
        Set<Integer> bridges = new HashSet<>();
        for (int i = 0; i < n.size(); i++) if (n.get(i)[1] - 1 - in.groundMid()[i] >= 4) bridges.add(i);

        gate(c, n, walk, s);
        paving(c, in, walk, bridges, s);
        sides(c, in, walk, landings, bridges, shrine, s);
        for (int i : landings) landing(c, in, i, walk, s);
        if (shrine >= 0) shrine(c, in, shrine, s);
        stairs(c, n, walk);
        c.anchor("top", n.get(0)[0], n.get(0)[1], n.get(0)[2]);
        int[] last = n.get(n.size() - 1);
        // Where one stands at the foot (on the step, when the last cell is one).
        c.anchor("bottom", last[0], c.isAir(last[0], last[1], last[2]) ? last[1] : last[1] + 1, last[2]);
        return c.finish();
    }

    // --- the walkway --------------------------------------------------------------------------------------------------------

    /** Which columns the walkway covers and at what level (no neighbour more than a block apart). */
    private static Map<Long, Col> raster(List<int[]> n) {
        Map<Long, Col> walk = new HashMap<>();
        for (int i = 0; i < n.size(); i++) {
            int[] p = n.get(i), q = n.get(Math.min(n.size() - 1, i + 1));
            double[] f = frame(n, i);
            for (double t = 0; t < (i + 1 < n.size() ? 1 : 0.01); t += 0.25) {
                double px = p[0] + (q[0] - p[0]) * t, pz = p[2] + (q[2] - p[2]) * t;
                for (double w = -1.3; w <= 1.31; w += 0.325) {
                    long k = col((int) Math.round(px + f[2] * w), (int) Math.round(pz + f[3] * w));
                    Col old = walk.get(k);
                    if (old == null || p[1] > old.feet() || p[1] == old.feet() && Math.abs(w) < Math.abs(old.w())) walk.put(k, new Col(p[1], i, w));
                }
            }
        }
        // Relax: a column never sits more than a block under a neighbour.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<Long, Col> e : walk.entrySet()) {
                int x = cx(e.getKey()), z = cz(e.getKey());
                for (Dir d : Dir.HORIZONTAL) {
                    Col o = walk.get(col(x + d.dx, z + d.dz));
                    if (o != null && o.feet() - 1 > e.getValue().feet()) {
                        e.setValue(new Col(o.feet() - 1, e.getValue().node(), e.getValue().w()));
                        changed = true;
                    }
                }
            }
        }
        return walk;
    }

    private static void paving(Canvas c, Input in, Map<Long, Col> walk, Set<Integer> bridges, int s) {
        Brush centre = Palette.patches2(s, 2.5, SpireKit.QUARTZ_BRICKS, 5, SpireKit.SMOOTH, 2, SpireKit.CALCITE, 2, "minecraft:mossy_stone_bricks", 1);
        Brush kerb = Palette.patches2(s + 1, 2, SpireKit.DIORITE, 4, SpireKit.CALCITE, 2, "minecraft:diorite", 1, "minecraft:mossy_cobblestone", 1);
        Brush foot = SpireKit.footing(s + 2);
        List<int[]> n = in.nodes();
        for (Map.Entry<Long, Col> e : walk.entrySet()) {
            int x = cx(e.getKey()), z = cz(e.getKey());
            Col k = e.getValue();
            int y = k.feet();
            for (int h = 0; h <= 3; h++) c.carve(x, y + h, z);
            if (k.node() < 0 || c.has(x, y - 1, z) && !c.isAir(x, y - 1, z)) continue;
            boolean mid = Math.abs(k.w()) < 0.7;
            c.set(x, y - 1, z, mid && k.node() % 8 == 0 ? SpireKit.GOLD : mid ? centre.at(x, y - 1, z) : kerb.at(x, y - 1, z));
            int i = k.node();
            if (!bridges.contains(i)) {
                c.set(x, y - 2, z, foot.at(x, y - 2, z));
                continue;
            }
            boolean pier = i % PIER_EVERY == 0 || !bridges.contains(i - 1) || !bridges.contains(i + 1);
            if (pier) {
                SpireKit.pier(c, x, z, y - 2, y - 3, foot);
            } else {
                // Between the piers a soffit, and next to them the arches spring.
                c.set(x, y - 2, z, SpireKit.QUARTZ_BRICKS);
                int toPier = (i + 1) % PIER_EVERY == 0 ? 1 : (i - 1) % PIER_EVERY == 0 ? -1 : 0;
                if (toPier != 0) {
                    double[] f = frame(n, i);
                    Dir d = Dir.toward((int) Math.round(f[0] * toPier * 2), (int) Math.round(f[1] * toPier * 2));
                    c.set(x, y - 3, z, SpireKit.QUARTZ_TRIM.stairs(d, true));
                } else {
                    c.set(x, y - 3, z, SpireKit.TRIM.slabTop());
                }
            }
        }
    }

    /** Every walkway column with a neighbour a block higher becomes a stair facing it. */
    private static void stairs(Canvas c, List<int[]> n, Map<Long, Col> walk) {
        for (Map.Entry<Long, Col> e : walk.entrySet()) {
            int x = cx(e.getKey()), z = cz(e.getKey());
            int y = e.getValue().feet();
            if (e.getValue().node() < 0) continue;
            double[] f = frame(n, e.getValue().node());
            Dir back = Dir.toward((int) Math.round(-f[0] * 2), (int) Math.round(-f[1] * 2));
            Dir up = null;
            Col along = walk.get(col(x + back.dx, z + back.dz));
            if (along != null && along.feet() == y + 1) up = back;
            for (Dir d : Dir.HORIZONTAL) {
                if (up != null) break;
                Col o = walk.get(col(x + d.dx, z + d.dz));
                if (o != null && o.feet() == y + 1) up = d;
            }
            if (up != null) c.set(x, y, z, St.stairs("minecraft:smooth_quartz_stairs", up, false));
        }
    }

    // --- its sides ----------------------------------------------------------------------------------------------------------

    private static void sides(Canvas c, Input in, Map<Long, Col> walk, Set<Integer> landings, Set<Integer> bridges, int shrine, int s) {
        List<int[]> n = in.nodes();
        Brush stone = SpireKit.wall(s + 3, 0);
        Brush foot = SpireKit.footing(s + 4);
        for (int i = 1; i < n.size(); i++) {
            int[] p = n.get(i);
            double[] f = frame(n, i);
            int y = p[1];
            boolean tunnel = in.groundIn()[i] >= y + 3 && in.groundOut()[i] >= y + 3;
            for (int side : new int[]{-1, 1}) {
                if (side == 1 && (landings.contains(i) || Math.abs(i - shrine) <= 1)) continue;
                int g = side < 0 ? in.groundIn()[i] : in.groundOut()[i];
                for (double t = -0.5; t <= 0.5; t += 0.5) {
                    int x = (int) Math.round(p[0] + f[2] * 2.1 * side + f[0] * t * 2), z = (int) Math.round(p[2] + f[3] * 2.1 * side + f[1] * t * 2);
                    if (walk.containsKey(col(x, z))) continue;
                    int fy = nearestFeet(walk, x, z, y);
                    if (fy - 1 - g >= 2) {
                        // A drop: a kerb on footing, a balustrade on it.
                        c.set(x, fy - 1, z, SpireKit.DIORITE);
                        // Over a bridge's span the kerb hangs with the deck; elsewhere it is a wall down the slope.
                        boolean span = bridges.contains(i) && i % PIER_EVERY != 0 && bridges.contains(i - 1) && bridges.contains(i + 1);
                        c.set(x, fy - 2, z, span ? SpireKit.QUARTZ_BRICKS : foot.at(x, fy - 2, z));
                        if (span) c.set(x, fy - 3, z, SpireKit.TRIM.slabTop());
                        c.set(x, fy, z, SpireKit.baluster(x, fy, z, s + 5));
                    } else if (g >= fy + 1) {
                        // A bank above the stair: a retaining wall, coped, with a pilaster now and then.
                        int top = Math.min(g, fy + 2);
                        boolean pilaster = i % 6 == 0 && t == 0;
                        for (int yy = fy - 1; yy <= top; yy++) c.set(x, yy, z, pilaster ? (yy == top ? SpireKit.CHISELED : SpireKit.pillar("y")) : stone.at(x, yy, z));
                        if (top < fy + 3 && !tunnel) c.set(x, top + 1, z, Noise.chance(x, top, z, s + 6, 0.2) ? St.mossCarpet() : SpireKit.TRIM.slabBottom());
                    }
                }
            }
            if (tunnel && i % 5 == 0) arch(c, p, f);
            if (i % LANTERN_EVERY == 3 && !tunnel && !landings.contains(i)) lantern(c, in, i, f, walk, s);
        }
    }

    private static int nearestFeet(Map<Long, Col> walk, int x, int z, int fallback) {
        int best = fallback;
        for (Dir d : Dir.HORIZONTAL) {
            Col o = walk.get(col(x + d.dx, z + d.dz));
            if (o != null) return o.feet();
        }
        return best;
    }

    /** An arch across the stair in a cutting: pillars either side, a lintel with a gold keystone. */
    private static void arch(Canvas c, int[] p, double[] f) {
        int y = p[1];
        int[] a = {(int) Math.round(p[0] - f[2] * 2.1), (int) Math.round(p[2] - f[3] * 2.1)};
        int[] b = {(int) Math.round(p[0] + f[2] * 2.1), (int) Math.round(p[2] + f[3] * 2.1)};
        for (int[] q : new int[][]{a, b}) {
            c.set(q[0], y - 1, q[1], SpireKit.QUARTZ);
            for (int h = 0; h <= 3; h++) c.set(q[0], y + h, q[1], h == 3 ? SpireKit.CHISELED : SpireKit.pillar("y"));
        }
        for (double w = -2.1; w <= 2.11; w += 0.35) {
            int x = (int) Math.round(p[0] + f[2] * w), z = (int) Math.round(p[2] + f[3] * w);
            c.set(x, y + 4, z, Math.abs(w) < 0.4 ? SpireKit.GOLD : SpireKit.SMOOTH);
        }
    }

    /** A lantern: on the balustrade's post where there is one, else on a short lamp post on the inner side. */
    private static void lantern(Canvas c, Input in, int i, double[] f, Map<Long, Col> walk, int s) {
        int[] p = in.nodes().get(i);
        for (int side : new int[]{1, -1}) {
            int x = (int) Math.round(p[0] + f[2] * 2.1 * side), z = (int) Math.round(p[2] + f[3] * 2.1 * side);
            if (walk.containsKey(col(x, z))) continue;
            int fy = nearestFeet(walk, x, z, p[1]);
            String here = c.get(x, fy, z);
            if (here != null && here.contains("_wall")) {
                c.set(x, fy + 1, z, St.lantern(false));
                return;
            }
            if (side == -1 && c.isAir(x, fy, z) && !c.isAir(x, fy - 1, z)) {
                Lighting.lampPost(c, x, fy, z, 1, SpireKit.CHISELED, SpireKit.BALUSTER, null, false);
                return;
            }
        }
    }

    // --- landings, the gate and the shrine ---------------------------------------------------------------------------------

    /** Nodes where the stair runs level for a while: a landing on the outer side every {@link #LANDING_EVERY} nodes or so. */
    private static Set<Integer> landings(Input in, int shrine) {
        Set<Integer> out = new HashSet<>();
        List<int[]> n = in.nodes();
        int next = LANDING_EVERY / 2 + 4;
        for (int i = 3; i < n.size() - 3; i++) {
            if (i < next || Math.abs(i - shrine) < 6) continue;
            int y = n.get(i)[1];
            boolean level = n.get(i - 2)[1] == y && n.get(i - 1)[1] == y && n.get(i + 1)[1] == y;
            if (!level) continue;
            out.add(i);
            next = i + LANDING_EVERY;
        }
        return out;
    }

    /** A landing: a bay thrown out on the outer side, a bench looking out over the drop, a lamp, a balustrade. */
    private static void landing(Canvas c, Input in, int i, Map<Long, Col> walk, int s) {
        int[] p = in.nodes().get(i);
        double[] f = frame(in.nodes(), i);
        int y = p[1];
        Dir outward = Dir.toward((int) Math.round(f[2] * 2), (int) Math.round(f[3] * 2));
        Brush foot = SpireKit.footing(s + 7);
        Brush paving = SpireKit.floor(s + 8);
        for (double a = -1.6; a <= 1.61; a += 0.4) {
            for (double w = 1.6; w <= 4.6; w += 0.4) {
                int x = (int) Math.round(p[0] + f[0] * a * 1.2 + f[2] * w), z = (int) Math.round(p[2] + f[1] * a * 1.2 + f[3] * w);
                if (walk.containsKey(col(x, z))) continue;
                boolean edge = w > 4.1 || Math.abs(a) > 1.5;
                c.set(x, y - 1, z, edge ? SpireKit.DIORITE : paving.at(x, y - 1, z));
                c.set(x, y - 2, z, foot.at(x, y - 2, z));
                for (int h = 0; h <= 3; h++) c.carve(x, y + h, z);
                if (edge) c.set(x, y, z, SpireKit.baluster(x, y, z, s + 9));
            }
        }
        int bx = (int) Math.round(p[0] + f[2] * 3.4), bz = (int) Math.round(p[2] + f[3] * 3.4);
        Dir across = outward.cw();
        for (int k = -1; k <= 1; k++) {
            int x = bx + across.dx * k, z = bz + across.dz * k;
            if (c.isAir(x, y, z) && !walk.containsKey(col(x, z))) c.set(x, y, z, St.stairs("minecraft:smooth_quartz_stairs", outward.opposite(), false));
        }
        int lx = (int) Math.round(p[0] + f[0] * 1.9 + f[2] * 3.0), lz = (int) Math.round(p[2] + f[1] * 1.9 + f[3] * 3.0);
        if (!walk.containsKey(col(lx, lz))) Lighting.lampPost(c, lx, y, lz, 2, SpireKit.CHISELED, SpireKit.BALUSTER, null, false);
    }

    /** The ruined gate where the stair reaches the platform: two pillars, a broken lintel, the rim opened between them. */
    private static void gate(Canvas c, List<int[]> n, Map<Long, Col> walk, int s) {
        int[] p = n.get(0);
        double len = Math.max(1e-6, Math.hypot(p[0], p[2]));
        double rx = p[0] / len, rz = p[2] / len;
        // A short ramp in from the rim: platform level (the stair's first step is one below it).
        for (double r = SummitLayout.RADIUS - 2.5; r < len - 0.6; r += 0.4) {
            for (double w = -1.3; w <= 1.31; w += 0.325) {
                int x = (int) Math.round(rx * r - rz * w), z = (int) Math.round(rz * r + rx * w);
                // On the platform only the rim is opened (node -1: carve, no paving); past it, paving at platform level.
                if (!walk.containsKey(col(x, z))) walk.put(col(x, z), new Col(p[1] + 1, r <= SummitLayout.RADIUS ? -1 : 0, w));
            }
        }
        for (int side : new int[]{-1, 1}) {
            int x = (int) Math.round(p[0] - rz * 2.4 * side - rx * 1.2), z = (int) Math.round(p[2] + rx * 2.4 * side - rz * 1.2);
            int h = side < 0 ? 5 : 3 + Noise.pick(x, 0, z, s + 10, 2);
            c.set(x, p[1] - 1, z, SpireKit.QUARTZ);
            c.set(x, p[1] - 2, z, "minecraft:stone_bricks");
            for (int y = p[1]; y < p[1] + h; y++) c.set(x, y, z, SpireKit.pillar("y"));
            c.set(x, p[1] + h, z, SpireKit.CHISELED);
            if (side < 0) {
                // What is left of the lintel reaches half way across.
                for (double w = 0.4; w <= 2.2; w += 0.35) {
                    int lx = (int) Math.round(x - rz * w), lz = (int) Math.round(z + rx * w);
                    c.set(lx, p[1] + h, lz, w > 1.8 ? SpireKit.TRIM.slabBottom() : SpireKit.SMOOTH);
                }
            }
        }
    }

    /** The wayside shrine on the outer side at node {@code i}: a niche under a canopy, candles, the summit's chest. */
    private static void shrine(Canvas c, Input in, int i, int s) {
        int[] p = in.nodes().get(i);
        double[] f = frame(in.nodes(), i);
        Dir out = Dir.toward((int) Math.round(f[2] * 2), (int) Math.round(f[3] * 2));
        Dir across = out.cw();
        int y = p[1];
        int bx = (int) Math.round(p[0] + f[2] * 2.2), bz = (int) Math.round(p[2] + f[3] * 2.2);
        Brush foot = SpireKit.footing(s + 11);
        Brush stone = SpireKit.wall(s + 12, y);
        for (int k = -2; k <= 2; k++) {
            for (int d = 0; d <= 3; d++) {
                int x = bx + across.dx * k + out.dx * d, z = bz + across.dz * k + out.dz * d;
                boolean wall = d == 3 || Math.abs(k) == 2;
                c.set(x, y - 1, z, d == 0 ? SpireKit.DIORITE : Math.abs(k) <= 1 && d <= 2 ? SpireKit.CHISELED : SpireKit.QUARTZ_BRICKS);
                c.set(x, y - 2, z, foot.at(x, y - 2, z));
                for (int h = 0; h <= 4; h++) {
                    boolean roof = h == 4;
                    if (roof) c.set(x, y + h, z, d == 0 ? SpireKit.TRIM.slabBottom() : SpireKit.ROOF.block());
                    else if (wall && d > 0) c.set(x, y + h, z, h == 3 ? SpireKit.CHISELED : Math.abs(k) == 2 && d == 1 ? SpireKit.pillar("y") : stone.at(x, y + h, z));
                    else c.carve(x, y + h, z);
                }
            }
        }
        // The front posts carry the canopy; the niche in the back wall holds the chest under a gold panel.
        for (int k : new int[]{-2, 2}) {
            int x = bx + across.dx * k, z = bz + across.dz * k;
            for (int h = 0; h <= 3; h++) c.set(x, y + h, z, h == 3 ? SpireKit.CHISELED : SpireKit.pillar("y"));
        }
        int cx = bx + out.dx * 2, cz = bz + out.dz * 2;
        c.set(cx, y, cz, St.of("chest", "facing", out.opposite().id(), "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(cx, y, cz, SpireKit.LOOT_SUMMIT));
        c.set(cx + out.dx, y + 1, cz + out.dz, SpireKit.GOLD);
        for (int k : new int[]{-1, 1}) {
            int x = cx + across.dx * k, z = cz + across.dz * k;
            c.set(x, y, z, SpireKit.TRIM.slabTop());
            c.set(x, y + 1, z, St.candle(k < 0 ? "white" : "yellow", 2 + Noise.pick(x, 0, z, s + 13, 3), true));
        }
        Lighting.hanging(c, bx + out.dx, y + 4, bz + out.dz, 1, false);
        c.anchor("shrine", bx + out.dx, y, bz + out.dz);
    }

    /** The box round everything the ascent can draw for these nodes (footings not included). */
    public static Box reach(List<int[]> n) {
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (int[] p : n) {
            x0 = Math.min(x0, p[0]);
            x1 = Math.max(x1, p[0]);
            z0 = Math.min(z0, p[2]);
            z1 = Math.max(z1, p[2]);
            y0 = Math.min(y0, p[1]);
            y1 = Math.max(y1, p[1]);
        }
        return new Box(x0 - 7, y0 - 4, z0 - 7, x1 + 7, y1 + 7, z1 + 7);
    }
}
