package org.papiricoh.supernaturalcraft.client.cain;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.cain.CainsAddressItem;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Client registration for Cain (v0.21): his renderers, his address's needle and his title cards' layer ({@link CainOverlay}, let
 * through camera sequences). {@link ClientCainFx} and {@link CainHud} listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CainClientEvents {

    private CainClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.CAIN.get(), CainRenderer::new);
        event.registerEntityRenderer(AllEntities.CAIN_NPC.get(), CainNpcRenderer::new);
    }

    /** His address's needle ({@code angle}, the 32 frames of {@code SNItemModelProvider.needles}) points at his gate. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(AllItems.CAINS_ADDRESS.get(), ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    TablePin pin = CainsAddressItem.target(stack);
                    return pin == null ? null : GlobalPos.of(Level.OVERWORLD, new BlockPos(pin.x(), 64, pin.z()));
                })));
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(CainOverlay.LAYER, new CainOverlay());
        // Only his title cards: they come with his camera shots and must be seen through them.
        CameraDirector.showDuringSequences(CainOverlay.LAYER);
    }
}
