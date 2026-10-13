package org.papiricoh.supernaturalcraft.client.allegiance;

/**
 * The power wheel's measures (pure, v0.20): up to eight slices it keeps its old size; with more (a General has 13) it grows
 * so every 24 px icon still has room, the icons riding further out on the ring. Angles run clockwise from the top.
 */
public record WheelLayout(float outer, float inner, float iconR, float costScale) {

    /** Up to this many slices the wheel keeps its first size. */
    public static final int COMPACT = 8;
    public static final int ICON = 24;
    /** The ring of {@code wheel.png} runs from r 52 to 120 of its 120: where the art's inner edge falls. */
    public static final float ART_INNER = 52f / 120f;

    public static WheelLayout of(int n) {
        return n <= COMPACT ? new WheelLayout(82, 30, 56, 0.75f) : new WheelLayout(108, 38, 80, 0.65f);
    }

    /** Where the ring starts (the art's inner edge when the art is there). */
    public float ringInner(boolean art) {
        return art ? ART_INNER * outer : inner;
    }

    /** The slice under a point {@code (dx, dy)} from the centre (screen y down), or -1 in the hub. */
    public int slice(double dx, double dy, int n) {
        if (n <= 0 || dx * dx + dy * dy < inner * inner * 0.6) return -1;
        double a = Math.atan2(dx, -dy);
        if (a < 0) a += Math.PI * 2;
        double step = Math.PI * 2 / n;
        return (int) Math.floor((a + step / 2) / step) % n;
    }

    /** The middle of slice {@code i}, in radians. */
    public static float mid(int i, int n) {
        return (float) (i * Math.PI * 2 / Math.max(1, n));
    }

    /** The room between two neighbouring icons' centres. */
    public float chord(int n) {
        return n <= 1 ? Float.MAX_VALUE : (float) (2 * iconR * Math.sin(Math.PI / n));
    }
}
