package org.papiricoh.supernaturalcraft.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.client.hud.ManaHudOverlay;
import org.papiricoh.supernaturalcraft.client.cinematic.ClientCinematics;
import org.papiricoh.supernaturalcraft.client.render.DemonRenderer;
import org.papiricoh.supernaturalcraft.client.render.LuciferRenderer;
import org.papiricoh.supernaturalcraft.client.render.TelegraphRenderer;
import org.papiricoh.supernaturalcraft.client.render.RitualAltarRenderer;
import org.papiricoh.supernaturalcraft.client.render.WardRenderer;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.particle.GlowParticle;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/** Mod-bus client registration: renderers, particle providers, layers, key mappings. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public class SNClientEvents {

    @SubscribeEvent
    public static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        if (net.neoforged.fml.ModList.get().isLoaded(org.papiricoh.supernaturalcraft.compat.pal.client.PalClient.MOD_ID)) {
            event.enqueueWork(org.papiricoh.supernaturalcraft.compat.pal.client.PalClient::register);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            event.enqueueWork(org.papiricoh.supernaturalcraft.compat.curios.client.CuriosClient::register);
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new org.papiricoh.supernaturalcraft.client.cinematic.CinematicSequences());
    }

    @SubscribeEvent
    public static void registerDimensionEffects(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(net.minecraft.world.level.dimension.BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                new org.papiricoh.supernaturalcraft.client.eclipse.EclipseOverworldEffects());
        event.register(org.papiricoh.supernaturalcraft.hell.HellDimension.EFFECTS, new org.papiricoh.supernaturalcraft.client.hell.HellEffects());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.HOLY_WATER.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(AllEntities.HELLFIRE_BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(AllEntities.SIGIL_BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(AllEntities.WARD.get(), WardRenderer::new);
        event.registerEntityRenderer(AllEntities.LINGERING_ZONE.get(), WardRenderer::new);
        event.registerBlockEntityRenderer(AllBlockEntities.RITUAL_ALTAR.get(), RitualAltarRenderer::new);
        event.registerBlockEntityRenderer(AllBlockEntities.CHOIR_ALTAR.get(), org.papiricoh.supernaturalcraft.client.render.ChoirAltarRenderer::new);
        event.registerEntityRenderer(AllEntities.LUCIFER.get(), LuciferRenderer::new);
        event.registerEntityRenderer(AllEntities.LUCIFER_ILLUSION.get(), LuciferRenderer::new);
        event.registerEntityRenderer(AllEntities.TELEGRAPH.get(), TelegraphRenderer::new);
        event.registerEntityRenderer(AllEntities.BOSS_SHARD.get(), NoopRenderer::new);
        event.registerEntityRenderer(AllEntities.VOID_ZONE.get(), org.papiricoh.supernaturalcraft.client.render.VoidZoneRenderer::new);
        event.registerEntityRenderer(AllEntities.AMARA.get(), org.papiricoh.supernaturalcraft.client.render.AmaraRenderer::new);
        event.registerEntityRenderer(AllEntities.AMARA_SHADE.get(), org.papiricoh.supernaturalcraft.client.render.AmaraShadeRenderer::new);
        event.registerEntityRenderer(AllEntities.BROKEN_CHORUS.get(), org.papiricoh.supernaturalcraft.client.render.ChorusRenderer::new);
        event.registerEntityRenderer(AllEntities.CHOIR_ECHO.get(), org.papiricoh.supernaturalcraft.client.render.ChoirEchoRenderer::new);
        event.registerEntityRenderer(AllEntities.SOUL_CRESCENT.get(), org.papiricoh.supernaturalcraft.client.render.CrescentRenderer::new);
        event.registerEntityRenderer(AllEntities.FLAME_TRAIL.get(), NoopRenderer::new);
        event.registerEntityRenderer(AllEntities.BLACK_EYED_DEMON.get(), ctx -> new DemonRenderer<>(ctx, "black_eyed_demon"));
        event.registerEntityRenderer(AllEntities.DEMON_OCCULTIST.get(), ctx -> new DemonRenderer<>(ctx, "demon_occultist"));
        event.registerEntityRenderer(AllEntities.HELLHOUND.get(), org.papiricoh.supernaturalcraft.client.render.HellhoundRenderer::new);
        event.registerEntityRenderer(AllEntities.LUCIFER_UNCAGED.get(), org.papiricoh.supernaturalcraft.client.render.LuciferUncagedRenderer::new);
        event.registerEntityRenderer(AllEntities.LILITH.get(), org.papiricoh.supernaturalcraft.client.render.LilithRenderer::new);
        event.registerEntityRenderer(AllEntities.BOUND_HELLHOUND.get(), org.papiricoh.supernaturalcraft.client.render.HellhoundRenderer::new);
        event.registerEntityRenderer(AllEntities.AZAZEL.get(), org.papiricoh.supernaturalcraft.client.render.AzazelRenderer::new);
        event.registerEntityRenderer(AllEntities.HURLED_DEBRIS.get(), org.papiricoh.supernaturalcraft.client.render.HurledDebrisRenderer::new);
        event.registerEntityRenderer(AllEntities.CAGED_LUCIFER.get(), org.papiricoh.supernaturalcraft.client.render.LuciferUncagedRenderer::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("mana"), new ManaHudOverlay());
        event.registerBelow(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("consumption"),
                new org.papiricoh.supernaturalcraft.client.amara.ConsumptionOverlay());
        event.registerAbove(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("colt"),
                new org.papiricoh.supernaturalcraft.client.colt.ColtOverlay());
        event.registerBelow(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("torment"),
                new org.papiricoh.supernaturalcraft.client.hell.HellClient.Overlay());
        event.registerAboveAll(SupernaturalCraft.asResource("cinematic"), new ClientCinematics.Overlay());
        event.registerAboveAll(org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.SKIP_LAYER,
                new org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.SkipHint());
    }

    /** GeckoLib weapons draw through a BEWLR; NeoForge needs it registered per item. */
    @SubscribeEvent
    public static void registerItemExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for (var holder : java.util.List.of(org.papiricoh.supernaturalcraft.registry.AllItems.SOUL_SCYTHE,
                org.papiricoh.supernaturalcraft.registry.AllItems.HELLFIRE_GREATSWORD,
                org.papiricoh.supernaturalcraft.registry.AllItems.CENSER_OF_GRACE,
                org.papiricoh.supernaturalcraft.registry.AllItems.FIRST_BLADE,
                org.papiricoh.supernaturalcraft.registry.AllItems.WHISPERING_CODEX,
                org.papiricoh.supernaturalcraft.registry.AllItems.PENUMBRA)) {
            registerGeo(event, holder.get());
        }
        event.registerItem(new org.papiricoh.supernaturalcraft.client.colt.ColtItemExtensions(),
                org.papiricoh.supernaturalcraft.registry.AllItems.THE_COLT.get());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerGeo(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event,
                                    net.minecraft.world.item.Item item) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private org.papiricoh.supernaturalcraft.client.render.GeoWeaponRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new org.papiricoh.supernaturalcraft.client.render.GeoWeaponRenderer((net.minecraft.world.item.Item & software.bernie.geckolib.animatable.GeoAnimatable) item);
                return renderer;
            }
        }, item);
    }

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(org.papiricoh.supernaturalcraft.registry.AllMenus.HELLFORGE.get(),
                org.papiricoh.supernaturalcraft.client.screen.HellforgeScreen::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SNKeys.CYCLE_SPELL);
        event.register(SNKeys.SKIP_CINEMATIC);
        event.register(SNKeys.RELOAD_WEAPON);
        event.register(SNKeys.INSPECT_WEAPON);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AllParticles.HELLFIRE.get(), s -> new GlowParticle.Provider(s, true, -0.02f, 14, 0.18f));
        event.registerSpriteSet(AllParticles.GRACE.get(), s -> new GlowParticle.Provider(s, true, 0.0f, 18, 0.14f));
        event.registerSpriteSet(AllParticles.VOID_MOTE.get(), s -> new GlowParticle.Provider(s, false, -0.005f, 34, 0.22f));
        event.registerSpriteSet(AllParticles.SIGIL.get(), s -> new GlowParticle.Provider(s, true, -0.005f, 30, 0.16f));
        event.registerSpriteSet(AllParticles.DEMON_SMOKE.get(), s -> new GlowParticle.Provider(s, false, -0.01f, 26, 0.35f));
        event.registerSpriteSet(AllParticles.FROST.get(), s -> new GlowParticle.Provider(s, true, 0.02f, 16, 0.12f));
        event.registerSpriteSet(AllParticles.ASH.get(), s -> new GlowParticle.Provider(s, false, 0.01f, 40, 0.12f));
        event.registerSpriteSet(AllParticles.YELLOW_SMOKE.get(), s -> new GlowParticle.Provider(s, false, -0.012f, 24, 0.17f));
        event.registerSpriteSet(AllParticles.WHITE_LIGHT.get(), s -> new GlowParticle.Provider(s, true, -0.004f, 20, 0.08f));
    }
}
