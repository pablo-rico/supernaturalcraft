package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Research ends by game time, even while its hunter is away (v0.17): every second for online players, and when one logs in.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ResearchTicker {

    public static final int INTERVAL = 20;

    private ResearchTicker() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % INTERVAL != 0) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            ResearchService.tick(p);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) ResearchService.tick(p);
    }
}
