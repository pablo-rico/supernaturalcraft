package org.papiricoh.supernaturalcraft.client.michael;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.papiricoh.supernaturalcraft.client.michael.MichaelGui.*;

/**
 * Michael's moments on the screen: each phase's celestial title card (rays turning behind, a pair of wings opening, the
 * roman numeral, the title and its line), the white of his transform, Heaven changing round you, a shower of steel
 * feathers, and the edges of sight while his grace favours you, while his lance pins you or while he wears you, and his
 * mark. Each is a {@link Moment} that ticks and draws itself until it is over (the pattern of the Author's overlay).
 */
public final class MichaelOverlay implements LayeredDraw.Layer {

    private static final List<Moment> MOMENTS = new ArrayList<>();

    /** Something on screen for a while. */
    public abstract static class Moment {
        protected int age;
        protected final int life;

        protected Moment(int life) {
            this.life = Math.max(1, life);
        }

        protected void tick() {
            age++;
        }

        protected boolean over() {
            return age >= life;
        }

        protected float time(float partial) {
            return age + partial;
        }

        /** 0-1: in over {@code in} ticks, out over the last {@code out}. */
        protected float fade(float partial, int in, int out) {
            float t = time(partial);
            return Mth.clamp(Math.min(t / Math.max(1, in), (life - t) / Math.max(1, out)), 0, 1);
        }

        protected abstract void render(GuiGraphics g, float partial, int w, int h);
    }

    public static void add(Moment m) {
        // One title card at a time: a new one takes the old one's place.
        if (m instanceof TitleCard) MOMENTS.removeIf(o -> o instanceof TitleCard);
        MOMENTS.add(m);
    }

    /** Whether a moment of this kind is on screen (tests, previews). */
    public static boolean showing(Class<? extends Moment> kind) {
        return MOMENTS.stream().anyMatch(kind::isInstance);
    }

    static void tick() {
        if (Minecraft.getInstance().level == null) {
            MOMENTS.clear();
            return;
        }
        for (Moment m : List.copyOf(MOMENTS)) {
            m.tick();
            if (m.over()) MOMENTS.remove(m);
        }
    }

    public static void clear() {
        MOMENTS.clear();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        states(g, mc, partial, w, h);
        for (Moment m : List.copyOf(MOMENTS)) m.render(g, partial, w, h);
    }

    /** What lasts as long as the server says: the possession and the mark. */
    private static void states(GuiGraphics g, Minecraft mc, float partial, int w, int h) {
        double t = mc.level.getGameTime() + partial;
        if (ClientMichael.possessed()) {
            float pulse = 0.55f + 0.15f * Mth.sin((float) t * 0.2f);
            edges(g, w, h, pulse, BLUE, 0.22f);
            Component line = Component.translatable("message.supernaturalcraft.michael.possessed");
            GuiDraw.centred(g, line, w / 2f, h * 0.22f, 1.3f, argb(0.85f, LIGHT), true);
        }
        if (ClientMichael.pinned()) edges(g, w, h, 0.5f, 0xFFFFFF, 0.12f);
        if (ClientMichael.marked(mc.player.getId())) {
            float pulse = 0.6f + 0.4f * Mth.sin((float) t * 0.3f);
            int size = 24;
            float x = w / 2f - size / 2f, y = 6;
            if (has(MARK)) glow(g, MARK, x, y, size, size, 0, 0, 32, 32, 32, 32, pulse, GOLD);
            else GuiDraw.centred(g, enochian("M"), w / 2f, y + 4, 2.5f, argb(pulse, GOLD), true);
            edges(g, w, h, 0.35f * pulse, GOLD_DEEP, 0.1f);
        }
    }

    /** A soft band of {@code rgb} round the edges of the screen, {@code depth} of its smaller side deep. */
    static void edges(GuiGraphics g, int w, int h, float a, int rgb, float depth) {
        int e = (int) (Math.min(w, h) * depth);
        int c = argb(a, rgb);
        g.fillGradient(0, 0, w, e, c, 0);
        g.fillGradient(0, h - e, w, h, 0, c);
        // Sideways: fill in thin vertical strips (fillGradient only runs top to bottom).
        for (int i = 0; i < e; i += 2) {
            int ci = argb(a * (1 - i / (float) e), rgb);
            g.fill(i, 0, i + 2, h, ci);
            g.fill(w - i - 2, 0, w - i, h, ci);
        }
    }

    // --- title cards -----------------------------------------------------------------------------------------------

    /** A phase begins (or he falls, or Heaven is silent): rays, opening wings, the numeral, the title and its line. */
    public static final class TitleCard extends Moment {
        private final int which;
        private final Component title, sub;

        public TitleCard(int which, int duration) {
            super(Mth.clamp(duration, 90, 150));
            this.which = which;
            String key = which == MichaelFxPayload.TITLE_DEATH ? "title.supernaturalcraft.michael.death"
                    : which == MichaelFxPayload.TITLE_VICTORY ? "title.supernaturalcraft.michael.victory"
                    : "title.supernaturalcraft.michael.phase" + which;
            title = Component.translatable(key);
            sub = Component.translatable(key + ".sub");
        }

