package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.UUID;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

/** Keeps every client's copy of every player's allegiance current: login, respawn, dimension, tracking, throttled essence. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AllegianceEvents {

    private static final int SYNC_INTERVAL = 5;

    private AllegianceEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Allegiances.sync(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Allegiances.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Allegiances.sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Allegiances.sync(p);
    }

    /** Someone new sees this player: tell them what they are. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer seen && event.getEntity() instanceof ServerPlayer viewer) {
            Send.toPlayer(viewer, Allegiances.payload(seen));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long sn$t = AuditStats.start();
        try {
            if (event.getServer().getTickCount() % SYNC_INTERVAL != 0 || Allegiances.DIRTY.isEmpty()) return;
            for (UUID id : java.util.List.copyOf(Allegiances.DIRTY)) {
                ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
                if (p != null) Allegiances.syncSelf(p);
                else Allegiances.DIRTY.remove(id);
            }
        } finally {
            AuditStats.stop("allegiance.syncFlush", sn$t);
        }
    }
}
