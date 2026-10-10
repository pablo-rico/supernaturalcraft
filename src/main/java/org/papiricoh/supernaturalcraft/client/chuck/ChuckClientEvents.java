package org.papiricoh.supernaturalcraft.client.chuck;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.AuthorPageFx;
import org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckOverlay;
import org.papiricoh.supernaturalcraft.client.chuck.particle.InkLetterParticle;
import org.papiricoh.supernaturalcraft.client.chuck.render.AuthorHandRenderer;
import org.papiricoh.supernaturalcraft.client.chuck.render.AuthorTargetRenderer;
import org.papiricoh.supernaturalcraft.client.chuck.render.ChuckRenderer;
import org.papiricoh.supernaturalcraft.client.chuck.render.FloatingWordRenderer;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.HunterAllyRenderer;
import org.papiricoh.supernaturalcraft.client.chuck.render.InkEchoRenderer;
import org.papiricoh.supernaturalcraft.client.chuck.render.TypewriterKeyRenderer;
import org.papiricoh.supernaturalcraft.client.particle.GlowParticle;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Client registration for the Author's fight: the renderers of all eight of its entities, its particles, and its two
 * GUI layers (the page's fallback wash under everything, the Author's words over everything).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ChuckClientEvents {

    private ChuckClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.CHUCK.get(), ChuckRenderer::new);
        event.registerEntityRenderer(AllEntities.AUTHOR_NPC.get(), ChuckRenderer::new);
        event.registerEntityRenderer(AllEntities.AUTHOR_HAND.get(), AuthorHandRenderer::new);
        event.registerEntityRenderer(AllEntities.AUTHOR_TARGET.get(), AuthorTargetRenderer::new);
        event.registerEntityRenderer(AllEntities.INK_ECHO.get(), InkEchoRenderer::new);
        event.registerEntityRenderer(AllEntities.FLOATING_WORD.get(), FloatingWordRenderer::new);
        event.registerEntityRenderer(AllEntities.TYPEWRITER_KEY.get(), TypewriterKeyRenderer::new);
        event.registerEntityRenderer(AllEntities.HUNTER_ALLY.get(), HunterAllyRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        // Letters of ink rise slowly and turn; scraps of paper flutter down; motes of gold twinkle and drift.
        event.registerSpriteSet(AllParticles.INK_LETTER.get(), s -> new InkLetterParticle.Provider(s, -0.002f, 40, 0.16f, 0.04f));
        event.registerSpriteSet(AllParticles.PAGE_SCRAP.get(), s -> new InkLetterParticle.Provider(s, 0.004f, 45, 0.14f, 0.12f));
        event.registerSpriteSet(AllParticles.GOLDEN_MOTE.get(), s -> new GlowParticle.Provider(s, true, -0.002f, 30, 0.07f));
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(SupernaturalCraft.asResource("author_page"), new AuthorPageFx.Fallback());
        // The page's look without its shader: the world looks the same through a camera shot as with the shader.
        org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.showDuringSequences(SupernaturalCraft.asResource("author_page"));
        event.registerAboveAll(SupernaturalCraft.asResource("author_words"), new ChuckOverlay());
        // Only moments (the chapter titles, the words): they come with his camera shots and must be seen through them.
        org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.showDuringSequences(SupernaturalCraft.asResource("author_words"));
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> GeoGuard.forget());
    }
}
