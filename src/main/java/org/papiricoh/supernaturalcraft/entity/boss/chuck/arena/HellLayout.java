package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaKind.*;

/**
 * Chapter two, Hell and the Cage: a floor of blackstone, basalt and netherrack split by cracks of magma, a ring of
 * Cage bars (with six doorways, frame posts, a lintel band and hanging chains) round a raised dais where the Author
 * stands, basalt spurs for cover, and hellfire braziers. Pure.
 */
public final class HellLayout {

    private static final int SALT = 0x4E11;
    public static final int DAIS_RADIUS = 3, BAR_HEIGHT = 8, DOORWAYS = 6;

    private HellLayout() {
    }

    /** Radius of the ring of bars. */
    public static int cageRadius(int r) {
        return Math.max(8, Math.round(r * 0.5f));
    }

    public static ArenaPlan plan(int r, long seed) {
        ArenaPlan.Builder b = new ArenaPlan.Builder(r);
        // --- the floor -----------------------------------------------------------------------------------------
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!b.inside(dx, dz)) continue;
                double n = ArenaNoise.smooth(seed, SALT, dx, dz, 5.5);
                double fleck = ArenaNoise.rand(seed, SALT + 1, dx, dz);
                ArenaKind k = n < 0.36 ? BLACKSTONE : n < 0.62 ? BASALT : NETHERRACK;
                if (k == BLACKSTONE && fleck < 0.08) k = SMOOTH_BASALT;
                if (k == NETHERRACK && fleck < 0.05) k = MAGMA;
                b.set(dx, 0, dz, k);
            }
        }
        // --- cracks of the lake of fire, crawling out from the dais -----------------------------------------
        int cracks = 9;
        for (int i = 0; i < cracks; i++) {
            double angle = Math.PI * 2 * i / cracks + ArenaNoise.rand(seed, SALT + 2, i, 0) * 0.5;
            double x = Math.cos(angle) * (DAIS_RADIUS + 3), z = Math.sin(angle) * (DAIS_RADIUS + 3);
            for (int step = 0; step < r; step++) {
                angle += (ArenaNoise.rand(seed, SALT + 3, i, step) - 0.5) * 0.7;
                x += Math.cos(angle);
                z += Math.sin(angle);
                int cx = (int) Math.round(x), cz = (int) Math.round(z);
                if (cx * cx + cz * cz > (r - 2) * (r - 2)) break;
                b.set(cx, 0, cz, MAGMA);
                if (ArenaNoise.rand(seed, SALT + 4, i, step) < 0.3) b.set(cx + (step % 2), 0, cz + 1 - (step % 2), MAGMA);
            }
        }
        // --- the dais ------------------------------------------------------------------------------------------
        for (int dx = -DAIS_RADIUS - 2; dx <= DAIS_RADIUS + 2; dx++) {
            for (int dz = -DAIS_RADIUS - 2; dz <= DAIS_RADIUS + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= DAIS_RADIUS + 0.5) {
                    b.set(dx, 0, dz, BLACKSTONE_BRICKS);
                    b.set(dx, 1, dz, BLACKSTONE_BRICKS);
                    b.set(dx, 2, dz, d <= 0.5 ? CRYING_OBSIDIAN : d <= 1.5 ? GILDED_BLACKSTONE : POLISHED_BLACKSTONE);
                } else if (d <= DAIS_RADIUS + 1.6) {
                    b.set(dx, 0, dz, BLACKSTONE_BRICKS);
                    b.set(dx, 1, dz, POLISHED_BLACKSTONE);
                }
            }
        }
        // --- the Cage: bars, frame posts, lintel band, chains ----------------------------------------------
        int cage = cageRadius(r);
        List<ArenaShapes.RingCell> ring = ArenaShapes.ring(cage);
        List<ArenaShapes.RingCell> inner = ArenaShapes.ring(cage - 1);
        for (int i = 0; i < ring.size(); i++) {
            ArenaShapes.RingCell c = ring.get(i);
            boolean door = doorway(c.angle(), cage);
            boolean post = !door && i % 9 == 0;
            b.set(c.x(), 0, c.z(), POLISHED_BLACKSTONE);
            if (post) {
                b.column(c.x(), c.z(), 1, BAR_HEIGHT + 1, BLACKSTONE_BRICKS);
            } else if (!door) {
                b.column(c.x(), c.z(), 1, BAR_HEIGHT, BARS);
            }
            b.set(c.x(), BAR_HEIGHT + 1, c.z(), door ? BLACKSTONE_BRICKS : POLISHED_BLACKSTONE);
        }
        for (ArenaShapes.RingCell c : inner) {
            if (b.get(c.x(), BAR_HEIGHT + 1, c.z()) != null) continue;
            b.set(c.x(), BAR_HEIGHT + 1, c.z(), POLISHED_BLACKSTONE);
            if (ArenaNoise.rand(seed, SALT + 5, c.x(), c.z()) < 0.18) {
                int len = 2 + (int) (ArenaNoise.rand(seed, SALT + 6, c.x(), c.z()) * 4);
                b.column(c.x(), c.z(), BAR_HEIGHT + 1 - len, BAR_HEIGHT, CHAIN);
            }
        }
        // Posts flank every doorway.
        for (ArenaShapes.RingCell c : ring) {
            if (doorway(c.angle(), cage)) continue;
            boolean nextToDoor = doorway(c.angle() + 1.0 / cage, cage) || doorway(c.angle() - 1.0 / cage, cage);
            if (nextToDoor) b.column(c.x(), c.z(), 1, BAR_HEIGHT + 1, BLACKSTONE_BRICKS);
        }
        // --- basalt spurs for cover, outside the Cage ------------------------------------------------------
        int spurs = Math.max(8, r / 3);
        for (int i = 0; i < spurs; i++) {
            double a = Math.PI * 2 * (i + ArenaNoise.rand(seed, SALT + 7, i, 1) * 0.6) / spurs;
            double d = cage + 3 + ArenaNoise.rand(seed, SALT + 7, i, 2) * Math.max(1, r - cage - 9);
            int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
            int h = 2 + (int) (ArenaNoise.rand(seed, SALT + 7, i, 3) * 5);
            b.column(x, z, 1, h, BASALT);
            if (h > 3) b.column(x + 1, z, 1, h - 2, BASALT);
            if (h > 4) b.column(x, z + 1, 1, h - 3, BASALT);
        }
        // --- hellfire braziers -----------------------------------------------------------------------------------
        int braziers = 8;
        double bd = Math.min(r - 4, cage + (r - cage) * 0.62);
        for (int k = 0; k < braziers; k++) {
            double a = Math.PI * 2 * (k + 0.5) / braziers;
            brazier(b, (int) Math.round(Math.cos(a) * bd), (int) Math.round(Math.sin(a) * bd));
        }
        return b.build(0, 3, 0);
    }

    /** A blackstone brazier: a short column, a bowl of netherrack, hellfire burning on it. */
    private static void brazier(ArenaPlan.Builder b, int x, int z) {
        b.set(x, 0, z, POLISHED_BLACKSTONE);
        b.column(x, z, 1, 2, BLACKSTONE_BRICKS);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = dx != 0 && dz != 0;
                b.set(x + dx, 3, z + dz, corner ? POLISHED_BLACKSTONE : NETHERRACK);
                if (corner) b.set(x + dx, 4, z + dz, POLISHED_BLACKSTONE);
                else b.set(x + dx, 4, z + dz, HELLFIRE);
            }
        }
    }

    /** Whether a ring cell at this angle is part of one of the six doorways (three cells wide). */
    public static boolean doorway(double angle, int cage) {
        for (int k = 0; k < DOORWAYS; k++) {
            if (ArenaShapes.angleDiff(angle, Math.PI * 2 * k / DOORWAYS) * cage <= 1.6) return true;
        }
        return false;
    }
}
