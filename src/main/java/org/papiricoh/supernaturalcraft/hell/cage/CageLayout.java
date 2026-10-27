package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.core.BlockPos;
import org.papiricoh.supernaturalcraft.hell.worldgen.PitShape;

import java.util.ArrayList;
import java.util.List;

/**
 * The plan of the Pit's centre, as pure geometry around the world origin: the numbers the fight, the iris and the tests rely
 * on. {@link CagePlan} draws the whole structure from them (block by block, as a kit canvas) and {@link CageBuilder} writes
 * that plan into the world.
 *
 * <pre>
 *   y 229-231  the great chain's boss, sunk in Hell's roof
 *   y 151-160  the Cage's crown: a stepped octagonal dome, eight pinnacles with braziers, the great chain up
 *   y 124-150  the Cage: an octagon of Enochian bars and posts, banded, the iris in its floor, Lucifer's throne inside
 *   y 113-123  its floor's underside: pendants, lanterns, the collar round the iris shaft
 *   y 97-113   eight compound pillars on the island (braziers on top), chained to the Cage's floor
 *   y 96-97    the fight floor (radius 28, flagstone, collapsible) round a raised dais with the 66 seals and four plinths;
 *              a masonry rim (to 31) with a broken balustrade
 *   y 54-92    the island's underside: tiers of abyssal masonry tapering to a spike over the lava, lanterns and chains
 * </pre>
 * Four bridges (arched, with balconies) run out along the axes to four gatehouses whose facades stand at
 * {@link #GATE_START}; their tunnels are cut through the Pit's wall to {@link #GATE_END}. Eight great chains run from the
 * Cage's corners to anchor plates in the wall above the gates.
 */
public final class CageLayout {

    /** The fight floor's block level (the island's surface); the dais stands one block higher. */
    public static final int ISLAND_Y = 96;
    /** The dais's top block level: the altar stands on it. */
    public static final int DAIS_Y = ISLAND_Y + 1;
    /** The fight floor's radius (the arena): collapsible flagstone. The masonry rim runs on to {@link #RIM_RADIUS}. */
    public static final int ISLAND_RADIUS = 28, RIM_RADIUS = 31;
    /** The lowest block of the island's spike. */
    public static final int ISLAND_BOTTOM = 54;
    /** The dais: its top covers {@code max(|x|, |z|) <= DAIS_HALF}; a ring of stairs and one of slabs lead up to it. */
    public static final int DAIS_HALF = 5;
    public static final int CAGE_FLOOR = 124, CAGE_ROOF = 150, CAGE_HALF = 9, CAGE_DIAG = 13;
    /** The iris in the Cage floor opens ring by ring: 0 (closed) to {@link #IRIS_STEPS} (fully open). */
    public static final int IRIS_STEPS = 3;
    public static final int SEAL_RING = 11;
    /** The pillars: their centres' radius, the level of the brazier on top, how many. */
    public static final int PILLAR_RADIUS = 22, PILLAR_TOP = 112, PILLARS = 8;
    /** The bridges run from the rim to the gatehouses' facades ({@link #BRIDGE_END} = {@link #GATE_START}). */
    public static final int BRIDGE_END = 62, GATE_START = 62, GATE_END = 90, GATE_HALF = 3, GATE_HEIGHT = 8;
    /** Over the bridges the way is clear of rock from here out, wherever the Pit's wall happens to stand. */
    public static final int CLEAR_START = 56;
    /** The great chains leave the Cage at {@link #CHAIN_Y} and reach the wall anchors at {@link #ANCHOR_Y}. */
    public static final int ANCHOR_Y = 150, CHAIN_Y = 143;
    /** Every block the structure touches lies within this many blocks of the origin (horizontally). */
    public static final int REACH = 96;
    /** The highest block (the great chain's boss in Hell's roof). */
    public static final int TOP_Y = 231;
    /** Where the ritual altar stands on the dais (the arena's centre) and where Lucifer waits in the Cage. */
    public static final BlockPos ALTAR = new BlockPos(0, DAIS_Y + 1, 0);
    public static final BlockPos THRONE = new BlockPos(0, CAGE_FLOOR + 1, 0);

    private CageLayout() {
    }

