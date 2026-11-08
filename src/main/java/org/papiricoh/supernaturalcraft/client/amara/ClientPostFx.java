package org.papiricoh.supernaturalcraft.client.amara;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.dyingsun.ClientDyingSun;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook;

/**
 * How the Darkness bends sight (v0.42): one post-processing pass ({@code shaders/post/amara_darkness.json}) with six
 * strengths, each eased here:
 * <ul>
 *   <li>{@code Warp} — the old sinuous bend and colour split, growing as you near her;</li>
 *   <li>{@code Desat} — the world drained of colour except what burns (light sources keep theirs), by her phase and, faintly,
 *   by the dying sun;</li>
 *   <li>{@code Chroma} — chromatic aberration when one of her blows lands on you ({@code CHROMA_HIT});</li>
 *   <li>{@code Ink} — ink creeping in from the screen's edges (phases V-VI, her rage, a wound to Chuck);</li>
 *   <li>{@code Wave} — a distortion wave racing out from the centre (found you, the shatter, the gutter);</li>
 *   <li>{@code Dark} — the sun's gutter dimming everything for a moment.</li>
 * </ul>
 * Behind {@code SNClientConfig.DISTORTION}; when that is off or the shader fails, {@link Fallback} (a GUI layer under the HUD)
 * stands in with a grey wash, ink at the edges and a tinted flash. The old {@code void_distort} shader is no longer used.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientPostFx {

    static final ResourceLocation SHADER = SupernaturalCraft.asResource("shaders/post/amara_darkness.json");
    static final ResourceLocation VIGNETTE = SupernaturalCraft.asResource("textures/misc/consumption_vignette.png");
    private static final int WAVE_TICKS = 28;

    private static PostChain chain;
    private static boolean failed;
    private static int width = -1, height = -1;

    /** Eased now/last values: warp, desat, chroma, ink, dark; the wave's age. */
    private static float warp, warpO, desat, desatO, chroma, chromaO, ink, inkO, dark, darkO;
    private static float wave = -1, waveO = -1, waveStrength;
    private static float wantChroma, extraInk;

    private ClientPostFx() {
    }

    // --- triggers --------------------------------------------------------------------------------------------------------

    /** One of her blows landed on the viewer. */
    public static void hit(float strength) {
        wantChroma = Math.max(wantChroma, Mth.clamp(strength, 0, 1));
    }

    /** A wave races out from the centre of the screen. */
    public static void waveOut(float strength) {
        wave = waveO = 0;
        waveStrength = Mth.clamp(strength, 0, 1);
    }

    /** Ink pours in a moment (Chuck's wound). */
    static void pourInk(float amount) {
        extraInk = Math.max(extraInk, Mth.clamp(amount, 0, 1));
    }

    // --- reads -----------------------------------------------------------------------------------------------------------

    /** 0..1, smoothed: how strongly she bends this player's sight (the old reading, for the consumption vignette). */
    public static float intensity(float partial) {
        return Mth.lerp(partial, warpO, warp);
    }

    public static float desat(float partial) {
        return Mth.lerp(partial, desatO, desat);
    }

    public static float chroma(float partial) {
        return Mth.lerp(partial, chromaO, chroma);
    }

    public static float ink(float partial) {
        return Mth.lerp(partial, inkO, ink);
    }

    public static float dark(float partial) {
        return Mth.lerp(partial, darkO, dark);
    }

    /** The wave's front, 0 (centre) .. 1.4 (past the corners), or -1 when none. */
    public static float waveFront(float partial) {
        return wave < 0 ? -1 : Mth.lerp(partial, waveO, wave) / WAVE_TICKS * 1.4f;
    }

    /** True when the cheap fallback should draw instead of the shader. */
    public static boolean fallback() {
        return !SNClientConfig.DISTORTION.get() || failed;
    }

    // --- ticking ---------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        warpO = warp;
        desatO = desat;
        chromaO = chroma;
        inkO = ink;
        darkO = dark;
        waveO = wave;
        if (wave >= 0 && ++wave > WAVE_TICKS) wave = waveO = -1;
        float wantWarp = 0, wantDesat = 0, wantInk = 0;
        if (mc.level != null && mc.player != null) {
            AmaraLook look = ClientAmara.current();
            Entity e = ClientAmara.nearestEntity();
            if (look != null) {
                double d = e == null ? 8 : mc.player.position().distanceTo(e.position().add(0, look.form() == AmaraLook.MASS ? AmaraEntity.MASS_CENTER : 1.6, 0));
                float near = Mth.clamp((float) (1 - (d - 6) / 26), 0, 1);
                float present = Mth.clamp((float) (1 - (d - 40) / 60), 0, 1);
                wantWarp = near * (look.form() == AmaraLook.MASS ? 0.55f : 0.35f);
                int phase = look.phase();
                wantDesat = present * (phase <= 1 ? 0.3f : 0.3f + 0.06f * phase);
                if (phase >= 5) wantInk = present * (phase == 5 ? 0.1f : 0.12f + 0.18f * (1 - look.countdown()));
                if (look.enraged()) wantInk = Math.max(wantInk, 0.35f * present);
                if (look.skyStage() == 5) {
                    wantDesat *= 0.3f;
                    wantInk = 0;
                }
            }
            if (mc.level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                wantDesat = Math.max(wantDesat, ClientDyingSun.darkness(1) * 0.4f);
            }
        }
        warp += (wantWarp - warp) * 0.05f;
        desat += (wantDesat - desat) * 0.03f;
        ink += (Math.max(wantInk, extraInk) - ink) * 0.06f;
        extraInk = Math.max(0, extraInk - 0.02f);
        chroma += (wantChroma - chroma) * 0.5f;
        wantChroma = Math.max(0, wantChroma - 0.08f);
        float wantDark = ClientAmara.gutter(1) * 0.7f;
        dark += (wantDark - dark) * 0.3f;
        if (warp < 0.002f) warp = 0;
        if (desat < 0.002f) desat = 0;
        if (ink < 0.002f) ink = 0;
        if (chroma < 0.002f) chroma = 0;
        if (dark < 0.002f) dark = 0;
    }

    private static boolean idle(float partial) {
        return intensity(partial) < 0.01f && desat(partial) < 0.01f && chroma(partial) < 0.01f && ink(partial) < 0.01f
                && dark(partial) < 0.01f && wave < 0;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (fallback() || idle(partial)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), SHADER);
            } catch (Exception e) {
                SupernaturalCraft.LOGGER.warn("The Darkness's shader failed to load; using the cheap stand-in instead", e);
                failed = true;
                return;
            }
        }
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w != width || h != height) {
            chain.resize(w, h);
            width = w;
            height = h;
        }
        chain.setUniform("Warp", intensity(partial));
        chain.setUniform("Desat", desat(partial));
        chain.setUniform("Chroma", chroma(partial));
        chain.setUniform("Ink", ink(partial));
        chain.setUniform("Dark", dark(partial));
        float wv = waveFront(partial);
        chain.setUniform("Wave", wv);
        chain.setUniform("WaveStrength", wv < 0 ? 0 : waveStrength * (1 - Mth.clamp(wv / 1.4f, 0, 1)));
        chain.setUniform("Time", (mc.level.getGameTime() + partial) / 20f);
        chain.process(partial);
        mc.getMainRenderTarget().bindWrite(false);
    }

    /** Forgets the chain (after a resource reload or a failed load). */
    public static void reset() {
        if (chain != null) chain.close();
        chain = null;
        failed = false;
        width = height = -1;
    }

    /** Without the shader: a grey wash for the drained colour, ink at the edges, a violet-red tint for a hit, a dim for the gutter. */
    public static class Fallback implements LayeredDraw.Layer {

        public static final ResourceLocation LAYER = SupernaturalCraft.asResource("amara_post_fallback");

        @Override
        public void render(GuiGraphics g, DeltaTracker delta) {
            if (!fallback()) return;
            float partial = delta.getGameTimeDeltaPartialTick(false);
            if (idle(partial)) return;
            int w = g.guiWidth(), h = g.guiHeight();
            float d = desat(partial), i = ink(partial), c = chroma(partial), k = dark(partial);
            if (d > 0.01f) g.fill(0, 0, w, h, MichaelGui.argb(d * 0.22f, 0x5A566A));
            if (i > 0.01f && MichaelGui.has(VIGNETTE)) {
                MichaelGui.blit(g, VIGNETTE, 0, 0, w, h, 0, 0, 128, 128, 128, 128, Math.min(1, i * 1.3f), 0xFFFFFF);
            }
            if (c > 0.01f) {
                g.fill(0, 0, w, h, MichaelGui.argb(c * 0.18f, 0xB040E0));
                int edge = Math.max(2, (int) (w * 0.012f * c));
                g.fill(0, 0, edge, h, MichaelGui.argb(c * 0.4f, 0xFF2040));
                g.fill(w - edge, 0, w, h, MichaelGui.argb(c * 0.4f, 0x2060FF));
            }
            if (k > 0.01f) g.fill(0, 0, w, h, MichaelGui.argb(k * 0.6f, 0x030206));
        }
    }
}
