package org.papiricoh.supernaturalcraft.bowl.spell;

import java.util.Locale;

/**
 * Pure: how a locating spell's smoke describes where it is going, as a compass point and a rough
 * distance ("north-east, far away"). Minecraft's north is -Z and east is +X.
 */
public final class Bearing {

    private Bearing() {
    }

    public enum Compass {
        NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String key() {
            return "direction.supernaturalcraft." + id();
        }
    }

    public enum Band {
        /** Within {@link #HERE_MAX} blocks. */
        HERE,
        NEAR,
        FAR,
        DISTANT;

        public String key() {
            return "message.supernaturalcraft.locate.band." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final double HERE_MAX = 16;
    public static final double NEAR_MAX = 128;
    public static final double FAR_MAX = 1000;

    /** The compass point nearest to the horizontal direction (dx, dz); north for no offset at all. */
    public static Compass compass(double dx, double dz) {
        if (dx == 0 && dz == 0) return Compass.NORTH;
        // Clockwise from north: north 0°, east 90°, south 180°, west -90°.
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        int index = Math.floorMod(Math.round(deg / 45.0), 8);
        return Compass.values()[index];
    }

    public static Band band(double distance) {
        if (distance < HERE_MAX) return Band.HERE;
        if (distance < NEAR_MAX) return Band.NEAR;
        if (distance < FAR_MAX) return Band.FAR;
        return Band.DISTANT;
    }
}
