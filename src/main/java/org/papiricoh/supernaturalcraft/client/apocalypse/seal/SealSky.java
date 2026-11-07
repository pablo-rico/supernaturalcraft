package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealAssets;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRules;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The broken seals in the Overworld's sky (v0.40), growing with the stage ({@link ClientSeals#stage}); faint in an echo, full once
 * Lucifer has risen. From the first stage the night's fog leans red; from the second ({@link SealRules#RED_MOON_STAGE}) the moon
 * is red ({@link SealAssets#RED_MOON}, a red disc without it); from the third the rain falls as blood (drops round the camera)
 * and storms bring hail; in the last stars fall. A seal breaking flashes the sky red ({@link #flash}). Drawn after the sky
 * ({@code AFTER_SKY}, a dome round the camera like {@code EclipseSky}); the lightmap's red cast is {@link #lightmap} (hooked from
 * {@code EclipseOverworldEffects}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class SealSky {

    static final ResourceLocation RED_MOON = SupernaturalCraft.asResource(SealAssets.RED_MOON);
    private static final int MAX_STARS = 24;

    private static float strength, strengthO, redFlash, redFlashO;
    private static final List<float[]> STARS = new ArrayList<>();

    private SealSky() {
    }

    /** A seal broke: the sky flashes red. */
    public static void flash(float power) {
        redFlash = Math.max(redFlash, Mth.clamp(power, 0, 1));
    }

    public static void clear() {
        strength = strengthO = redFlash = redFlashO = 0;
        STARS.clear();
    }

    /** How strongly the omens show now (0-1), smoothed. */
    public static float strength(float partial) {
        return Mth.lerp(partial, strengthO, strength);
    }

    /** How dark the sky is for the omens to show (0 by day, 1 deep at night). */
    private static float night(ClientLevel level, float partial) {
        return Mth.clamp(level.getStarBrightness(partial) * 2.2f, 0, 1);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        strengthO = strength;
        redFlashO = redFlash;
        redFlash = Math.max(0, redFlash - 0.02f);
        if (level == null || mc.player == null) {
            clear();
            SealOverlay.clear();
            return;
        }
        SealOverlay.tick();
        float want = ClientSeals.shows(level) ? ClientSeals.omens() : 0;
        strength += Mth.clamp(want - strength, -0.01f, 0.01f);
        if (strength <= 0.01f || mc.isPaused()) return;
        int stage = ClientSeals.risen() ? SealRules.MAX_STAGE : ClientSeals.stage();
        RandomSource r = level.random;
        // The rain falls as blood; the storms bring hail.
        if (stage >= 3 && level.isRaining()) {
            int n = Math.round(6 * strength);
            for (int i = 0; i < n; i++) {
                double x = mc.player.getX() + (r.nextDouble() - 0.5) * 24, z = mc.player.getZ() + (r.nextDouble() - 0.5) * 24;
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(x, 0, z));
                double y = Math.max(top.getY(), mc.player.getY()) + 6 + r.nextDouble() * 6;
                if (y < top.getY()) continue;
                level.addParticle(AllParticles.BLOOD_DROP.get(), x, y, z, 0, -0.6, 0);
                if (level.isThundering() && r.nextInt(3) == 0) level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.9, 0);
            }
        }
        // Stars fall in the last stage.
        if (stage >= SealRules.MAX_STAGE && night(level, 0) > 0.3f && STARS.size() < MAX_STARS && r.nextFloat() < 0.04f * strength) {
            float az = r.nextFloat() * Mth.TWO_PI, el = (25 + 50 * r.nextFloat()) * Mth.DEG_TO_RAD;
            STARS.add(new float[]{az, el, 0, 0.4f + r.nextFloat() * 0.6f});
        }
        for (Iterator<float[]> it = STARS.iterator(); it.hasNext(); ) {
            float[] s = it.next();
            s[2] += 1;
            if (s[2] > 40) it.remove();
        }
    }

    /** The night's fog and horizon lean red with the omens, and flash red as a seal breaks. */
    @SubscribeEvent
    public static void onFog(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !ClientSeals.shows(level)) return;
        float partial = (float) event.getPartialTick();
        float k = strength(partial) * (0.25f + 0.75f * night(level, partial)) * 0.45f;
        float f = Mth.lerp(partial, redFlashO, redFlash) * 0.6f;
        k = Math.max(k, f);
        if (k <= 0.005f) return;
        event.setRed(Mth.lerp(k, event.getRed(), Math.max(event.getRed(), 0.45f)));
        event.setGreen(Mth.lerp(k, event.getGreen(), event.getGreen() * 0.35f));
        event.setBlue(Mth.lerp(k, event.getBlue(), event.getBlue() * 0.3f));
    }

    /**
     * The lightmap's red cast under a red moon ({@code EclipseOverworldEffects.adjustLightmapColors} calls this after its own):
     * the sky's share of the light tinged red at night from the red moon's stage.
     */
    public static void lightmap(ClientLevel level, float partial, float skyLight, Vector3f colors) {
        if (!ClientSeals.shows(level) || ClientSeals.stage() < SealRules.RED_MOON_STAGE && !ClientSeals.risen()) return;
        float k = strength(partial) * night(level, partial) * skyLight * 0.25f;
        if (k <= 0.001f) return;
        colors.set(colors.x + k * 0.2f, colors.y * (1 - k * 0.6f), colors.z * (1 - k * 0.7f));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !ClientSeals.shows(level)) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float k = strength(partial);
        int stage = ClientSeals.risen() ? SealRules.MAX_STAGE : ClientSeals.stage();
        float dark = night(level, partial) * (1 - level.getRainLevel(partial) * 0.7f);
        if (k <= 0.01f || dark <= 0.02f) return;
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (stage >= SealRules.RED_MOON_STAGE) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-90f));
            pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partial) * 360f));
            moon(pose.last().pose(), k * dark);
            pose.popPose();
        }
        if (!STARS.isEmpty()) stars(pose.last().pose(), partial, k * dark);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /** The red moon where the moon is (opposite the sun, as vanilla draws it), with a red halo. */
    private static void moon(Matrix4f m, float a) {
        float y = -100, s = 22;
        if (GeoGuard.exists(RED_MOON)) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, RED_MOON);
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int al = Math.round(a * 220);
            b.addVertex(m, -s, y, s).setUv(0, 1).setColor(255, 255, 255, al);
            b.addVertex(m, s, y, s).setUv(1, 1).setColor(255, 255, 255, al);
            b.addVertex(m, s, y, -s).setUv(1, 0).setColor(255, 255, 255, al);
            b.addVertex(m, -s, y, -s).setUv(0, 0).setColor(255, 255, 255, al);
            draw(b);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quad(b, m, y, s * 0.55f, 0xC8281A, a * 0.55f);
        quad(b, m, y, s * 1.6f, 0x8A140C, a * 0.18f);
        quad(b, m, y, s * 3.2f, 0x5A0A06, a * 0.08f);
        draw(b);
    }

    private static void quad(BufferBuilder b, Matrix4f m, float y, float s, int rgb, float a) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, bl = rgb & 0xFF, al = Math.round(Mth.clamp(a, 0, 1) * 255);
        b.addVertex(m, -s, y, s).setColor(r, g, bl, al);
        b.addVertex(m, s, y, s).setColor(r, g, bl, al);
        b.addVertex(m, s, y, -s).setColor(r, g, bl, al);
        b.addVertex(m, -s, y, -s).setColor(r, g, bl, al);
    }

    /** Falling stars: short red-white streaks sliding down the dome. */
    private static void stars(Matrix4f m, float partial, float a) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (float[] s : STARS) {
            float age = s[2] + partial, el = s[1] - age * 0.012f, az = s[0];
            Vector3f head = dir(az, el).mul(100), tail = dir(az, el + 0.08f).mul(100);
            Vector3f side = new Vector3f(head).cross(0, 1, 0).normalize().mul(0.5f * s[3]);
            float al = a * Mth.clamp(Math.min(age / 4f, (40 - age) / 10f), 0, 1);
            int hi = Math.round(al * 255);
            b.addVertex(m, head.x - side.x, head.y, head.z - side.z).setColor(255, 230, 210, hi);
            b.addVertex(m, head.x + side.x, head.y, head.z + side.z).setColor(255, 230, 210, hi);
            b.addVertex(m, tail.x + side.x, tail.y, tail.z + side.z).setColor(200, 40, 30, 0);
            b.addVertex(m, tail.x - side.x, tail.y, tail.z - side.z).setColor(200, 40, 30, 0);
        }
        draw(b);
    }

    private static Vector3f dir(float az, float el) {
        return new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
    }

    private static void draw(BufferBuilder b) {
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
}
