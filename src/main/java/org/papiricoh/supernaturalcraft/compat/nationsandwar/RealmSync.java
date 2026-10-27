package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.network.ChainRealmPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.nationsandwar.api.NationsAndWarApi;
import org.papiricoh.nationsandwar.api.WarApi;
import org.papiricoh.nationsandwar.model.Nation;
import org.papiricoh.nationsandwar.model.Politics;
import org.papiricoh.nationsandwar.model.Town;
import org.papiricoh.nationsandwar.war.War;

import java.util.ArrayList;
import java.util.List;

/** Sends the book the dominions, the guild-nations and the chains' Nations &amp; War wars ({@link ChainRealmPayload}) when they change. */
final class RealmSync {
    private static ChainRealmPayload last = ChainRealmPayload.SECTORS;

    private RealmSync() {
    }

    static ChainRealmPayload build(MinecraftServer server) {
        if (!NWPresence.active()) return ChainRealmPayload.SECTORS;
        Politics pol = NationsAndWarApi.politics(server);
        List<War> wars = WarApi.wars(server);
        List<ChainRealmPayload.Dominion> dominions = new ArrayList<>();
        for (ChainFaction f : ChainFaction.FIXED) {
            for (Town t : PowerNations.towns(server, f)) {
                BlockPos c = PowerNations.centre(t);
                boolean contested = wars.stream().anyMatch(w -> w.live() && (w.sieges.stream().anyMatch(s -> s.town.equals(t.id)) || w.coreOpen.contains(t.id)));
                dominions.add(new ChainRealmPayload.Dominion(f.id(), t.name, c.getX(), c.getZ(), PowerNations.isSeat(t), contested));
            }
        }
        var data = ChainSavedData.get(server);
        List<ChainRealmPayload.GuildNation> nations = new ArrayList<>();
        for (ChainFaction slot : ChainFaction.GUILDS) {
            if (!GuildNations.linked(data, slot)) continue;
            Nation n = GuildNations.nationOf(server, slot);
            if (n != null) nations.add(new ChainRealmPayload.GuildNation(slot.id(), n.name, n.color, pol.citizens(n).size()));
        }
        List<ChainRealmPayload.RealmWar> out = new ArrayList<>();
        for (War w : wars) {
            if (!w.live()) continue;
            Nation a = pol.nations.get(w.attacker), b = pol.nations.get(w.defender);
            ChainFaction pa = PowerNations.powerOf(a), pb = PowerNations.powerOf(b);
            boolean ga = GuildNations.slotOf(server, a) != null, gb = GuildNations.slotOf(server, b) != null;
            if (pa == null && pb == null && !(ga && gb)) continue;
            String kind = pa != null && pb != null ? "powers" : pa != null ? "incursion" : pb != null ? "assault" : "guilds";
            out.add(new ChainRealmPayload.RealmWar(label(a, pa), label(b, pb), kind, w.phase.name(), w.scoreAttackers, w.scoreDefenders));
        }
        return new ChainRealmPayload(false, dominions, nations, out);
    }

    private static String label(Nation n, ChainFaction power) {
        return power != null ? power.id() : n == null ? "?" : n.name;
    }

    static void tick(MinecraftServer server) {
        ChainRealmPayload now = build(server);
        if (now.equals(last)) return;
        last = now;
        PacketDistributor.sendToAllPlayers(now);
    }

    static void send(ServerPlayer p) {
        Send.toPlayer(p, build(p.server));
    }

    static void reset() {
        last = ChainRealmPayload.SECTORS;
    }
}