        public int which() {
            return which;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = fade(partial, 10, 25), t = time(partial);
            if (a <= 0.01f) return;
            // Low enough that the wings and numeral clear his boss bar at every GUI scale.
            float cx = w / 2f, cy = Math.max(h * 0.3f, 104);
            boolean phase = which >= 1 && which <= 6;
            // Rays turning slowly behind it all.
            float rays = 150 + 40 * Mth.clamp(t / 30f, 0, 1);
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(t * 0.01f));
            if (has(TITLE_RAYS)) {
                glow(g, TITLE_RAYS, -rays / 2, -rays / 2, (int) rays, (int) rays, 0, 0, 256, 256, 256, 256, a * 0.8f, GOLD);
            } else {
                for (int i = 0; i < 16; i++) {
                    float ang = i * Mth.TWO_PI / 16;
                    GuiDraw.line(g, 0, 0, Mth.cos(ang) * rays / 2, Mth.sin(ang) * rays / 2, i % 2 == 0 ? 3f : 1.5f, argb(a * 0.25f, GOLD));
                }
            }
            g.pose().popPose();
            // The wings opening above the title.
            int frame = Mth.clamp((int) (t / 2.5f), 0, 7);
            if (has(TITLE_WINGS)) {
                blit(g, TITLE_WINGS, cx - 128, cy - 52, 256, 64, 0, frame * 64, 256, 64, 256, 512, a, 0xFFFFFF);
            }
            if (phase) {
                GuiDraw.centred(g, Component.literal(roman(which)), cx, cy - 36, 3f, argb(a, GOLD), true);
            }
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            GuiDraw.centred(g, title, cx, cy - 4, scale, argb(a, 0xFFFBEA), true);
            float fy = cy - 4 + 9 * scale + 3;
            float grow = Mth.clamp((t - 6) / 14f, 0, 1);
            if (has(TITLE_FLOURISH)) {
                int fw = (int) (256 * grow);
                if (fw > 0) blit(g, TITLE_FLOURISH, cx - fw / 2f, fy, fw, 24, 128 - fw / 2f, 0, fw, 24, 256, 24, a, 0xFFFFFF);
            } else {
                GuiDraw.line(g, cx - 90 * grow, fy + 4, cx + 90 * grow, fy + 4, 1.2f, argb(a, GOLD_DEEP));
            }
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.2f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, fy + 24, ss, argb(sa, LIGHT), true);
        }
    }

    // --- light ---------------------------------------------------------------------------------------------------

    /** The screen goes white up to {@code peak} and back (his transform; Heaven changing). */
    public static final class Flash extends Moment {
        private final int peak, rgb;
        private final float strength;

        public Flash(int life, int peak, int rgb, float strength) {
            super(life);
            this.peak = Math.max(1, peak);
            this.rgb = rgb;
            this.strength = strength;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = time(partial);
            float a = t < peak ? (t / peak) * (t / peak) : 1 - (t - peak) / Math.max(1, life - peak);
            if (a <= 0.01f) return;
            g.fill(0, 0, w, h, argb(a * strength, rgb));
        }
    }

    /** A grace, a pin: the edges of sight tinted for a while. */
    public static final class Edges extends Moment {
        private final int rgb;
        private final float strength;

        public Edges(int life, int rgb, float strength) {
            super(life);
            this.rgb = rgb;
            this.strength = strength;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = fade(partial, 10, 30);
            float pulse = 0.8f + 0.2f * Mth.sin(time(partial) * 0.25f);
            edges(g, w, h, a * strength * pulse, rgb, 0.12f);
        }
    }

    /** Steel feathers blown across the screen. */
    public static final class Feathers extends Moment {
        private final float[][] f;

        public Feathers(int count) {
            super(40);
            Random r = new Random();
            f = new float[Math.min(24, Math.max(4, count))][];
            for (int i = 0; i < f.length; i++) {
                f[i] = new float[]{r.nextFloat(), -0.1f - r.nextFloat() * 0.3f, (r.nextFloat() - 0.5f) * 0.01f, 0.012f + r.nextFloat() * 0.015f,
                        r.nextFloat() * Mth.TWO_PI, (r.nextFloat() - 0.5f) * 0.3f};
            }
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = time(partial), a = fade(partial, 4, 14);
            for (float[] p : f) {
                float x = (p[0] + p[2] * t) * w, y = (p[1] + p[3] * t) * h;
                g.pose().pushPose();
                g.pose().translate(x, y, 0);
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(p[4] + p[5] * t));
                if (has(FEATHER)) blit(g, FEATHER, -8, -8, 16, 16, 0, 0, 16, 16, 16, 16, a, 0xDDE6F0);
                else g.fill(-1, -6, 1, 6, argb(a, 0xDDE6F0));
                g.pose().popPose();
            }
        }
    }
}
