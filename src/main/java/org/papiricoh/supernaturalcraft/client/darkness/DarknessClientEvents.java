package org.papiricoh.supernaturalcraft.client.darkness;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.ChuckRenderer;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;

/** Season 11's renderers (v0.42; the core's people; Amara herself is the art's, in {@code client.amara}). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class DarknessClientEvents {

    private DarknessClientEvents() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DarknessRegistry.CASIFER.get(), DarknessRenderers.Casifer::new);
        event.registerEntityRenderer(DarknessRegistry.CHUCK_ALLY.get(), ChuckRenderer::new);
        event.registerEntityRenderer(DarknessRegistry.AMARA_VISITOR.get(), DarknessRenderers.Visitor::new);
        event.registerEntityRenderer(DarknessRegistry.FOGGED_VILLAGER.get(), DarknessRenderers.Fogged::new);
    }
}
