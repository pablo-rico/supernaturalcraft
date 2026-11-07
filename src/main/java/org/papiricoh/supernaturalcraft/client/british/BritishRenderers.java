package org.papiricoh.supernaturalcraft.client.british;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.HessEnvoyEntity;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentAssets;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentEntity;
import org.papiricoh.supernaturalcraft.british.birth.BirthAssets;
import org.papiricoh.supernaturalcraft.british.birth.KellyKlineEntity;
import org.papiricoh.supernaturalcraft.british.birth.RourkeLuciferEntity;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseRenderers;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonAssets;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessAssets;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessEntity;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchAssets;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * Season 12's rigs on the client (v0.41), each of its contract ({@code *Assets}); drawn once their files have loaded.
 * <ul>
 *   <li>Ketch, Hess (and her envoy at dawn: the same rig) and the agents (three textures; the beret only on the third).</li>
 *   <li>Dagon: her eyes and aura shell, and their glow, only while her power shows ({@link DagonEntity#powerShows}); she
 *   burns from inside as she dies.</li>
 *   <li>Kelly Kline, and Lucifer in Rourke's body (his red eyes glow once he shows them).</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class BritishRenderers {

    /** Previews: light (or darken) Dagon's power whatever she is doing (null: her own). */
    private static @Nullable Boolean forcedDagonPower;

    private BritishRenderers() {
    }

    public static void forceDagonPower(@Nullable Boolean on) {
        forcedDagonPower = on;
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BritishRegistry.KETCH.get(), Ketch::new);
        event.registerEntityRenderer(BritishRegistry.HESS.get(), ctx -> new ApocalypseRenderers.Rig<HessEntity>(ctx, HessAssets.RIG, 0.45f, false));
        event.registerEntityRenderer(BritishRegistry.HESS_ENVOY.get(), ctx -> new ApocalypseRenderers.Rig<HessEnvoyEntity>(ctx, HessAssets.RIG, 0.45f, true));
        event.registerEntityRenderer(BritishRegistry.BRITISH_AGENT.get(), Agent::new);
        event.registerEntityRenderer(BritishRegistry.DAGON.get(), Dagon::new);
        event.registerEntityRenderer(BritishRegistry.KELLY_KLINE.get(), ctx -> new ApocalypseRenderers.Rig<KellyKlineEntity>(ctx, BirthAssets.RIG, 0.45f, true));
        event.registerEntityRenderer(BritishRegistry.ROURKE_LUCIFER.get(), Rourke::new);
        event.registerEntityRenderer(BritishRegistry.HEX_GRENADE_ENTITY.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientBritishFx.tick();
    }

    public static boolean dagonPower(DagonEntity e) {
        return forcedDagonPower != null ? forcedDagonPower : e.powerShows();
    }

    public static class Ketch extends ApocalypseRenderers.Rig<KetchEntity> {
        public Ketch(EntityRendererProvider.Context ctx) {
            super(ctx, KetchAssets.RIG, 0.45f, false);
        }
    }

    public static class Agent extends ApocalypseRenderers.Rig<BritishAgentEntity> {
        public Agent(EntityRendererProvider.Context ctx) {
            super(ctx, BritishAgentAssets.RIG, 0.45f, true, e -> SupernaturalCraft.asResource("textures/entity/" + BritishAgentAssets.RIG + "_" + e.variant() + ".png"));
        }

        @Override
        public void preRender(PoseStack pose, BritishAgentEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) ApocalypseRenderers.show(baked, "beret", e.variant() == BritishAgentEntity.VARIANTS - 1);
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    public static class Dagon extends ApocalypseRenderers.Rig<DagonEntity> {
        public Dagon(EntityRendererProvider.Context ctx) {
            super(ctx, DagonAssets.RIG, 0.5f, false);
            addRenderLayer(new ApocalypseRenderers.Glow<>(this, BritishRenderers::dagonPower, DagonAssets.HIT_TICKS.getOrDefault("death", 50), 0xFF8A2A));
        }

        @Override
        public void preRender(PoseStack pose, DagonEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                boolean on = dagonPower(e);
                ApocalypseRenderers.show(baked, "aura", on);
                ApocalypseRenderers.show(baked, "eyes", on);
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    public static class Rourke extends ApocalypseRenderers.Rig<RourkeLuciferEntity> {
        public Rourke(EntityRendererProvider.Context ctx) {
            super(ctx, BirthAssets.ROURKE, 0.45f, true);
            addRenderLayer(new ApocalypseRenderers.Glow<>(this, e -> e.tickCount > 60, -1));
        }
    }
}
