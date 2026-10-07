package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraShade;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** A shade is her final form, small: the same model, only the {@code form} bones, under half size. */
public class AmaraShadeRenderer extends GeoEntityRenderer<AmaraShade> {

    public static final float SCALE = 0.42f;
    private static final String[] HIDDEN = {"mass", "ring_0", "ring_1", "ring_2", "ring_3", "tentacle_0", "tentacle_1", "tentacle_2",
            "tentacle_3", "tentacle_4", "tentacle_5", "form_halo"};

    public AmaraShadeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("amara"), true) {
            @Override
            public net.minecraft.resources.ResourceLocation getAnimationResource(AmaraShade animatable) {
                return SupernaturalCraft.asResource("animations/entity/amara.animation.json");
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.3f;
        scaleWidth = scaleHeight = SCALE;
    }

    @Override
    public void preRender(PoseStack pose, AmaraShade e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        for (String b : HIDDEN) {
            getGeoModel().getBone(b).ifPresent(bone -> {
                bone.setHidden(true);
                bone.setChildrenHidden(true);
            });
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
