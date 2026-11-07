package org.papiricoh.supernaturalcraft.client.apocworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.army.AuPrisonerEntity;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierAssets;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierEntity;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseRenderers;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelAssets;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.function.Predicate;

/**
 * The Apocalypse World's rigs on the client (v0.41), each of its contract, drawn only once their files have loaded
 * ({@link GeoGuard}); the renderer only shows and hides the contract's optional bones, it never moves one.
 * <ul>
 *   <li>Michael's soldiers ({@link AuSoldierAssets}): four vessels, helmet and blade always on, their eyes lit while they smite.</li>
 *   <li>The prisoners ({@link AuSoldierAssets#PRISONER}): four looks.</li>
 *   <li>AU Michael ({@link AuMichaelAssets}): his wings shown from phase II, his eyes when he shows them; nothing at all once he is
 *   through the rift.</li>
 * </ul>
 */
public final class ApocWorldRenderers {

    private ApocWorldRenderers() {
    }

    static ResourceLocation tex(String path) {
        return SupernaturalCraft.asResource(path);
    }

    /** {@code textures/entity/<rig>_<n>.png} if the art has it, else the rig's own texture (which is variant 0). */
    static ResourceLocation variant(String rig, int n) {
        if (n <= 0) return tex("textures/entity/" + rig + ".png");
        ResourceLocation v = tex("textures/entity/" + rig + "_" + n + ".png");
        return GeoGuard.exists(v) ? v : tex("textures/entity/" + rig + ".png");
    }

    /** The glowmask of the texture drawn ({@code <texture>_glowmask.png}, else {@code fallback}), full bright while {@code lit} says so. */
    static final class Glow<T extends LivingEntity & GeoEntity> extends GeoRenderLayer<T> {
        private final ResourceLocation fallback;
        private final Predicate<T> lit;

        Glow(GeoRenderer<T> renderer, String fallback, Predicate<T> lit) {
            super(renderer);
            this.fallback = tex(fallback);
            this.lit = lit;
        }

        @Override
        public void render(PoseStack pose, T e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            if (!lit.test(e)) return;
            ResourceLocation glow = GeoGuard.glowmask(getTextureResource(e));
            if (!GeoGuard.exists(glow)) glow = fallback;
            if (!GeoGuard.exists(glow)) return;
            RenderType eyes = RenderType.eyes(glow);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, 0xF000F0, overlay, 0xFFFFFFFF);
        }
    }

    public static class Soldier extends ApocalypseRenderers.Rig<AuSoldierEntity> {
        public Soldier(EntityRendererProvider.Context ctx) {
            super(ctx, AuSoldierAssets.RIG, 0.5f, true, e -> variant(AuSoldierAssets.RIG, e.vessel()));
            addRenderLayer(new Glow<>(this, AuSoldierAssets.GLOW, AuSoldierEntity::eyesLit));
            addRenderLayer(new org.papiricoh.supernaturalcraft.client.angel.SmiteGlowLayer<>(this));
        }

        @Override
        public void preRender(PoseStack pose, AuSoldierEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) ApocalypseRenderers.show(baked, "eyes", e.eyesLit());
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    public static class Prisoner extends ApocalypseRenderers.Rig<AuPrisonerEntity> {
        public Prisoner(EntityRendererProvider.Context ctx) {
            super(ctx, AuSoldierAssets.PRISONER, 0.45f, true, e -> variant(AuSoldierAssets.PRISONER, e.look()));
        }
    }

    public static class Michael extends ApocalypseRenderers.Rig<AuMichaelEntity> {
        public Michael(EntityRendererProvider.Context ctx) {
            super(ctx, AuMichaelAssets.RIG, 0.6f, true);
            addRenderLayer(new Glow<>(this, AuMichaelAssets.GLOW, e -> e.wingsOut() || e.eyesLit()));
        }

        @Override
        public void render(AuMichaelEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (e.gone()) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, AuMichaelEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                ApocalypseRenderers.show(baked, "wings", e.wingsOut());
                ApocalypseRenderers.show(baked, "eyes", e.eyesLit());
                ApocalypseRenderers.show(baked, "blade", !e.fleeing());
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }
}
