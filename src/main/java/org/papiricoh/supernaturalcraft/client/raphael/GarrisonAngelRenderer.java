package org.papiricoh.supernaturalcraft.client.raphael;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.GarrisonAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAssets;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * An angel of Raphael's garrison (v0.16): the Host's rig ({@link RaphaelAssets#GARRISON_GEO}) in storm grey and silver, its
 * folded wings of smoke, its eyes and blade lit; never the captain's helm, plume, tabard, cloak or open wings.
 */
public class GarrisonAngelRenderer extends GeoEntityRenderer<GarrisonAngelEntity> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(RaphaelAssets.GARRISON_GEO),
            ANIMATION = SupernaturalCraft.asResource(RaphaelAssets.GARRISON_ANIM),
            TEXTURE = SupernaturalCraft.asResource(RaphaelAssets.GARRISON_TEXTURE), GLOW = GeoGuard.glowmask(TEXTURE);
    private static final String[] CAPTAIN_ONLY = {"helmet", "plume", "wings_open", "tabard", "cloak"};

    public GarrisonAngelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new Eyes(this));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(GarrisonAngelEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(MODEL, ANIMATION)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public RenderType getRenderType(GarrisonAngelEntity e, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void preRender(PoseStack pose, GarrisonAngelEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            for (String bone : CAPTAIN_ONLY) AllegianceGeo.show(getGeoModel(), bone, false);
            AllegianceGeo.show(getGeoModel(), "wings_folded", true);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    static final class Model extends GeoModel<GarrisonAngelEntity> {
        @Override
        public ResourceLocation getModelResource(GarrisonAngelEntity e) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(GarrisonAngelEntity e) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(GarrisonAngelEntity e) {
            return ANIMATION;
        }
    }

    /** Its glowmask (eyes, blade edge, the wings' light) at full bright. */
    static final class Eyes extends GeoRenderLayer<GarrisonAngelEntity> {
        Eyes(GeoRenderer<GarrisonAngelEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, GarrisonAngelEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                           VertexConsumer buffer, float partialTick, int light, int overlay) {
            if (!GeoGuard.exists(GLOW)) return;
            RenderType eyes = RenderType.eyes(GLOW);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, LightTexture.FULL_BRIGHT, overlay,
                    0xFFFFFFFF);
        }
    }
}
