package org.papiricoh.supernaturalcraft.darkness;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity;
import org.papiricoh.supernaturalcraft.darkness.fog.FogOutbreaks;
import org.papiricoh.supernaturalcraft.darkness.superior.Superior;
import org.papiricoh.supernaturalcraft.debug.AuditStats;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Season 11 in the world (v0.42), once a second: each hunter's road moves on (Superior reached, the Cage visited, Casifer at
 * their side), and at dawn (the first {@link #DAWN} ticks of the day) Amara visits, Castiel sends them to the Cage, Chuck shows
 * himself, Amara takes Casifer; at night the fog breaks out in villages near hunters on the road ({@link FogOutbreaks}).
 * Nothing of this runs in the GameTest server unless a test asks ({@link #setTestEvents}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DarknessEvents {

    /** Dawn: the first ticks of the day. */
    public static final long DAWN = 1200;
    private static final Map<UUID, Integer> LEAVING = new HashMap<>();
    private static boolean testEvents;

    private DarknessEvents() {
    }

    public static void setTestEvents(boolean on) {
        testEvents = on;
    }

    /** {@code e} leaves (in a little light) after {@code ticks}. */
    public static void leaveLater(Entity e, int ticks) {
        LEAVING.put(e.getUUID(), e.tickCount + ticks);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld.getGameTime() % 20 != 0) return;
        if (net.neoforged.neoforge.gametest.GameTestHooks.isGametestServer() && !testEvents) return;
        long sn$t = AuditStats.start();
        try {
            tick(server);
        } finally {
            AuditStats.stop("darkness.tick", sn$t);
        }
    }

    static void tick(MinecraftServer server) {
        long clock = WorldClock.day(server), day = clock / 24000L, time = Math.floorMod(clock, 24000L);
        boolean dawn = time < DAWN;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            Darkness d = Darknesses.get(p);
            DarknessRules.Step step = d.stage();
            if (step == DarknessRules.Step.NONE || step == DarknessRules.Step.DONE) continue;
            switch (step) {
                case RELEASED -> {
                    DarknessStory.giveCompass(p);
                    BlockPos sq = Superior.square(server);
                    if (sq != null && p.level().dimension() == Level.OVERWORLD && p.blockPosition().distSqr(sq) <= (long) Superior.REACHED * Superior.REACHED) {
                        DarknessStory.reachSuperior(p);
                    }
                }
                case SUPERIOR -> {
                    if (dawn && DarknessRules.cageDue(step, d.since(), day)) DarknessStory.sendToCage(p);
                    if (d.seen(DarknessStory.CASTIEL_SENT) && DarknessStory.atCage(p)) DarknessStory.visitCage(p);
                }
                case CAGE_VISITED -> Darknesses.advance(p, DarknessRules.Step.CASIFER);
                case CASIFER -> {
                    if (CasiferEntity.with(p) == null && p.tickCount > 100) CasiferEntity.arrive(p);
                    if (dawn && DarknessRules.revealDue(step, d.handCount())) DarknessStory.reveal(p);
                }
                case REVEALED -> {
                    if (CasiferEntity.with(p) == null && p.tickCount > 100) CasiferEntity.arrive(p);
                    if (dawn && DarknessRules.takenDue(step, d.since(), day)) DarknessStory.take(p);
                }
                default -> {
                }
            }
            if (dawn && p.level().dimension() == Level.OVERWORLD) DarknessStory.visit(p, day);
        }
        if (!dawn && time > 13000 && time < 23000) FogOutbreaks.night(server);
        LEAVING.entrySet().removeIf(e -> {
            for (ServerLevel level : server.getAllLevels()) {
                Entity x = level.getEntity(e.getKey());
                if (x == null) continue;
                if (x.tickCount < e.getValue()) return false;
                if (x instanceof org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity c) c.leave();
                else x.discard();
                return true;
            }
            return true;
        });
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        LEAVING.clear();
        testEvents = false;
    }
}
