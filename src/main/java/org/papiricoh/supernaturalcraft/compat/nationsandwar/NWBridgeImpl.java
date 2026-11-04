package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.compat.nationsandwar.client.NWClientCompat;

/** Found by name by {@link NWPresence}: only compiled with {@code -Pnw}. The client methods are never called on a server. */
@SuppressWarnings("unused")
public final class NWBridgeImpl implements NWBridge {
    @Override
    public void init() {
        NWCompat.init();
    }

    @Override
    public void registerClient() {
        NWClientCompat.register();
    }

    @Override
    public @Nullable String myNationName() {
        return NWClientCompat.myNationName();
    }
}
