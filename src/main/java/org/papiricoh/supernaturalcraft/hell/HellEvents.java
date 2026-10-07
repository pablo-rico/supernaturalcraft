package org.papiricoh.supernaturalcraft.hell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;
import org.papiricoh.supernaturalcraft.network.TormentPayload;

/** Hell's housekeeping: rifts closing on time, the Cage's iris, and Torment. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HellEvents {

    private HellEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        HellRifts.tick(level);
        // The Cage lives in Hell; anywhere else only if someone has built one there (commands, tests).
        if (HellDimension.isHell(level) || CageController.has(level)) CageController.get(level).tick(level);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) Torment.tick(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) PacketDistributor.sendToPlayer(p, new TormentPayload(Torment.get(p)));
    }
}
