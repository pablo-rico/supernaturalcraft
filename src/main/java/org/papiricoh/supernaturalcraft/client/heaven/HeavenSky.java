package org.papiricoh.supernaturalcraft.client.heaven;

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
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;

/**
 * Heaven's sky (v0.18, drawn for {@link HeavenEffects}): a vault from a clear azure overhead to a warm ivory horizon and a
 * luminous white below it, a high noon sun in a wide soft halo and faint motes of light; under the islands vanilla's clouds
 * ({@link HeavenEffects#CLOUD_HEIGHT}) over the art's sea of cloud ({@link #cloudSea}). The fog is warm and far, so distant islands melt into the light. A staged memory pulls all of it toward its tint
 * ({@link MemoryTint}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class HeavenSky {

    static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    static final ResourceLocation PANORAMA = SupernaturalCraft.asResource(HeavenAssets.SKY_TEXTURE);
    /** Overhead, at the horizon, below it. */
    static final int ZENITH = 0x8EC3F2, HORIZON = 0xFFF1D6, NADIR = 0xFFFDF6;
    /** The fog's warm white. */
    static final int FOG = 0xFBEFD8;
    static final int MOTES = 260, LAT = 18, LON = 24;
    private static final float[] MOTE_QUADS = buildMotes();

    private HeavenSky() {
    }

    static boolean inHeaven() {
        var level = Minecraft.getInstance().level;
        return level != null && HeavenDimension.isHeaven(level);
    }

    /** RGB 0-1 of {@code rgb}, pulled toward the memory's tint. */
    static float[] tinted(int rgb, float partial, float amount) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float k = MemoryTint.strength(partial) * amount;
        return k <= 0 ? new float[]{r, g, b} : MemoryTint.mix(r, g, b, k);
    }

    /** The whole sky; called by {@link HeavenEffects#renderSky}. */
    static void render(Matrix4f modelView, float partial, int ticks) {
        PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        vault(pose.last().pose(), partial);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((ticks + partial) * 0.004f));
        motes(pose.last().pose(), ticks + partial);
        pose.popPose();
        sun(pose.last().pose(), partial, ticks + partial);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /** Colour at elevation {@code e} (-1 straight down, 0 the horizon, 1 straight up). */
    private static float[] skyAt(float e, float partial) {
        float[] zen = tinted(ZENITH, partial, 0.55f), hor = tinted(HORIZON, partial, 0.7f), nad = tinted(NADIR, partial, 0.5f);
        float[] out = new float[3];
        if (e >= 0) {
            // A wide warm band at the horizon, the blue deepening only high up.
            float k = (float) Math.pow(Mth.clamp(e, 0, 1), 0.55);
            for (int i = 0; i < 3; i++) out[i] = Mth.lerp(k, hor[i], zen[i]);
        } else {
            float k = Mth.clamp(-e * 2.2f, 0, 1);
            for (int i = 0; i < 3; i++) out[i] = Mth.lerp(k, hor[i], nad[i]);
        }
        return out;
    }

    private static void vault(Matrix4f m, float partial) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 120f;
        for (int i = 0; i < LAT; i++) {
            float t0 = Mth.PI * i / LAT, t1 = Mth.PI * (i + 1) / LAT;
            float[] c0 = skyAt(Mth.cos(t0), partial), c1 = skyAt(Mth.cos(t1), partial);
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

    /** The sea of cloud's height: under the plots, below vanilla's clouds and above the rescue line. */
    static final float SEA_Y = org.papiricoh.supernaturalcraft.heaven.HeavenDimension.PLOT_Y - 42;
    /** How far the sea reaches round the camera, and how many blocks one tile of its texture covers. */
    static final float SEA_REACH = 420, SEA_TILE = 160;
    static final int SEA_GRID = 14;

    /**
     * The art's sea of cloud ({@code HeavenAssets.SKY_TEXTURE}, a tileable sheet whose alpha is its density) as a vast plane under
     * the islands, drifting slowly, fading toward its rim; drawn with the clouds (depth tested, so the islands hide it).
     */
    static void cloudSea(Matrix4f modelView, float partial, int ticks, double camX, double camY, double camZ) {
        if (!GeoGuard.exists(PANORAMA)) return;
        PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        Matrix4f m = pose.last().pose();
        float y = (float) (SEA_Y - camY);
        float drift = (ticks + partial) * 0.012f;
        float[] tint = tinted(0xFFFFFF, partial, 0.5f);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PANORAMA);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float cell = SEA_REACH * 2 / SEA_GRID;
        for (int i = 0; i < SEA_GRID; i++) {
            for (int j = 0; j < SEA_GRID; j++) {
                float x0 = -SEA_REACH + i * cell, z0 = -SEA_REACH + j * cell;
                sea(b, m, x0, y, z0, camX, camZ, drift, tint);
                sea(b, m, x0, y, z0 + cell, camX, camZ, drift, tint);
                sea(b, m, x0 + cell, y, z0 + cell, camX, camZ, drift, tint);
                sea(b, m, x0 + cell, y, z0, camX, camZ, drift, tint);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void sea(BufferBuilder b, Matrix4f m, float x, float y, float z, double camX, double camZ, float drift, float[] c) {
        double wx = camX + x, wz = camZ + z;
        float u = (float) ((wx / SEA_TILE + drift) % 1024), v = (float) ((wz / SEA_TILE + drift * 0.4f) % 1024);
        float d = Mth.sqrt(x * x + z * z) / SEA_REACH;
        float a = Mth.clamp(1.15f - d * 1.15f, 0, 1) * 0.95f;
        b.addVertex(m, x, y, z).setUv(u, v).setColor(c[0], c[1], c[2], a);
    }

    /** The noon sun: a wide halo (added light) and vanilla's disc, a little warmer, overhead and a touch to the south. */
    private static void sun(Matrix4f base, float partial, float time) {
        PoseStack pose = new PoseStack();
        pose.mulPose(base);
        pose.mulPose(Axis.XP.rotationDegrees(-12f));
        Matrix4f m = pose.last().pose();
        float[] warm = tinted(0xFFE6B0, partial, 0.5f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (int ring = 0; ring < 3; ring++) {
            float rad = 22f + ring * 26f + Mth.sin(time * 0.01f + ring) * 1.5f;
            float a = 0.42f - ring * 0.12f;
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            b.addVertex(m, 0f, 100f, 0f).setColor(warm[0], warm[1], warm[2], a);
            for (int i = 0; i <= 32; i++) {
                float ang = Mth.TWO_PI * i / 32;
                b.addVertex(m, Mth.cos(ang) * rad, 100f, Mth.sin(ang) * rad).setColor(warm[0], warm[1], warm[2], 0f);
            }
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        RenderSystem.setShaderColor(1f, 0.97f, 0.88f, 1f);
        float s = 26f;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, -s, 100f, -s).setUv(0f, 0f);
        b.addVertex(m, s, 100f, -s).setUv(1f, 0f);
        b.addVertex(m, s, 100f, s).setUv(1f, 1f);
        b.addVertex(m, -s, 100f, s).setUv(0f, 1f);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static float[] buildMotes() {
        RandomSource r = RandomSource.create(1818L);
        float[] out = new float[MOTES * 14];
        for (int i = 0; i < MOTES; i++) {
            float yaw = r.nextFloat() * Mth.TWO_PI, el = 0.05f + r.nextFloat() * 1.1f;
            float x = Mth.cos(yaw) * Mth.cos(el), y = Mth.sin(el), z = Mth.sin(yaw) * Mth.cos(el);
            float size = 0.25f + r.nextFloat() * 0.45f;
            int o = i * 14;
            out[o] = x * 100;
            out[o + 1] = y * 100;
            out[o + 2] = z * 100;
            out[o + 3] = size;
            out[o + 4] = r.nextFloat() * Mth.TWO_PI;
        }
        return out;
    }

    /** Faint motes of gold drifting high up, twinkling. */
    private static void motes(Matrix4f m, float time) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < MOTES; i++) {
            int o = i * 14;
            float x = MOTE_QUADS[o], y = MOTE_QUADS[o + 1], z = MOTE_QUADS[o + 2], s = MOTE_QUADS[o + 3];
            float a = 0.25f + 0.25f * Mth.sin(time * 0.05f + MOTE_QUADS[o + 4]);
            // A flat cross facing the centre is enough at this distance.
            b.addVertex(m, x - s, y, z).setColor(1f, 0.92f, 0.7f, a);
            b.addVertex(m, x, y - s, z).setColor(1f, 0.92f, 0.7f, a);
            b.addVertex(m, x + s, y, z).setColor(1f, 0.92f, 0.7f, a);
            b.addVertex(m, x, y + s, z).setColor(1f, 0.92f, 0.7f, a);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    // --- fog -----------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inHeaven()) return;
        float[] c = tinted(FOG, (float) event.getPartialTick(), 0.7f);
        event.setRed(c[0]);
        event.setGreen(c[1]);
        event.setBlue(c[2]);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!inHeaven() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        // Soft and far: the edge of the world dissolves into light instead of ending.
        float far = event.getFarPlaneDistance();
        float memory = MemoryTint.strength((float) event.getPartialTick());
        event.setNearPlaneDistance(far * Mth.lerp(memory, 0.45f, 0.25f));
        event.setFarPlaneDistance(far * Mth.lerp(memory, 1.05f, 0.85f));
        event.setCanceled(true);
    }
}
