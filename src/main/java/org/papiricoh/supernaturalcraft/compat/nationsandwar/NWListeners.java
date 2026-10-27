package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.nationsandwar.api.event.AllianceEvent;
import org.papiricoh.nationsandwar.api.event.NationEvent;
import org.papiricoh.nationsandwar.api.event.TownEvent;
import org.papiricoh.nationsandwar.api.event.WarEvent;

/** The bridge's listeners on {@code NeoForge.EVENT_BUS} (registered by {@link NWCompat#init} only when Nations &amp; War is here). */
public final class NWListeners {
    private static long lastDay = -1;

    private NWListeners() {
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        PowerNations.reserve(event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PowerNations.forget();
        ArenaBypass.stopped();
        RealmSync.reset();
        lastDay = -1;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        if (t % 20 == 0) ArenaBypass.tick(server);
        if (!NWPresence.active() || !SNConfig.CHAIN_ENABLED.get()) return;
        if (t % 100 == 0) {
            var data = ChainSavedData.get(server);
            for (ChainFaction f : ChainFaction.FIXED) if (data.awake(f) && !data.headless(f)) PowerNations.ensure(server, f);
            PowerNations.buildPending(server);
            GuildNations.audit(server);
            grantAdvancement(server);
        }
        if (t % 40 == 0) RealmSync.tick(server);
        if (t % 600 == 300) NWWars.warParties(server);
        long day = ChainEvents.day(server);
        if (day != lastDay) {
            if (lastDay >= 0) NWWars.dawn(server, day);
            lastDay = day;
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) RealmSync.send(p);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        NWWars.death(event);
    }

    // --- Nations & War's events: membership, alliances and wars move the guild-nations ---------------

    @SubscribeEvent
    public static void residentLeft(TownEvent.ResidentLeft event) {
        audit(event.server());
    }

    @SubscribeEvent
    public static void townRemoved(TownEvent.Removed event) {
        PowerNations.ruin(event.server(), event.town());
        audit(event.server());
    }

    @SubscribeEvent
    public static void nationRemoved(NationEvent.Removed event) {
        audit(event.server());
    }

    @SubscribeEvent
    public static void leaderChanged(NationEvent.LeaderChanged event) {
        audit(event.server());
    }

    @SubscribeEvent
    public static void townLeft(NationEvent.TownLeft event) {
        audit(event.server());
    }

    @SubscribeEvent
    public static void allianceJoined(AllianceEvent.Joined event) {
        audit(event.server());
    }

    @SubscribeEvent
    public static void allianceLeft(AllianceEvent.Left event) {
        audit(event.server());
    }

    @SubscribeEvent
    public static void warStarted(WarEvent.Started event) {
        audit(event.server());
    }

    @SubscribeEvent
    public static void coreOpened(WarEvent.CoreOpened event) {
        if (NWPresence.active()) NWWars.coreOpened(event);
    }

    @SubscribeEvent
    public static void warEnded(WarEvent.Ended event) {
        if (NWPresence.active()) NWWars.ended(event);
    }

    /** Unlocks the book's entries on Nations & War for those who have seen Lucifer fall. */
    private static void grantAdvancement(MinecraftServer server) {
        var lucifer = server.getAdvancements().get(org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("main/devil_went_down"));
        var mine = server.getAdvancements().get(org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("main/nations_of_the_chain"));
        if (lucifer == null || mine == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (p.getAdvancements().getOrStartProgress(lucifer).isDone() && !p.getAdvancements().getOrStartProgress(mine).isDone())
                org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, "main/nations_of_the_chain");
    }

    private static void audit(MinecraftServer server) {
        if (NWPresence.active() && SNConfig.CHAIN_ENABLED.get()) GuildNations.audit(server);
    }
}
