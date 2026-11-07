package org.papiricoh.supernaturalcraft.apocworld;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.army.AuArmy;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressWorld;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRifts;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelSummoning;

/**
 * The Apocalypse World's clock (v0.41, game bus): its rifts close on time, the fortress goes up in batches, the patrols come,
 * AU Michael waits at his throne; a hunter arriving by any road (a command, a portal of another mod) is welcomed as by a rift; one
 * away when he fled is settled on login.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ApocWorldEvents {

    private ApocWorldEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) AuRifts.tick(level);
        FortressWorld.tick(server);
        AuArmy.tick(server);
        if (server.getTickCount() % 20 == 13) AuMichaelSummoning.tick(server);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !event.getTo().equals(ApocWorldDimension.LEVEL)) return;
        if (!ApocalypseWorld.standing(p).visited()) ApocalypseWorld.arrived(p, 0);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) AuMichaelSpoils.settle(p);
    }
}
