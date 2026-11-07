package org.papiricoh.supernaturalcraft.entity.boss.shadow;

/**
 * The Shadow's numbers (v0.41, pure, tested by {@code ShadowBalanceTest}). It was asleep; now it wants everyone to sleep again.
 * Three phases of a third of its health, a tendril more each (the renderer shows {@code tendril_n} by phase):
 * <ol>
 *   <li><b>Woken</b>: a tendril whipped forward (a violet lane), one that seizes and drags (a violet lane), the lull (a violet
 *   circle: drowsiness, slowness and blindness).</li>
 *   <li><b>Angry</b>: it sinks and rises behind a hunter, and the tar surges out of it (a violet cone).</li>
 *   <li><b>The Empty itself</b>: three tendrils at once, the lull wider, the surge round it all (a violet ring).</li>
 * </ol>
 * It answers to nothing holy more than to anything else: the Empty is older than Heaven and Hell.
 */
public final class ShadowBalance {

    public static final int PHASES = 3;
    public static final int ARENA_RADIUS = 22;

    private ShadowBalance() {
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 30;
            default -> 24;
        };
    }

    public static final float LASH_LENGTH = 14f, LASH_WIDTH = 1.4f, LASH_DAMAGE = 13f, LASH_SPREAD = 25f;
    public static final int LASH_WINDUP = 18;
    public static final float GRAB_LENGTH = 16f, GRAB_WIDTH = 1.2f, GRAB_DAMAGE = 8f, GRAB_PULL = 1.8f;
    public static final int GRAB_WINDUP = 22;
    public static final float LULL_RADIUS = 4f, LULL_RADIUS_LAST = 6f, LULL_DAMAGE = 4f;
    public static final int LULL_WINDUP = 34, LULL_TICKS = 80;
    public static final float SURGE_REACH = 7f, SURGE_DAMAGE = 14f;
    public static final int SURGE_WINDUP = 26;
    public static final float ENGULF_RADIUS = 9f, ENGULF_DAMAGE = 16f;
    public static final int ENGULF_WINDUP = 40;
    public static final int SINK_WINDUP = 16;
}
