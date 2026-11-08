package org.papiricoh.supernaturalcraft.client.dyingsun;

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
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.amara.AmaraSky;

/**
 * The dying sun over the Overworld (v0.42), from {@link ClientDyingSun#darkness}: once the Darkness is out the sun shrinks and
 * reddens day by day, the sky goes dusky, the fog leans to a dried-blood brown, the sky's share of the world's light fades
 * (torches and lava keep theirs: {@link #lightmap}) and the stars go out one by one. Drawn after vanilla's sky
 * ({@code AFTER_SKY}): vanilla's sun is covered by a patch of the sky's own colour and a smaller, redder sun drawn in its place,
 * then the dusk is laid over the whole vault; then {@link AmaraSky} paints her fight's sky on top. Nothing changes at
 * darkness 0, so a world that never let her out never sees it.
 *
 * <p>Hooks for the overworld's {@code DimensionSpecialEffects}: {@link #lightmap} from {@code adjustLightmapColors} and
 * {@link #cloudsHidden} from {@code renderClouds}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class DyingSunSky {

    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    static final int STARS = 700;
    /** Each star: direction (x, y, z), size, and the darkness at which it goes out. */
    private static final float[] STAR_DATA = buildStars();

    private DyingSunSky() {
    }

    static boolean overworld(ClientLevel level) {
        return level != null && level.dimension() == Level.OVERWORLD;
    }

    // --- hooks for the overworld's effects -------------------------------------------------------------------------------

    /**
     * The lightmap under a dying sun ({@code adjustLightmapColors(level, partial, skyDarken, blockLightRedFlicker, skyLight,
     * pixelX, pixelY, colors)} passes its arguments straight here): the sky's share of the light dims toward an ember glow,
     * the blocks' share is untouched. No change at darkness 0.
     */
    public static void lightmap(ClientLevel level, float partial, float skyDarken, float blockLightRedFlicker, float skyLight,
                                int pixelX, int pixelY, Vector3f colors) {
        if (!overworld(level)) return;
        float d = ClientDyingSun.darkness(partial);
        if (d <= 0.001f) return;
        float b = LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        Vector3f block = new Vector3f(b, b * ((b * 0.6f + 0.4f) * 0.6f + 0.4f), b * (b * b * 0.6f + 0.4f));
        // What is left of the sky's light: dimmer, and redder as it goes.
        Vector3f sky = new Vector3f(skyDarken, skyDarken, 1f).lerp(new Vector3f(1f, 1f, 1f), 0.35f).mul(skyLight);
        float keep = 1 - 0.88f * d;
        sky.mul(keep, keep * (1 - 0.3f * d), keep * (1 - 0.5f * d));
        Vector3f dying = block.add(sky);
        dying.lerp(new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);
        colors.lerp(dying, Mth.clamp(d * 1.4f, 0, 1));
    }

    /** Whether the clouds should not be drawn (the dusk would leave them glowing white). */
    public static boolean cloudsHidden(float partial) {
        return ClientDyingSun.darkness(partial) > 0.45f;
    }

    // --- the sky ---------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (overworld(level)) {
            float d = ClientDyingSun.darkness(partial);
            if (d > 0.002f) {
                PoseStack pose = new PoseStack();
                pose.mulPose(event.getModelViewMatrix());
                begin();
                drawSky(pose, level, event.getCamera().getPosition(), partial, d);
                end();
            }
        }
        AmaraSky.render(event, partial);
    }

    static void begin() {
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    static void end() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawSky(PoseStack pose, ClientLevel level, Vec3 cam, float partial, float d) {
        float rain = level.getRainLevel(partial);
        float tod = level.getTimeOfDay(partial);
        float time = (level.getGameTime() + partial) / 20f;
        // Where vanilla put the sun: (0, 100, 0) turned by -90° about Y, then by the time of day about X.
        float sunUp = Mth.cos(tod * Mth.TWO_PI);
        Vec3 skyColour = level.getSkyColor(cam, partial);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.mulPose(Axis.XP.rotationDegrees(tod * 360f));
        Matrix4f sun = pose.last().pose();
        if (sunUp > -0.15f) {
            // Cover vanilla's sun with the sky's own colour...
            float cover = Mth.clamp(d * 2.5f, 0, 1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int r = (int) (Mth.clamp((float) skyColour.x, 0, 1) * 255), g = (int) (Mth.clamp((float) skyColour.y, 0, 1) * 255),
                    bl = (int) (Mth.clamp((float) skyColour.z, 0, 1) * 255), a = (int) (cover * 255);
            float s = 31f;
            b.addVertex(sun, -s, 99.5f, -s).setColor(r, g, bl, a);
            b.addVertex(sun, s, 99.5f, -s).setColor(r, g, bl, a);
            b.addVertex(sun, s, 99.5f, s).setColor(r, g, bl, a);
            b.addVertex(sun, -s, 99.5f, s).setColor(r, g, bl, a);
            draw(b);
            // ...and draw it again smaller and redder, its glow guttering once it is far gone.
            float size = 30f * (1 - 0.55f * d);
            float flicker = d > 0.55f ? 0.85f + 0.15f * Mth.sin(time * 7.1f) * Mth.sin(time * 2.3f) : 1f;
            float sr = 1f, sg = 1f - 0.55f * d, sb = 1f - 0.85f * d;
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, SUN);
            RenderSystem.setShaderColor(sr * flicker, sg * flicker, sb * flicker, (1 - rain) * cover);
            BufferBuilder t = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            t.addVertex(sun, -size, 100f, -size).setUv(0f, 0f);
            t.addVertex(sun, size, 100f, -size).setUv(1f, 0f);
            t.addVertex(sun, size, 100f, size).setUv(1f, 1f);
            t.addVertex(sun, -size, 100f, size).setUv(0f, 1f);
            draw(t);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            glow(sun, 100f, size * 2.6f, 0xB0301A, 0.22f * d * (1 - rain) * flicker);
            RenderSystem.defaultBlendFunc();
        }
        pose.popPose();
        // The dusk over the whole vault: darkest overhead, an ember band at the horizon.
        vault(pose.last().pose(), 0.8f * d, d);
        // The stars that are left: one by one they go out.
        float night = level.getStarBrightness(partial) * (1 - rain);
        if (night > 0.02f) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-90f));
            pose.mulPose(Axis.XP.rotationDegrees(tod * 360f));
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            stars(pose.last().pose(), night * 1.6f, d, time);
            RenderSystem.defaultBlendFunc();
            pose.popPose();
        }
    }

    /** A box round the camera, {@code alpha} opaque: near-black overhead, ember-brown at the horizon. */
    public static void vault(Matrix4f m, float alpha, float d) {
        if (alpha <= 0.002f) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 50f;
        int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        int top = 0x06040A, low = lerp(0x120C1E, 0x2E0E0A, d);
        for (int f = 0; f < 6; f++) {
            for (Vector3f v : face(f, r)) {
                int c = v.y > 0 ? top : low;
                b.addVertex(m, v.x, v.y, v.z).setColor(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, a);
            }
        }
        draw(b);
    }

    static int lerp(int a, int b, float k) {
        k = Mth.clamp(k, 0, 1);
        return (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255) << 16 | (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255) << 8
                | (int) Mth.lerp(k, a & 255, b & 255);
    }

    static Vector3f[] face(int f, float r) {
        return switch (f) {
            case 0 -> new Vector3f[]{new Vector3f(-r, r, -r), new Vector3f(r, r, -r), new Vector3f(r, r, r), new Vector3f(-r, r, r)};
            case 1 -> new Vector3f[]{new Vector3f(-r, -r, -r), new Vector3f(-r, -r, r), new Vector3f(r, -r, r), new Vector3f(r, -r, -r)};
            case 2 -> new Vector3f[]{new Vector3f(-r, -r, -r), new Vector3f(r, -r, -r), new Vector3f(r, r, -r), new Vector3f(-r, r, -r)};
            case 3 -> new Vector3f[]{new Vector3f(-r, -r, r), new Vector3f(-r, r, r), new Vector3f(r, r, r), new Vector3f(r, -r, r)};
            case 4 -> new Vector3f[]{new Vector3f(-r, -r, -r), new Vector3f(-r, r, -r), new Vector3f(-r, r, r), new Vector3f(-r, -r, r)};
            default -> new Vector3f[]{new Vector3f(r, -r, -r), new Vector3f(r, -r, r), new Vector3f(r, r, r), new Vector3f(r, r, -r)};
        };
    }

    /** A round glow in the plane y = {@code y}: {@code rgb} at alpha {@code a} in the middle, nothing at the rim. */
    static void glow(Matrix4f m, float y, float radius, int rgb, float a) {
        if (a <= 0.002f) return;
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, bl = rgb & 0xFF, al = Math.round(Mth.clamp(a, 0, 1) * 255);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        b.addVertex(m, 0, y, 0).setColor(r, g, bl, al);
        for (int i = 0; i <= 32; i++) {
            double t = Math.PI * 2 * i / 32;
            b.addVertex(m, (float) (Math.cos(t) * radius), y, (float) (Math.sin(t) * radius)).setColor(r, g, bl, 0);
        }
        draw(b);
    }

    private static float[] buildStars() {
        RandomSource r = RandomSource.create(1155L);
        float[] out = new float[STARS * 5];
        for (int i = 0; i < STARS; i++) {
            float x = r.nextFloat() * 2 - 1, y = r.nextFloat() * 2 - 1, z = r.nextFloat() * 2 - 1;
            float l = Mth.sqrt(x * x + y * y + z * z);
            if (l < 0.1f || l > 1f) {
                i--;
                continue;
            }
            out[i * 5] = x / l;
            out[i * 5 + 1] = y / l;
            out[i * 5 + 2] = z / l;
            out[i * 5 + 3] = 0.12f + r.nextFloat() * 0.2f;
            out[i * 5 + 4] = r.nextFloat() * 0.95f;
        }
        return out;
    }

    /** The stars still burning at darkness {@code d}: each goes out at its own darkness, with a last flare. */
    static void stars(Matrix4f m, float bright, float d, float time) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int drawn = 0;
        for (int i = 0; i < STARS; i++) {
            int o = i * 5;
            float out = STAR_DATA[o + 4];
            if (d >= out + 0.03f) continue;
            // Close to its end a star flares and reddens.
            float near = Mth.clamp(1 - (out - d) / 0.06f, 0, 1);
            float a = Mth.clamp(bright, 0, 1) * (1 - near * near) * (0.6f + 0.4f * Mth.sin(time * 1.3f + i));
            if (a <= 0.01f) continue;
            float size = STAR_DATA[o + 3] * (1 + near * 1.5f);
            int al = (int) (a * 255), g = (int) (235 - near * 120), bl = (int) (255 - near * 200);
            Vector3f c = new Vector3f(STAR_DATA[o], STAR_DATA[o + 1], STAR_DATA[o + 2]).mul(100);
            Vector3f t1 = new Vector3f(c).cross(Math.abs(STAR_DATA[o + 1]) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0)).normalize().mul(size);
            Vector3f t2 = new Vector3f(c).cross(t1).normalize().mul(size);
            b.addVertex(m, c.x - t1.x - t2.x, c.y - t1.y - t2.y, c.z - t1.z - t2.z).setColor(255, g, bl, al);
            b.addVertex(m, c.x + t1.x - t2.x, c.y + t1.y - t2.y, c.z + t1.z - t2.z).setColor(255, g, bl, al);
            b.addVertex(m, c.x + t1.x + t2.x, c.y + t1.y + t2.y, c.z + t1.z + t2.z).setColor(255, g, bl, al);
            b.addVertex(m, c.x - t1.x + t2.x, c.y - t1.y + t2.y, c.z - t1.z + t2.z).setColor(255, g, bl, al);
            drawn++;
        }
        if (drawn > 0) draw(b);
        else b.build();
    }

    static void draw(BufferBuilder b) {
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    // --- fog -------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!overworld(level)) return;
        float d = ClientDyingSun.darkness((float) event.getPartialTick());
        if (d <= 0.002f) return;
        float k = Mth.clamp(d * 0.85f, 0, 1);
        event.setRed(Mth.lerp(k, event.getRed(), 0.09f));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.045f));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.04f));
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!overworld(level) || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float d = ClientDyingSun.darkness((float) event.getPartialTick());
        if (d <= 0.2f) return;
        // The far fog draws in a little as the light goes: the world's edge is lost in the dusk.
        float far = event.getFarPlaneDistance(), want = Mth.lerp((d - 0.2f) / 0.8f, far, Math.min(far, 72f));
        if (want >= far - 0.5f) return;
        event.setFarPlaneDistance(want);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), want * 0.4f));
        event.setCanceled(true);
    }
}
