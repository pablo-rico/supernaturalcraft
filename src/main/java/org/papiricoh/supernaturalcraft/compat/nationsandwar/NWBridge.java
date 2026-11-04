package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import org.jetbrains.annotations.Nullable;

/**
 * The way into the Nations &amp; War bridge from the rest of the mod (no import of it). The bridge is only compiled with
 * {@code -Pnw} (the jar in {@code libs/}); without it {@link NWPresence#bridge()} is null and Nations &amp; War is ignored even if
 * installed. The implementation is {@code NWBridgeImpl}, found by name.
 */
public interface NWBridge {
    /** Common setup: the bridge's listeners (see {@code NWCompat.init}). */
    void init();

    /** Client setup, on the main thread (see {@code NWClientCompat.register}). Client only. */
    void registerClient();

    /** The nation the local hunter is a citizen of, or null. Client only. */
    @Nullable String myNationName();
}
