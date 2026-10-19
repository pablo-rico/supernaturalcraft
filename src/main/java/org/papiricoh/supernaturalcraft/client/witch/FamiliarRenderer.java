package org.papiricoh.supernaturalcraft.client.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchAssets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * A witch's familiar (v0.21): the cat's or the raven's rig by its variant, its eyes always glowing, its head turned toward what
 * it watches, and what it stole ({@link FamiliarEntity#held()}, a taglock) carried crosswise in its mouth or beak at the prop bone
 * {@code held_item} ({@link WitchAssets#PROPS}). The raven's wings are left to its clips.
 */
public class FamiliarRenderer extends GeoEntityRenderer<FamiliarEntity> {

    public FamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new OptionalGlowLayer<>(this));
        addRenderLayer(new HeldItemLayer(this));
        shadowRadius = 0.3f;
    }

    @Override
    public void render(FamiliarEntity f, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        String rig = f.variant().rig();
        if (!GeoGuard.ready(GeoGuard.model(rig), GeoGuard.animation(rig)) || WitchGeo.vanished(f)) return;
        super.render(f, yaw, partialTick, pose, buffers, light);
    }

    /** The rig follows the variant ({@code familiar_cat}, {@code familiar_raven}); the head follows the look. */
    static final class Model extends GeoModel<FamiliarEntity> {
        private static ResourceLocation res(String path) {
            return SupernaturalCraft.asResource(path);
        }

        @Override
        public ResourceLocation getModelResource(FamiliarEntity f) {
            return res(WitchAssets.geo(f.variant().rig()));
        }

        @Override
        public ResourceLocation getTextureResource(FamiliarEntity f) {
            return res(WitchAssets.texture(f.variant().rig()));
        }

        @Override
        public ResourceLocation getAnimationResource(FamiliarEntity f) {
            return res(WitchAssets.anim(f.variant().rig()));
        }

        @Override
        public void setCustomAnimations(FamiliarEntity f, long instanceId, AnimationState<FamiliarEntity> state) {
            super.setCustomAnimations(f, instanceId, state);
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (data == null) return;
            getBone("head").ifPresent(b -> {
                b.setRotX(b.getRotX() + Mth.clamp(data.headPitch(), -40, 40) * Mth.DEG_TO_RAD);
                b.setRotY(b.getRotY() + Mth.clamp(data.netHeadYaw(), -60, 60) * Mth.DEG_TO_RAD);
            });
        }
    }

    /** The stolen thing in its mouth, crosswise like a fox's (the bone is an empty locator at the tip of the mouth/beak). */
    static final class HeldItemLayer extends GeoRenderLayer<FamiliarEntity> {
        HeldItemLayer(FamiliarRenderer renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(PoseStack pose, FamiliarEntity f, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                                  float partialTick, int light, int overlay) {
            if (!"held_item".equals(bone.getName())) return;
            ItemStack held = f.held();
            if (held.isEmpty()) return;
            pose.pushPose();
            pose.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
            // Crosswise in the jaws: the sprite's plane runs along the body, so it reads from the side; a raven's smaller.
            boolean raven = f.variant() == FamiliarEntity.Variant.RAVEN;
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-35));
            float s = raven ? 0.55f : 0.7f;
            pose.scale(s, s, s);
            pose.translate(0, -0.12, 0);
            Minecraft.getInstance().getItemRenderer().renderStatic(f, held, ItemDisplayContext.GROUND, false, pose, buffers, f.level(), light,
                    overlay, f.getId());
            pose.popPose();
        }
    }
}
