package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.InkEchoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An old enemy written back in ink: the boss's own GeckoLib model with Art-2's ink texture
 * ({@code textures/entity/ink/<boss>.png}, same UV layout), translucent, running down the page as
 * {@link InkEchoEntity#fade()} grows (it sinks, sags and thins out). The two great multipart bosses come back small:
 * Amara as her final form alone (as her shades are), the Broken Chorus whole but shrunk, its ruined variants hidden.
 */
public class InkEchoRenderer extends EntityRenderer<InkEchoEntity> {

    private record Look(String geo, float scale, List<String> hidden) {
    }

    private static final List<String> LUCIFER_WINGS = List.of("wing_r1", "wing_l1", "wing_r2", "wing_l2", "wing_r3", "wing_l3");
    private static final Map<String, Look> LOOKS = Map.of(
            "lucifer", new Look("lucifer", 1f, concat(LUCIFER_WINGS, List.of("halo"))),
            "lucifer_uncaged", new Look("lucifer_uncaged", 1.15f, List.of("wing_r2", "wing_l2", "wing_r3", "wing_l3", "halo")),
            "azazel", new Look("azazel", 1f, List.of()),
            "lilith", new Look("lilith", 1f, List.of()),
            "metatron", new Look("metatron", 1f, List.of("wings", "robe", "tablet")),
            "amara", new Look("amara", 1.1f, List.of("mass", "ring_0", "ring_1", "ring_2", "ring_3", "tentacle_0", "tentacle_1",
                    "tentacle_2", "tentacle_3", "tentacle_4", "tentacle_5", "form_halo")),
            "broken_chorus", new Look("broken_chorus", 0.6f, chorusRuins()));

    private final Map<String, Echo> echoes = new HashMap<>();

    public InkEchoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        LOOKS.forEach((boss, look) -> echoes.put(boss, new Echo(ctx, boss, look)));
        shadowRadius = 0f;
    }

    @Override
    public void render(InkEchoEntity echo, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Echo r = echoes.get(echo.boss());
        if (r == null || echo.fade() >= 0.995f) return;
        if (!GeoGuard.ready(GeoGuard.model(r.look.geo), GeoGuard.animation(r.look.geo))) return;
        r.render(echo, yaw, partialTick, pose, buffers, Math.max(light, 0xA000A0));
    }

    @Override
    public boolean shouldRender(InkEchoEntity echo, Frustum frustum, double x, double y, double z) {
        return echo.shouldRender(x, y, z) && frustum.isVisible(echo.getBoundingBox().inflate(4, 6, 4));
    }

    @Override
    public ResourceLocation getTextureLocation(InkEchoEntity echo) {
        return ink(echo.boss());
    }

    public static ResourceLocation ink(String boss) {
        return SupernaturalCraft.asResource("textures/entity/ink/" + boss + ".png");
    }

    private static final class Echo extends GeoEntityRenderer<InkEchoEntity> {

        private final Look look;

        Echo(EntityRendererProvider.Context ctx, String boss, Look look) {
            super(ctx, new DefaultedEntityGeoModel<InkEchoEntity>(SupernaturalCraft.asResource(look.geo)) {
                @Override
                public ResourceLocation getTextureResource(InkEchoEntity animatable) {
                    return ink(boss);
                }
            });
            this.look = look;
            addRenderLayer(new OptionalGlowLayer<>(this));
            scaleWidth = scaleHeight = look.scale;
            shadowRadius = 0f;
        }

        @Override
        public void preRender(PoseStack pose, InkEchoEntity echo, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                for (String b : look.hidden) {
                    getGeoModel().getBone(b).ifPresent(bone -> {
                        bone.setHidden(true);
                        bone.setChildrenHidden(true);
                    });
                }
                pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, echo.yRotO, echo.getYRot())));
                // Running down the page: it sinks a little, sags, and its ink spreads at the feet.
                float f = Mth.clamp(echo.fade(), 0, 1);
                float wobble = 0.015f * Mth.sin((float) (echo.level().getGameTime() + partialTick) * 0.4f);
                pose.translate(0, -0.6f * f * f, 0);
                pose.scale(1 + 0.18f * f + wobble, 1 - 0.32f * f, 1 + 0.18f * f - wobble);
            }
            super.preRender(pose, echo, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        /** The boss's baked bones are shared with its own renderer: put back what the echo hid. */
        @Override
        public void render(InkEchoEntity echo, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            super.render(echo, yaw, partialTick, pose, buffers, light);
            for (String b : look.hidden) {
                getGeoModel().getBone(b).ifPresent(bone -> {
                    bone.setHidden(false);
                    bone.setChildrenHidden(false);
                });
            }
        }

        @Override
        public Color getRenderColor(InkEchoEntity echo, float partialTick, int light) {
            float a = Mth.clamp(0.92f * (1 - echo.fade()), 0, 1);
            return Color.ofARGB((int) (a * 255), 255, 255, 255);
        }

        @Override
        public RenderType getRenderType(InkEchoEntity echo, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }

        @Override
        protected float getDeathMaxRotation(InkEchoEntity echo) {
            return 0f;
        }
    }

    private static List<String> concat(List<String> a, List<String> b) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(a);
        out.addAll(b);
        return List.copyOf(out);
    }

    /** The Chorus whole: no broken faces, stumps, sockets or loose fragments. */
    private static List<String> chorusRuins() {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(List.of("fragments"));
        for (String f : org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry.FACE_NAMES) {
            out.add("face_" + f + "_cracked");
            out.add("face_" + f + "_broken");
        }
        for (String w : org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry.WING_NAMES) out.add("wing_" + w + "_stump");
        for (int i = 0; i < org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry.EYES; i++) out.add("eye_" + i + "_socket");
        return List.copyOf(out);
    }
}
