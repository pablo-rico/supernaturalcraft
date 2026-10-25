package org.papiricoh.supernaturalcraft.client.angel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Angels and the fallen (v0.22), one rig ({@link AngelAssets}): the vessel's texture, its eyes lit (blue, or dimmed for the
 * fallen), the angel blade in its right hand, and its wings only while {@link AngelEntity#wingsShown()}.
 */
public class AngelRenderer extends GeoEntityRenderer<AngelEntity> {

    public AngelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new Eyes(this));
        addRenderLayer(new SmiteGlowLayer<>(this));
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected @Nullable ItemStack getStackForBone(GeoBone bone, AngelEntity e) {
                return AngelAssets.RIGHT_ITEM.equals(bone.getName()) ? e.getMainHandItem() : null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, AngelEntity e) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack pose, GeoBone bone, ItemStack stack, AngelEntity e, MultiBufferSource buffers,
                                              float partialTick, int light, int overlay) {
                // right_item hangs at the hand with no rest rotation: turn the stack as vanilla's ItemInHandLayer does.
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
                super.renderStackForBone(pose, bone, stack, e, buffers, partialTick, light, overlay);
            }
        });
        shadowRadius = 0.5f;
    }

    public static ResourceLocation texture(AngelEntity e) {
        return SupernaturalCraft.asResource(AngelAssets.texture(e.fallen(), e.vessel()));
    }

    @Override
    public void render(AngelEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model("angel"), GeoGuard.animation("angel"))) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, AngelEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            getGeoModel().getBone(AngelAssets.WINGS).ifPresent(b -> {
                b.setHidden(!e.wingsShown());
                b.setChildrenHidden(!e.wingsShown());
            });
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    public RenderType getRenderType(AngelEntity e, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
        // The shadow of the wings is see-through.
        return RenderType.entityTranslucent(texture);
    }

    static class Model extends DefaultedEntityGeoModel<AngelEntity> {
        Model() {
            super(SupernaturalCraft.asResource("angel"), true);
        }

        @Override
        public ResourceLocation getTextureResource(AngelEntity e) {
            ResourceLocation t = texture(e);
            return GeoGuard.exists(t) ? t : SupernaturalCraft.asResource(AngelAssets.texture(e.fallen(), 0));
        }
    }

    /** The eyes: each vessel's own glowmask. */
    static class Eyes extends GeoRenderLayer<AngelEntity> {
        Eyes(GeoRenderer<AngelEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, AngelEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                           VertexConsumer buffer, float partialTick, int light, int overlay) {
            ResourceLocation glow = SupernaturalCraft.asResource(AngelAssets.glowmask(e.fallen(), e.vessel()));
            if (!GeoGuard.exists(glow)) return;
            RenderType eyes = RenderType.eyes(glow);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, 0xF000F0, overlay, 0xFFFFFFFF);
        }
    }
}
