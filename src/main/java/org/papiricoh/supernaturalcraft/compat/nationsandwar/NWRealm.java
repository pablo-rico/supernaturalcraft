package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.realm.Realm;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.nationsandwar.api.WarApi;
import org.papiricoh.nationsandwar.model.Nation;
import org.papiricoh.nationsandwar.model.Town;
import org.papiricoh.nationsandwar.war.War;

import java.util.List;
import java.util.Random;

/**
 * The chains' ground with Nations &amp; War: no ring of sectors, but each power's seat and enclaves and each guild-nation's
 * towns. Officers roam out from those towns; the war between the powers is a Nations &amp; War war fought over enclaves;
 * guilds take no ground of their own (their nation's is theirs) and their stances follow their nations.
 */
public final class NWRealm implements Realm {
    public static final NWRealm INSTANCE = new NWRealm();
    /** True while the chain's war is being closed from here (so the Nations &amp; War side does not close it twice). */
    static boolean settling;

    private NWRealm() {
    }

    @Override
    public boolean sectors() {
        return false;
    }

    @Override
    public SeatSite.Site roam(MinecraftServer server, Officer o, SeatSite.Site seat) {
        ChainFaction camp = o.camp();
        List<Town> towns = camp.guild() ? GuildNations.towns(server, camp) : PowerNations.towns(server, camp);
        if (o.rank.seated() || towns.isEmpty()) return seat;
        Random rng = new Random(server.overworld().getSeed() * 13 + o.faction.ordinal() * 7919L + o.id * 104729L);
        Town home = towns.get(rng.nextInt(towns.size()));
        BlockPos c = PowerNations.centre(home);
        int reach = switch (o.rank) {
            case SOLDIER -> 48;
            case CAPTAIN -> 128;
            default -> 256;
        };
        return new SeatSite.Site(c.getX() + rng.nextInt(reach * 2 + 1) - reach, c.getZ() + rng.nextInt(reach * 2 + 1) - reach);
    }

    @Override
    public boolean admit(MinecraftServer server, ChainFaction f) {
        if (f.guild()) return false;
        return PowerNations.ensure(server, f);
    }

    @Override
    public int release(MinecraftServer server, ChainFaction f) {
        return f.guild() ? 0 : PowerNations.razeEnclaves(server, f);
    }

    @Override
    public void clash(MinecraftServer server, Officer winner, Officer loser, ChainFaction side, int points, long day) {
        // No sectors to press: a clash scores in the war (scored) and wins prestige, as before.
    }

    @Override
    public boolean warPair(ChainFaction a, ChainFaction b) {
        return !a.guild() && !b.guild();
    }

    @Override
    public void warDeclared(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        Nation a = PowerNations.nation(server, data.warSides[0]), b = PowerNations.nation(server, data.warSides[1]);
        if (a == null || b == null) return;
        long ticks = Math.max(1, data.warUntil - day + 1) * 24000L;
        WarApi.declare(server, a.id, b.id, WarApi.WarOptions.free().times(0, ticks).noCooldown().reason("supernaturalcraft.war.powers"));
    }

    @Override
    public void scored(MinecraftServer server, ChainFaction side, int points) {
        War w = powersWar(server);
        Nation n = PowerNations.nation(server, side);
        if (w != null && n != null) WarApi.addScore(server, w, n.id, points);
    }

    @Override
    public String warSettled(MinecraftServer server, @Nullable ChainFaction winner, @Nullable ChainFaction loser, long day) {
        War w = powersWar(server);
        boolean anyFell = false;
        if (w != null && w.live()) {
            settling = true;
            try {
                Nation win = winner == null ? null : PowerNations.nation(server, winner);
                War.Outcome o = win == null ? War.Outcome.WHITE_PEACE : w.attacker.equals(win.id) ? War.Outcome.ATTACKERS : War.Outcome.DEFENDERS;
                anyFell = !w.fallen.isEmpty();
                WarApi.end(server, w, o);
            } finally {
                settling = false;
            }
        }
        if (winner == null || loser == null || anyFell) return "";
        // The winner takes ground even without a siege won: the loser's enclave nearest the winner's seat.
        String name = PowerNations.transferEnclave(server, loser, winner);
        return name == null ? "" : "enclave:" + name;
    }

    /** The live Nations &amp; War war of the chain's own war (the two powers in {@code warSides}), or null. */
    static @Nullable War powersWar(MinecraftServer server) {
        var data = ChainSavedData.get(server);
        // By its sides only: the chain's war may already be closed on its side (endWar clears warUntil before settling).
        if (data.warSides[0] == null || data.warSides[1] == null) return null;
        Nation a = PowerNations.nation(server, data.warSides[0]), b = PowerNations.nation(server, data.warSides[1]);
        if (a == null || b == null) return null;
        for (War w : WarApi.wars(server)) if (w.live() && w.enemies(a.id, b.id)) return w;
        return null;
    }

    @Override
    public @Nullable String standardRefusal(ServerPlayer p, BlockPos at, @Nullable ChainFaction slot) {
        return GuildNations.standardRefusal(p, at, slot);
    }

    @Override
    public boolean ringRules() {
        return false;
    }

    @Override
    public boolean stanceLocked(MinecraftServer server, ChainFaction a, ChainFaction b) {
        var data = ChainSavedData.get(server);
        return a.guild() && b.guild() && GuildNations.linked(data, a) && GuildNations.linked(data, b);
    }

    @Override
    public void guildFounded(MinecraftServer server, ChainFaction slot, ServerPlayer founder) {
        GuildNations.link(server, slot, founder);
    }

    @Override
    public void standardPlanted(MinecraftServer server, ChainFaction slot, ServerPlayer p, BlockPos at) {
        GuildNations.standardPlanted(server, slot, p, at);
    }

    @Override
    public void guildDissolving(MinecraftServer server, ChainFaction slot) {
        GuildNations.unlink(server, slot);
    }

    @Override
    public boolean oathOpen(ServerPlayer p, ChainFaction slot) {
        return GuildNations.oathOpen(p, slot);
    }

    @Override
    public @Nullable String inviteRefusal(MinecraftServer server, ChainFaction slot, ServerPlayer target) {
        return GuildNations.inviteRefusal(server, slot, target);
    }
}
