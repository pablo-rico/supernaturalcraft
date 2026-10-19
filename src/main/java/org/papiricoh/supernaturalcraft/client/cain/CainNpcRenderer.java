package org.papiricoh.supernaturalcraft.client.cain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cain.CainNpcEntity;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAssets;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Cain at home (v0.21): the boss's rig and texture, the Mark, the blade and the red eyes hidden (no glow). */
public class CainNpcRenderer extends GeoEntityRenderer<CainNpcEntity> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(CainAssets.GEO), ANIMATION = SupernaturalCraft.asResource(CainAssets.ANIM),
            TEXTURE = SupernaturalCraft.asResource(CainAssets.TEXTURE);

    public CainNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(CainNpcEntity e) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(CainNpcEntity e) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(CainNpcEntity e) {
                return ANIMATION;
            }
        });
        shadowRadius = 0.5f;
    }

    @Override
    public void render(CainNpcEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(MODEL, ANIMATION)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, CainNpcEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            GeoModel<CainNpcEntity> geo = getGeoModel();
            for (String bone : CainAssets.PROCEDURAL) AllegianceGeo.show(geo, bone, false);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
