package org.papiricoh.supernaturalcraft.client.hands;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;

/** Client registration for season 11's creatures (v0.42, mod bus): their renderers ({@link HandsRenderers}). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class HandsClientEvents {

    private HandsClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HandsRegistry.THULE_SOLDIER.get(), HandsRenderers.ThuleSoldier::new);
        event.registerEntityRenderer(HandsRegistry.THULE_OFFICER.get(), HandsRenderers.ThuleOfficer::new);
        event.registerEntityRenderer(HandsRegistry.BANSHEE.get(), HandsRenderers.Banshee::new);
        event.registerEntityRenderer(HandsRegistry.CHITTER.get(), HandsRenderers.Chitter::new);
    }
}
