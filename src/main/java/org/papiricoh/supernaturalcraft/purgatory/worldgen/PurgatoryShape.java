package org.papiricoh.supernaturalcraft.purgatory.worldgen;

/**
 * The arithmetic of Purgatory's land (see {@link PurgatoryTerrain}), kept free of Minecraft types so the tests can run it without
 * a game. Four noise samples come in (each about -1..1, from the world's seed): {@code hills} rolls the land and sinks the
 * hollows, {@code cliffs} raises it in sudden terraces, {@code ravine} cuts narrow gorges where it is near zero, and {@code detail}
 * (3D) roughens the cliffs into overhangs.
 */
public final class PurgatoryShape {

    /** The land's middle height and how far the hills lift and drop it. */
    public static final double BASE = 72, ROLL = 14;
    /** Below this {@code hills} the land sinks into a mist-filled hollow (the mire), this much deeper per unit. */
    public static final double HOLLOW = -0.42, HOLLOW_DEPTH = 36;
    /** The cliffs: two terraces, each this tall, where {@code cliffs} crosses their edges. */
    public static final double CLIFF_LOW = 0.18, CLIFF_HIGH = 0.48, CLIFF_STEP = 10, CLIFF_EDGE = 0.035;
    /** The ravines: where |ravine| is under this, a gorge this deep at its middle. */
    public static final double RAVINE_WIDTH = 0.045, RAVINE_DEPTH = 26;
    /** Deep rock is always solid, high sky always air. */
    public static final int ROCK_Y = 24, SKY_Y = 150;
    /** How much the 3D detail moves a cliff face. */
    public static final double OVERHANG = 0.55;

    private PurgatoryShape() {
    }

    static double smooth(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    /** How much of a cliff a column carries (0 flat, 1 on the high terrace; in between on a face). */
    public static double cliffness(double cliffs) {
        return 0.5 * smooth(CLIFF_LOW - CLIFF_EDGE, CLIFF_LOW + CLIFF_EDGE, cliffs)
                + 0.5 * smooth(CLIFF_HIGH - CLIFF_EDGE, CLIFF_HIGH + CLIFF_EDGE, cliffs);
    }

    /** The height of the land's top at a column (before the ravines). */
    public static double ground(double hills, double cliffs) {
        double h = BASE + ROLL * hills;
        if (hills < HOLLOW) h -= (HOLLOW - hills) * HOLLOW_DEPTH;
        h += 2 * CLIFF_STEP * cliffness(cliffs);
        return h;
    }

    /** How deep a ravine cuts this column (0 outside one). */
    public static double gorge(double ravine) {
        double a = Math.abs(ravine);
        if (a >= RAVINE_WIDTH) return 0;
        double k = 1 - a / RAVINE_WIDTH;
        return RAVINE_DEPTH * Math.sqrt(k);
    }

    /** The land's top after the ravines. */
    public static double surface(double hills, double cliffs, double ravine) {
        return ground(hills, cliffs) - gorge(ravine);
    }

    /** Positive in the ground (solid), negative in the air. */
    public static double density(int y, double hills, double cliffs, double ravine, double detail) {
        if (y <= ROCK_Y) return 1.0;
        if (y >= SKY_Y) return -1.0;
        double top = surface(hills, cliffs, ravine);
        double c = cliffness(cliffs);
        // On a cliff's face (c between its terraces), the 3D detail pushes the face in and out: overhangs and ledges.
        double face = 1 - Math.abs(2 * (c * 2 % 1) - 1);
        double d = (top - y) / 6.0 + detail * OVERHANG * face;
        return Math.max(-1.0, Math.min(1.0, d));
    }
}
