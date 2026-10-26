package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;

@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ChainClient {
    private ChainClient() {}
    public static void sync(ChainSyncPayload p) { ClientChain.officers = p.officers(); ClientChain.history = p.history(); }
    public static void notice(ChainNoticePayload p) {
        if (p.kind() == ChainNoticePayload.TITLE) { ClientChain.lastTitle = p; ChainOverlay.show(p); }
        else if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(p.key(), p.args().toArray()), true);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        ChainOverlay.tick();
        OfficerAura.tick(Minecraft.getInstance());
        OfficerPuppets.tick();
    }
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ChainOverlay.LAYER, new ChainOverlay());
        CameraDirector.showDuringSequences(ChainOverlay.LAYER);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientChain.clear(); ChainOverlay.clear(); OfficerPuppets.clear(); }
}
