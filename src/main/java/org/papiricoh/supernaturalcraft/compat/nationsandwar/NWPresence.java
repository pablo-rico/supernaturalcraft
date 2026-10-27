package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.neoforged.fml.ModList;
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
     * Held off by the GameTest server, so the rest of the suite tests SupernaturalCraft alone even with Nations &amp; War
     * loaded ({@code -Pnw}); the bridge's own tests switch it on for their run.
     */
    private static volatile boolean suspended;

    /** Loaded, switched on in the server config ({@code nationsandwar.enabled}) and not held off by the tests. */
    public static boolean active() {
        return LOADED && !suspended && SNConfig.SPEC.isLoaded() && SNConfig.NW_ENABLED.get();
    }

    public static void suspend(boolean held) {
        suspended = held;
    }
}
