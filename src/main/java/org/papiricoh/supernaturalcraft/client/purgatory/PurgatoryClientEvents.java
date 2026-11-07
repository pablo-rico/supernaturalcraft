package org.papiricoh.supernaturalcraft.client.purgatory;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.alpha.AlphaRenderers;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.eve.EveOverlay;
import org.papiricoh.supernaturalcraft.client.eve.EveRenderer;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryDimension;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Client registration for Purgatory, Eve and the Alphas (v0.39, mod bus): their renderers, Purgatory's sky and fog
 * ({@link PurgatoryEffects}), and Eve's title cards' layer ({@link EveOverlay}, let through camera sequences). {@code EveHud},
 * {@code ClientEveFx} and {@link PurgatoryClient} listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class PurgatoryClientEvents {

    private PurgatoryClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.EVE.get(), EveRenderer::new);
        event.registerEntityRenderer(AllEntities.ALPHA_VAMPIRE.get(), AlphaRenderers.Vampire::new);
        event.registerEntityRenderer(AllEntities.ALPHA_WEREWOLF.get(), AlphaRenderers.Werewolf::new);
        event.registerEntityRenderer(AllEntities.ALPHA_SHAPESHIFTER.get(), AlphaRenderers.Shapeshifter::new);
        event.registerEntityRenderer(AllEntities.PURGATORY_VAMPIRE.get(), SoulRenderers.Vampire::new);
        event.registerEntityRenderer(AllEntities.PURGATORY_WEREWOLF.get(), SoulRenderers.Werewolf::new);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(PurgatoryDimension.EFFECTS, new PurgatoryEffects());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(EveOverlay.LAYER, EveOverlay.layer());
        // Only her title cards: they come with her camera shots and must be seen through them.
        CameraDirector.showDuringSequences(EveOverlay.LAYER);
    }
}
