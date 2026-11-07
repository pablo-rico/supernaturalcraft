package org.papiricoh.supernaturalcraft.apocworld.worldgen;

/**
 * The arithmetic of the Apocalypse World's land (see {@link AUTerrain}), free of Minecraft types so the tests can run it without
 * a game. Five noise samples come in (each about -1..1, from the world's seed):
 * <ul>
 *   <li>{@code hills} rolls the scorched land gently up and down;</li>
 *   <li>{@code ridges} raises a broken ridge where it is near zero (a long line of high, jagged rock);</li>
 *   <li>{@code pits} sinks a crater where it peaks, with a low rim of thrown-up earth round it;</li>
 *   <li>{@code river} cuts a dried riverbed (flat-bottomed, no water) where it is near zero;</li>
 *   <li>{@code detail} (3D) breaks up the ridges' tops and the craters' walls.</li>
 * </ul>
 */
public final class AUShape {

    /** The land's middle height and how far the hills lift and drop it. */
    public static final double BASE = 70, ROLL = 9;
    /** The ridges: where |ridges| is under this, a ridge this tall at its spine. */
    public static final double RIDGE_WIDTH = 0.11, RIDGE_HEIGHT = 28;
    /** The craters: where {@code pits} passes the edge, a bowl this deep at its heart; a rim this high just outside it. */
    public static final double CRATER_EDGE = 0.5, CRATER_FULL = 0.72, CRATER_DEPTH = 13, RIM_START = 0.4, RIM_HEIGHT = 2.5;
    /** The dried riverbeds: where |river| is under this, a channel this deep (flat across most of its width). */
    public static final double RIVER_WIDTH = 0.04, RIVER_DEPTH = 6;
    /** Deep rock is always solid, high sky always air. */
    public static final int ROCK_Y = 24, SKY_Y = 160;
    /** How much the 3D detail roughens the land, and how much more on a ridge. */
    public static final double ROUGH = 0.12, RIDGE_ROUGH = 0.8;

    private AUShape() {
    }

    static double smooth(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    /** How much of a ridge a column carries (0 off it, 1 on its spine). */
    public static double ridgeness(double ridges) {
        double a = Math.abs(ridges);
        if (a >= RIDGE_WIDTH) return 0;
        double k = 1 - a / RIDGE_WIDTH;
        return Math.pow(k, 1.4);
    }

    /** How deep a crater sinks this column (0 outside one), and the rim's lift (negative depth) just outside it. */
    public static double crater(double pits) {
        if (pits >= CRATER_EDGE) return CRATER_DEPTH * smooth(CRATER_EDGE, CRATER_FULL, pits);
        if (pits > RIM_START) {
            double t = (pits - RIM_START) / (CRATER_EDGE - RIM_START);
            return -RIM_HEIGHT * Math.sin(Math.PI * t);
        }
        return 0;
    }

    /** How deep a dried riverbed cuts this column (0 outside one); flat across the middle, steep banks. */
    public static double riverbed(double river) {
        double a = Math.abs(river);
        if (a >= RIVER_WIDTH) return 0;
        return RIVER_DEPTH * smooth(0, 0.45, 1 - a / RIVER_WIDTH);
    }

    /** The height of the land's top at a column. */
    public static double surface(double hills, double ridges, double pits, double river) {
        double h = BASE + ROLL * hills;
        double r = ridgeness(ridges);
        h += RIDGE_HEIGHT * r;
        // Craters and riverbeds do not cut the ridges' spines (the ridge stands over them).
        double open = 1 - r;
        h -= crater(pits) * open;
        h -= riverbed(river) * open;
        return h;
    }

    /** Positive in the ground (solid), negative in the air. */
    public static double density(int y, double hills, double ridges, double pits, double river, double detail) {
        if (y <= ROCK_Y) return 1.0;
        if (y >= SKY_Y) return -1.0;
        double top = surface(hills, ridges, pits, river);
        double rough = ROUGH + RIDGE_ROUGH * ridgeness(ridges) + 0.3 * Math.min(1, Math.max(0, crater(pits) / CRATER_DEPTH));
        double d = (top - y) / 5.0 + detail * rough;
        return Math.max(-1.0, Math.min(1.0, d));
    }
}
