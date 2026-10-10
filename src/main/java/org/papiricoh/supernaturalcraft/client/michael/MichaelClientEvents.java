package org.papiricoh.supernaturalcraft.client.michael;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.michael.render.HostAngelRenderer;
import org.papiricoh.supernaturalcraft.client.michael.render.LanceItemRenderer;
import org.papiricoh.supernaturalcraft.client.michael.render.LanceRenderer;
import org.papiricoh.supernaturalcraft.client.michael.render.MichaelRenderer;
import org.papiricoh.supernaturalcraft.client.michael.render.ProjectileRenderer;
import org.papiricoh.supernaturalcraft.client.michael.render.SeraphWingsChestLayer;
import org.papiricoh.supernaturalcraft.client.particle.GlowParticle;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Client registration for the Archangel Michael: his renderers and the Host's, the lances (thrown and in hand), feathers
 * and spears, the Seraph Wings in the chest slot, his overlay and the wings' stamina, and his particles once the art has
 * described them.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class MichaelClientEvents {

    private MichaelClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.MICHAEL.get(), MichaelRenderer::new);
        event.registerEntityRenderer(AllEntities.HOST_ANGEL.get(), HostAngelRenderer::new);
        event.registerEntityRenderer(AllEntities.MICHAEL_LANCE.get(), LanceRenderer::new);
        event.registerEntityRenderer(AllEntities.THROWN_LANCE.get(), LanceRenderer::new);
        event.registerEntityRenderer(AllEntities.STEEL_FEATHER.get(), ctx -> new ProjectileRenderer<>(ctx, "steel_feather", 0.7f, 0.25f));
        event.registerEntityRenderer(AllEntities.LIGHT_SPEAR.get(), ctx -> new ProjectileRenderer<>(ctx, "light_spear", 1.6f, 0.3f));
    }

    /** The Seraph Wings in the chest slot, on every player model. */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            if (event.getSkin(model) instanceof PlayerRenderer renderer) renderer.addLayer(new SeraphWingsChestLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void registerItems(RegisterClientExtensionsEvent event) {
        lance(event, AllItems.MICHAEL_LANCE.get());
        lance(event, AllItems.BORROWED_LANCE.get());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void lance(RegisterClientExtensionsEvent event, Item item) {
        event.registerItem(new IClientItemExtensions() {
            private LanceItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new LanceItemRenderer();
                return renderer;
            }
        }, item);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("michael_flight"), new FlightHud());
        event.registerAboveAll(SupernaturalCraft.asResource("michael"), new MichaelOverlay());
        // Title cards must be seen through a camera shot (the layer hides its lasting states itself).
        org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.showDuringSequences(SupernaturalCraft.asResource("michael"));
    }

    /** His particles, only once their descriptions exist (the art writes them): a provider without one fails the reload. */
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        if (described("steel_feather")) {
            event.registerSpriteSet(AllParticles.STEEL_FEATHER.get(), s -> new GlowParticle.Provider(s, false, 0.03f, 40, 0.12f));
        }
        if (described("halo_ray")) {
            event.registerSpriteSet(AllParticles.HALO_RAY.get(), s -> new GlowParticle.Provider(s, true, -0.01f, 20, 0.14f));
        }
    }

    static boolean described(String particle) {
        return MichaelClientEvents.class.getResource("/assets/" + SupernaturalCraft.MODID + "/particles/" + particle + ".json") != null;
    }
}
