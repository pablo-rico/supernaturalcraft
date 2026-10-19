package org.papiricoh.supernaturalcraft.client.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The glowmask drawn only as strongly as {@link Strength} says (v0.21): a witch's eyes and runes while she casts, fading in
 * and out instead of snapping, with a faint flicker at full strength. The emissive pass is additive, so scaling its colour
 * fades it. Quiet while the texture has no {@code _glowmask}.
 */
public class FadingGlowLayer<T extends Entity & GeoAnimatable> extends AutoGlowingGeoLayer<T> {

    /** How strongly the glowmask shows (0-1) this frame. */
    @FunctionalInterface
    public interface Strength<T> {
        float of(T entity, float partial);
    }

    private final Strength<T> strength;

    public FadingGlowLayer(GeoRenderer<T> renderer, Strength<T> strength) {
        super(renderer);
        this.strength = strength;
    }

    /** Casting ({@code casting} true) fades the glow in over 3 ticks and out over 8. */
    public static <T extends Entity & GeoAnimatable> FadingGlowLayer<T> whileCasting(GeoRenderer<T> renderer,
                                                                                  java.util.function.BiPredicate<T, Float> casting) {
        return new FadingGlowLayer<>(renderer, (e, partial) -> {
            float k = WitchGeo.fade(e, "glow", casting.test(e, partial) ? 1 : 0, 3, 8, partial);
            return k * (0.88f + 0.12f * Mth.sin((e.tickCount + partial) * 0.9f + e.getId()));
        });
    }

    @Override
    public void render(PoseStack pose, T animatable, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                       float partialTick, int light, int overlay) {
        if (!GeoGuard.exists(GeoGuard.glowmask(getTextureResource(animatable)))) return;
        float k = Mth.clamp(strength.of(animatable, partialTick), 0, 1);
        if (k < 0.02f) return;
        RenderType glow = getRenderType(animatable, buffers);
        if (glow == null) return;
        int c = getRenderer().getRenderColor(animatable, partialTick, light).argbInt();
        int r = Math.round((c >> 16 & 0xFF) * k), g = Math.round((c >> 8 & 0xFF) * k), b = Math.round((c & 0xFF) * k);
        int a = Math.round((c >>> 24) * k);
        getRenderer().reRender(model, pose, buffers, animatable, glow, buffers.getBuffer(glow), partialTick, 0xF00000, overlay,
                a << 24 | r << 16 | g << 8 | b);
    }
}
