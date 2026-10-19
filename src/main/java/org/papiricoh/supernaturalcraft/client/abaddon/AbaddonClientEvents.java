package org.papiricoh.supernaturalcraft.client.abaddon;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.KnightsCleaverItem;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Client registration for Abaddon (v0.21): her renderer and her retinue's ({@link AbaddonRenderers}), the Knights' Cleaver's
 * ({@link CleaverRenderer}: its veins beating) and her title cards' layer ({@link AbaddonOverlay}, let through camera sequences).
 * {@link ClientAbaddonFx} and {@link AbaddonHud} listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AbaddonClientEvents {

    private AbaddonClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.ABADDON.get(), AbaddonRenderers.Abaddon::new);
        event.registerEntityRenderer(AllEntities.ABADDON_KNIGHT.get(), AbaddonRenderers.Knight::new);
        event.registerEntityRenderer(AllEntities.ABADDON_HERALD.get(), AbaddonRenderers.Herald::new);
    }

    @SubscribeEvent
    public static void registerItems(RegisterClientExtensionsEvent event) {
        KnightsCleaverItem cleaver = AllItems.KNIGHTS_CLEAVER.get();
        event.registerItem(new IClientItemExtensions() {
            private CleaverRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new CleaverRenderer(cleaver);
                return renderer;
            }
        }, cleaver);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(AbaddonOverlay.LAYER, new AbaddonOverlay());
        // Only her title cards: they come with her camera shots and must be seen through them.
        CameraDirector.showDuringSequences(AbaddonOverlay.LAYER);
    }
}
