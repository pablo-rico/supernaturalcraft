package org.papiricoh.supernaturalcraft.client.colt;

import org.papiricoh.supernaturalcraft.reward.ColtItem;

import java.util.HashMap;
import java.util.Map;

/**
 * The angle of each Colt's cylinder, eased a fifth of a turn whenever its chamber changes, so the
 * model and the HUD turn it the same way. Always forward: chamber 4 to 0 is one more step.
 */
public final class ColtCylinder {

    public static final float STEP = 360f / ColtItem.CAPACITY, TURN_TICKS = 4;

    private static final class Turn {
        int chamber;
        double start;
        float from, to;
    }

    private static final Map<Long, Turn> TURNS = new HashMap<>();

    private ColtCylinder() {
    }

    /** Degrees the cylinder of gun {@code geoId} stands at, its chamber being {@code chamber} at time {@code now} (ticks). */
    public static float angle(long geoId, int chamber, double now) {
        Turn t = TURNS.get(geoId);
        if (t == null) {
            t = new Turn();
            t.chamber = chamber;
            t.from = t.to = chamber * STEP;
            t.start = now - TURN_TICKS;
            TURNS.put(geoId, t);
        }
        if (chamber != t.chamber) {
            int steps = Math.floorMod(chamber - t.chamber, ColtItem.CAPACITY);
            t.from = current(t, now);
            t.to = t.to + steps * STEP;
            t.chamber = chamber;
            t.start = now;
        }
        return current(t, now);
    }

    private static float current(Turn t, double now) {
        double p = Math.min(1, Math.max(0, (now - t.start) / TURN_TICKS));
        return (float) (t.from + (t.to - t.from) * easeOutBack(p));
    }

    static double easeOutBack(double x) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }
}
