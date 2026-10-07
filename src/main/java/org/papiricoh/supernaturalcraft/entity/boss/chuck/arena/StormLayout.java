package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaKind.*;

/**
 * Chapter three, the Chorus's storm: islands of cloud over a drop, a white temple at the centre (a ring of broken
 * columns round the Author's plinth), cloud bridges, more broken columns and their fallen drums on the outer islands,
 * and great bells of gold hung from quartz arches (raw gold, since a real bell is a block entity). The gaps are carved
 * deep enough that whoever falls is caught by the arena's rescue; a sea of cloud lies at their bottom. Pure.
 */
public final class StormLayout {

    private static final int SALT = 0x5707;
    /** How deep the gaps are carved: the arena's rescue catches anyone who falls past {@code dy = -6}. */
    public static final int PIT_DEPTH = 7;

    private StormLayout() {
    }

    /**
     * An island of cloud: the seed point of its cell, its size (half the distance to the nearest other seed) and
     * whether it has fallen away entirely (a wide hole in the floor).
     */
    public record Island(double x, double z, double radius, boolean hole) {
    }

    public static int coreRadius(int r) {
        return Math.max(5, Math.round(r * 0.22f));
    }

    /** Typical spacing between the islands' seeds. */
    private static final double SPACING = 9.5;
    /** How much bigger the temple's cell is than a cloud's. */
    private static final double CORE_BONUS = 5.0;

