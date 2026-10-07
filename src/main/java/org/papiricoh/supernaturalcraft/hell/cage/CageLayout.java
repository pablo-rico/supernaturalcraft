package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The plan of the Pit's centre, as pure geometry around the world origin. Everything the builder
 * places and the controller opens comes from here, so the tests can check it without a world.
 *
 * <pre>
 *   y 151-153  crown + the great chain up into the roof
 *   y 124-150  the Cage: an octagon of Enochian bars, iris in its floor
 *   y 97-109   six pillars on the island, chained to the Cage's floor
 *   y 96       the island floor, the dais and the ring of 66 seals
 *   y 60-95    the island's body tapering to a point over the lava
 * </pre>
 * Four bridges run out at y 96 to four gates cut into the Pit's wall; eight great chains run from
 * the Cage's corners to anchors in the wall.
 */
public final class CageLayout {

    public static final int ISLAND_Y = 96, ISLAND_RADIUS = 26, ISLAND_BOTTOM = 60;
    public static final int CAGE_FLOOR = 124, CAGE_ROOF = 150, CAGE_HALF = 9, CAGE_DIAG = 13;
    /** The iris in the Cage floor opens ring by ring: 0 (closed) to {@link #IRIS_STEPS} (fully open). */
    public static final int IRIS_STEPS = 3;
    public static final int SEAL_RING = 11;
    public static final int PILLAR_RADIUS = 18, PILLAR_TOP = 109, PILLARS = 6;
    public static final int BRIDGE_END = 76, GATE_START = 70, GATE_END = 94, GATE_HALF = 3, GATE_HEIGHT = 8;
    /** Over the bridges the way is cut clear of rock from here out, wherever the Pit's wall happens to stand. */
    public static final int CLEAR_START = 56;
    public static final int ANCHOR_RADIUS = 74, ANCHOR_Y = 160, CHAIN_Y = 137;
    /** Every block the structure touches lies within this many blocks of the origin (horizontally). */
    public static final int REACH = 100;
    /** Where the ritual altar stands on the dais (the arena's centre) and where Lucifer waits in the Cage. */
    public static final BlockPos ALTAR = new BlockPos(0, ISLAND_Y + 1, 0);
    public static final BlockPos THRONE = new BlockPos(0, CAGE_FLOOR + 1, 0);

    private CageLayout() {
    }

    /** Whether column (x, z) lies inside the Cage's octagon. */
    public static boolean inOctagon(int x, int z) {
        return Math.abs(x) <= CAGE_HALF && Math.abs(z) <= CAGE_HALF && Math.abs(x) + Math.abs(z) <= CAGE_DIAG;
    }

    /** Inside the octagon with at least one horizontal neighbour outside it: where the bars stand. */
    public static boolean onOctagonEdge(int x, int z) {
        return inOctagon(x, z) && (!inOctagon(x + 1, z) || !inOctagon(x - 1, z) || !inOctagon(x, z + 1) || !inOctagon(x, z - 1));
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

    /** Horizontal bands of glowing seals that ring the Cage's walls. */
    public static boolean isBand(int y) {
        return y == 131 || y == 137 || y == 143;
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

    /** The ring of the 66 seals around the dais. */
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

    /** The four plinths of the summoning circle, raised one block on the dais. */
    public static List<BlockPos> plinths() {
        int y = ISLAND_Y + 1;
        return List.of(new BlockPos(4, y, 0), new BlockPos(-4, y, 0), new BlockPos(0, y, 4), new BlockPos(0, y, -4));
    }

    /** The six pillars' bases (on the island floor). */
    public static List<BlockPos> pillars() {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < PILLARS; i++) {
            double a = Math.PI * 2 * i / PILLARS + Math.PI / 12;
            out.add(new BlockPos((int) Math.round(Math.cos(a) * PILLAR_RADIUS), ISLAND_Y + 1, (int) Math.round(Math.sin(a) * PILLAR_RADIUS)));
        }
        return out;
    }

    /** The island's top slab is this thick; beneath it only a narrower core tapers down to a point. */
    public static final int SLAB = 4, CORE_RADIUS = 15;

    /**
     * The radius of the island's body at height {@code y}: a slab {@link #SLAB} blocks thick over the whole
     * island, then a core of {@link #CORE_RADIUS} tapering to a point (so the slab's rim can fall away).
     */
    public static double islandRadius(int y, double wobble) {
        if (y > ISLAND_Y || y < ISLAND_BOTTOM) return -1;
        if (y > ISLAND_Y - SLAB) return ISLAND_RADIUS;
        double t = (double) (y - ISLAND_BOTTOM) / (ISLAND_Y - SLAB - ISLAND_BOTTOM);
        return CORE_RADIUS * Math.pow(t, 0.7) + wobble;
    }

    /** Wall anchors for the eight great chains, in the same directions as the Cage's corners. */
    public static List<BlockPos> anchors() {
        List<BlockPos> out = new ArrayList<>();
        for (int[] v : vertices()) {
            double a = Math.atan2(v[1], v[0]);
            out.add(new BlockPos((int) Math.round(Math.cos(a) * ANCHOR_RADIUS), ANCHOR_Y, (int) Math.round(Math.sin(a) * ANCHOR_RADIUS)));
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

    /** The four bridge directions: +X, -X, +Z, -Z. */
    public static final int[][] SPOKES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
}
