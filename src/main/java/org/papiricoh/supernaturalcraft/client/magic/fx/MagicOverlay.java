package org.papiricoh.supernaturalcraft.client.magic.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.mark.ClientMark;

/**
 * The magic's screen layer (v0.21): a blinding flash when a banishing or a judgement goes off close by, and the Mark's red at the
 * edges of a marked player's view, growing with their thirst ({@link ClientMark#thirst()}) and beating like a heart past one half;
 * each MARK_FLARE on the local player throbs it; a magenta seam stitched across the view when a silenced player tries to cast
 * ({@link #stitched()}); and when a witch's familiar spots the local player, eyes opening in a violet dusk at the edges of the
 * view ({@link #watched}: a cat's amber slits or a raven's beady magenta). No title cards, so it is simply hidden during camera
 * sequences.
 */
public final class MagicOverlay implements LayeredDraw.Layer {

    private static float flash, flashO;
    private static int flashRgb = 0xFFFFFF;
    private static float throb, throbO;
    private static int beat;
    private static float stitch, stitchO;
    private static float watch, watchO;
    private static boolean watchRaven;
    /** Ticks the familiar's eyes stay at the edges of the view. */
    public static final int WATCH_TICKS = 60;
    /** The Curse of Silence's magenta. */
    public static final int SILENCE = 0xD03CA8;

