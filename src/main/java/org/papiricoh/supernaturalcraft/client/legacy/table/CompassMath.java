package org.papiricoh.supernaturalcraft.client.legacy.table;

/** The case compass's sums (v0.19), pure: bearings clockwise from north, Minecraft yaws (0 = south, 90 = west). */
public final class CompassMath {

    private static final String[] POINTS = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    private CompassMath() {
    }

    /** Bearing from (fromX, fromZ) to (toX, toZ), degrees clockwise from north, in [0, 360). */
    public static double bearing(double fromX, double fromZ, double toX, double toZ) {
        double deg = Math.toDegrees(Math.atan2(toX - fromX, -(toZ - fromZ)));
        return ((deg % 360) + 360) % 360;
    }

    /** The bearing the player faces for a yaw. */
    public static double heading(float yaw) {
        return ((yaw + 180) % 360 + 360) % 360;
    }

    /** How far right (positive) or left of the player's heading a bearing lies, in (-180, 180]. */
    public static double relative(double bearing, float yaw) {
        double d = (bearing - heading(yaw)) % 360;
        if (d > 180) d -= 360;
        if (d <= -180) d += 360;
        return d;
    }

    /** The compass point of a bearing ({@code north}, {@code north_east}, …: the case brief's {@code dir} keys). */
    public static String point(double bearing) {
        return POINTS[(int) Math.floor(((bearing % 360 + 360) % 360 + 22.5) / 45) % 8];
    }
}
