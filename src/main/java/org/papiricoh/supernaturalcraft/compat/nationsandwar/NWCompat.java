package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.supernaturalcraft.chain.realm.ChainRealms;
import org.papiricoh.nationsandwar.api.ClaimVetoes;
import org.papiricoh.nationsandwar.api.ProtectionBypass;
import org.papiricoh.nationsandwar.api.WarApi;

/**
 * Nations &amp; War as an optional dependency (v0.32): the only door into its API. Called from the mod's constructor when
 * {@link NWPresence#LOADED}: registers the powers' providers, the combatant resolver, the entities no protection stops, the
 * reserved seats, the realm of dominions (while {@code nationsandwar.enabled}) and the listeners.
 */
public final class NWCompat {
    private NWCompat() {
    }

    public static void init() {
        PowerNations.registerProviders();
        WarApi.registerCombatant(NWWars::combatant);
        ProtectionBypass.exemptEntities(ArenaBypass::exempt);
        ClaimVetoes.register(PowerNations::reserved);
        ChainRealms.install(server -> NWPresence.active() ? NWRealm.INSTANCE : null);
        NeoForge.EVENT_BUS.register(NWListeners.class);
    }
}
