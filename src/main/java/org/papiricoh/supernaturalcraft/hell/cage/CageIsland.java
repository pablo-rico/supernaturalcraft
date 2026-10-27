package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.core.BlockPos;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.hell.cage.CageKit.*;
import static org.papiricoh.supernaturalcraft.hell.cage.CageLayout.*;

/**
 * The island (pure): the fight floor and its inlays, the dais with the seals and plinths, the eight pillars, the masonry rim
 * with its broken balustrade, and the tiers that hang beneath it over the lava.
 *
 * <p>The contract with the fight: the floor at {@link CageLayout#ISLAND_Y} is flat and clear out to
 * {@link CageLayout#ISLAND_RADIUS} but for the dais and the pillars; between {@code UncagedTerrain.LAST_STAND_RADIUS} and that
 * radius it is {@link CageLayout#SLAB} blocks of flagstone and vanilla stone with nothing under it, so the last phase can drop
 * it all into the Pit. The rim beyond is masonry the fight never touches.
 */
final class CageIsland {

    private CageIsland() {
    }

    /** Inside this the floor never falls (and may hold seals and frame); it matches {@code UncagedTerrain.LAST_STAND_RADIUS}. */
    static final int KEEP = 17;
    private static final int RIM_DEPTH = 6;

    static void draw(Canvas c) {
        floor(c);
        dais(c);
        rim(c);
        balustrade(c);
        for (BlockPos p : pillars()) pillar(c, p.getX(), p.getZ());
        underside(c);
    }

    static double dist(int x, int z) {
        return Math.sqrt(x * x + z * z);
    }

    /** Whether the angle of (x, z) lies within half a cell of one of {@code n} evenly spaced rays starting at {@code offset}. */
    static boolean onRay(int x, int z, int n, double offset) {
        double d = dist(x, z);
        if (d < 0.5) return false;
        double a = Math.atan2(z, x) - offset, step = Math.PI * 2 / n;
        double off = Math.abs(a - Math.round(a / step) * step);
        return off * d < 0.5;
    }

    // --- the floor -------------------------------------------------------------------------------------------------------------

