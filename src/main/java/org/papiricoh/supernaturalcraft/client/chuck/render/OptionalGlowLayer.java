package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** {@link AutoGlowingGeoLayer} that stays quiet while the texture has no {@code _glowmask} (yet). */
public class OptionalGlowLayer<T extends GeoAnimatable> extends AutoGlowingGeoLayer<T> {

    public OptionalGlowLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack pose, T animatable, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                       float partialTick, int light, int overlay) {
        if (!GeoGuard.exists(GeoGuard.glowmask(getTextureResource(animatable)))) return;
        super.render(pose, animatable, model, type, buffers, buffer, partialTick, light, overlay);
    }
}
