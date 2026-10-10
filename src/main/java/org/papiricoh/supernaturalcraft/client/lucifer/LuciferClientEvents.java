package org.papiricoh.supernaturalcraft.client.lucifer;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;

/**
 * Client registration for the Cage's HUD: the title cards' layer (drawn through the camera sequences they come with), and
 * forgetting the glyphs on a resource reload.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class LuciferClientEvents {

    private LuciferClientEvents() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LuciferOverlay.LAYER, new LuciferOverlay());
        CameraDirector.showDuringSequences(LuciferOverlay.LAYER);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> Gothic.forget());
    }
}
