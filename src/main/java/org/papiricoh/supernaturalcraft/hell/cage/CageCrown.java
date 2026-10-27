package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.core.BlockPos;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.hell.worldgen.PitShape;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.hell.cage.CageKit.*;
import static org.papiricoh.supernaturalcraft.hell.cage.CageLayout.*;

/**
 * The Cage (pure): its banded octagon of posts and bars, the floor with the iris and the shaft collar beneath it, pendants
 * and lanterns under the floor, the throne inside, the stepped crown with its pinnacles, the great chain up into Hell's roof
 * and the eight great chains out to the wall.
 */
final class CageCrown {

    private CageCrown() {
    }

    /** The lowest level the Cage's own body reaches (its pendants' lanterns). */
    static final int UNDER = CAGE_FLOOR - 12;
    /** The top of the crown (the boss the great chain leaves from). */
    static final int CROWN_TOP = CAGE_ROOF + 9;

    /** The Cage's body (what {@code CageBuilder.cageOnly} writes): floor, walls, roof, crown, pendants. */
    static Box box() {
        return new Box(-CAGE_HALF - 1, UNDER, -CAGE_HALF - 1, CAGE_HALF + 1, CROWN_TOP, CAGE_HALF + 1);
    }

    static void draw(Canvas c) {
        c.inZone("cage", () -> {
            floor(c);
            walls(c);
            roof(c);
            throne(c);
            crown(c);
            c.anchor("throne", THRONE.getX(), THRONE.getY(), THRONE.getZ());
        });
        greatChain(c);
        greatChains(c);
    }

    /** The closed iris: seals in its centre and outer ring, frame between. */
    static String irisState(int x, int z) {
        return irisRing(x, z) % 2 == 0 ? SEAL : FRAME;
    }

    private static boolean wall(int x, int z) {
        return onOctagonEdge(x, z);
    }

    // --- the floor and what hangs under it ---------------------------------------------------------------------------------

