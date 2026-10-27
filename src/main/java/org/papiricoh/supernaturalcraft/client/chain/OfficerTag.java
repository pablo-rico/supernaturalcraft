package org.papiricoh.supernaturalcraft.client.chain;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;

/** Reads the officer id a mob syncs, which carries a guild's slot above its own bits (v0.29; {@code chain.OfficerSpawner#syncedId}). */
final class OfficerTag {
    private OfficerTag() {
    }

    /** The entry a synced id names ({@code plain}: the faction of a mob of the world's chains), or null. */
    static @Nullable Entry entry(int synced, String plain) {
        if (synced < 0) return null;
        int slot = synced >>> org.papiricoh.supernaturalcraft.chain.OfficerSpawner.GUILD_SHIFT;
        if (slot == 0) return ClientChain.entry(plain, synced);
        if (slot > ChainFaction.values().length) return null;
        return ClientChain.entry(ChainFaction.values()[slot - 1].id(), synced & ((1 << org.papiricoh.supernaturalcraft.chain.OfficerSpawner.GUILD_SHIFT) - 1));
    }
}
