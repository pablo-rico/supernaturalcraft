package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

/** Seeded hashes and smooth value noise for the Author's layouts (pure, deterministic). */
public final class ArenaNoise {

    private ArenaNoise() {
    }

    /** A well-mixed 64-bit hash of a seed, a salt and two coordinates. */
    public static long hash(long seed, int salt, int x, int z) {
        long h = seed * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        h ^= x * 0x165667B19E3779F9L;
        h = Long.rotateLeft(h, 27) * 0x94D049BB133111EBL;
        h ^= z * 0x27D4EB2F165667C5L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h;
    }

    /** A uniform number in [0, 1) for this seed, salt and cell. */
    public static double rand(long seed, int salt, int x, int z) {
        return (hash(seed, salt, x, z) >>> 11) * 0x1.0p-53;
    }

    /** Smooth value noise in [0, 1] with features about {@code scale} cells across. */
    public static double smooth(long seed, int salt, double x, double z, double scale) {
        double fx = x / scale, fz = z / scale;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fade(fx - x0), tz = fade(fz - z0);
        double a = rand(seed, salt, x0, z0), b = rand(seed, salt, x0 + 1, z0);
        double c = rand(seed, salt, x0, z0 + 1), d = rand(seed, salt, x0 + 1, z0 + 1);
        double top = a + (b - a) * tx, bottom = c + (d - c) * tx;
        return top + (bottom - top) * tz;
    }

    private static double fade(double t) {
        return t * t * (3 - 2 * t);
    }
}
