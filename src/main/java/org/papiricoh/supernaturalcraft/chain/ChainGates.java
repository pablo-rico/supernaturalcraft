package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.angel.HostsSavedData;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.ArrayList;
import java.util.List;

/**
 * When each chain of command wakes. The Pit wakes once any hunter has slain Azazel; the Host once the angels walk the Earth
 * (after Lilith, {@link HostsSavedData#angels()}); angels may Fall only once the fallen do (after Metatron). A sleeping chain
 * recruits nobody, holds no conclave and sends no one. On waking, a chain is founded with a full staff of officers, so a world
 * that is already past those bosses finds its chains at once.
 */
public final class ChainGates {

    /** The founding staff: one Lord, then Lieutenants, Captains and Soldiers. */
    private static final int LIEUTENANTS = 2, CAPTAINS = 3, SOLDIERS = 4;

    private ChainGates() {
    }

    public static boolean fallenOpen(MinecraftServer server) {
        return HostsSavedData.get(server).fallen();
    }

    /** Wakes whichever chain is due (called once a second while players are online; cheap once both are awake). */
    static void check(MinecraftServer server) {
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        ChainSavedData data = ChainSavedData.get(server);
        if (!data.awake(ChainFaction.DEMON) && anyDone(server, BossProgression.Boss.AZAZEL)) wake(server, ChainFaction.DEMON);
        if (!data.awake(ChainFaction.ANGEL) && (HostsSavedData.get(server).angels() || anyDone(server, BossProgression.Boss.LILITH)))
            wake(server, ChainFaction.ANGEL);
    }

    /** Wakes a chain and, if it has nobody yet, founds it. */
    public static void wake(MinecraftServer server, ChainFaction faction) {
        ChainSavedData data = ChainSavedData.get(server);
        data.setAwake(faction, true);
        Hierarchy h = data.hierarchy(faction);
        if (!h.alive().isEmpty()) return;
        long day = ChainEvents.day(server);
        var rng = ChainRules.rng(server.overworld().getSeed(), -20 - faction.ordinal(), day);
        Conclave.RecruitSource source = ChainEvents.recruits(rng);
        Officer lord = found(h, ChainRank.LORD, Officer.NONE, rng, source);
        if (lord == null) return;
        List<Officer> lieutenants = new ArrayList<>(), captains = new ArrayList<>();
        for (int i = 0; i < LIEUTENANTS; i++) add(lieutenants, found(h, ChainRank.LIEUTENANT, lord.id, rng, source));
        for (int i = 0; i < CAPTAINS; i++) add(captains, found(h, ChainRank.CAPTAIN, lieutenants.get(i % lieutenants.size()).id, rng, source));
        for (int i = 0; i < SOLDIERS; i++) found(h, ChainRank.SOLDIER, captains.get(i % captains.size()).id, rng, source);
        h.reassignOrphans();
        data.log(new ChainSavedData.History(day, "founded", faction, lord.id, ""));
        data.changed();
    }

    private static Officer found(Hierarchy h, ChainRank rank, int superior, org.papiricoh.supernaturalcraft.legacy.gen.GenSeed.Rng rng,
                                 Conclave.RecruitSource source) {
        Conclave.Recruit r = source.recruit(h.faction(), ChainFaction.Branch.LOYAL);
        Officer o = h.recruit(rank, superior, rng.nextLong(), r.entityType(), r.variant());
        if (o != null) o.level = rank.ordinal() * 2;
        return o;
    }

    private static void add(List<Officer> to, Officer o) {
        if (o != null) to.add(o);
    }

    private static boolean anyDone(MinecraftServer server, BossProgression.Boss boss) {
        AdvancementHolder adv = server.getAdvancements().get(SupernaturalCraft.asResource(boss.advancement));
        if (adv == null) return false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.getAdvancements().getOrStartProgress(adv).isDone()) return true;
        return false;
    }
}
