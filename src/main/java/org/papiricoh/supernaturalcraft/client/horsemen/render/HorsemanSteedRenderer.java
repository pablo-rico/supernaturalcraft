package org.papiricoh.supernaturalcraft.client.horsemen.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanSteedEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenAnimations;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** A Horseman's horse: one rig, four coats ({@code horseman_steed_<horseman>.png}); the saddle shows once it has one. */
public class HorsemanSteedRenderer extends GeoEntityRenderer<HorsemanSteedEntity> {

    public HorsemanSteedRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("horseman_steed"), false) {
            @Override
            public ResourceLocation getTextureResource(HorsemanSteedEntity steed) {
                return SupernaturalCraft.asResource("textures/entity/" + steed.kind().steedTexture() + ".png");
            }
        });
        addRenderLayer(new OptionalGlowLayer<>(this));
        shadowRadius = 0.9f;
    }

    @Override
    public void render(HorsemanSteedEntity steed, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model("horseman_steed"), GeoGuard.animation("horseman_steed"))) return;
        super.render(steed, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, HorsemanSteedEntity steed, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        HorsemanRenderer.show(model, HorsemenAnimations.SADDLE_BONE, steed.isSaddled());
        super.preRender(pose, steed, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
