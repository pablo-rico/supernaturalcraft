package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorHandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * One of the Author's giant hands: {@code author_hand.geo.json} (a right hand) at its own size, mirrored in X for his
 * left. Light, not flesh: translucent and full bright, turned by the entity's yaw and pitch.
 */
public class AuthorHandRenderer extends GeoEntityRenderer<AuthorHandEntity> {

    /** The model is built at the size it is drawn. */
    public static final float SCALE = 1f;

    public AuthorHandRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("author_hand")));
        addRenderLayer(new OptionalGlowLayer<>(this));
        scaleWidth = scaleHeight = SCALE;
        shadowRadius = 0f;
    }

    @Override
    public void render(AuthorHandEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model("author_hand"), GeoGuard.animation("author_hand"))) return;
        super.render(entity, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    public void preRender(PoseStack pose, AuthorHandEntity hand, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            // Not a living thing: GeckoLib leaves its yaw to us. Turn first, then mirror the right hand into a left.
            pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, hand.yRotO, hand.getYRot())));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, hand.xRotO, hand.getXRot())));
            if (hand.left()) pose.scale(-1, 1, 1);
        }
        super.preRender(pose, hand, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** Translucent (and never culled, so the mirrored left hand keeps its faces). */
    @Override
    public RenderType getRenderType(AuthorHandEntity hand, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRender(AuthorHandEntity hand, Frustum frustum, double x, double y, double z) {
        return hand.shouldRender(x, y, z) && frustum.isVisible(hand.getBoundingBox().inflate(6));
    }
}
