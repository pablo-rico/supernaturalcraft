package org.papiricoh.supernaturalcraft.client.apocworld;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldDimension;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;

/**
 * The Apocalypse World's sky (v0.41, drawn for {@link ApocWorldEffects}): a vault from a dark smoke-brown overhead to a dull ember
 * glow at the horizon, the art's overcast of smoke with ember streaks over it ({@code apocalypse_world_sky.png}, a box like the
 * End sky, drifting), and the dim red sun low in the west ({@code apocalypse_world_sun.png}, or a plain red disc while the art is
 * missing). The fog is the same brown, close.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ApocWorldSky {

    static final ResourceLocation SKY = SupernaturalCraft.asResource("textures/environment/apocalypse_world_sky.png");
    static final ResourceLocation SUN = SupernaturalCraft.asResource("textures/environment/apocalypse_world_sun.png");
    /** Overhead, at the horizon, below it; the fog. */
    static final int ZENITH = 0x2E241E, HORIZON = 0x7A4A2E, NADIR = 0x3A2C24, FOG = 0x5A4434;
    static final int LAT = 16, LON = 24;

    private ApocWorldSky() {
    }

    static boolean inAu() {
        var level = Minecraft.getInstance().level;
        return level != null && ApocWorldDimension.isReal(level);
    }

    static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f};
    }

    /** The whole sky; called by {@link ApocWorldEffects#renderSky}. */
    static void render(Matrix4f modelView, float partial, int ticks) {
        PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        vault(pose.last().pose());
        sun(pose, ticks + partial);
        if (GeoGuard.exists(SKY)) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((ticks + partial) * 0.002f));
            smoke(pose);
            pose.popPose();
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static float[] skyAt(float e) {
        float[] zen = rgb(ZENITH), hor = rgb(HORIZON), nad = rgb(NADIR), out = new float[3];
        if (e >= 0) {
            float k = (float) Math.pow(Mth.clamp(e, 0, 1), 0.45);
            for (int i = 0; i < 3; i++) out[i] = Mth.lerp(k, hor[i], zen[i]);
        } else {
            float k = Mth.clamp(-e * 2f, 0, 1);
            for (int i = 0; i < 3; i++) out[i] = Mth.lerp(k, hor[i], nad[i]);
        }
        return out;
    }

    private static void vault(Matrix4f m) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 120f;
        for (int i = 0; i < LAT; i++) {
            float t0 = Mth.PI * i / LAT, t1 = Mth.PI * (i + 1) / LAT;
            float[] c0 = skyAt(Mth.cos(t0)), c1 = skyAt(Mth.cos(t1));
            for (int j = 0; j < LON; j++) {
                float p0 = Mth.TWO_PI * j / LON, p1 = Mth.TWO_PI * (j + 1) / LON;
                vertex(b, m, r, t0, p0, c0);
                vertex(b, m, r, t1, p0, c1);
                vertex(b, m, r, t1, p1, c1);
                vertex(b, m, r, t0, p1, c0);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float r, float theta, float phi, float[] c) {
        b.addVertex(m, r * Mth.sin(theta) * Mth.cos(phi), r * Mth.cos(theta), r * Mth.sin(theta) * Mth.sin(phi)).setColor(c[0], c[1], c[2], 1f);
    }

    /** The overcast: the art's sheet on the six faces of a box round the camera, half see-through, the upper faces denser. */
    private static void smoke(PoseStack pose) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SKY);
        for (int face = 0; face < 6; face++) {
            pose.pushPose();
            switch (face) {
                case 1 -> pose.mulPose(Axis.XP.rotationDegrees(90));
                case 2 -> pose.mulPose(Axis.XP.rotationDegrees(-90));
                case 3 -> pose.mulPose(Axis.XP.rotationDegrees(180));
                case 4 -> pose.mulPose(Axis.ZP.rotationDegrees(90));
                case 5 -> pose.mulPose(Axis.ZP.rotationDegrees(-90));
                default -> {
                }
            }
            Matrix4f m = pose.last().pose();
            float a = face == 3 ? 0.9f : face == 0 ? 0.25f : 0.6f;
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            b.addVertex(m, -100f, -100f, -100f).setUv(0f, 0f).setColor(1f, 0.9f, 0.85f, a);
            b.addVertex(m, -100f, -100f, 100f).setUv(0f, 4f).setColor(1f, 0.9f, 0.85f, a);
            b.addVertex(m, 100f, -100f, 100f).setUv(4f, 4f).setColor(1f, 0.9f, 0.85f, a);
            b.addVertex(m, 100f, -100f, -100f).setUv(4f, 0f).setColor(1f, 0.9f, 0.85f, a);
            BufferUploader.drawWithShader(b.buildOrThrow());
            pose.popPose();
        }
    }

    /** The dim red sun, low in the west through the smoke. */
    private static void sun(PoseStack base, float time) {
        PoseStack pose = new PoseStack();
        pose.mulPose(base.last().pose());
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.mulPose(Axis.XP.rotationDegrees(62f));
        Matrix4f m = pose.last().pose();
        float s = 18f + Mth.sin(time * 0.01f) * 0.6f;
        if (GeoGuard.exists(SUN)) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, SUN);
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.9f);
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.addVertex(m, -s, 100f, -s).setUv(0f, 0f);
            b.addVertex(m, s, 100f, -s).setUv(1f, 0f);
            b.addVertex(m, s, 100f, s).setUv(1f, 1f);
            b.addVertex(m, -s, 100f, s).setUv(0f, 1f);
            BufferUploader.drawWithShader(b.buildOrThrow());
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        b.addVertex(m, 0f, 100f, 0f).setColor(0.75f, 0.18f, 0.08f, 0.85f);
        for (int i = 0; i <= 32; i++) {
            float ang = Mth.TWO_PI * i / 32;
            b.addVertex(m, Mth.cos(ang) * s * 0.6f, 100f, Mth.sin(ang) * s * 0.6f).setColor(0.55f, 0.12f, 0.05f, 0.5f);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    // --- fog -----------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inAu()) return;
        float[] c = rgb(FOG);
        event.setRed(c[0]);
        event.setGreen(c[1]);
        event.setBlue(c[2]);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!inAu() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        // Smoke on the air: the far land is lost in brown haze.
        float far = event.getFarPlaneDistance();
        event.setNearPlaneDistance(far * 0.15f);
        event.setFarPlaneDistance(far * 0.8f);
        event.setCanceled(true);
    }
}
