package org.papiricoh.supernaturalcraft.client.gabriel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.PieProjectile;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The cream pie in flight (v0.14): its own small model, tumbling end over end as it flies. Nothing until the model is baked. */
public class PieRenderer extends GeoEntityRenderer<PieProjectile> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(GabrielAssets.PIE_GEO),
            TEXTURE = SupernaturalCraft.asResource(GabrielAssets.PIE_TEXTURE);

    public PieRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(PieProjectile p) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(PieProjectile p) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(PieProjectile p) {
                // No clips: GeckoLib only asks when a controller plays one.
                return SupernaturalCraft.asResource("animations/entity/gabriel_pie.animation.json");
            }
        });
        shadowRadius = 0.15f;
    }

    @Override
    public void render(PieProjectile pie, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(MODEL, null)) return;
        float t = pie.tickCount + partialTick;
        pose.pushPose();
        // Spin about its own middle: up, turn, back down.
        pose.translate(0, 0.15, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-pie.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(t * 24f));
        pose.mulPose(Axis.ZP.rotationDegrees(t * 9f));
        pose.translate(0, -0.15, 0);
        super.render(pie, 0, partialTick, pose, buffers, light);
        pose.popPose();
    }
}
