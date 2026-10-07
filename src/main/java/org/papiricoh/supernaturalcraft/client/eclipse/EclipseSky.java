package org.papiricoh.supernaturalcraft.client.eclipse;

import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Paints the eclipse over vanilla's sky: a near-black vault, a dense field of stars, and where the
 * sun should be a black disc in its corona. Also pulls the fog in close and dark.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class EclipseSky {

    public static final ResourceLocation CORONA = SupernaturalCraft.asResource("textures/environment/eclipse_corona.png");
    static final int STARS = 1500;
    static final float CORONA_SIZE = 46f, DISC_RADIUS = CORONA_SIZE * 0.4f, FOG_FAR = 56f;
    private static final float[] STAR_QUADS = buildStars();

    private EclipseSky() {
    }

    private static boolean overworld() {
        var level = Minecraft.getInstance().level;
        return level != null && level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !overworld() || !SNClientConfig.ECLIPSE_SKY.get()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float k = ClientEclipse.intensity(partial);
        if (k <= 0f) return;
        var level = Minecraft.getInstance().level;
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        vault(pose.last().pose(), 0.94f * k);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partial) * 360f));
        stars(pose.last().pose(), k);
        Matrix4f sun = pose.last().pose();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        corona(sun, k, (level.getGameTime() + partial) / 20f);
        RenderSystem.defaultBlendFunc();
        disc(sun, k);
        pose.popPose();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /** A box around the camera, darkest overhead, a little violet at the horizon. */
    private static void vault(Matrix4f m, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 50f;
        int a = (int) (alpha * 255), top = 0x050308, low = 0x120C1E;
        for (int face = 0; face < 6; face++) {
            Vector3f[] q = face(face, r);
            for (Vector3f v : q) {
                int c = v.y > 0 ? top : low;
                b.addVertex(m, v.x, v.y, v.z).setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, a);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static Vector3f[] face(int f, float r) {
        return switch (f) {
            case 0 -> new Vector3f[]{v(-r, r, -r), v(r, r, -r), v(r, r, r), v(-r, r, r)};
            case 1 -> new Vector3f[]{v(-r, -r, -r), v(-r, -r, r), v(r, -r, r), v(r, -r, -r)};
            case 2 -> new Vector3f[]{v(-r, -r, -r), v(r, -r, -r), v(r, r, -r), v(-r, r, -r)};
            case 3 -> new Vector3f[]{v(-r, -r, r), v(-r, r, r), v(r, r, r), v(r, -r, r)};
            case 4 -> new Vector3f[]{v(-r, -r, -r), v(-r, r, -r), v(-r, r, r), v(-r, -r, r)};
            default -> new Vector3f[]{v(r, -r, -r), v(r, -r, r), v(r, r, r), v(r, r, -r)};
        };
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static float[] buildStars() {
        RandomSource r = RandomSource.create(4004L);
        float[] out = new float[STARS * 13];
        for (int i = 0; i < STARS; i++) {
            Vector3f d = new Vector3f(r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1);
            if (d.lengthSquared() < 0.01f || d.lengthSquared() > 1f) {
                i--;
                continue;
            }
            d.normalize();
            Vector3f t1 = new Vector3f(d).cross(Math.abs(d.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0)).normalize();
            Vector3f t2 = new Vector3f(d).cross(t1).normalize();
            float size = 0.12f + r.nextFloat() * 0.18f, bright = 0.45f + r.nextFloat() * 0.55f;
            int o = i * 13;
            Vector3f c = new Vector3f(d).mul(100f);
            for (int k = 0; k < 4; k++) {
                float su = (k == 0 || k == 3) ? -size : size, sv = (k < 2) ? -size : size;
                out[o + k * 3] = c.x + t1.x * su + t2.x * sv;
                out[o + k * 3 + 1] = c.y + t1.y * su + t2.y * sv;
                out[o + k * 3 + 2] = c.z + t1.z * su + t2.z * sv;
            }
            out[o + 12] = bright;
        }
        return out;
    }

    private static void stars(Matrix4f m, float k) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < STARS; i++) {
            int o = i * 13, a = (int) (255 * k * STAR_QUADS[o + 12]);
            for (int v = 0; v < 4; v++) {
                b.addVertex(m, STAR_QUADS[o + v * 3], STAR_QUADS[o + v * 3 + 1], STAR_QUADS[o + v * 3 + 2]).setColor(235, 232, 255, a);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** The corona breathes slowly; it is drawn additively, so it can only add light. */
    private static void corona(Matrix4f m, float k, float time) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, CORONA);
        float s = CORONA_SIZE * (1f + 0.03f * Mth.sin(time * 0.7f));
        int a = (int) (255 * k);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.addVertex(m, -s, 100f, -s).setUv(0f, 0f).setColor(255, 255, 255, a);
        b.addVertex(m, s, 100f, -s).setUv(1f, 0f).setColor(255, 255, 255, a);
        b.addVertex(m, s, 100f, s).setUv(1f, 1f).setColor(255, 255, 255, a);
        b.addVertex(m, -s, 100f, s).setUv(0f, 1f).setColor(255, 255, 255, a);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** The moon's black disc, covering the vanilla sun. */
    private static void disc(Matrix4f m, float k) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        int a = (int) (255 * k);
        b.addVertex(m, 0f, 100f, 0f).setColor(2, 1, 4, a);
        for (int i = 0; i <= 48; i++) {
            float ang = Mth.TWO_PI * i / 48;
            b.addVertex(m, Mth.cos(ang) * DISC_RADIUS, 100f, Mth.sin(ang) * DISC_RADIUS).setColor(2, 1, 4, a);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!overworld()) return;
        float k = ClientEclipse.intensity((float) event.getPartialTick());
        if (k <= 0f) return;
        event.setRed(Mth.lerp(k, event.getRed(), 0.035f));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.025f));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.06f));
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!overworld() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float k = ClientEclipse.intensity((float) event.getPartialTick());
        if (k <= 0f || event.getFarPlaneDistance() <= FOG_FAR) return;
        event.setFarPlaneDistance(Mth.lerp(k, event.getFarPlaneDistance(), FOG_FAR));
        event.setNearPlaneDistance(Mth.lerp(k, event.getNearPlaneDistance(), FOG_FAR * 0.25f));
        event.setCanceled(true);
    }
}
