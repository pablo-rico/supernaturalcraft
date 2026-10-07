package org.papiricoh.supernaturalcraft.client.hell;

import net.minecraft.util.Mth;

/** This player's Torment as the server last told it, eased so the visions swell and ebb rather than jump. */
public final class ClientTorment {

    private static float target, shown;

    private ClientTorment() {
    }

    public static void set(float value) {
        target = Mth.clamp(value, 0f, 1f);
    }

    /** Called once a client tick. */
    static void tick() {
        shown += (target - shown) * 0.05f;
        if (Math.abs(target - shown) < 0.001f) shown = target;
    }

    public static float value() {
        return shown;
    }
}
