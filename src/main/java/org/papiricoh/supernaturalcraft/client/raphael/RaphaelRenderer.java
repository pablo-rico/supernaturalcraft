package org.papiricoh.supernaturalcraft.client.raphael;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAssets;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

import java.util.Map;

/**
 * Raphael (v0.16): his season-five vessel ({@link RaphaelAssets#GEO}). Drawn translucent (his storm-cloud wings fade at their
 * edges) with the glowmask at full bright: his eyes always, the lightning in his wings, and the veins of light that burn
 * through the vessel in phase III. What shows follows the contract: {@code wings} only while the server says (the reveal, the
 * wrath), the vein shells ({@link RaphaelAssets#VEIN_BONES}) only with {@link RaphaelEntity#veinsLit()}, each prop of
 * {@link RaphaelAssets#PROPS} only while its clip plays. Held in a ring of holy fire he is lit by it, flickering, and shakes.
 */
public class RaphaelRenderer extends GeoEntityRenderer<RaphaelEntity> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(RaphaelAssets.GEO),
            ANIMATION = SupernaturalCraft.asResource(RaphaelAssets.ANIM), TEXTURE = SupernaturalCraft.asResource(RaphaelAssets.TEXTURE),
            GLOW = SupernaturalCraft.asResource(RaphaelAssets.GLOW);

    public RaphaelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new Glow(this));
        shadowRadius = 0.5f;
    }

    /** Whether the rig and its clips are loaded (the art may still be on its way). */
    public static boolean ready() {
        return GeoGuard.ready(MODEL, ANIMATION);
    }

    /** Whether his wings show: the synced flag, or the reveal while it plays. */
    static boolean wings(RaphaelEntity e, String clip) {
        return e.wingsShown() || "wings_reveal".equals(clip);
    }

    @Override
    public void render(RaphaelEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!ready()) return;
        pose.pushPose();
        if (e.trapped() && e.isAlive()) {
            // Held in the fire: he strains against it.
            float t = e.tickCount + partialTick;
            pose.translate(0.025f * Mth.sin(t * 2.7f), 0, 0.025f * Mth.cos(t * 3.1f));
        }
        super.render(e, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }

    /** His wings reach far past his hitbox. */
    @Override
    public boolean shouldRender(RaphaelEntity e, Frustum frustum, double x, double y, double z) {
        if (!e.shouldRender(x, y, z)) return false;
        double r = e.wingsShown() ? 2.6 : 0.5;
        return frustum.isVisible(e.getBoundingBox().inflate(r, 1, r));
    }

    @Override
    public RenderType getRenderType(RaphaelEntity e, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void preRender(PoseStack pose, RaphaelEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            GeoModel<RaphaelEntity> geo = getGeoModel();
            String clip = AllegianceGeo.clip(e, getInstanceId(e), "action");
            AllegianceGeo.show(geo, "wings", wings(e, clip));
            boolean veins = e.veinsLit();
            for (String bone : RaphaelAssets.VEIN_BONES) AllegianceGeo.show(geo, bone, veins);
            for (Map.Entry<String, String> prop : RaphaelAssets.PROPS.entrySet()) {
                AllegianceGeo.show(geo, prop.getKey(), prop.getValue().equals(clip));
            }
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** In the holy fire: lit gold and red by it, the light flickering. */
    @Override
    public Color getRenderColor(RaphaelEntity e, float partialTick, int light) {
        if (!e.trapped()) return super.getRenderColor(e, partialTick, light);
        float t = e.tickCount + partialTick;
        float f = 0.85f + 0.1f * Mth.sin(t * 0.9f) + 0.05f * Mth.sin(t * 2.3f);
        return Color.ofARGB(1f, 1f, 0.78f * f, 0.55f * f);
    }

    @Override
    protected float getDeathMaxRotation(RaphaelEntity e) {
        return 0f;
    }

    /** The rig, the clips and the one texture. */
    static final class Model extends GeoModel<RaphaelEntity> {
        @Override
        public ResourceLocation getModelResource(RaphaelEntity e) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(RaphaelEntity e) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(RaphaelEntity e) {
            return ANIMATION;
        }
    }

    /** The glowmask at full bright, over whatever shows. */
    static final class Glow extends GeoRenderLayer<RaphaelEntity> {
        Glow(GeoRenderer<RaphaelEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, RaphaelEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                           VertexConsumer buffer, float partialTick, int light, int overlay) {
            if (!GeoGuard.exists(GLOW)) return;
            RenderType eyes = RenderType.eyes(GLOW);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, LightTexture.FULL_BRIGHT, overlay,
                    0xFFFFFFFF);
        }
    }
}
