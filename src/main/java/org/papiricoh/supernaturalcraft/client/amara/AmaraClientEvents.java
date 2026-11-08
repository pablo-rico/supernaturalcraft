package org.papiricoh.supernaturalcraft.client.amara;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;

/**
 * Client registration for the Darkness (v0.42): her cards' layer ({@link AmaraOverlay}, above everything and let through camera
 * sequences: it draws no game HUD) and the post-processing's stand-in ({@link ClientPostFx.Fallback}, under the HUD). Her
 * renderers stay where they were registered ({@code SNClientEvents}: {@code AmaraRenderer::new}); {@link AmaraHud},
 * {@link AmaraFxRenderer}, {@link ClientAmara}, {@link ClientPostFx} and the skies listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AmaraClientEvents {

    private AmaraClientEvents() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.HOTBAR, ClientPostFx.Fallback.LAYER, new ClientPostFx.Fallback());
        event.registerAboveAll(AmaraOverlay.LAYER, new AmaraOverlay());
        CameraDirector.showDuringSequences(AmaraOverlay.LAYER);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> ClientPostFx.reset());
    }
}
