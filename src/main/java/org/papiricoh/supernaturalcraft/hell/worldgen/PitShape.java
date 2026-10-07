package org.papiricoh.supernaturalcraft.hell.worldgen;

/**
 * The arithmetic of the Pit's shaft (see {@link HellPit}), kept free of Minecraft types so the
 * tests can run it without a game.
 */
public final class PitShape {

    public static final double RADIUS = 72, WALL = 6;
    public static final int FLOOR_Y = 20, ROOF_Y = 228;
    /** Far outside the Pit nothing changes: the cavern density always wins the {@code min}. */
    public static final double SOLID = 64.0;
    /** How much the ends of the shaft close in over their last blocks (a lip under the roof). */
    private static final double END_BLEND = 20.0;

    private PitShape() {
    }

    /** The wall's distance from the axis at this angle and height. */
    public static double wallRadius(double radius, double angle, int y) {
        return radius + 3.5 * Math.sin(3 * angle + y * 0.045) + 2.0 * Math.sin(7 * angle - y * 0.02) + 1.2 * Math.sin(13 * angle);
    }

    /** Negative inside the shaft (air), positive in the wall and beyond. */
    public static double at(double radius, double wall, int floorY, int roofY, int x, int y, int z) {
        if (y < floorY || y > roofY) return SOLID;
        double r = Math.sqrt((double) x * x + (double) z * z);
        if (r > radius + 16) return SOLID;
        double d = (r - wallRadius(radius, Math.atan2(z, x), y)) / wall;
        double toEnd = Math.min(y - floorY, roofY - y);
        if (toEnd < END_BLEND) d += (1 - toEnd / END_BLEND) * 3.0;
        return Math.max(-1.0, Math.min(SOLID, d));
    }

    public static double at(int x, int y, int z) {
        return at(RADIUS, WALL, FLOOR_Y, ROOF_Y, x, y, z);
    }
}
