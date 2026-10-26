package org.papiricoh.supernaturalcraft.client.chain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.angel.SmiteGlowLayer;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.chain.ChainMissile;
import org.papiricoh.supernaturalcraft.entity.chain.ChainMobAssets;
import org.papiricoh.supernaturalcraft.entity.demon.DemonEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renderers of the Chain's rank and file ({@link ChainMobAssets}) and of their missiles. Nothing is drawn until the art exists. */
public final class ChainMobRenderers {

    private ChainMobRenderers() {
    }

    /** A demon of the rank and file: its own rig and skin, black eyes glowing from the glowmask. */
    public static class Demon<T extends DemonEntity> extends GeoEntityRenderer<T> {
        private final ChainMobAssets.Mob mob;

        public Demon(EntityRendererProvider.Context ctx, ChainMobAssets.Mob mob) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(mob.id()), true));
            this.mob = mob;
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = mob.width() * 0.8f;
        }

        @Override
        public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(GeoGuard.model(mob.id()), GeoGuard.animation(mob.id()))) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }
    }

    /** An angel of the rank and file: its own rig, its eyes lit, the shadow of its wings only while shown (always, fallen). */
    public static class Angel<T extends AngelEntity> extends GeoEntityRenderer<T> {
        private final ChainMobAssets.Mob mob;

        public Angel(EntityRendererProvider.Context ctx, ChainMobAssets.Mob mob) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(mob.id()), true));
            this.mob = mob;
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            addRenderLayer(new SmiteGlowLayer<>(this));
            shadowRadius = mob.width() * 0.8f;
        }

        @Override
        public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(GeoGuard.model(mob.id()), GeoGuard.animation(mob.id()))) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, T e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                getGeoModel().getBone(ChainMobAssets.WINGS).ifPresent(b -> {
                    b.setHidden(!e.wingsShown());
                    b.setChildrenHidden(!e.wingsShown());
                });
            }
            super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        @Override
        public RenderType getRenderType(T e, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    /** A missile: its sprite facing the camera (the lance laid along its flight); the hook trails its chain back to the thrower. */
    public static class Missile extends EntityRenderer<ChainMissile> {
        public Missile(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public ResourceLocation getTextureLocation(ChainMissile e) {
            return SupernaturalCraft.asResource(ChainMobAssets.PROJECTILES.get(e.kind().ordinal()));
        }

        @Override
        public void render(ChainMissile e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            ResourceLocation tex = getTextureLocation(e);
            if (!GeoGuard.exists(tex)) return;
            boolean lance = e.kind() == ChainMissile.Kind.LANCE;
            pose.pushPose();
            if (lance) {
                pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, e.yRotO, e.getYRot()) - 90));
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, e.xRotO, e.getXRot())));
                pose.scale(1.4f, 0.6f, 1);
            } else {
                pose.mulPose(entityRenderDispatcher.cameraOrientation());
                pose.scale(0.6f, 0.6f, 0.6f);
            }
            quad(pose.last().pose(), buffers.getBuffer(lance ? RenderType.eyes(tex) : RenderType.entityCutoutNoCull(tex)),
                    lance ? 0xF000F0 : light);
            if (lance) {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                quad(pose.last().pose(), buffers.getBuffer(RenderType.eyes(tex)), 0xF000F0);
            }
            pose.popPose();
            if (e.kind() == ChainMissile.Kind.HOOK && e.getOwner() != null) chain(e, e.getOwner(), partialTick, pose, buffers, light);
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        private static void quad(Matrix4f m, VertexConsumer v, int light) {
            vertex(v, m, -0.5f, -0.5f, 0, 1, light);
            vertex(v, m, 0.5f, -0.5f, 1, 1, light);
            vertex(v, m, 0.5f, 0.5f, 1, 0, light);
            vertex(v, m, -0.5f, 0.5f, 0, 0, light);
        }

        private static void vertex(VertexConsumer v, Matrix4f m, float x, float y, float u, float w, int light) {
            v.addVertex(m, x, y, 0).setColor(255, 255, 255, 255).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(0, 1, 0);
        }

        /** Links of dark iron from the hook back to the thrower's right hand. */
        private void chain(ChainMissile e, Entity owner, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            Vec3 from = e.getPosition(partialTick);
            Vec3 to = owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.6, 0);
            Vec3 d = to.subtract(from);
            int links = Math.max(2, (int) (d.length() * 3));
            VertexConsumer v = buffers.getBuffer(RenderType.lines());
            Matrix4f m = pose.last().pose();
            Vec3 n = d.normalize();
            for (int i = 0; i < links; i += 2) {
                Vec3 a = d.scale(i / (double) links), b = d.scale((i + 1) / (double) links);
                float sag = (float) (Math.sin(Math.PI * i / links) * 0.15);
                v.addVertex(m, (float) a.x, (float) a.y - sag, (float) a.z).setColor(60, 58, 56, 255).setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
                v.addVertex(m, (float) b.x, (float) b.y - sag, (float) b.z).setColor(60, 58, 56, 255).setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
            }
        }
    }
}
