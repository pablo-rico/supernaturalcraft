package org.papiricoh.supernaturalcraft.chain.realm;

import net.minecraft.server.MinecraftServer;

import java.util.function.Function;

/** The realm in force: the sectors unless a compat bridge installed another one (and it is on for this server). */
public final class ChainRealms {
    private static Function<MinecraftServer, Realm> provider = s -> SectorRealm.INSTANCE;

    private ChainRealms() {
    }

    public static Realm of(MinecraftServer server) {
        Realm r = provider.apply(server);
        return r == null ? SectorRealm.INSTANCE : r;
    }

    /** A bridge's realm ({@code null} from the function: the sectors, e.g. while its config is off). */
    public static void install(Function<MinecraftServer, Realm> realm) {
        provider = realm;
    }
}
