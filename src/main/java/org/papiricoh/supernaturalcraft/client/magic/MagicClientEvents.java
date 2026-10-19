package org.papiricoh.supernaturalcraft.client.magic;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicTextures;
import org.papiricoh.supernaturalcraft.client.magic.fx.SigilBoltRenderer;
import org.papiricoh.supernaturalcraft.client.mark.ClientMark;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Client registrations of the magic (v0.21): the sigil bolt's renderer, the coloured magic particles ({@link MagicParticle}), the
 * screen layer (flashes and the Mark's thirst, {@link MagicOverlay}), forgetting cached effect textures on a resource reload, and
 * forgetting the synced magic state on leaving a world.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class MagicClientEvents {

    private MagicClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.SIGIL_BOLT.get(), SigilBoltRenderer::new);
        event.registerEntityRenderer(AllEntities.SIGIL_GLYPH.get(), org.papiricoh.supernaturalcraft.client.magic.fx.GlyphTrapRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AllParticles.MAGIC_SPARK.get(), s -> new MagicParticle.Provider(s, MagicParticle.Kind.SPARK));
        event.registerSpriteSet(AllParticles.MAGIC_GLYPH.get(), s -> new MagicParticle.Provider(s, MagicParticle.Kind.GLYPH));
        event.registerSpriteSet(AllParticles.RUNE_EMBER.get(), s -> new MagicParticle.Provider(s, MagicParticle.Kind.EMBER));
        event.registerSpriteSet(AllParticles.WITCH_MOTE.get(), s -> new MagicParticle.Provider(s, MagicParticle.Kind.MOTE));
        event.registerSpriteSet(AllParticles.SALT_GRAIN.get(), s -> new MagicParticle.Provider(s, MagicParticle.Kind.SALT));
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SupernaturalCraft.asResource("magic_overlay"), new MagicOverlay());
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> MagicTextures.reload());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMark.reset();
        ClientMastery.reset();
    }
}
