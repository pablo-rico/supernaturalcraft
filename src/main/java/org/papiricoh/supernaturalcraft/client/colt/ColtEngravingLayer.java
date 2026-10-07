package org.papiricoh.supernaturalcraft.client.colt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * NON TIMEBO MALA, lit in gold: it flares when the gun fires and fades over a second, and glows
 * steadily while its holder turns the gun to read it.
 */
public class ColtEngravingLayer extends GeoRenderLayer<ColtItem> {

    public static final ResourceLocation TEXTURE = SupernaturalCraft.asResource("textures/item/the_colt_engraving.png");
    public static final float SHOT_FADE = 25, INSPECT_IN = 8, INSPECT_HOLD = 30, INSPECT_OUT = 12;

    public ColtEngravingLayer(ColtRenderer renderer) {
        super(renderer);
    }

    /** How brightly the motto burns, from ticks since the last shot and since an inspection began. */
    public static float glow(double sinceShot, double sinceInspect) {
        float shot = sinceShot >= 0 && sinceShot < SHOT_FADE ? 1 - (float) (sinceShot / SHOT_FADE) : 0;
        float inspect = 0;
        if (sinceInspect >= 0 && sinceInspect < INSPECT_IN + INSPECT_HOLD + INSPECT_OUT) {
            inspect = (float) Math.min(Math.min(1, sinceInspect / INSPECT_IN),
                    (INSPECT_IN + INSPECT_HOLD + INSPECT_OUT - sinceInspect) / INSPECT_OUT) * 0.8f;
        }
        return Mth.clamp(Math.max(shot, inspect), 0, 1);
    }

    @Override
    public void render(PoseStack poseStack, ColtItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        ColtRenderer renderer = (ColtRenderer) getRenderer();
        long id = renderer.gunId();
        float a = glow(ColtClient.sinceShot(id, partialTick), ColtClient.sinceInspect(id, partialTick));
        if (a <= 0.02f) return;
        RenderType gold = RenderType.entityTranslucentEmissive(TEXTURE);
        renderer.reRender(bakedModel, poseStack, bufferSource, animatable, gold, bufferSource.getBuffer(gold), partialTick,
                LightTexture.FULL_BRIGHT, packedOverlay, FastColor.ARGB32.color(Math.round(a * 255), 255, 255, 255));
    }
}
