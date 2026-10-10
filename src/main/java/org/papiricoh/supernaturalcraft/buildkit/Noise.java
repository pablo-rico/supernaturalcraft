package org.papiricoh.supernaturalcraft.buildkit;

/**
 * Deterministic noise for layouts (pure, stateless: no {@code java.util.Random}): the same arguments always give the same value,
 * on every machine, so a layout is the same every time it is drawn. Every function takes a {@code seed} (a salt): use a different
 * one per purpose so two decisions at the same cell do not correlate.
 *
 * <ul>
 *   <li>{@link #hash01}: white noise in [0, 1) per cell — scattering, picking from a mix;</li>
 *   <li>{@link #value2}/{@link #value3}: smooth value noise in [0, 1) with a feature size of {@code scale} blocks;</li>
 *   <li>{@link #fbm2}/{@link #fbm3}: fractal sums of value noise (octaves halving in size), [0, 1);</li>
 *   <li>{@link #ridged2}: ridged noise in [0, 1), sharp crests where value noise crosses 0.5 (veins, cracks, ridgelines);</li>
 *   <li>{@link #jitter}: a small signed integer offset per cell (irregular outlines).</li>
 * </ul>
 */
public final class Noise {

    private Noise() {
    }

    /** A 64-bit mix of a cell and a seed (SplitMix-style finaliser). */
    public static long hash(int x, int y, int z, int seed) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ seed * 0x27D4EB2F165667C5L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /** White noise in [0, 1). */
    public static double hash01(int x, int y, int z, int seed) {
        return (hash(x, y, z, seed) >>> 11) * 0x1.0p-53;
    }

    /** White noise in [0, 1) for a column. */
    public static double hash01(int x, int z, int seed) {
        return hash01(x, 0, z, seed);
    }

    /** True with probability {@code p} at this cell. */
    public static boolean chance(int x, int y, int z, int seed, double p) {
        return hash01(x, y, z, seed) < p;
    }

    /** An integer in [0, n). */
    public static int pick(int x, int y, int z, int seed, int n) {
        return (int) Math.floor(hash01(x, y, z, seed) * n);
    }

    /** A signed offset in [-amp, amp] per column (irregular edges). */
    public static int jitter(int x, int z, int seed, int amp) {
        return (int) Math.floor(hash01(x, 0, z, seed) * (2 * amp + 1)) - amp;
    }

    private static double fade(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Smooth value noise in [0, 1) over the plane, features about {@code scale} blocks wide. */
    public static double value2(double x, double z, double scale, int seed) {
        double fx = x / scale, fz = z / scale;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fade(fx - ix), tz = fade(fz - iz);
        double a = hash01(ix, 0, iz, seed), b = hash01(ix + 1, 0, iz, seed);
        double c = hash01(ix, 0, iz + 1, seed), d = hash01(ix + 1, 0, iz + 1, seed);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    /** Smooth value noise in [0, 1) in space. */
    public static double value3(double x, double y, double z, double scale, int seed) {
        double fx = x / scale, fy = y / scale, fz = z / scale;
        int ix = (int) Math.floor(fx), iy = (int) Math.floor(fy), iz = (int) Math.floor(fz);
        double tx = fade(fx - ix), ty = fade(fy - iy), tz = fade(fz - iz);
        double x00 = lerp(hash01(ix, iy, iz, seed), hash01(ix + 1, iy, iz, seed), tx);
        double x10 = lerp(hash01(ix, iy + 1, iz, seed), hash01(ix + 1, iy + 1, iz, seed), tx);
        double x01 = lerp(hash01(ix, iy, iz + 1, seed), hash01(ix + 1, iy, iz + 1, seed), tx);
        double x11 = lerp(hash01(ix, iy + 1, iz + 1, seed), hash01(ix + 1, iy + 1, iz + 1, seed), tx);
        return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
    }

    /** Fractal value noise in [0, 1): {@code octaves} layers, each half the size and half the weight of the one before. */
    public static double fbm2(double x, double z, double scale, int octaves, int seed) {
        double sum = 0, amp = 1, norm = 0, s = scale;
        for (int o = 0; o < octaves; o++) {
            sum += amp * value2(x, z, s, seed + o * 1013);
            norm += amp;
            amp *= 0.5;
            s = Math.max(1, s * 0.5);
        }
        return Math.min(0.999999, sum / norm);
    }

    public static double fbm3(double x, double y, double z, double scale, int octaves, int seed) {
        double sum = 0, amp = 1, norm = 0, s = scale;
        for (int o = 0; o < octaves; o++) {
            sum += amp * value3(x, y, z, s, seed + o * 1013);
            norm += amp;
            amp *= 0.5;
            s = Math.max(1, s * 0.5);
        }
        return Math.min(0.999999, sum / norm);
    }

    // --- uniform smooth noise ---------------------------------------------------------------------------------------------------

    /** Quantiles of {@link #value3} (sampled once): smooth noise piles up round 0.5, these spread it back to uniform. */
    private static final double[] Q3 = quantiles(true), Q2 = quantiles(false);

    private static double[] quantiles(boolean three) {
        int n = 40000;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            double x = hash01(i, 1, 0, 9001) * 997, y = hash01(i, 2, 0, 9001) * 997, z = hash01(i, 3, 0, 9001) * 997;
            v[i] = three ? value3(x, y, z, 1, 77) : value2(x, z, 1, 77);
        }
        java.util.Arrays.sort(v);
        double[] q = new double[101];
        for (int i = 0; i <= 100; i++) q[i] = v[Math.min(n - 1, i * (n - 1) / 100)];
        q[0] = 0;
        q[100] = 1;
        return q;
    }

    private static double uniformize(double v, double[] q) {
        int lo = 0, hi = 100;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (q[mid] <= v) lo = mid;
            else hi = mid;
        }
        double span = q[hi] - q[lo];
        double t = span <= 0 ? 0 : (v - q[lo]) / span;
        return Math.min(0.999999, Math.max(0, (lo + t) / 100.0));
    }

    /**
     * Smooth noise spread evenly over [0, 1) (patches about {@code scale} blocks across): a threshold {@code p} covers a fraction
     * {@code p} of cells, in clumps instead of salt-and-pepper. Use it to place moss, cracked bricks, flower drifts.
     */
    public static double patch3(int x, int y, int z, double scale, int seed) {
        return uniformize(value3(x, y, z, scale, seed), Q3);
    }

    public static double patch2(int x, int z, double scale, int seed) {
        return uniformize(value2(x, z, scale, seed), Q2);
    }

    /** Ridged noise in [0, 1): 1 on the crests (where the fbm crosses its middle), falling off to 0 away from them. */
    public static double ridged2(double x, double z, double scale, int octaves, int seed) {
        double v = fbm2(x, z, scale, octaves, seed);
        return Math.min(0.999999, Math.max(0, 1 - Math.abs(v - 0.5) * 4));
    }
}
