package org.papiricoh.supernaturalcraft.hunter.wall;

/**
 * The shape of an Enochian ward (v0.36), pure: each painted ward guards a sphere round the block it is painted in
 * ({@link #RADIUS}; a supreme ward, painted by one who read the second chapter of the Angel Tablet, twice that). Wards add
 * up: a point is warded when it lies inside any of them.
 */
public final class WardGeometry {

    public static final int RADIUS = 8, SUPREME_RADIUS = 16;

    private WardGeometry() {
    }

    public static int radius(boolean supreme) {
        return supreme ? SUPREME_RADIUS : RADIUS;
    }

    /** Whether ({@code x, y, z}) lies within {@code radius} of the centre of block ({@code bx, by, bz}). */
    public static boolean inside(double x, double y, double z, int bx, int by, int bz, int radius) {
        double dx = x - (bx + 0.5), dy = y - (by + 0.5), dz = z - (bz + 0.5);
        return dx * dx + dy * dy + dz * dz <= (double) radius * radius;
    }

    /**
     * Where to push something out of a ward: the horizontal unit vector from the ward's centre to it ({@code {1, 0}} when it
     * stands right on the centre).
     */
    public static double[] outward(double x, double z, int bx, int bz) {
        double dx = x - (bx + 0.5), dz = z - (bz + 0.5);
        double len = Math.sqrt(dx * dx + dz * dz);
        return len < 1e-4 ? new double[]{1, 0} : new double[]{dx / len, dz / len};
    }
}