    /** A flash of {@code power} (0-1) seen from the camera, fading out at {@code range} blocks. */
    public static void flash(Vec3 at, int rgb, float power, double range) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return;
        double d = mc.gameRenderer.getMainCamera().getPosition().distanceTo(at);
        float k = (float) Mth.clamp(1 - d / range, 0, 1) * power;
        if (k <= flash) return;
        flash = flashO = Math.min(0.9f, k);
        flashRgb = rgb;
    }

    /** The local player's Mark throbs ({@code strength} 0-1). */
    public static void throb(float strength) {
        throb = Math.max(throb, Mth.clamp(strength, 0, 1));
    }

    /** A silenced player tried to cast a sigil or a power: their lips are stitched. */
    public static void stitched() {
        stitch = stitchO = 1;
    }

    /** A familiar spotted the local player ({@code raven}: a raven's eyes, else a cat's). */
    public static void watched(boolean raven) {
        watch = watchO = 1;
        watchRaven = raven;
    }

    static void tick() {
        watchO = watch;
        watch = Math.max(0, watch - 1f / WATCH_TICKS);
        stitchO = stitch;
        stitch = stitch < 0.01f ? 0 : stitch * 0.9f;
        flashO = flash;
        flash = flash < 0.01f ? 0 : flash * 0.78f;
        throbO = throb;
        throb = throb < 0.01f ? 0 : throb * 0.9f;
        beat++;
    }

    static void reset() {
        flash = flashO = throb = throbO = stitch = stitchO = watch = watchO = 0;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || CameraDirector.active()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        float thirst = ClientMark.thirst();
        float th = Mth.lerp(partial, throbO, throb);
        if (thirst > 0.1f || th > 0.01f) {
            float base = thirst > 0.1f ? 0.1f + 0.4f * (thirst - 0.1f) / 0.9f : 0;
            // Past one half the edges beat: a double thump about once a second.
            if (thirst >= 0.5f) {
                float b = ((beat + partial) % 22) / 22f;
                float thump = Math.max(bump(b, 0.0f), 0.6f * bump(b, 0.18f));
                base += 0.18f * thump * (thirst - 0.4f);
            }
            float a = Mth.clamp(base + th * 0.45f, 0, 0.85f);
            int depth = (int) (Math.min(w, h) * (0.16f + 0.14f * thirst + 0.08f * th));
            edges(g, w, h, depth, FxColor.BLOOD, a);
        }
        float st = Mth.lerp(partial, stitchO, stitch);
        if (st > 0.01f) {
            edges(g, w, h, (int) (Math.min(w, h) * 0.12f), 0x5A1048, 0.35f * st);
            float shake = st > 0.7f ? Mth.sin((beat + partial) * 3.1f) * 3 * (st - 0.7f) / 0.3f : 0;
            stitches(g, w / 2f + shake, h / 2f + 22, 70, st);
        }
        float wa = Mth.lerp(partial, watchO, watch);
        if (wa > 0.005f) watchedEyes(g, w, h, wa);
        float f = Mth.lerp(partial, flashO, flash);
        if (f > 0.01f) g.fill(0, 0, w, h, (int) (f * 255) << 24 | flashRgb & 0xFFFFFF);
    }

    /**
     * A seam with cross-stitches, {@code width} wide, centred on (cx, cy), in the Curse of Silence's magenta with a dark edge: drawn
     * over the silenced sigil HUD, the power wheel and the emblem too.
     */
    public static void stitches(GuiGraphics g, float cx, float cy, float width, float alpha) {
        if (alpha <= 0.01f) return;
        int dark = (int) (alpha * 200) << 24 | 0x1A0414, col = (int) (alpha * 255) << 24 | SILENCE;
        float x0 = cx - width / 2, x1 = cx + width / 2;
        org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.line(g, x0, cy, x1, cy, 3, dark);
        org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.line(g, x0, cy, x1, cy, 1.2f, col);
        int n = Math.max(2, Math.round(width / 10));
        float step = width / n, sh = Math.min(5, width / 12 + 2);
        for (int i = 0; i < n; i++) {
            float x = x0 + step * (i + 0.5f);
            org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.line(g, x - sh * 0.6f, cy - sh, x + sh * 0.6f, cy + sh, 2.6f, dark);
            org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.line(g, x + sh * 0.6f, cy - sh, x - sh * 0.6f, cy + sh, 2.6f, dark);
            org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.line(g, x - sh * 0.6f, cy - sh, x + sh * 0.6f, cy + sh, 1.2f, col);
            org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.line(g, x + sh * 0.6f, cy - sh, x - sh * 0.6f, cy + sh, 1.2f, col);
        }
    }

    /**
     * The familiar's eyes at the edges of the view, {@code k} running 1 → 0: a violet dusk at the edges, and an eye at each side
     * that opens, blinks once, and closes again.
     */
    private static void watchedEyes(GuiGraphics g, int w, int h, float k) {
        float p = 1 - k;
        float dusk = Mth.clamp(p / 0.1f, 0, 1) * Mth.clamp(k / 0.3f, 0, 1);
        edges(g, w, h, (int) (Math.min(w, h) * 0.16f), 0x1C0620, 0.45f * dusk);
        float open = smooth(p / 0.18f) * (1 - smooth((p - 0.72f) / 0.22f));
        float blink = Math.abs(p - 0.46f) < 0.04f ? Math.abs(p - 0.46f) / 0.04f : 1;
        open *= blink;
        if (open <= 0.02f) return;
        float ew = Math.min(w, h) * 0.075f, eh = ew * 0.42f;
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        eye(vc, m, ew * 1.3f, h * 0.42f, ew, eh, open, dusk);
        eye(vc, m, w - ew * 1.3f, h * 0.42f, ew, eh, open, dusk);
        g.flush();
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** One almond eye centred on (cx, cy), {@code ew} half-wide, {@code eh} half-high when open, drawn in vertical strips. */
    private static void eye(VertexConsumer vc, Matrix4f m, float cx, float cy, float ew, float eh, float open, float alpha) {
        int n = 18;
        float iris = eh * 0.95f, pupil = iris * 0.45f;
        int sclera = (int) (alpha * 220) << 24 | 0x14040F;
        int irisRgb = watchRaven ? 0xD03CA8 : 0xE8A82A, rim = (int) (alpha * 150) << 24 | irisRgb;
        int irisCol = (int) (alpha * 235) << 24 | irisRgb, pupilCol = (int) (alpha * 245) << 24;
        for (int i = 0; i < n; i++) {
            float u0 = -1 + 2f * i / n, u1 = -1 + 2f * (i + 1) / n;
            float x0 = cx + u0 * ew, x1 = cx + u1 * ew;
            float h0 = lid(u0) * eh * open, h1 = lid(u1) * eh * open;
            // A faint glow round the lids, then the dark of the eye.
            strip(vc, m, x0, h0 * 1.35f + 1.5f, x1, h1 * 1.35f + 1.5f, cy, rim & 0x00FFFFFF | (int) (alpha * 60) << 24);
            strip(vc, m, x0, h0, x1, h1, cy, sclera);
            float d0 = x0 - cx, d1 = x1 - cx;
            float i0 = Math.min(h0, circle(iris, d0)), i1 = Math.min(h1, circle(iris, d1));
            if (i0 > 0 || i1 > 0) strip(vc, m, x0, Math.max(0, i0), x1, Math.max(0, i1), cy, irisCol);
            if (watchRaven) {
                float p0 = Math.min(h0, circle(pupil, d0)), p1 = Math.min(h1, circle(pupil, d1));
                if (p0 > 0 || p1 > 0) strip(vc, m, x0, Math.max(0, p0), x1, Math.max(0, p1), cy, pupilCol);
            }
        }
        if (!watchRaven) {
            // A cat's slit: a thin lens, as tall as the lids allow.
            float ph = Math.min(eh * open, iris * 0.92f), pw = Math.max(1.2f, iris * 0.22f);
            strip(vc, m, cx - pw, 0, cx, ph, cy, pupilCol);
            strip(vc, m, cx, ph, cx + pw, 0, cy, pupilCol);
        }
    }

    /** The almond's half-height (0-1) across it ({@code u} -1 → 1). */
    private static float lid(float u) {
        return (float) Math.pow(Math.max(0, 1 - u * u), 0.75);
    }

    private static float circle(float r, float d) {
        return d * d >= r * r ? 0 : Mth.sqrt(r * r - d * d);
    }

    /** A vertical strip from x0 to x1, symmetric about cy, half-heights h0 and h1. */
    private static void strip(VertexConsumer vc, Matrix4f m, float x0, float h0, float x1, float h1, float cy, int argb) {
        if (h0 <= 0 && h1 <= 0) return;
        vc.addVertex(m, x0, cy - h0, 0).setColor(argb);
        vc.addVertex(m, x0, cy + h0, 0).setColor(argb);
        vc.addVertex(m, x1, cy + h1, 0).setColor(argb);
        vc.addVertex(m, x1, cy - h1, 0).setColor(argb);
    }

    private static float bump(float x, float at) {
        float d = (x - at) / 0.07f;
        return (float) Math.exp(-d * d);
    }

    /** Four gradients from the screen's edges inward. */
    private static void edges(GuiGraphics g, int w, int h, int depth, int rgb, float alpha) {
        int a = (int) (alpha * 255) << 24, c = a | rgb & 0xFFFFFF, clear = rgb & 0xFFFFFF;
        g.fillGradient(0, 0, w, depth, c, clear);
        g.fillGradient(0, h - depth, w, h, clear, c);
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        horizontal(vc, m, 0, 0, depth, h, c, clear);
        horizontal(vc, m, w - depth, 0, w, h, clear, c);
        g.flush();
    }

    private static void horizontal(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int left, int right) {
        vc.addVertex(m, x0, y0, 0).setColor(left);
        vc.addVertex(m, x0, y1, 0).setColor(left);
        vc.addVertex(m, x1, y1, 0).setColor(right);
        vc.addVertex(m, x1, y0, 0).setColor(right);
    }
}