    /** Whether column (x, z) lies inside the Cage's octagon. */
    public static boolean inOctagon(int x, int z) {
        return inOctagon(x, z, CAGE_HALF, CAGE_DIAG);
    }

    /** Whether (x, z) lies in the octagon {@code |x|, |z| <= half, |x| + |z| <= diag}. */
    public static boolean inOctagon(int x, int z, int half, int diag) {
        return Math.abs(x) <= half && Math.abs(z) <= half && Math.abs(x) + Math.abs(z) <= diag;
    }

    /**
     * Where the Cage's walls stand: the octagon's straight faces, and on its diagonal faces a two-cell staircase (the outer
     * cells hold posts, the inner bars), so the wall is closed face to face all round.
     */
    public static boolean onOctagonEdge(int x, int z) {
        if (!inOctagon(x, z)) return false;
        int ax = Math.abs(x), az = Math.abs(z);
        return ax == CAGE_HALF || az == CAGE_HALF || ax + az >= CAGE_DIAG - 1 && ax >= CAGE_DIAG - CAGE_HALF && az >= CAGE_DIAG - CAGE_HALF;
    }

    /** The octagon's eight corners, where the frame's posts and the great chains attach. */
    public static List<int[]> vertices() {
        int a = CAGE_HALF, b = CAGE_DIAG - CAGE_HALF;
        return List.of(new int[]{a, b}, new int[]{b, a}, new int[]{-b, a}, new int[]{-a, b},
                new int[]{-a, -b}, new int[]{-b, -a}, new int[]{b, -a}, new int[]{a, -b});
    }

    public static boolean isVertex(int x, int z) {
        for (int[] v : vertices()) {
            if (v[0] == x && v[1] == z) return true;
        }
        return false;
    }

    /** Horizontal rings of the Cage's walls: frame rails at the foot and top, frame bands, the glowing seal band in the middle. */
    public static boolean isBand(int y) {
        return y == CAGE_FLOOR + 1 || y == 131 || y == 137 || y == 143 || y == CAGE_ROOF - 1;
    }

    /** The band of glowing seals (the others are frame). */
    public static boolean isSealBand(int y) {
        return y == 137;
    }

    /** The iris: the 7×7 square in the middle of the Cage's floor. */
    public static boolean inIris(int x, int z) {
        return Math.abs(x) <= 3 && Math.abs(z) <= 3;
    }

    /** The iris ring a cell belongs to (0 = centre 3×3, 1, 2 = outermost), the order it opens in. */
    public static int irisRing(int x, int z) {
        return Math.max(0, Math.max(Math.abs(x), Math.abs(z)) - 1);
    }

    /** Whether a floor cell of the iris is open after {@code step} steps (0 = closed). */
    public static boolean irisOpen(int x, int z, int step) {
        return inIris(x, z) && irisRing(x, z) < step;
    }

