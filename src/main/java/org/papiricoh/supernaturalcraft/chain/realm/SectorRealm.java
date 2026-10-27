package org.papiricoh.supernaturalcraft.chain.realm;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainLife;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Front;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.Territory;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;

/** The ring of 24 sectors around the Pit ({@link Front}, v0.27): the realm of every world without Nations &amp; War. */
public final class SectorRealm implements Realm {
    public static final SectorRealm INSTANCE = new SectorRealm();

    private SectorRealm() {
    }

    @Override
    public boolean sectors() {
        return true;
    }

    @Override
    public SeatSite.Site roam(MinecraftServer server, Officer o, SeatSite.Site seat) {
        return Territory.of(server.overworld().getSeed(), o, seat, ChainLife.front(server));
    }

    @Override
    public boolean admit(MinecraftServer server, ChainFaction f) {
        Front front = ChainLife.front(server);
        return front.held(f) == 0 && front.admit(f, SeatWorld.site(server, f)) > 0;
    }

    @Override
    public int release(MinecraftServer server, ChainFaction f) {
        return ChainLife.front(server).release(f);
    }

    @Override
    public void clash(MinecraftServer server, Officer winner, Officer loser, ChainFaction side, int points, long day) {
        var data = ChainSavedData.get(server);
        Front front = ChainLife.front(server);
        var site = ChainLife.territory(server, loser);
        int sector = Front.sector(site.x(), site.z());
        ChainFaction held = front.owner(sector);
        if (front.push(sector, side, points)) {
            data.log(new ChainSavedData.History(day, "sector_taken", winner.faction, winner.id, "sector:" + (sector + 1)));
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ChainFaction mine = ChainLife.sworn(p);
                if (mine == side) ChainEvents.notice(p, "sector_taken", Integer.toString(sector + 1));
                else if (mine == held) ChainEvents.notice(p, "sector_lost", Integer.toString(sector + 1));
            }
        }
    }

    @Override
    public String warSettled(MinecraftServer server, @Nullable ChainFaction winner, @Nullable ChainFaction loser, long day) {
        if (winner == null || loser == null) return "";
        Front front = ChainLife.front(server);
        int sector = front.border(loser, winner, (int) day);
        if (sector < 0) sector = front.border(loser, (int) day);
        if (sector >= 0 && front.held(loser) > 1) front.set(sector, winner);
        return sector >= 0 ? "sector:" + (sector + 1) : "";
    }
}