    /**
     * The islands: the floor is cracked into cells round seed points scattered by dart-throwing (the first is the temple
     * at the centre); one cloud in eight has fallen away whole.
     */
    public static List<Island> islands(int r, long seed) {
        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{0, 0});
        for (int tries = 0; tries < 3000; tries++) {
            double a = ArenaNoise.rand(seed, SALT + 10, tries, 0) * Math.PI * 2;
            double d = Math.sqrt(ArenaNoise.rand(seed, SALT + 10, tries, 1)) * (r + 2);
            double x = Math.cos(a) * d, z = Math.sin(a) * d;
            if (d < coreRadius(r) + SPACING * 0.6) continue;
            boolean fits = true;
            for (double[] o : pts) {
                if (Math.hypot(x - o[0], z - o[1]) < SPACING) {
                    fits = false;
                    break;
                }
            }
            if (fits) pts.add(new double[]{x, z});
        }
        List<Island> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            double nearest = Double.MAX_VALUE;
            for (int j = 0; j < pts.size(); j++) {
                if (j != i) nearest = Math.min(nearest, Math.hypot(pts.get(i)[0] - pts.get(j)[0], pts.get(i)[1] - pts.get(j)[1]));
            }
            boolean hole = i > 0 && ArenaNoise.rand(seed, SALT + 12, i, 0) < 0.125
                    && Math.hypot(pts.get(i)[0], pts.get(i)[1]) < r - 3;
            out.add(new Island(pts.get(i)[0], pts.get(i)[1], i == 0 ? coreRadius(r) : nearest / 2, hole));
        }
        return out;
    }

    /** Whether a column holds cloud rather than a gap: inside a cell that has not fallen, away from the cracks. */
    public static boolean solid(List<Island> islands, long seed, int dx, int dz) {
        double d1 = Double.MAX_VALUE, d2 = Double.MAX_VALUE;
        int nearest = -1;
        for (int i = 0; i < islands.size(); i++) {
            Island is = islands.get(i);
            double d = Math.hypot(dx - is.x(), dz - is.z()) - (i == 0 ? CORE_BONUS : 0);
            if (d < d1) {
                d2 = d1;
                d1 = d;
                nearest = i;
            } else if (d < d2) {
                d2 = d;
            }
        }
        if (nearest == 0) return d2 - d1 > 3.0;
        if (islands.get(nearest).hole()) return false;
        double crack = 1.9 + (ArenaNoise.smooth(seed, SALT, dx, dz, 3.0) - 0.5) * 1.2;
        return d2 - d1 > crack;
    }

    public static ArenaPlan plan(int r, long seed) {
        ArenaPlan.Builder b = new ArenaPlan.Builder(r);
        int core = coreRadius(r);
        List<Island> islands = islands(r, seed);
        boolean[][] solid = new boolean[2 * r + 3][2 * r + 3];
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) solid[dx + r + 1][dz + r + 1] = dx * dx + dz * dz <= r * r && solid(islands, seed, dx, dz);
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!b.inside(dx, dz)) continue;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (solid[dx + r + 1][dz + r + 1]) {
                    ArenaKind top;
                    if (d <= core - 1.5) {
                        top = d <= 1.5 ? CHISELED_QUARTZ : Math.abs(d - (core - 3)) <= 0.5 ? QUARTZ_BRICKS : CALCITE;
                    } else {
                        top = ArenaNoise.smooth(seed, SALT + 1, dx, dz, 4) < 0.3 ? SNOW : CLOUD;
                    }
                    b.set(dx, 0, dz, top);
                    boolean edge = !solid[dx + r][dz + r + 1] || !solid[dx + r + 2][dz + r + 1]
                            || !solid[dx + r + 1][dz + r] || !solid[dx + r + 1][dz + r + 2];
                    if (edge) {
                        // The island's flank, so the gaps show cloud rather than the ground beneath.
                        for (int y = -1; y >= -(PIT_DEPTH - 1); y--) b.set(dx, y, dz, y >= -2 ? CLOUD : CLOUD_SHADE);
                    }
                } else {
                    for (int y = 0; y >= -(PIT_DEPTH - 1); y--) b.set(dx, y, dz, AIR);
                    b.set(dx, -PIT_DEPTH, dz, ArenaNoise.smooth(seed, SALT + 2, dx, dz, 3) < 0.5 ? CLOUD : CLOUD_SHADE);
                }
            }
        }
        // --- the temple: the Author's plinth and a ring of broken columns ------------------------------------
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                b.set(dx, 1, dz, CALCITE);
                b.set(dx, 2, dz, dx == 0 && dz == 0 ? SEA_LANTERN : QUARTZ_BRICKS);
            }
        }
        double colD = core - 1.5;
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * (k + 0.5) / 8;
            int x = (int) Math.round(Math.cos(a) * colD), z = (int) Math.round(Math.sin(a) * colD);
            int h = 4 + (int) (ArenaNoise.rand(seed, SALT + 3, k, 0) * 7);
            column(b, x, z, h, h >= 9);
        }
        // --- bells hung from arches on three of the bigger islands, well apart -------------------------------
        List<Integer> bells = bellIslands(islands, r);
        for (int i : bells) {
            Island is = islands.get(i);
            double a = Math.atan2(is.z(), is.x());
            bell(b, (int) Math.round(is.x()), (int) Math.round(is.z()), -Math.sin(a), Math.cos(a));
        }
        // --- columns and fallen drums on the other islands ----------------------------------------------------
        for (int i = 1; i < islands.size(); i++) {
            Island is = islands.get(i);
            if (bells.contains(i) || is.hole() || !solid(islands, seed, (int) Math.round(is.x()), (int) Math.round(is.z()))) continue;
            double roll = ArenaNoise.rand(seed, SALT + 4, i, 0);
            int x = (int) Math.round(is.x()), z = (int) Math.round(is.z());
            if (roll < 0.5) {
                column(b, x, z, 3 + (int) (ArenaNoise.rand(seed, SALT + 4, i, 1) * 8), roll < 0.18);
            }
            if (roll > 0.4 && is.radius() >= 3.2) {
                boolean alongX = ArenaNoise.rand(seed, SALT + 4, i, 2) < 0.5;
                int ox = x + (alongX ? -1 : 2), oz = z + (alongX ? 2 : -1);
                for (int j = 0; j < 3; j++) {
                    int fx = ox + (alongX ? j : 0), fz = oz + (alongX ? 0 : j);
                    ArenaKind under = b.get(fx, 0, fz);
                    if (under != null && under != AIR && b.get(fx, 1, fz) == null) b.set(fx, 1, fz, alongX ? QUARTZ_PILLAR_X : QUARTZ_PILLAR_Z);
                }
            }
        }
        return b.build(0, 3, 0);
    }

    /** Up to three of the biggest islands between a third and four fifths of the way out, at least 90 degrees apart. */
    public static List<Integer> bellIslands(List<Island> islands, int r) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < islands.size(); i++) {
            double d = Math.hypot(islands.get(i).x(), islands.get(i).z());
            if (d >= r * 0.33 && d <= r * 0.8 && !islands.get(i).hole()) candidates.add(i);
        }
        candidates.sort((x, y) -> Double.compare(islands.get(y).radius(), islands.get(x).radius()));
        List<Integer> out = new ArrayList<>();
        for (int i : candidates) {
            double a = Math.atan2(islands.get(i).z(), islands.get(i).x());
            boolean apart = out.stream().allMatch(j -> ArenaShapes.angleDiff(a, Math.atan2(islands.get(j).z(), islands.get(j).x())) >= Math.PI / 2);
            if (apart && out.size() < 3) out.add(i);
        }
        return out;
    }

    /** A fluted column: a chiseled base, a pillar shaft, and (if whole) a capital with a light on it. */
    private static void column(ArenaPlan.Builder b, int x, int z, int h, boolean whole) {
        b.set(x, 1, z, CHISELED_QUARTZ);
        b.column(x, z, 2, h, QUARTZ_PILLAR);
        if (whole) {
            b.set(x, h + 1, z, CHISELED_QUARTZ);
            b.set(x, h + 2, z, SEA_LANTERN);
        } else {
            b.set(x, h + 1, z, QUARTZ_SLAB);
        }
    }

    /** An arch of two columns and a lintel along ({@code tx}, {@code tz}), and a bell of gold hanging from it. */
    private static void bell(ArenaPlan.Builder b, int x, int z, double tx, double tz) {
        int top = 11;
        int span = 4;
        int lx = (int) Math.round(tx * span), lz = (int) Math.round(tz * span);
        for (int side : new int[]{-1, 1}) {
            int px = x + lx * side, pz = z + lz * side;
            b.set(px, 1, pz, CHISELED_QUARTZ);
            b.column(px, pz, 2, top - 1, QUARTZ_PILLAR);
        }
        for (int i = -span; i <= span; i++) {
            b.set(x + (int) Math.round(tx * i), top, z + (int) Math.round(tz * i), i == 0 ? CHISELED_QUARTZ : QUARTZ_BRICKS);
        }
        b.set(x, top - 1, z, CHAIN);
        // The bell: a crown, a waist, a flared rim; hollow, with a lantern for its clapper.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 1.0) b.set(x + dx, 9, z + dz, BELL);
                if (d <= 1.5) b.set(x + dx, 8, z + dz, BELL);
                if (d > 0.5 && d <= 2.0) b.set(x + dx, 7, z + dz, BELL);
                if (d > 1.5 && d <= 2.6) b.set(x + dx, 6, z + dz, GOLD);
            }
        }
        b.set(x, 7, z, HANGING_LANTERN);
    }
}
