package org.papiricoh.supernaturalcraft.client.abaddon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.KnightsCleaverItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The Knights' Cleaver (v0.21): its GeckoLib model with the glowmask (the veins along the blade) beating with the {@code idle}
 * clip's pulse (a 3 s loop: a slow swell, never quite dark); its wave is {@code AbaddonWorldFx.Cleave}.
 */
public class CleaverRenderer extends GeoItemRenderer<KnightsCleaverItem> {

    public CleaverRenderer(KnightsCleaverItem item) {
        super(new DefaultedItemGeoModel<>(BuiltInRegistries.ITEM.getKey(item)));
        addRenderLayer(new Veins(this));
    }

    /** The veins' strength this frame (0.45-1). */
    static float pulse(float partial) {
        Minecraft mc = Minecraft.getInstance();
        float t = (mc.level != null ? mc.level.getGameTime() : 0) + partial;
        float s = 0.5f + 0.5f * Mth.sin(t * Mth.TWO_PI / 60f);
        return 0.45f + 0.55f * s * s;
    }

    static final class Veins extends AutoGlowingGeoLayer<KnightsCleaverItem> {
        Veins(CleaverRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, KnightsCleaverItem item, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            if (!GeoGuard.exists(GeoGuard.glowmask(getTextureResource(item)))) return;
            RenderType glow = getRenderType(item, buffers);
            if (glow == null) return;
            float k = pulse(partialTick);
            int c = Math.round(255 * k);
            getRenderer().reRender(model, pose, buffers, item, glow, buffers.getBuffer(glow), partialTick, 0xF00000, overlay,
                    0xFF << 24 | c << 16 | Math.round(c * 0.92f) << 8 | Math.round(c * 0.9f));
        }
    }
}
