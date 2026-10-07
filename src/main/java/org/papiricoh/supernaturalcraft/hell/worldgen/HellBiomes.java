package org.papiricoh.supernaturalcraft.hell.worldgen;

/**
 * Which of Hell's four regions a column belongs to. The Pit always surrounds the world origin
 * (the Cage hangs at its heart); elsewhere two climate noises share the caverns out between the
 * Ash Wastes (hot), the Rack (wet with blood) and Crowley's Corridors (neither).
 *
 * <p>Pure, so the layout can be checked without a world.
 */
public final class HellBiomes {

    public enum Kind { THE_RACK, ASH_WASTES, CROWLEYS_CORRIDORS, THE_PIT }

    /** Radius of the Pit region (blocks): the shaft plus the dark rock around it. */
    public static final double PIT_RADIUS = 150;
    public static final float HOT = 0.18f, WET = -0.05f;

    private HellBiomes() {
    }

    /**
     * @param x           block x
     * @param z           block z
     * @param temperature climate temperature, about -1..1
     * @param humidity    climate humidity, about -1..1
     */
    public static Kind pick(int x, int z, float temperature, float humidity) {
        double d = Math.sqrt((double) x * x + (double) z * z);
        double edge = PIT_RADIUS + 14 * Math.sin(Math.atan2(z, x) * 5) + 20 * temperature;
        if (d < edge) return Kind.THE_PIT;
        if (temperature > HOT) return Kind.ASH_WASTES;
        if (humidity > WET) return Kind.THE_RACK;
        return Kind.CROWLEYS_CORRIDORS;
    }
}