    private static void floor(Canvas c) {
        int r = ISLAND_RADIUS;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = dist(x, z);
                if (d > r + 0.5) continue;
                c.set(x, ISLAND_Y, z, floorAt(x, z, d));
                for (int y = ISLAND_Y - SLAB + 1; y < ISLAND_Y; y++) c.set(x, y, z, d <= KEEP ? masonry(x, y, z) : y == ISLAND_Y - SLAB + 1 ? vault(x, z, d) : FLAG);
            }
        }
    }

    /**
     * The fight floor's pattern: the seal ring between two courses of polished blackstone, the Enochian glyph-strokes round
     * it, a ring of frame where the last stand ends, then flagstone ruled by sixteen rays, a ring at the pillars and a lip.
     */
    static String floorAt(int x, int z, double d) {
        int m = Math.max(Math.abs(x), Math.abs(z));
        if (m <= DAIS_HALF + 2) return BEDROCK;
        if (onSealRing(x, z)) return SEAL;
        if (d >= SEAL_RING - 1.5 && d < SEAL_RING + 1.5) return PB_BRICKS;
        if (d < 15.5) return d >= 12.5 && onRay(x, z, 24, 0) ? RITUAL : d >= 13.5 && d < 14.5 && onRay(x, z, 24, Math.PI / 24) ? CHISELED : FLAG;
        if (d < 16.5) return FRAME;
        if (d >= ISLAND_RADIUS - 0.5) return PB_BRICKS;
        if (d >= PILLAR_RADIUS - 0.5 && d < PILLAR_RADIUS + 0.5) return POLISHED;
        if (onRay(x, z, 16, 0)) return d < 19.5 ? CHISELED : PB_BRICKS;
        return FLAG;
    }

    /** The slab's underside past the last stand, seen from below: flagstone ribbed with polished blackstone (all of it may fall). */
    static String vault(int x, int z, double d) {
        if (onRay(x, z, 16, Math.PI / 16)) return PB_BRICKS;
        if (Math.abs(d - 21) < 0.5 || Math.abs(d - 25) < 0.5) return POLISHED;
        return (x + z & 1) == 0 && hash(x, 0, z, 23) < 0.3 ? BLACKSTONE : FLAG;
    }

    // --- the dais --------------------------------------------------------------------------------------------------------------

    /** Raised one block: a square top (the circle is drawn on it), a ring of stairs and one of slabs up from the floor. */
    private static void dais(Canvas c) {
        int h = DAIS_HALF;
        for (int x = -h - 2; x <= h + 2; x++) {
            for (int z = -h - 2; z <= h + 2; z++) {
                int ax = Math.abs(x), az = Math.abs(z), m = Math.max(ax, az);
                if (m == h + 2) {
                    c.set(x, DAIS_Y, z, slab("bottom"));
                } else if (m == h + 1) {
                    Dir in = ax >= az ? (x > 0 ? Dir.WEST : Dir.EAST) : (z > 0 ? Dir.NORTH : Dir.SOUTH);
                    c.set(x, DAIS_Y, z, stairs(in, false));
                } else {
                    String s;
                    if (m <= 1) s = RITUAL;
                    else if (m == h) s = FRAME;
                    else if (ax == az) s = SEAL;
                    else if (m == 4 && (ax == 0 || az == 0)) s = FRAME;
                    else s = (x + z & 1) == 0 ? BEDROCK : BRICKS;
                    c.set(x, DAIS_Y, z, s);
                }
            }
        }
        for (BlockPos p : plinths()) c.set(p.getX(), p.getY(), p.getZ(), RITUAL);
        c.anchor("altar", ALTAR.getX(), ALTAR.getY(), ALTAR.getZ());
    }

    // --- the rim and its balustrade ----------------------------------------------------------------------------------------------

    /** Whether (x, z) lies in a bridge's mouth (where the rim opens on a bridge). */
    static boolean inMouth(int x, int z, int half) {
        return Math.abs(z) <= half && Math.abs(x) > 20 || Math.abs(x) <= half && Math.abs(z) > 20;
    }

    private static void rim(Canvas c) {
        int r = RIM_RADIUS;
        for (int x = -r - 1; x <= r + 1; x++) {
            for (int z = -r - 1; z <= r + 1; z++) {
                double d = dist(x, z);
                if (d <= ISLAND_RADIUS + 0.5 || d > r + 0.5) continue;
                boolean outer = d > r - 0.5;
                for (int y = ISLAND_Y - RIM_DEPTH; y <= ISLAND_Y; y++) {
                    if (y < ISLAND_Y - RIM_DEPTH + 2 && d <= ISLAND_RADIUS + 1.5) continue;
                    String s = masonry(x, y, z);
                    if (y == ISLAND_Y) s = outer ? FRAME : onRay(x, z, 32, Math.PI / 32) ? CHISELED : hash(x, y, z, 5) < 0.15 ? CRACKED : BRICKS;
                    else if (y == ISLAND_Y - 2 && outer) s = onRay(x, z, 16, Math.PI / 16) ? SEAL : FRAME;
                    c.set(x, y, z, s);
                }
                // Brackets under the outer edge, a lantern under every other one.
                if (outer && onRay(x, z, 48, 0)) {
                    int y = ISLAND_Y - RIM_DEPTH - 1;
                    c.set(x, y, z, BRICKS);
                    c.set(x, y - 1, z, BEDROCK);
                    if (onRay(x, z, 24, 0)) hang(c, x, y - 2, y - 3 - (int) (hash(x, 0, z, 9) * 3), z, true);
                }
            }
        }
    }

    /** The cells of a face-connected ring of radius {@code r}, in order round it. */
    static List<int[]> ring(double r) {
        List<int[]> out = new ArrayList<>();
        int n = (int) Math.ceil(r * 16);
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * i / n;
            int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
            if (!out.isEmpty()) {
                int[] l = out.getLast();
                if (l[0] == x && l[1] == z) continue;
                if (l[0] != x && l[1] != z) {
                    // Turn the corner through whichever cell lies nearer the circle.
                    double e1 = Math.abs(dist(x, l[1]) - r), e2 = Math.abs(dist(l[0], z) - r);
                    out.add(e1 <= e2 ? new int[]{x, l[1]} : new int[]{l[0], z});
                }
            }
            if (out.size() > 1 && out.getFirst()[0] == x && out.getFirst()[1] == z) break;
            out.add(new int[]{x, z});
        }
        return out;
    }

    /**
     * Iron bars between thick posts on the rim's outer edge: piers with braziers where the pillars look out, watchers between
     * them on the diagonals, gaps where the balustrade has broken away.
     */
    private static void balustrade(Canvas c) {
        List<int[]> ring = ring(RIM_RADIUS);
        int y = ISLAND_Y + 1;
        for (int i = 0; i < ring.size(); i++) {
            int x = ring.get(i)[0], z = ring.get(i)[1];
            if (inMouth(x, z, 5)) continue;
            double a = Math.atan2(z, x);
            double toPier = angleTo(a, Math.PI / 8, Math.PI / 4), toWatcher = angleTo(a, Math.PI / 4, Math.PI / 2);
            if (toWatcher * RIM_RADIUS < 0.6) {
                // A watcher on the diagonal, flanked by two piers.
                c.set(x, y, z, watcher(inward(x, z)));
                continue;
            }
            if (toWatcher * RIM_RADIUS < 2.6 || toPier * RIM_RADIUS < 0.6) {
                c.set(x, y, z, BRICKS);
                c.set(x, y + 1, z, toPier * RIM_RADIUS < 0.6 ? CHISELED : PB_BRICKS);
                c.set(x, y + 2, z, BRAZIER);
                continue;
            }
            // Broken stretches: a few runs of the ring have lost their bars (a slab of rubble lies there).
            int seg = i / 5;
            if (hash(seg, 3, 0, 41) < 0.2) {
                if (hash(x, y, z, 7) < 0.4) c.set(x, y, z, slab("bottom"));
                continue;
            }
            boolean post = i % 4 == 0;
            c.set(x, y, z, post ? post() : bars());
            c.set(x, y + 1, z, post ? post() : bars());
            if (post) c.set(x, y + 2, z, hash(x, 0, z, 13) < 0.3 ? lantern(false) : post());
        }
    }

    /** The horizontal direction from (x, z) toward the centre, along its larger axis. */
    static Dir inward(int x, int z) {
        return Math.abs(x) >= Math.abs(z) ? (x > 0 ? Dir.WEST : Dir.EAST) : (z > 0 ? Dir.NORTH : Dir.SOUTH);
    }

    /** How far (radians) {@code a} is from the nearest of the rays {@code offset + k * step}. */
    static double angleTo(double a, double offset, double step) {
        double t = a - offset;
        return Math.abs(t - Math.round(t / step) * step);
    }

    // --- the pillars -------------------------------------------------------------------------------------------------------------

    /**
     * A compound pier: a plinth with a skirt of stairs, five Enochian shafts in a cross with posts in its corners, a frame band,
     * capitals, an impost with a seal, and a brazier; a chain runs from its inner corner up to the Cage's floor.
     */
    private static void pillar(Canvas c, int px, int pz) {
        int base = ISLAND_Y + 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int ax = Math.abs(dx), az = Math.abs(dz), x = px + dx, z = pz + dz;
                if (ax == 2 || az == 2) {
                    Dir in = ax >= az ? (dx > 0 ? Dir.WEST : Dir.EAST) : (dz > 0 ? Dir.NORTH : Dir.SOUTH);
                    c.set(x, base, z, stairs(in, false));
                    continue;
                }
                boolean corner = ax == 1 && az == 1;
                c.set(x, base, z, corner ? FRAME : BRICKS);
                for (int y = base + 1; y < PILLAR_TOP - 1; y++) {
                    String s;
                    if (corner) s = y == base + 7 ? FRAME : post();
                    else if (y == base + 1) s = capital(false);
                    else if (y == PILLAR_TOP - 2) s = capital(true);
                    else s = CageKit.pillar();
                    c.set(x, y, z, s);
                }
                c.set(x, PILLAR_TOP - 1, z, ax == 0 && az == 0 ? SEAL : corner ? FRAME : BRICKS);
                if (corner) c.set(x, PILLAR_TOP, z, post());
            }
        }
        c.set(px, PILLAR_TOP, pz, BRAZIER);
        // The corner nearest the Cage carries the chain up to the floor's rim, above the matching corner.
        int sx = -Integer.signum(px), sz = -Integer.signum(pz);
        int[] v = nearestVertex(px, pz);
        chain(c, new int[]{px + sx, PILLAR_TOP + 1, pz + sz}, new int[]{v[0], CAGE_FLOOR - 2, v[1]});
    }

    static int[] nearestVertex(int x, int z) {
        int[] best = null;
        double bd = Double.MAX_VALUE;
        for (int[] v : vertices()) {
            double d = Math.hypot(v[0] - x, v[1] - z);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    // --- under the island ------------------------------------------------------------------------------------------------------

    /**
     * Tiers of masonry narrowing to a spike (no wider than the last stand, so the outer floor falls clear), each ledge edged
     * with frame and seals; chains with lanterns and stone drips hang from the ledges, gibbets from the widest.
     */
    private static void underside(Canvas c) {
        int[][] tiers = TIERS;
        for (int i = 0; i < tiers.length; i++) {
            int top = tiers[i][0], bottom = tiers[i][1], r = tiers[i][2];
            int next = i + 1 < tiers.length ? tiers[i + 1][2] : -1;
            for (int x = -r - 1; x <= r + 1; x++) {
                for (int z = -r - 1; z <= r + 1; z++) {
                    double d = dist(x, z);
                    for (int y = top; y >= bottom; y--) {
                        double wob = (hash(x, y, z, 3) - 0.5) * 1.2;
                        if (d > r - 0.5 + (y == bottom ? 0 : wob * 0.6)) continue;
                        boolean ledge = y == bottom && d > next + 0.5;
                        String s = CageKit.underside(x, y, z);
                        if (ledge) s = onRay(x, z, 12, i * 0.4) ? SEAL : d > r - 1.5 ? FRAME : s;
                        else if (y == top && d > r - 1.5 && i > 0) s = FRAME;
                        c.set(x, y, z, s);
                    }
                }
            }
            if (next < 0) continue;
            // From the ledge's underside: chains with lanterns, drips of stone, and on the widest tier gibbets.
            for (int k = 0; k < 18; k++) {
                double a = Math.PI * 2 * k / 18 + i * 0.37;
                double rr = (r + next) / 2.0 + 0.5;
                int x = (int) Math.round(Math.cos(a) * rr), z = (int) Math.round(Math.sin(a) * rr);
                if (!c.has(x, bottom, z) || c.isAir(x, bottom, z)) continue;
                int y = bottom - 1;
                double h = hash(x, i, z, 17);
                if (i == 1 && k % 6 == 0) {
                    int len = 6 + (int) (h * 5);
                    hang(c, x, y, y - len, z, false);
                    gibbet(c, x, y - len - 1, z, 2);
                } else if (k % 2 == 0) {
                    hang(c, x, y, y - 2 - (int) (h * (4 + 2 * i)), z, true);
                } else {
                    drip(c, x, y, z, 2 + (int) (h * 3));
                }
            }
        }
    }

    /** A stalactite of masonry ending in dripstone. */
    private static void drip(Canvas c, int x, int y, int z, int len) {
        for (int k = 0; k < len; k++) c.set(x, y - k, z, k == 0 ? BEDROCK : BLACKSTONE);
        c.set(x, y - len, z, "minecraft:pointed_dripstone[thickness=frustum,vertical_direction=down,waterlogged=false]");
        c.set(x, y - len - 1, z, "minecraft:pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]");
    }
}
