package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.amara.ClientAmara;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Darkness (v0.42): one renderer, two GeckoLib rigs, as the Author's is. {@link AmaraLook#form()} picks which:
 * {@code MASS} draws the {@code amara} rig (phase I: rings moved onto their hit boxes, tentacles and cysts by the parts still
 * standing); any other form draws the {@code amara_form} rig ({@link AmaraFormRenderer}) in that form's age, with the toggled
 * bones of the phase. When the form changes every animation controller is reset, so no clip of one body lingers on the other.
 * Each half stays silent until GeckoLib has its files.
 */
public class AmaraRenderer extends EntityRenderer<AmaraEntity> {

    public static final ResourceLocation MASS_TEXTURE = SupernaturalCraft.asResource(AmaraAssets.MASS_TEXTURE);

    private final Mass mass;
    private final AmaraFormRenderer<AmaraEntity> form;
    private final Map<Integer, Integer> lastForm = new HashMap<>();

    public AmaraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        mass = new Mass(ctx);
        form = new AmaraFormRenderer<>(ctx, e -> {
            AmaraLook look = ClientAmara.look(e);
            return look == null || look.age() == null ? AmaraAssets.Age.WOMAN : look.age();
        }, AmaraRenderer::shownBones);
        shadowRadius = 0f;
    }

    /** The toggled bones the boss shows for her phase. */
    static List<String> shownBones(AmaraEntity e) {
        AmaraLook look = ClientAmara.look(e);
        List<String> on = new ArrayList<>();
        if (look == null) return on;
        int phase = look.phase(), f = look.form();
        boolean spent = f == AmaraLook.SPENT;
        if (f == AmaraLook.VOID || (phase == 3 || phase == 4) && !e.onGround()) on.add("smoke_skirt");
        if (phase >= 3 && !spent) on.add("tendrils");
        if ((phase >= 5 || look.enraged()) && !spent) on.add("corona");
        if (look.skyStage() == 5) on.add("tears");
        if (ClientAmara.chainsShown(e.getId())) on.add("chains");
        return on;
    }

    @Override
    public void render(AmaraEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        AmaraLook look = ClientAmara.look(entity);
        int f = look == null ? AmaraLook.MASS : look.form();
        Integer before = lastForm.put(entity.getId(), f);
        if (before != null && before != f) resetAnimations(entity);
        if (lastForm.size() > 32) lastForm.keySet().removeIf(id -> id != entity.getId());
        if (f == AmaraLook.MASS) {
            if (GeoGuard.ready(GeoGuard.model(AmaraAssets.RIG_MASS), GeoGuard.animation(AmaraAssets.RIG_MASS))) {
                mass.render(entity, yaw, partialTick, pose, buffers, light);
            }
        } else {
            form.render(entity, yaw, partialTick, pose, buffers, light);
        }
    }

    /** Every controller back to its start: the next frame plays the new body's own clips from the beginning. */
    private static void resetAnimations(AmaraEntity e) {
        var manager = e.getAnimatableInstanceCache().getManagerForId(e.getId());
        for (AnimationController<?> c : manager.getAnimationControllers().values()) c.forceAnimationReset();
    }

    @Override
    public ResourceLocation getTextureLocation(AmaraEntity entity) {
        return MASS_TEXTURE;
    }

    @Override
    public boolean shouldRender(AmaraEntity e, Frustum frustum, double x, double y, double z) {
        return e.shouldRender(x, y, z);
    }

    // --- phase I: the mass ---------------------------------------------------------------------------------------------

    static class Mass extends GeoEntityRenderer<AmaraEntity> {

        Mass(EntityRendererProvider.Context ctx) {
            super(ctx, new MassModel());
            addRenderLayer(new OptionalGlowLayer<>(this));
            shadowRadius = 0f;
            scaleWidth = scaleHeight = AmaraEntity.MODEL_SCALE;
        }

        @Override
        public void preRender(PoseStack pose, AmaraEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                scaleWidth = scaleHeight = AmaraEntity.MODEL_SCALE;
                show("mass", true);
                show("form", false);
                boolean tentacles = false;
                for (int i = 0; i < AmaraEntity.TENTACLES; i++) tentacles |= e.partAlive(AmaraEntity.FIRST_TENTACLE + i);
                for (int i = 0; i < AmaraEntity.ANCHORS; i++) show("ring_" + i, e.partAlive(AmaraEntity.FIRST_ANCHOR + i));
                for (int i = 0; i < AmaraEntity.TENTACLES; i++) {
                    show("tentacle_" + i, tentacles);
                    if (tentacles) show("cyst_" + i, e.partAlive(AmaraEntity.FIRST_TENTACLE + i));
                }
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
    }

    static class MassModel extends DefaultedEntityGeoModel<AmaraEntity> {

        MassModel() {
            super(SupernaturalCraft.asResource(AmaraAssets.RIG_MASS), false);
        }

        @Override
        public void setCustomAnimations(AmaraEntity e, long instanceId, AnimationState<AmaraEntity> state) {
            super.setCustomAnimations(e, instanceId, state);
            float partial = state.getPartialTick();
            double t = e.level().getGameTime() + partial;
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
                ring.setRotY((float) (t * 0.06 + i));
                ring.setRotX(0.6f + 0.2f * (float) Math.sin(t * 0.03 + i));
            }
        }
    }
}
