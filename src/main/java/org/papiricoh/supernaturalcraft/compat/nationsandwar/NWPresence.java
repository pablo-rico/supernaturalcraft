package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.papiricoh.supernaturalcraft.SNConfig;

/**
 * Whether Nations &amp; War is here (no import of it: safe to ask from anywhere). Everything else in this package touches its
 * API and must only run when {@link #LOADED}.
 */
public final class NWPresence {
    public static final String MODID = "nationsandwar";
    public static final boolean LOADED = ModList.get() != null && ModList.get().isLoaded(MODID);

    private NWPresence() {
    }

    /**
     * Held off on the GameTest server only, so the rest of the suite tests SupernaturalCraft alone even with Nations &amp; War
     * loaded; the bridge's own tests switch it on for their run. Dev clients and servers also register the GameTests, so
     * this must never be decided by the generator (it once left the bridge off, and the sectors' ring on, in every dev world).
     */
    private static volatile boolean suspended = GameTestHooks.isGametestServer();

    /** Loaded, switched on in the server config ({@code nationsandwar.enabled}) and not held off by the tests. */
    public static boolean active() {
        return LOADED && !suspended && SNConfig.SPEC.isLoaded() && SNConfig.NW_ENABLED.get();
    }

    public static boolean suspended() {
        return suspended;
    }

    public static void suspend(boolean held) {
        suspended = held;
    }
}
