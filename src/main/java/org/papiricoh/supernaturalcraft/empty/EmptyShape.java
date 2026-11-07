package org.papiricoh.supernaturalcraft.empty;

/**
 * The Empty's one floor (v0.41), pure: a round island of black ground, {@link #RADIUS} across, its edge broken, and the
 * sleepers laid on it in rings. Offsets from the island's centre (on its top surface, y = 0); tested by {@code EmptyShapeTest}.
 */
public final class EmptyShape {

    public static final int RADIUS = 24;
    /** The ring the hunter lands on, and where the Shadow rises (the centre). */
    public static final int LANDING = 16;
    /** Sleepers in each ring, and the rings' radii. */
    public static final int[] RINGS = {9, 13, 20};
    public static final int PER_RING = 10;

    private EmptyShape() {
    }

    /** Whether there is floor at ({@code dx}, {@code dz}): the disc, its rim gnawed by a hash. */
    public static boolean floor(int dx, int dz) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= RADIUS - 2) return true;
        if (d > RADIUS) return false;
        return Math.floorMod(dx * 73_856_093 ^ dz * 19_349_663, 7) > 2;
    }

    /** How deep the island is under ({@code dx}, {@code dz}): a shallow bowl, deeper at the middle. */
    public static int depth(int dx, int dz) {
        double d = Math.sqrt(dx * dx + dz * dz);
        return 2 + (int) Math.round(6 * Math.max(0, 1 - d / RADIUS));
    }

    /** The {@code i}th sleeper of ring {@code ring}: its offset and the way it lies (0-3, a horizontal direction's index). */
    public static int[] sleeper(int ring, int i) {
        double a = Math.PI * 2 * i / PER_RING + ring * 0.37;
        int r = RINGS[ring];
        int dx = (int) Math.round(Math.cos(a) * r), dz = (int) Math.round(Math.sin(a) * r);
        int facing = Math.floorMod(i + ring, 4);
        return new int[]{dx, dz, facing};
    }
}