    public static List<BlockPos> irisCells() {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) out.add(new BlockPos(x, CAGE_FLOOR, z));
        }
        return out;
    }

    /** The ring of the 66 seals round the dais, in the fight floor. */
    public static boolean onSealRing(int x, int z) {
        double d = Math.sqrt(x * x + z * z);
        return d >= SEAL_RING - 0.5 && d < SEAL_RING + 0.5;
    }

    public static int sealCount() {
        int n = 0;
        for (int x = -SEAL_RING - 1; x <= SEAL_RING + 1; x++) {
            for (int z = -SEAL_RING - 1; z <= SEAL_RING + 1; z++) if (onSealRing(x, z)) n++;
        }
        return n;
    }

    /** The four plinths of the summoning circle, standing on the dais round the altar. */
    public static List<BlockPos> plinths() {
        int y = ALTAR.getY();
        return List.of(new BlockPos(4, y, 0), new BlockPos(-4, y, 0), new BlockPos(0, y, 4), new BlockPos(0, y, -4));
    }

    /** The pillars' centres at the foot of their shafts (one above the island floor), between the bridges. */
    public static List<BlockPos> pillars() {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < PILLARS; i++) {
            double a = Math.PI * 2 * i / PILLARS + Math.PI / PILLARS;
            out.add(new BlockPos((int) Math.round(Math.cos(a) * PILLAR_RADIUS), ISLAND_Y + 1, (int) Math.round(Math.sin(a) * PILLAR_RADIUS)));
        }
        return out;
    }

    /** The fight floor is this many blocks thick (all of it may fall); beneath it only the core tapers down. */
    public static final int SLAB = 4, CORE_RADIUS = 16;
    /** The underside's tiers below the slab: {top y, bottom y, radius}, each narrower than the last. */
    static final int[][] TIERS = {{92, 88, 16}, {87, 81, 13}, {80, 73, 10}, {72, 66, 7}, {65, 61, 4}, {60, 57, 2}, {56, ISLAND_BOTTOM, 1}};

    /**
     * The radius of the island's body at height {@code y} (the masonry rim aside): the fight floor's slab {@link #SLAB} blocks
     * thick over the whole floor, then tiers no wider than {@link #CORE_RADIUS} tapering to a point (so the slab's outer ring
     * can fall away). -1 outside the island's heights. {@code wobble} is added below the slab.
     */
    public static double islandRadius(int y, double wobble) {
        if (y > ISLAND_Y || y < ISLAND_BOTTOM) return -1;
        if (y > ISLAND_Y - SLAB) return ISLAND_RADIUS;
        for (int[] t : TIERS) if (y <= t[0] && y >= t[1]) return Math.max(0, t[2] - 0.5 + wobble);
        return -1;
    }

    /** The four bridge directions, as quarter turns of +X (east, south, west, north), and their unit vectors. */
    public static final int[][] SPOKES = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    /** World (x, z) of the point {@code t} out along spoke {@code q} and {@code w} to its right (seen walking outward). */
    public static int[] spoke(int q, int t, int w) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-w, t};
            case 2 -> new int[]{-t, -w};
            case 3 -> new int[]{w, -t};
            default -> new int[]{t, w};
        };
    }

    /** Whether the Pit's rock stands at (x, y, z) (the shaft's own shape; the caverns beyond may still be hollow). */
    public static boolean rock(int x, int y, int z) {
        return PitShape.at(x, y, z) > 0;
    }

    /** How far out along spoke {@code q}, {@code w} to the side, at height {@code y}, the Pit's wall begins. */
    public static int wallAt(int q, int w, int y) {
        for (int t = 40; t <= REACH; t++) {
            int[] p = spoke(q, t, w);
            if (rock(p[0], y, p[1])) return t;
        }
        return REACH;
    }

    /** The lateral offsets of the two great chains on each side of the Cage (they leave its corners parallel to the spoke). */
    public static final int CHAIN_W = CAGE_DIAG - CAGE_HALF;

    /** The anchor plates above the gates: half their width and half their height round {@link #ANCHOR_Y}. */
    public static final int PLATE_HALF = 8, PLATE_RISE = 5;

    /** How far out along spoke {@code q} the face of its anchor plate stands (two blocks proud of the wall's nearest point). */
    public static int plateFront(int q) {
        int min = REACH;
        for (int w = -PLATE_HALF; w <= PLATE_HALF; w++) {
            for (int dy = -PLATE_RISE; dy <= PLATE_RISE; dy++) {
                if (Math.abs(w) + Math.abs(dy) > PLATE_HALF + PLATE_RISE - 2) continue;
                min = Math.min(min, wallAt(q, w, ANCHOR_Y + dy) - 2);
            }
        }
        return min;
    }

    /**
     * The chain anchors in the wall, one per great chain (two on the plate above each gate, in spoke order, left then right):
     * the {@code chain_anchor} block in front of the plate, where the chain ends.
     */
    public static List<BlockPos> anchors() {
        List<BlockPos> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            int t = plateFront(q) - 1;
            for (int w : new int[]{-CHAIN_W, CHAIN_W}) {
                int[] p = spoke(q, t, w);
                out.add(new BlockPos(p[0], ANCHOR_Y, p[1]));
            }
        }
        return out;
    }

    /** Cells of a straight run from {@code a} to {@code b}, one per step along the longest axis. */
    public static List<BlockPos> line(BlockPos a, BlockPos b) {
        int dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), dz = b.getZ() - a.getZ();
        int n = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            BlockPos p = new BlockPos((int) Math.round(a.getX() + dx * t), (int) Math.round(a.getY() + dy * t),
                    (int) Math.round(a.getZ() + dz * t));
            if (out.isEmpty() || !out.getLast().equals(p)) out.add(p);
        }
        return out;
    }
}
