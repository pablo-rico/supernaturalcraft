package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;

/**
 * Client registration for the seals (v0.40, the Seals slice; mod bus): the cards and mini HUD's layer ({@link SealOverlay}, let
 * through camera sequences; it hides its HUD itself while one plays). {@link SealSky} listens on the game bus on its own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class SealClientEvents {

    private SealClientEvents() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SealOverlay.LAYER, new SealOverlay());
        CameraDirector.showDuringSequences(SealOverlay.LAYER);
    }

    /** Game bus: forgets the world's seals on leaving it. */
    @EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
    public static final class Game {
        private Game() {
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientSeals.reset();
            SealSky.clear();
            SealOverlay.clear();
        }
    }
}