    private static void floor(Canvas c) {
        int[][] layers = {{CAGE_FLOOR, CAGE_HALF, CAGE_DIAG}, {CAGE_FLOOR - 1, CAGE_HALF, CAGE_DIAG}, {CAGE_FLOOR - 2, CAGE_HALF - 1, CAGE_DIAG - 1},
                {CAGE_FLOOR - 3, CAGE_HALF - 2, CAGE_DIAG - 3}};
        for (int[] l : layers) {
            int y = l[0];
            for (int x = -CAGE_HALF; x <= CAGE_HALF; x++) {
                for (int z = -CAGE_HALF; z <= CAGE_HALF; z++) {
                    if (!inOctagon(x, z, l[1], l[2])) continue;
                    int m = Math.max(Math.abs(x), Math.abs(z));
                    if (y == CAGE_FLOOR) {
                        c.set(x, y, z, inIris(x, z) ? irisState(x, z) : m == 4 || wall(x, z) ? FRAME
                                : x == 0 || z == 0 || Math.abs(x) == Math.abs(z) ? PB_BRICKS : BEDROCK);
                        continue;
                    }
                    if (m <= 3) continue;    // the shaft under the iris stays open
                    boolean rim = !inOctagon(x + 1, z, l[1], l[2]) || !inOctagon(x - 1, z, l[1], l[2]) || !inOctagon(x, z + 1, l[1], l[2])
                            || !inOctagon(x, z - 1, l[1], l[2]);
                    c.set(x, y, z, m == 4 ? FRAME : rim ? (y == CAGE_FLOOR - 1 ? SEAL : FRAME) : masonry(x, y, z));
                }
            }
        }
        // The collar round the shaft, hanging a little below the floor.
        int y0 = CAGE_FLOOR - 4;
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != 4) continue;
                c.set(x, y0, z, (x == 0 || z == 0) ? SEAL : FRAME);
                if (Math.abs(x) == 4 && Math.abs(z) == 4) c.set(x, y0 - 1, z, capital(true));
            }
        }
        // Pendants under the floor's corners, a lantern on a chain from each; chains with lanterns between them.
        int py = CAGE_FLOOR - 4;
        for (int[] v : vertices()) {
            int x = (int) Math.round(v[0] * 0.75), z = (int) Math.round(v[1] * 0.75);
            c.set(x, py, z, capital(true));
            for (int y = py - 1; y >= py - 3; y--) c.set(x, y, z, pillar());
            c.set(x, py - 4, z, capital(false));
            hang(c, x, py - 5, py - 6, z, true);
        }
        for (int k = 0; k < 4; k++) {
            int[] p = spoke(k, 7, 0);
            hang(c, p[0], CAGE_FLOOR - 4, CAGE_FLOOR - 9, p[1], true);
        }
    }

    // --- walls -----------------------------------------------------------------------------------------------------------------

    /**
     * Posts at the corners and the faces' middles, bars between, the diagonal faces a staircase of posts (outside) and bars
     * (inside); frame rails at the foot and the top, frame bands, the band of glowing seals in the middle. Inside: air.
     */
    private static void walls(Canvas c) {
        for (int x = -CAGE_HALF; x <= CAGE_HALF; x++) {
            for (int z = -CAGE_HALF; z <= CAGE_HALF; z++) {
                if (!inOctagon(x, z)) continue;
                int ax = Math.abs(x), az = Math.abs(z);
                boolean edge = wall(x, z), straight = ax == CAGE_HALF || az == CAGE_HALF;
                boolean post = isVertex(x, z) || straight && (x == 0 || z == 0) || !straight && ax + az == CAGE_DIAG;
                for (int y = CAGE_FLOOR + 1; y < CAGE_ROOF; y++) {
                    if (!edge) {
                        c.carve(x, y, z);
                        continue;
                    }
                    String s;
                    if (isSealBand(y)) s = isVertex(x, z) ? FRAME : SEAL;
                    else if (isBand(y)) s = FRAME;
                    else s = post ? post() : bars();
                    c.set(x, y, z, s);
                }
            }
        }
        // Lanterns hanging inside from the roof.
        for (int k = 0; k < 4; k++) {
            int[] p = spoke(k, 5, 5);
            hang(c, p[0], CAGE_ROOF - 1, CAGE_ROOF - 6, p[1], true);
        }
    }

    private static void roof(Canvas c) {
        for (int x = -CAGE_HALF; x <= CAGE_HALF; x++) {
            for (int z = -CAGE_HALF; z <= CAGE_HALF; z++) {
                if (!inOctagon(x, z)) continue;
                int m = Math.max(Math.abs(x), Math.abs(z));
                String s = m <= 1 ? SEAL : wall(x, z) || m == 5 ? FRAME : x == 0 || z == 0 ? PB_BRICKS : masonry(x, CAGE_ROOF, z);
                c.set(x, CAGE_ROOF, z, s);
            }
        }
    }

    /** Lucifer's throne against the north bars: a seat on a dais, a tall back with a seal, braziers each side. */
    private static void throne(Canvas c) {
        int y = CAGE_FLOOR + 1;
        for (int x = -3; x <= 3; x++) {
            for (int z = -8; z <= -5; z++) {
                if (!inOctagon(x, z) || wall(x, z)) continue;
                if (Math.abs(x) == 3) {
                    if (z == -7) {
                        c.set(x, y, z, BRICKS);
                        c.set(x, y + 1, z, BRAZIER);
                    }
                    continue;
                }
                if (z == -5) c.set(x, y, z, stairs(Dir.NORTH, false));
                else c.set(x, y, z, z == -8 ? BRICKS : (x + z & 1) == 0 ? BEDROCK : PB_BRICKS);
            }
        }
        c.set(0, y + 1, -7, stairs(Dir.NORTH, false));
        c.set(-1, y + 1, -7, slab("bottom"));
        c.set(1, y + 1, -7, slab("bottom"));
        for (int x = -1; x <= 1; x++) {
            for (int yy = y + 1; yy <= y + 5; yy++) c.set(x, yy, -8, yy == y + 5 && x == 0 ? SEAL : x == 0 ? FRAME : BRICKS);
        }
        c.set(-1, y + 6, -8, post());
        c.set(1, y + 6, -8, post());
        c.set(0, y + 6, -8, capital(false));
    }

    // --- the crown ---------------------------------------------------------------------------------------------------------------

    /** A parapet ring, a stepped octagonal dome with ribs, pinnacles with braziers at the corners, a boss on top. */
    private static void crown(Canvas c) {
        int y0 = CAGE_ROOF + 1;
        for (int x = -CAGE_HALF; x <= CAGE_HALF; x++) {
            for (int z = -CAGE_HALF; z <= CAGE_HALF; z++) {
                if (!wall(x, z)) continue;
                boolean vertex = isVertex(x, z);
                if (vertex) {
                    for (int y = y0; y <= y0 + 3; y++) c.set(x, y, z, pillar());
                    c.set(x, y0 + 4, z, capital(true));
                    c.set(x, y0 + 5, z, BRAZIER);
                } else {
                    c.set(x, y0, z, FRAME);
                    if (Math.abs(x) + Math.abs(z) == CAGE_DIAG || Math.abs(x) == CAGE_HALF || Math.abs(z) == CAGE_HALF) {
                        if ((x + z & 1) == 0) c.set(x, y0 + 1, z, post());
                    }
                }
            }
        }
        int[][] steps = {{7, 10}, {6, 8}, {5, 7}, {4, 5}, {3, 4}, {2, 2}};
        for (int k = 0; k < steps.length; k++) {
            int y = y0 + k, half = steps[k][0], diag = steps[k][1];
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    if (!inOctagon(x, z, half, diag)) continue;
                    boolean edge = !inOctagon(x + 1, z, half, diag) || !inOctagon(x - 1, z, half, diag) || !inOctagon(x, z + 1, half, diag)
                            || !inOctagon(x, z - 1, half, diag);
                    String s = masonry(x, y, z);
                    if (edge) s = rib(x, z) ? FRAME : stairs(CageIsland.inward(x, z), false);
                    c.set(x, y, z, s);
                }
            }
        }
        int top = y0 + steps.length;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) c.set(x, top, z, x == 0 && z == 0 ? SEAL : FRAME);
        c.set(0, top + 1, 0, FRAME);
        c.set(0, top + 2, 0, SEAL);
    }

    /** The eight ribs of the dome: cells on the lines from the centre to the corners. */
    private static boolean rib(int x, int z) {
        for (int[] v : vertices()) {
            double cross = Math.abs(x * v[1] - z * v[0]) / Math.hypot(v[0], v[1]);
            if (cross < 0.6 && x * v[0] + z * v[1] > 0) return true;
        }
        return false;
    }

    // --- the chains --------------------------------------------------------------------------------------------------------------

    /** The chain the Cage hangs from, up to a boss sunk in Hell's roof. */
    private static void greatChain(Canvas c) {
        int from = CROWN_TOP + 1;
        int roof = from;
        while (roof < TOP_Y - 2 && PitShape.at(0, roof, 0) < 0) roof++;
        for (int y = from; y < roof; y++) c.set(0, y, 0, chain("y"));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int m = Math.max(Math.abs(x), Math.abs(z));
                for (int y = roof; y <= Math.min(TOP_Y, roof + 2); y++) {
                    if (m == 2 && y == roof) continue;
                    c.set(x, y, z, x == 0 && z == 0 && y == roof ? SEAL : m == 2 || y > roof ? BRICKS : FRAME);
                }
            }
        }
    }

    /**
     * From each corner of the Cage a great chain runs out parallel to the nearest spoke, sagging, to its anchor above the gate;
     * a gibbet hangs from each a third of the way out.
     */
    private static void greatChains(Canvas c) {
        List<BlockPos> anchors = anchors();
        for (int q = 0; q < 4; q++) {
            for (int i = 0; i < 2; i++) {
                int w = i == 0 ? -CHAIN_W : CHAIN_W;
                BlockPos a = anchors.get(q * 2 + i);
                int t0 = CAGE_HALF + 1, t1 = tOf(q, a) - 1;
                List<int[]> pts = new ArrayList<>();
                for (int t = t0; t < t1; t += 3) pts.add(sag(q, t, w, t0, t1));
                pts.add(sag(q, t1, w, t0, t1));
                chain(c, pts);
                int tg = t0 + (t1 - t0) / 3 + i * 4;
                int[] g = sag(q, tg, w, t0, t1);
                int len = 4 + (int) (hash(q, i, 0, 61) * 5);
                hang(c, g[0], g[1] - 1, g[1] - len, g[2], false);
                gibbet(c, g[0], g[1] - len - 1, g[2], 2);
            }
        }
    }

    private static int tOf(int q, BlockPos a) {
        return switch (q) {
            case 1 -> a.getZ();
            case 2 -> -a.getX();
            case 3 -> -a.getZ();
            default -> a.getX();
        };
    }

    /** The point of a sagging chain at {@code t} between t0 (at {@link CageLayout#CHAIN_Y}) and t1 (at the anchor's height). */
    private static int[] sag(int q, int t, int w, int t0, int t1) {
        double u = (double) (t - t0) / (t1 - t0);
        int y = (int) Math.round(CHAIN_Y + (ANCHOR_Y - CHAIN_Y) * u - 9 * 4 * u * (1 - u));
        int[] p = spoke(q, t, w);
        return new int[]{p[0], y, p[1]};
    }
}
