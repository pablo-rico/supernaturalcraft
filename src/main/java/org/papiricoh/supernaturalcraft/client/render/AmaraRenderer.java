package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The Darkness. One model holds both her mass (phases 1-3) and her final form; the phase picks
 * which shows. Rings and cysts are moved every frame to where their hit boxes are.
 */
public class AmaraRenderer extends GeoEntityRenderer<AmaraEntity> {

    public AmaraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0f;
    }

    static class Model extends DefaultedEntityGeoModel<AmaraEntity> {

        Model() {
            super(SupernaturalCraft.asResource("amara"), false);
        }

        @Override
        public void setCustomAnimations(AmaraEntity e, long instanceId, AnimationState<AmaraEntity> state) {
            super.setCustomAnimations(e, instanceId, state);
            float partial = state.getPartialTick();
            float t = e.tickCount + partial;
            for (int i = 0; i < AmaraEntity.ANCHORS; i++) {
                int part = AmaraEntity.FIRST_ANCHOR + i;
                GeoBone ring = getAnimationProcessor().getBone("ring_" + i);
                if (ring == null || ring.isHidden()) continue;
                // GeckoLib negates bone X, and the model is turned 180° (she faces south, yaw 0).
                Vec3 o = e.partOffset(part, partial);
                float k = 16 / AmaraEntity.MODEL_SCALE;
                ring.setPosX((float) o.x * k);
                ring.setPosY((float) (o.y + 1.5) * k);
                ring.setPosZ((float) -o.z * k);
                ring.setRotY(t * 0.06f + i);
                ring.setRotX(0.6f + 0.2f * (float) Math.sin(t * 0.03 + i));
            }
        }
    }

    @Override
    public void preRender(PoseStack pose, AmaraEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        boolean form = e.phase() == 4;
        scaleWidth = scaleHeight = form ? AmaraEntity.FORM_SCALE : AmaraEntity.MODEL_SCALE;
        show("mass", !form);
        show("form", form);
        for (int i = 0; i < AmaraEntity.ANCHORS; i++) show("ring_" + i, !form && e.phase() == 1 && e.partAlive(AmaraEntity.FIRST_ANCHOR + i));
        for (int i = 0; i < AmaraEntity.TENTACLES; i++) {
            show("tentacle_" + i, e.phase() == 2);
            if (e.phase() == 2) show("cyst_" + i, e.partAlive(AmaraEntity.FIRST_TENTACLE + i));
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    private void show(String bone, boolean visible) {
        getGeoModel().getBone(bone).ifPresent(b -> {
            b.setHidden(!visible);
            b.setChildrenHidden(!visible);
        });
    }

    @Override
    protected float getDeathMaxRotation(AmaraEntity entity) {
        return 0f;
    }

    @Override
    public boolean shouldRender(AmaraEntity e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
