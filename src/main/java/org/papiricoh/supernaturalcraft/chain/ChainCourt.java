package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A chain's court over the days (server side of {@link Feud}): the interregnum after its Lord dies, the crowning of the heir,
 * and the daily feud — rolled each morning, settled in the records the next morning unless a hunter came upon it
 * ({@link FeudStage}).
 */
public final class ChainCourt {

    private ChainCourt() {
    }

    /** The Lord is gone: the throne is sealed for 3–5 days while the Commanders fight over it. */
    public static void vacate(MinecraftServer server, ChainFaction f, long day) {
        var data = ChainSavedData.get(server);
        var court = data.court(f);
        if (court.vacant()) return;
        court.vacantUntil = day + Feud.interregnumDays(ChainRules.rng(server.overworld().getSeed(), -20 - f.ordinal(), day));
        court.claims.clear();
        data.hierarchy(f).sealThrone(true);
        data.destabilize(f, court.vacantUntil);
        data.log(new ChainSavedData.History(day, "vacant", f, Officer.NONE, "days:" + (court.vacantUntil - day)));
        data.changed();
        announce(server, f, "vacant_news", Long.toString(court.vacantUntil - day));
    }

    /** Ends the interregnum: the heir takes the throne (or, with nobody to take it, the throne waits for a recruit). */
    public static @Nullable Officer crown(MinecraftServer server, ChainFaction f, long day) {
        var data = ChainSavedData.get(server);
        var court = data.court(f);
        Hierarchy h = data.hierarchy(f);
        h.sealThrone(false);
        // The interregnum's unrest ends with it, even when the crown comes early (a lost war's keeps its own date).
        if (data.destabilizedUntil(f) == court.vacantUntil && day < court.vacantUntil) data.destabilize(f, day);
        court.vacantUntil = -1;
        Officer heir = h.lord() != null ? null : Feud.heir(h, court.claims);
        court.claims.clear();
        if (heir != null) {
            heir.rank = ChainRank.LORD;
            heir.log("crowned");
            h.reassignOrphans();
            data.log(new ChainSavedData.History(day, "crowned", f, heir.id, ""));
            announce(server, f, "crowned", ChainNames.name(heir));
        }
        court.lords++;
        data.changed();
        return heir;
    }

    /** Once per world day, after the conclave: settles yesterday's unseen feud, ends a due interregnum, rolls today's feud. */
    public static void daily(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        for (ChainFaction f : ChainFaction.values()) {
            var court = data.court(f);
            // A guild's throne and its quarrels are its hunters' ({@link GuildLife}).
            if (f.guild() || !data.awake(f) || data.headless(f) || court.lastFeudDay >= day) continue;
            if (court.feuding() && !FeudStage.staged(f)) settle(server, f, null);
            if (court.vacant() && day >= court.vacantUntil) crown(server, f, day);
            court.lastFeudDay = day;
            var rng = ChainRules.rng(server.overworld().getSeed(), -30 - f.ordinal(), day);
            if (!FeudStage.staged(f) && Feud.rolls(rng, court.vacant())) {
                Feud.Pair pair = Feud.pick(data.hierarchy(f), court.vacant(), rng);
                // A feud with a post a hunter holds is a challenge they must answer, not a fight the dice can settle.
                if (pair != null && (pair.challenger().boss() || pair.defender().boss())) pair = null;
                if (pair != null && (pair.challenger().held() || pair.defender().held())) {
                    Officer npc = pair.challenger().held() ? pair.defender() : pair.challenger();
                    Officer post = npc == pair.challenger() ? pair.defender() : pair.challenger();
                    if (!npc.held() && ChainLife.pendingFor(data, post.player) == null)
                        ChainLife.issue(server, npc, post.player, false, npc.rank.ordinal() < post.rank.ordinal() ? "post" : "grudge", day);
                    pair = null;
                }
                if (pair != null) {
                    court.feudChallenger = pair.challenger().id;
                    court.feudDefender = pair.defender().id;
                }
            }
            data.changed();
        }
    }

    /** Today's feud of {@code f}, or null (also when one of the two is gone). */
    public static @Nullable Feud.Pair pending(MinecraftServer server, ChainFaction f) {
        var data = ChainSavedData.get(server);
        var court = data.court(f);
        if (!court.feuding()) return null;
        Officer a = data.hierarchy(f).get(court.feudChallenger), b = data.hierarchy(f).get(court.feudDefender);
        if (a == null || b == null || !a.alive() && !b.alive()) {
            court.clearFeud();
            data.changed();
            return null;
        }
        return new Feud.Pair(a, b);
    }

    /** Settles today's feud in the records: by the dice, or with the winner a hunter saw. */
    public static @Nullable Feud.Outcome settle(MinecraftServer server, ChainFaction f, @Nullable Officer forcedWinner) {
        var data = ChainSavedData.get(server);
        var court = data.court(f);
        Feud.Pair pair = pending(server, f);
        court.clearFeud();
        if (pair == null) return null;
        long day = ChainEvents.day(server);
        Officer winner = forcedWinner;
        // A feud one side already left (killed by a hunter) goes to the other.
        if (winner == null && !pair.challenger().alive()) winner = pair.defender();
        if (winner == null && !pair.defender().alive()) winner = pair.challenger();
        Feud.Outcome out = Feud.resolve(data.hierarchy(f), pair, ChainRules.rng(server.overworld().getSeed(), -40 - f.ordinal(), day), day, winner);
        if (court.vacant() && out.winner().alive() && out.winner().rank == ChainRank.COMMANDER) court.claim(out.winner().id);
        data.log(new ChainSavedData.History(day, out.died() ? "feud_death" : out.swapped() ? "feud_usurp" : "feud",
                f, out.winner().id, "rank:" + out.winner().rank.name(), f, out.loser().id));
        // The loser holds it against the winner; a winner who held a grudge has had its satisfaction.
        Bonds.Actor w = Bonds.Actor.of(out.winner()), l = Bonds.Actor.of(out.loser());
        data.bonds().adjust(l, w, Bonds.LOST_FEUD);
        if (data.bonds().regard(w, l) < 0) data.bonds().adjust(w, l, -Bonds.LOST_FEUD);
        data.changed();
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (out.winner().killsOf(p.getUUID()) > 0 || out.loser().killsOf(p.getUUID()) > 0)
                ChainEvents.notice(p, "feud_news", ChainNames.name(out.winner()), ChainNames.name(out.loser()));
        return out;
    }

    /** Tells the hunters this chain has marked. */
    private static void announce(MinecraftServer server, ChainFaction f, String key, String arg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (ChainMark.get(p).heat(f) > 0) ChainEvents.notice(p, key, arg);
    }
}
