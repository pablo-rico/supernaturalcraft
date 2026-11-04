package org.papiricoh.supernaturalcraft.client.crowley;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Client registration for Crowley (v0.35): his renderer (the boss and the companion share it) and his retinue's
 * ({@link CrowleyRenderers}), and his title cards' layer ({@link CrowleyOverlay}, let through camera sequences).
 * {@link ClientCrowleyFx} and {@link CrowleyHud} listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CrowleyClientEvents {

    private CrowleyClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.CROWLEY.get(), CrowleyRenderers.Crowley::new);
        event.registerEntityRenderer(AllEntities.CROWLEY_COMPANION.get(), CrowleyRenderers.Crowley::new);
        event.registerEntityRenderer(AllEntities.CROWLEY_SUIT.get(), CrowleyRenderers.Suit::new);
        event.registerEntityRenderer(AllEntities.POSSESSED_PATIENT.get(), CrowleyRenderers.Patient::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(CrowleyOverlay.LAYER, new CrowleyOverlay());
        // Only his title cards: they come with his camera shots and must be seen through them.
        CameraDirector.showDuringSequences(CrowleyOverlay.LAYER);
    }
}
