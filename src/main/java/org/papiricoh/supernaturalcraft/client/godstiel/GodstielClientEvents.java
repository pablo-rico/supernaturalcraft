package org.papiricoh.supernaturalcraft.client.godstiel;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Client registration for season 6 (v0.39): Godstiel's, Castiel's and Crowley-the-King's renderers ({@link GodstielRenderers}),
 * and Godstiel's title cards' layer ({@link GodstielOverlay}, let through camera sequences). {@link ClientGodstielFx} and
 * {@link GodstielHud} listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class GodstielClientEvents {

    private GodstielClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.GODSTIEL.get(), GodstielRenderers.Godstiel::new);
        event.registerEntityRenderer(AllEntities.CASTIEL.get(), GodstielRenderers.Castiel::new);
        event.registerEntityRenderer(AllEntities.CASTIEL_COMPANION.get(), GodstielRenderers.Castiel::new);
        event.registerEntityRenderer(AllEntities.CROWLEY_KING.get(), GodstielRenderers.CrowleyKing::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GodstielOverlay.LAYER, new GodstielOverlay());
        // Only cards, voices and a flash: they come with his camera shots and must be seen through them.
        CameraDirector.showDuringSequences(GodstielOverlay.LAYER);
    }
}
