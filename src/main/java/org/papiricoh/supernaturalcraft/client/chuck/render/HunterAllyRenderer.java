package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.HunterAllyEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dean, Sam and Castiel at the very end: {@code hunter_<who>.geo.json} on the shared {@code hunter_ally.animation.json}.
 * They step out of the light (fading in over a second and a half, rimmed in warm light) and go back into it when the
 * Author approves (or when he is gone), dimming out with the same rim.
 */
public class HunterAllyRenderer extends EntityRenderer<HunterAllyEntity> {

    /** Ticks to fade in, and to fade out. */
    public static final int APPEAR = 30, FADE = 50;

    private final Ally[] allies = new Ally[HunterAllyEntity.NAMES.length];
    /** When each ally began to fade (client ticks of the level), or absent. */
    private static final Map<HunterAllyEntity, Long> FADING = new WeakHashMap<>();

    public HunterAllyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        for (int i = 0; i < allies.length; i++) allies[i] = new Ally(ctx, HunterAllyEntity.NAMES[i]);
        shadowRadius = 0.5f;
    }

    @Override
    public void render(HunterAllyEntity ally, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int who = Mth.clamp(ally.who(), 0, allies.length - 1);
        String name = HunterAllyEntity.NAMES[who];
        if (!GeoGuard.ready(GeoGuard.model(name), GeoGuard.animation("hunter_ally"))) return;
        allies[who].render(ally, yaw, partialTick, pose, buffers, light);
    }

    /** 0 (not there) to 1 (fully there): in from the light, and back out when the Author approves. */
    public static float presence(HunterAllyEntity ally, float partialTick) {
        float in = Mth.clamp((ally.tickCount + partialTick) / APPEAR, 0, 1);
        long now = ally.level().getGameTime();
        Long since = FADING.get(ally);
        if (since == null && leaving(ally)) FADING.put(ally, since = now);
        float out = since == null ? 1 : Mth.clamp(1 - (now - since + partialTick) / FADE, 0, 1);
        return in * out;
    }

    private static boolean leaving(HunterAllyEntity ally) {
        var bosses = ally.level().getEntitiesOfClass(ChuckEntity.class, ally.getBoundingBox().inflate(48));
        if (bosses.isEmpty()) return ally.tickCount > APPEAR * 2;
        ChuckEntity chuck = bosses.getFirst();
        return !chuck.isAlive() || chuck.finaleStage() >= ChuckEntity.FINALE_APPROVAL;
    }

    @Override
    public ResourceLocation getTextureLocation(HunterAllyEntity ally) {
        return texture(HunterAllyEntity.NAMES[Mth.clamp(ally.who(), 0, HunterAllyEntity.NAMES.length - 1)]);
    }

    static ResourceLocation texture(String name) {
        return SupernaturalCraft.asResource("textures/entity/" + name + ".png");
    }

    private static final class Ally extends GeoEntityRenderer<HunterAllyEntity> {

        Ally(EntityRendererProvider.Context ctx, String name) {
            super(ctx, new DefaultedEntityGeoModel<HunterAllyEntity>(SupernaturalCraft.asResource(name), true) {
                @Override
                public ResourceLocation getAnimationResource(HunterAllyEntity animatable) {
                    return GeoGuard.animation("hunter_ally");
                }
            });
            addRenderLayer(new OptionalGlowLayer<>(this));
            addRenderLayer(new Rim(this));
            shadowRadius = 0.5f;
        }

        @Override
        public void preRender(PoseStack pose, HunterAllyEntity ally, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, ally.yRotO, ally.getYRot())));
            super.preRender(pose, ally, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        @Override
        public Color getRenderColor(HunterAllyEntity ally, float partialTick, int light) {
            return Color.ofARGB((int) (255 * presence(ally, partialTick)), 255, 255, 255);
        }

        @Override
        public RenderType getRenderType(HunterAllyEntity ally, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
            return presence(ally, partialTick) < 0.999f ? RenderType.entityTranslucent(texture) : super.getRenderType(ally, texture, buffers, partialTick);
        }
    }

    /** The light they step out of: a warm emissive shell, strongest while they are only half there. */
    private static final class Rim extends GeoRenderLayer<HunterAllyEntity> {

        Rim(GeoRenderer<HunterAllyEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, HunterAllyEntity ally, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                           VertexConsumer buffer, float partialTick, int light, int overlay) {
            float p = presence(ally, partialTick);
            float glow = p < 0.999f ? 0.25f + 0.75f * (1 - Math.abs(p * 2 - 1))
                    : 0.18f + 0.06f * Mth.sin((ally.tickCount + partialTick) * 0.12f);
            int a = (int) (Mth.clamp(glow, 0, 1) * 150);
            if (a <= 2) return;
            RenderType rt = RenderType.entityTranslucentEmissive(getTextureResource(ally));
            pose.pushPose();
            pose.scale(1.04f, 1.015f, 1.04f);
            getRenderer().reRender(model, pose, buffers, ally, rt, buffers.getBuffer(rt), partialTick, 0xF000F0, overlay,
                    (a << 24) | 0xFFE9B0);
            pose.popPose();
        }
    }
}
