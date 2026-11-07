package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.chain.Bonds;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Hierarchy;
import org.papiricoh.supernaturalcraft.chain.Officer;

/**
 * The chains of command hear about the seals (v0.40): an angel who holds one wins a little regard ({@link SealRules#HOST_REGARD})
 * with every officer of the Host; a demon who lets one break under their nose, with every officer of the Pit. Through the
 * bonds ({@code ChainSavedData.bonds()}), the same regard their talks read ({@code ChainTalk.regard}). Nothing while the chain
 * sleeps.
 */
public final class SealRegard {

    private SealRegard() {
    }

    public static void host(ServerPlayer p) {
        add(p, ChainFaction.ANGEL, SealRules.HOST_REGARD);
    }

    public static void pit(ServerPlayer p) {
        add(p, ChainFaction.DEMON, SealRules.PIT_REGARD);
    }

    /** {@code amount} regard from every living officer of {@code faction} (not the posts hunters hold). @return how many */
    public static int add(ServerPlayer p, ChainFaction faction, int amount) {
        ChainSavedData data = ChainSavedData.get(p.server);
        if (!data.awake(faction)) return 0;
        Hierarchy h = data.hierarchy(faction);
        if (h == null) return 0;
        Bonds.Actor me = Bonds.Actor.of(p.getUUID());
        int n = 0;
        for (Officer o : h.npcs()) {
            data.bonds().adjust(Bonds.Actor.of(o), me, amount);
            n++;
        }
        if (n > 0) data.changed();
        return n;
    }
}
