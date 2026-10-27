package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.server.MinecraftServer;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.nationsandwar.api.ManagedNationProvider;
import org.papiricoh.nationsandwar.model.Nation;
import org.papiricoh.nationsandwar.model.Town;
import org.papiricoh.nationsandwar.war.Treaty;
import org.papiricoh.nationsandwar.war.War;

/**
 * What Nations &amp; War asks a power (the Pit, the Host, SucroCorp): only a guild-nation hostile to it may declare on it (the
 * assault on its seat); when it wins it takes tribute and a vassal; what it loses is an enclave razed (to mortals) or handed
 * over (to the other power), never its seat.
 */
final class PowerProvider implements ManagedNationProvider {
    private final ChainFaction faction;

    PowerProvider(ChainFaction faction) {
        this.faction = faction;
    }

    @Override
    public String key() {
        return PowerNations.key(faction);
    }

    @Override
    public boolean mayBeDeclaredOn(MinecraftServer server, Nation self, Nation attacker) {
        if (!NWPresence.active() || self.id.equals(attacker.overlord)) return false;
        ChainFaction slot = GuildNations.slotOf(server, attacker);
        return slot != null && ChainSavedData.get(server).hostile(slot, faction);
    }

    @Override
    public Treaty termsWhenWinning(MinecraftServer server, War war, Nation self, Nation loser) {
        Treaty t = new Treaty();
        t.tribute = (int) Math.floor(Math.max(0, loser.bank) * SNConfig.NW_TRIBUTE_SHARE.get());
        t.vassalDays = loser.managed() ? 0 : SNConfig.NW_VASSAL_DAYS.get();
        return t;
    }

    @Override
    public FallenTown onFallen(MinecraftServer server, War war, Nation self, Town town, Nation winner) {
        if (PowerNations.isSeat(town)) return FallenTown.KEEP;
        if (winner.managed()) {
            // Between the powers an enclave changes hands: its shrine is rebuilt for its new master.
            PowerNations.ruin(server, town);
            town.ext.putBoolean(PowerNations.BUILT, false);
            town.color = winner.color;
            return FallenTown.TRANSFER;
        }
        PowerNations.ruin(server, town);
        return FallenTown.RAZE;
    }
}
