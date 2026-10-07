package org.papiricoh.supernaturalcraft.client.horsemen;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.horsemen.fx.DeathClockOverlay;
import org.papiricoh.supernaturalcraft.client.horsemen.fx.LimboFx;
import org.papiricoh.supernaturalcraft.client.horsemen.render.HorsemanRenderer;
import org.papiricoh.supernaturalcraft.client.horsemen.render.HorsemanSteedRenderer;
import org.papiricoh.supernaturalcraft.client.horsemen.render.SimpleGeoRenderer;
import org.papiricoh.supernaturalcraft.client.horsemen.render.WarMirageRenderer;
import org.papiricoh.supernaturalcraft.client.particle.GlowParticle;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.HungryThrallEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Client registration for the Four Horsemen: renderers of the four, their horses, War's standards and mirages (the
 * black-eyed demon's model), Famine's thralls (the vanilla husk), the reapers (seen only when Death allows) and the
 * particle-only flies and light out of limbo; their particles; the death clock and limbo's grey wash.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class HorsemenClientEvents {

    private HorsemenClientEvents() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.WAR.get(), ctx -> new HorsemanRenderer<>(ctx, "war"));
        event.registerEntityRenderer(AllEntities.FAMINE.get(), ctx -> new HorsemanRenderer<>(ctx, "famine"));
        event.registerEntityRenderer(AllEntities.PESTILENCE.get(), ctx -> new HorsemanRenderer<>(ctx, "pestilence"));
        event.registerEntityRenderer(AllEntities.DEATH.get(), ctx -> new HorsemanRenderer<>(ctx, "death"));
        event.registerEntityRenderer(AllEntities.HORSEMAN_STEED.get(), HorsemanSteedRenderer::new);
        event.registerEntityRenderer(AllEntities.WAR_STANDARD.get(), ctx -> new SimpleGeoRenderer<>(ctx, "war_standard", 0.4f, e -> true));
        event.registerEntityRenderer(AllEntities.WAR_MIRAGE.get(), WarMirageRenderer::new);
        event.registerEntityRenderer(AllEntities.HUNGRY_THRALL.get(), ctx -> (EntityRenderer<HungryThrallEntity>) (EntityRenderer) new HuskRenderer(ctx));
        event.registerEntityRenderer(AllEntities.REAPER.get(), ctx -> new SimpleGeoRenderer<>(ctx, "reaper", 0.0f, e -> LimboView.reapersVisible()));
        event.registerEntityRenderer(AllEntities.FLY_SWARM.get(), NoopRenderer::new);
        event.registerEntityRenderer(AllEntities.LIMBO_EXIT.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AllParticles.FLY.get(), s -> new GlowParticle.Provider(s, false, 0.0f, 14, 0.04f));
        event.registerSpriteSet(AllParticles.PLAGUE_SPORE.get(), s -> new GlowParticle.Provider(s, false, -0.004f, 36, 0.1f));
        event.registerSpriteSet(AllParticles.SOUL_WISP.get(), s -> new GlowParticle.Provider(s, true, -0.01f, 30, 0.12f));
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(SupernaturalCraft.asResource("limbo_grey"), new LimboFx.Fallback());
        event.registerAbove(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("death_clock"), new DeathClockOverlay());
    }
}
