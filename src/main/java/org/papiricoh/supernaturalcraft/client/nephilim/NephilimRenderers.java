package org.papiricoh.supernaturalcraft.client.nephilim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseRenderers;
import org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusAssets;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselAssets;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselEntity;
import org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowAssets;
import org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackEntity;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * v0.41 core's rigs on the client, each of its contract.
 * <ul>
 *   <li>Jack ({@link JackAssets}): the companion with his own texture, his gold eyes lit only while he strikes; soulless Jack
 *   with his, the eyes always lit, and his gold halo ({@code aura}) only aloft in his last phase.</li>
 *   <li>Asmodeus: his yellow eyes and the grace vial lit from his second phase.</li>
 *   <li>Michael's vessel: the wing-shadows only from his second phase (never on the envoy at dawn); his projection in the
 *   mindscape is the same rig.</li>
 *   <li>The Shadow: its tendrils one more per phase (two in the first), its face lit.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class NephilimRenderers {

    private static final int GOLD = 0xFFD86A, BLUE = 0x9FD8FF, VIOLET = 0x8A6EC8;

    private NephilimRenderers() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        ResourceLocation soulless = SupernaturalCraft.asResource(JackAssets.SOULLESS_TEXTURE);
        event.registerEntityRenderer(NephilimRegistry.JACK.get(), ctx -> new Toggled<>(ctx, JackAssets.RIG, 0.5f, null,
                (b, e) -> ApocalypseRenderers.show(b, "aura", false), e -> false, -1, GOLD));
        event.registerEntityRenderer(NephilimRegistry.SOULLESS_JACK.get(), ctx -> new Toggled<>(ctx, JackAssets.RIG, 0.5f, soulless,
                (b, e) -> ApocalypseRenderers.show(b, "aura", e instanceof SoullessJackEntity j && j.isAerialPhase()), e -> true,
                JackAssets.HIT_TICKS.getOrDefault("death", 50), GOLD));
        event.registerEntityRenderer(NephilimRegistry.ASMODEUS.get(), ctx -> new Toggled<>(ctx, AsmodeusAssets.RIG, 0.6f, null,
                (b, e) -> {
                }, e -> e instanceof LuciferEntity l && l.phase() >= 2, AsmodeusAssets.HIT_TICKS.getOrDefault("death", 50), GOLD));
        event.registerEntityRenderer(NephilimRegistry.MICHAEL_VESSEL.get(), ctx -> new Toggled<>(ctx, MichaelVesselAssets.RIG, 0.5f, null,
                (b, e) -> ApocalypseRenderers.show(b, "wings", e instanceof MichaelVesselEntity m && m.phase() >= 2), e -> true,
                MichaelVesselAssets.HIT_TICKS.getOrDefault("death", 40), BLUE));
        event.registerEntityRenderer(NephilimRegistry.MICHAEL_ENVOY.get(), ctx -> new Toggled<>(ctx, MichaelVesselAssets.RIG, 0.5f, null,
                (b, e) -> ApocalypseRenderers.show(b, "wings", false), e -> true, -1, BLUE));
        event.registerEntityRenderer(NephilimRegistry.SHADOW.get(), ctx -> new Toggled<>(ctx, ShadowAssets.RIG, 0.9f, null,
                (b, e) -> {
                    int phase = e instanceof LuciferEntity l ? l.phase() : 1;
                    for (int i = 1; i <= 6; i++) ApocalypseRenderers.show(b, "tendril_" + i, i <= 2 * phase);
                }, e -> true, ShadowAssets.HIT_TICKS.getOrDefault("death", 50), VIOLET));
    }

    /** A contract rig whose bones {@code toggles} shows or hides each frame, with a glow while {@code lit}. */
    static class Toggled<T extends LivingEntity & GeoEntity> extends ApocalypseRenderers.Rig<T> {
        private final java.util.function.BiConsumer<BakedGeoModel, T> toggles;

        Toggled(EntityRendererProvider.Context ctx, String rig, float shadow, @Nullable ResourceLocation texture,
                java.util.function.BiConsumer<BakedGeoModel, T> toggles, java.util.function.Predicate<T> lit, int flarePeak, int flare) {
            super(ctx, rig, shadow, true, texture == null ? null : e -> texture);
            this.toggles = toggles;
            addRenderLayer(new ApocalypseRenderers.Glow<>(this, lit, flarePeak, flare));
        }

        @Override
        public void preRender(PoseStack pose, T e, BakedGeoModel baked, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) toggles.accept(baked, e);
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }
}
