package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;

/**
 * The page: near the Author the world is drawn as ink on paper ({@code shaders/post/author_page.json}), faintly in
 * the first chapters, strongly from the third, and in the fifth it goes blank ({@link ChuckEntity#whiteness()} and the
 * {@code WHITE_OUT} payload). Off when no Author is near. Shares the {@code distortion} client option with the
 * Darkness's shader; when that is off (or the shader fails) a cheap sepia wash and white-out stand in, drawn as a GUI
 * layer under the HUD ({@link Fallback}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AuthorPageFx {

    /** Page strength per chapter (1-5) at the Author's side. */
    private static final float[] STRENGTH = {0.22f, 0.3f, 0.55f, 0.7f, 0.85f};

    private static PostChain chain;
    private static boolean failed;
    private static int width = -1, height = -1;
    private static float intensity, lastIntensity, white, lastWhite;
    /** The WHITE_OUT payload's ramp: from, to, over how long, how far along. */
    private static float outFrom, outTo;
    private static int outAge, outLife;

    private AuthorPageFx() {
    }

    public static void whiteOut(float to, int duration) {
        outFrom = ramp();
        outTo = Mth.clamp(to, 0, 1);
        outLife = Math.max(1, duration);
        outAge = 0;
    }

    private static float ramp() {
        if (outLife <= 0) return 0;
        float p = Mth.clamp((float) outAge / outLife, 0, 1);
        return Mth.lerp(p * p * (3 - 2 * p), outFrom, outTo);
    }

    public static float intensity(float partial) {
        return Mth.lerp(partial, lastIntensity, intensity);
    }

    public static float whiteness(float partial) {
        return Mth.lerp(partial, lastWhite, white);
    }

    public static boolean fallback() {
        return !SNClientConfig.DISTORTION.get() || failed;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        lastIntensity = intensity;
        lastWhite = white;
        float want = 0, wantWhite = 0;
        boolean author = false;
        if (mc.level != null && mc.player != null) {
            for (ChuckEntity c : mc.level.getEntitiesOfClass(ChuckEntity.class, mc.player.getBoundingBox().inflate(96))) {
                author = true;
                double d = mc.player.position().distanceTo(c.position());
                float near = Mth.clamp((float) (1 - (d - 45) / 35), 0, 1);
                want = Math.max(want, STRENGTH[c.chapter().ordinal()] * near);
                wantWhite = Math.max(wantWhite, c.whiteness() * near);
            }
        }
        if (outLife > 0) {
            outAge = Math.min(outAge + 1, outLife);
            if (author) wantWhite = Math.max(wantWhite, ramp());
            else outLife = 0;
        }
        intensity += (want - intensity) * 0.04f;
        white += (wantWhite - white) * 0.08f;
        if (intensity < 0.002f) intensity = 0;
        if (white < 0.002f) white = 0;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float k = intensity(partial), w = whiteness(partial);
        if ((k < 0.01f && w < 0.01f) || fallback()) return;
        Minecraft mc = Minecraft.getInstance();
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        SupernaturalCraft.asResource("shaders/post/author_page.json"));
            } catch (Exception e) {
                SupernaturalCraft.LOGGER.warn("Author page shader failed to load; using the sepia wash instead", e);
                failed = true;
                return;
            }
        }
        int ww = mc.getWindow().getWidth(), wh = mc.getWindow().getHeight();
        if (ww != width || wh != height) {
            chain.resize(ww, wh);
            width = ww;
            height = wh;
        }
        chain.setUniform("Intensity", k);
        chain.setUniform("Whiteness", w);
        chain.setUniform("Time", (mc.level.getGameTime() + partial) / 20f);
        chain.process(partial);
        mc.getMainRenderTarget().bindWrite(false);
    }

    /** Without the shader: a sepia wash, darker edges, and the white-out as a sheet of paper over the world. */
    public static class Fallback implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker delta) {
            if (!fallback()) return;
            float partial = delta.getGameTimeDeltaPartialTick(false);
            float k = intensity(partial), w = whiteness(partial);
            if (k < 0.01f && w < 0.01f) return;
            int gw = g.guiWidth(), gh = g.guiHeight();
            g.fill(0, 0, gw, gh, ChuckText.argb(k * 0.3f, 0xB89A68));
            int edge = gh / 4;
            g.fillGradient(0, 0, gw, edge, ChuckText.argb(k * 0.35f, 0x1A1410), 0);
            g.fillGradient(0, gh - edge, gw, gh, 0, ChuckText.argb(k * 0.35f, 0x1A1410));
            if (w > 0.01f) g.fill(0, 0, gw, gh, ChuckText.argb(w * 0.85f, 0xFBF8F2));
        }
    }
}
