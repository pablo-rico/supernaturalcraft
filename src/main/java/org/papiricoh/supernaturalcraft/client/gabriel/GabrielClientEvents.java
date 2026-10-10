package org.papiricoh.supernaturalcraft.client.gabriel;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Client registration for Gabriel, the Trickster (v0.14): his renderer (shared with his doubles), the pie's, the remote in
 * hand, and TV Land's overlay. The boss bar ({@link GabrielHud}), the party hats and the six-winged shadow
 * ({@link GabrielWorldFx}) listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class GabrielClientEvents {

    private GabrielClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.GABRIEL.get(), GabrielRenderer::new);
        event.registerEntityRenderer(AllEntities.GABRIEL_DOUBLE.get(), GabrielRenderer::new);
        event.registerEntityRenderer(AllEntities.GABRIEL_PIE.get(), PieRenderer::new);
    }

    @SubscribeEvent
    public static void registerItems(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private RemoteItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new RemoteItemRenderer();
                return renderer;
            }
        }, AllItems.TRICKSTER_REMOTE.get());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SupernaturalCraft.asResource("gabriel"), new GabrielOverlay());
        // His title cards come with his camera shots: the layer must draw through them (it hides its HUD itself).
        org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.showDuringSequences(SupernaturalCraft.asResource("gabriel"));
    }
}
