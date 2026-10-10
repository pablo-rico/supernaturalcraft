package org.papiricoh.supernaturalcraft.client.lucifer;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;

/** Plays out each {@link LuciferFxPayload} on the client (the title cards, {@link LuciferOverlay}) and ticks the Cage's HUD. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientLucifer {

    private ClientLucifer() {
    }

    public static void handle(LuciferFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (p.kind() == LuciferFxPayload.TITLE) {
            LuciferOverlay.add(new LuciferOverlay.TitleCard(p.arg2() == LuciferFxPayload.UNCAGED, p.arg(), p.duration()));
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        LuciferOverlay.tick();
        LuciferHud.tick();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        LuciferOverlay.clear();
        LuciferHud.clear();
    }
}
