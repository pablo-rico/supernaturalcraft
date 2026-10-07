package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import static org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaKind.*;

/**
 * Chapter one, Eden: a golden heaven. A garden of moss and white flowers cut by quartz walks with gold inlay,
 * a ring path round the tree of knowledge (oak and flowering azalea, hung with glowing golden apples), six
 * reflecting pools, shafts of light and a white colonnade at the rim. The Author stands under the tree. Pure.
 */
public final class EdenLayout {

    private static final int SALT = 0xEDE;

    private EdenLayout() {
    }

    /** Radius of the ring path round the tree. */
    public static int ringRadius(int r) {
        return Math.max(6, Math.round(r * 0.24f));
    }

    public static ArenaPlan plan(int r, long seed) {
        ArenaPlan.Builder b = new ArenaPlan.Builder(r);
        int ring = ringRadius(r);
        int pools = 6;
        double poolD = r * 0.52, poolR = Math.max(2.0, Math.min(3.6, r * 0.105));
        int paths = 6;
        // --- the floor -----------------------------------------------------------------------------------------
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!b.inside(dx, dz)) continue;
                double d = Math.sqrt(dx * dx + dz * dz), a = ArenaShapes.angle(dx, dz);
                ArenaKind floor;
                if (d <= ring - 1.5) {
                    floor = MOSS;
                } else if (d <= ring + 0.5) {
                    floor = onSpoke(a, 8, ring) ? GOLD : QUARTZ_BRICKS;
                } else if (d > r - 2.5) {
                    floor = QUARTZ_BRICKS;
                } else if (pathOffset(dx, dz, paths) <= 1.0) {
                    floor = pathOffset(dx, dz, paths) <= 0.5 && Math.round(d) % 5 == 0 ? GOLD : QUARTZ;
                } else {
                    floor = ArenaNoise.smooth(seed, SALT, dx, dz, 6) < 0.42 ? GRASS : MOSS;
                }
                b.set(dx, 0, dz, floor);
            }
        }
        // --- reflecting pools between the walks ----------------------------------------------------------------
        for (int k = 0; k < pools; k++) {
            double a = Math.PI * 2 * k / pools;
            double px = Math.cos(a) * poolD, pz = Math.sin(a) * poolD;
            int span = (int) Math.ceil(poolR + 1);
            for (int dx = (int) Math.floor(px) - span; dx <= (int) Math.ceil(px) + span; dx++) {
                for (int dz = (int) Math.floor(pz) - span; dz <= (int) Math.ceil(pz) + span; dz++) {
                    double dd = Math.hypot(dx - px, dz - pz);
                    if (dd <= poolR) {
                        b.set(dx, 0, dz, WATER);
                        b.set(dx, -1, dz, QUARTZ);
                    } else if (dd <= poolR + 1.0) {
                        b.set(dx, 0, dz, QUARTZ_BRICKS);
                    }
                }
            }
        }
        // --- the garden's flowers and bushes -------------------------------------------------------------------
        ArenaKind[] flowers = {WHITE_TULIP, LILY, DAISY, BLUET, LILY, WHITE_TULIP};
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                ArenaKind floor = b.get(dx, 0, dz);
                if (floor != MOSS && floor != GRASS) continue;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < 4.5) continue;
                double roll = ArenaNoise.rand(seed, SALT + 1, dx, dz);
                if (roll < 0.035) {
                    b.set(dx, 1, dz, AZALEA);
                } else if (roll < 0.05 && d > ring + 2) {
                    b.set(dx, 1, dz, FLOWERING_LEAVES);
                    if (roll < 0.042) b.set(dx, 2, dz, FLOWERING_LEAVES);
                } else if (roll < 0.30) {
                    b.set(dx, 1, dz, flowers[(int) (ArenaNoise.rand(seed, SALT + 2, dx, dz) * flowers.length)]);
                }
            }
        }
        tree(b, seed, r);
        // --- shafts of light between the pools ----------------------------------------------------------------
        int shafts = 6;
        double shaftD = r * 0.74;
        for (int k = 0; k < shafts; k++) {
            double a = Math.PI * 2 * (k + 0.5) / shafts;
            int x = (int) Math.round(Math.cos(a) * shaftD), z = (int) Math.round(Math.sin(a) * shaftD);
            // Off the walk, which runs along this very angle.
            int ox = (int) Math.round(-Math.sin(a) * 3), oz = (int) Math.round(Math.cos(a) * 3);
            for (int side : new int[]{-1, 1}) {
                int sx = x + ox * side, sz = z + oz * side;
                b.set(sx, 0, sz, CHISELED_QUARTZ);
                b.set(sx, 1, sz, CHISELED_QUARTZ);
                b.column(sx, sz, 2, 9 + (k % 3) * 2, LIGHT_SHAFT);
            }
        }
        // --- the colonnade at the rim ----------------------------------------------------------------------------
        double colD = r - 1.5;
        int columns = Math.max(12, (int) Math.round(2 * Math.PI * colD / 7));
        for (int k = 0; k < columns; k++) {
            double a = Math.PI * 2 * (k + 0.25) / columns;
            int x = (int) Math.round(Math.cos(a) * colD), z = (int) Math.round(Math.sin(a) * colD);
            int h = 5 + (int) (ArenaNoise.rand(seed, SALT + 3, k, 0) * 3);
            b.set(x, 1, z, CHISELED_QUARTZ);
            b.column(x, z, 2, h, QUARTZ_PILLAR);
            b.set(x, h + 1, z, CHISELED_QUARTZ);
            if (k % 3 == 0) b.set(x, h + 2, z, GOLD);
        }
        return b.build(0, 1, 3);
    }

    /** The tree of knowledge at the centre: a flared oak trunk, four boughs, and a crown hung with golden apples. */
    private static void tree(ArenaPlan.Builder b, long seed, int r) {
        int top = 9;
        b.column(0, 0, 1, top + 1, OAK_LOG);
        int[][] roots = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 0; i < roots.length; i++) {
            b.set(roots[i][0], 1, roots[i][1], OAK_WOOD);
            if (i % 2 == 0) b.set(roots[i][0], 2, roots[i][1], OAK_WOOD);
        }
        int[][] boughs = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int[] d : boughs) {
            for (int i = 1; i <= 3; i++) b.set(d[0] * i, 6 + i, d[1] * i, OAK_WOOD);
        }
        double rx = Math.min(6.5, Math.max(4.0, r * 0.19)), ry = 3.6, cy = 10.5;
        int span = (int) Math.ceil(rx) + 1;
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                for (int y = (int) Math.floor(cy - ry) - 1; y <= (int) Math.ceil(cy + ry) + 1; y++) {
                    double e = (dx * dx + dz * dz) / (rx * rx) + (y - cy) * (y - cy) / (ry * ry);
                    double jag = (ArenaNoise.rand(seed, SALT + 4, dx * 31 + y, dz) - 0.5) * 0.35;
                    if (e > 1 + jag || b.get(dx, y, dz) != null) continue;
                    double roll = ArenaNoise.rand(seed, SALT + 5, dx * 17 + y, dz);
                    b.set(dx, y, dz, roll < 0.42 ? AZALEA_LEAVES : roll < 0.75 ? FLOWERING_LEAVES : OAK_LEAVES);
                }
            }
        }
        // Golden apples hang from the underside of the crown.
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                for (int y = 4; y <= 14; y++) {
                    ArenaKind k = b.get(dx, y, dz);
                    if (k != AZALEA_LEAVES && k != FLOWERING_LEAVES && k != OAK_LEAVES) continue;
                    if (b.get(dx, y - 1, dz) != null) continue;
                    if (ArenaNoise.rand(seed, SALT + 6, dx * 13 + y, dz) < 0.22) b.set(dx, y, dz, GOLDEN_APPLE);
                    break;
                }
            }
        }
    }

    /** True on the gold inlay cells of a ring: {@code n} spokes round it. */
    private static boolean onSpoke(double a, int n, int radius) {
        double step = Math.PI * 2 / n;
        double off = ArenaShapes.angleDiff(a, Math.round(a / step) * step);
        return off * radius <= 0.6;
    }

    /** Distance from a column to the nearest of {@code n} straight walks running out from the centre. */
    private static double pathOffset(int dx, int dz, int n) {
        double best = Double.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            double a = Math.PI * 2 * (k + 0.5) / n;
            double ux = Math.cos(a), uz = Math.sin(a);
            double along = dx * ux + dz * uz;
            if (along < 0) continue;
            best = Math.min(best, Math.abs(-dx * uz + dz * ux));
        }
        return best;
    }
}
