package org.papiricoh.supernaturalcraft.client.leviathan;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;

/**
 * Client registration for Dick Roman (v0.25): his title cards' layer ({@link DickOverlay}, let through camera sequences). His and
 * the Leviathans' renderers are registered in {@code SNClientEvents}; {@link ClientDickFx} and {@link DickHud} listen on the game
 * bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class DickClientEvents {

    private DickClientEvents() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(DickOverlay.LAYER, DickOverlay.layer());
        // Only his title cards: they come with his camera shots and must be seen through them.
        CameraDirector.showDuringSequences(DickOverlay.LAYER);
    }
}
