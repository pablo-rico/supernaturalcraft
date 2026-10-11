package org.papiricoh.supernaturalcraft.legacy.cases.table;

/**
 * A square of the land a war table shows (v0.19), pure. Tiles sit on a world grid per level of detail: {@link #SIZE} samples a
 * side, {@link #SPACING} blocks apart, plus one more row and column (the next tile's first) so the hologram can shade its edge.
 *
 * <p>Data: {@link #SAMPLES} big-endian shorts of height (the ocean floor's heightmap, so below sea level is water), then as many
 * surface bytes: a {@link TerrainKind} in the low bits, {@link #WATER} and {@link #REAL} (read from a loaded chunk rather than
 * foretold from the world's generator).
 */
public final class TerrainTile {

    public static final int SIZE = 64;
    public static final int SIDE = SIZE + 1;
    public static final int SAMPLES = SIDE * SIDE;
    public static final int BYTES = SAMPLES * 3;
    /** Blocks between samples, by level of detail: near (a 512-block tile) and far (2048). */
    public static final int[] SPACING = {8, 32};
    public static final int NEAR = 0, FAR = 1;
    public static final int KIND_MASK = 0x1F;
    public static final int WATER = 0x40;
    public static final int REAL = 0x80;
    /** How far from its table a war table maps, in blocks. */
    public static final int RANGE = 1800;

    private TerrainTile() {
    }

    /** Blocks a tile covers at {@code lod}. */
    public static int span(int lod) {
        return SIZE * SPACING[lod];
    }

    public static long key(int lod, int tx, int tz) {
        return ((long) lod << 60) | (((long) tx & 0x3FFFFFFFL) << 30) | ((long) tz & 0x3FFFFFFFL);
    }

    public static int lod(long key) {
        return (int) (key >>> 60);
    }

    public static int tx(long key) {
        return (int) ((key << 4) >> 34);
    }

    public static int tz(long key) {
        return (int) ((key << 34) >> 34);
    }

    /** The tile column holding block x (or z) at {@code lod}. */
    public static int tileOf(int lod, int block) {
        return Math.floorDiv(block, span(lod));
    }

    /** World x (or z) of a tile's first sample. */
    public static int origin(int lod, int tile) {
        return tile * span(lod);
    }

    /** The level of detail to draw at, for so many blocks per screen pixel. */
    public static int pickLod(double blocksPerPixel) {
        return blocksPerPixel > 4 ? FAR : NEAR;
    }

    /** Whether a tile lies (at least partly) within {@link #RANGE} of a table at (x, z). */
    public static boolean inRange(long key, int x, int z) {
        int lod = lod(key);
        if (lod != NEAR && lod != FAR) return false;
        int span = span(lod);
        int ox = origin(lod, tx(key)), oz = origin(lod, tz(key));
        return ox + span >= x - RANGE && ox <= x + RANGE && oz + span >= z - RANGE && oz <= z + RANGE;
    }

    /** The keys of the tiles at {@code lod} covering the block box, west to east, north to south. */
    public static long[] covering(int lod, int minX, int minZ, int maxX, int maxZ) {
        int x0 = tileOf(lod, minX), x1 = tileOf(lod, maxX), z0 = tileOf(lod, minZ), z1 = tileOf(lod, maxZ);
        long[] out = new long[(x1 - x0 + 1) * (z1 - z0 + 1)];
        int n = 0;
        for (int tz = z0; tz <= z1; tz++) for (int tx = x0; tx <= x1; tx++) out[n++] = key(lod, tx, tz);
        return out;
    }

    public static int index(int i, int j) {
        return j * SIDE + i;
    }

    public static int height(byte[] data, int i, int j) {
        int k = index(i, j) * 2;
        return (short) (((data[k] & 0xFF) << 8) | (data[k + 1] & 0xFF));
    }

    public static int surface(byte[] data, int i, int j) {
        return data[SAMPLES * 2 + index(i, j)] & 0xFF;
    }

    public static void set(byte[] data, int i, int j, int height, int surface) {
        int k = index(i, j) * 2;
        data[k] = (byte) (height >> 8);
        data[k + 1] = (byte) height;
        data[SAMPLES * 2 + index(i, j)] = (byte) surface;
    }

    public static int surface(TerrainKind kind, boolean water, boolean real) {
        return kind.ordinal() | (water ? WATER : 0) | (real ? REAL : 0);
    }
}
