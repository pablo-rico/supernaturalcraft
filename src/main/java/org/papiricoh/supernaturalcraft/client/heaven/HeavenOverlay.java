package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Heaven's moments on the screen (v0.18): the white wash of an arrival, the Memory Wipe's whiteout, the tint of a staged memory,
 * title cards (Heaven's, a memory's, Naomi's and Zachariah's phases), and under them the lasting HUD: the plot being written,
 * the chair's struggle ({@link QteOverlay}), the training test, the form ({@link FormHud}), the docket ({@link DocketHud}) and
 * a Termination Notice. Each passing thing is a {@link Moment} that ticks and draws itself until it is over (the pattern of
 * Raphael's overlay).
 */
public final class HeavenOverlay implements LayeredDraw.Layer {

    private static final List<Moment> MOMENTS = new ArrayList<>();

    /** Something on screen for a while. */
    public abstract static class Moment {
        protected int age;
        protected final int life;

        protected Moment(int life) {
            this.life = Math.max(1, life);
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

        /** Washes go under the HUD, cards over it. */
        protected boolean under() {
            return false;
        }

        protected abstract void render(GuiGraphics g, float partial, int w, int h);
    }

    public static void add(Moment m) {
        if (m instanceof TitleCard) MOMENTS.removeIf(o -> o instanceof TitleCard);
        MOMENTS.add(m);
    }

    public static boolean showing(Class<? extends Moment> kind) {
        return MOMENTS.stream().anyMatch(kind::isInstance);
    }

    static void tick() {
        if (Minecraft.getInstance().level == null) {
            MOMENTS.clear();
            return;
        }
        for (Moment m : List.copyOf(MOMENTS)) {
            m.age++;
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
        MemoryTint.renderVignette(g, partial, w, h);
        for (Moment m : List.copyOf(MOMENTS)) if (m.under()) m.render(g, partial, w, h);
        // The layer draws through camera shots (for the title cards and washes); the game's HUD and its prompts do not.
        boolean camera = org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.active();
        if (!mc.options.hideGui && !camera) {
            PlotProgress.render(g, partial, w, h);
            TrainingTest.render(g, partial, w, h);
            FormHud.render(g, partial, w, h);
            DocketHud.render(g, partial, w, h);
            Termination.render(g, partial, w, h);
        }
        if (!camera) QteOverlay.render(g, partial, w, h);
        for (Moment m : List.copyOf(MOMENTS)) if (!m.under()) m.render(g, partial, w, h);
    }

    // --- washes ------------------------------------------------------------------------------------------------------

    /**
     * The screen floods with a colour and lets go of it: at full {@code peak} after {@code in} ticks, held for {@code hold},
     * gone by the end. An arrival is white fading out at once; the Memory Wipe holds its white.
     */
    public static final class Wash extends Moment {
        private final int rgb, in, hold;
        private final float peak;

        public Wash(int rgb, float peak, int in, int hold, int life) {
            super(Math.max(life, in + hold + 1));
            this.rgb = rgb;
            this.peak = peak;
            this.in = in;
            this.hold = hold;
        }

        @Override
        protected boolean under() {
            return true;
        }

        public float alpha(float partial) {
            float t = time(partial);
            if (t < in) return peak * t / Math.max(1, in);
            if (t < in + hold) return peak;
            return peak * Mth.clamp(1 - (t - in - hold) / Math.max(1, life - in - hold), 0, 1);
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = alpha(partial);
            if (a <= 0.005f) return;
            g.fill(0, 0, w, h, HeavenGui.argb(a, rgb));
            // A brighter heart, as light pours in from somewhere ahead.
            int cx = w / 2, cy = h / 2;
            for (int i = 1; i <= 4; i++) {
                int rx = w * i / 10, ry = h * i / 10;
                g.fill(cx - rx, cy - ry, cx + rx, cy + ry, HeavenGui.argb(a * 0.12f, 0xFFFFFF));
            }
        }
    }

    // --- title cards ---------------------------------------------------------------------------------------------------

    /**
     * A title in Heaven: a band of light across the screen (its look by {@link HeavenGui.Style}), an optional numeral over the
     * title, its line in italics under a rule that grows.
     */
    public static final class TitleCard extends Moment {
        private final Component numeral, title, sub;
        private final HeavenGui.Style style;
        private final int accent;

        public TitleCard(Component numeral, Component title, Component sub, HeavenGui.Style style, int accent, int life) {
            super(Mth.clamp(life, 60, 160));
            this.numeral = numeral;
            this.title = title;
            this.sub = sub;
            this.style = style;
            this.accent = accent;
        }

        public Component title() {
            return title;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = fade(partial, 10, 28), t = time(partial);
            if (a <= 0.01f) return;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 90);
            band(g, a, t, cx, cy, w);
            if (!numeral.getString().isEmpty()) GuiDraw.centred(g, numeral, cx, cy - 34, 2.4f, HeavenGui.argb(a, accent), true);
            float scale = Math.min(2.3f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            int ink = style == HeavenGui.Style.BAR ? 0xF6EBD0 : style == HeavenGui.Style.OFFICE ? 0x262630 : 0xFFFFFF;
            boolean shadow = style != HeavenGui.Style.OFFICE;
            GuiDraw.centred(g, title, cx, cy - 10, scale, HeavenGui.argb(a, ink), shadow);
            float fy = cy - 10 + 9 * scale + 3;
            float grow = Mth.clamp((t - 6) / 16f, 0, 1);
            GuiDraw.line(g, cx - 90 * grow, fy + 3, cx + 90 * grow, fy + 3, 1.2f, HeavenGui.argb(a * 0.85f, accent));
            if (sub.getString().isEmpty()) return;
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, fy + 10, ss, HeavenGui.argb(sa, style == HeavenGui.Style.OFFICE ? 0x3A3A44 : 0xFFF4DA), shadow);
        }

        private void band(GuiGraphics g, float a, float t, float cx, float cy, int w) {
            int band = 50;
            int base = switch (style) {
                case CLINIC -> 0x9ED9E6;
                case OFFICE -> 0xF2EBD6;
                case BAR -> 0x2B1C12;
                default -> 0xFFF2CF;
            };
            float strength = style == HeavenGui.Style.OFFICE ? 0.85f : 0.55f;
            for (int i = 0; i < band; i += 2) {
                float k = 1 - Math.abs(i - band / 2f) / (band / 2f);
                g.fill(0, (int) (cy - band / 2f + i), w, (int) (cy - band / 2f + i + 2), HeavenGui.argb(a * strength * k, base));
            }
            switch (style) {
                case HEAVEN -> {
                    // Slow rays of light fanning from behind the title.
                    for (int i = 0; i < 9; i++) {
                        float ang = (i - 4) * 0.28f + Mth.sin(t * 0.02f + i) * 0.03f;
                        float x1 = cx + Mth.sin(ang) * 260, y1 = cy - Mth.cos(ang) * 120;
                        GuiDraw.line(g, cx, cy + 10, x1, y1, 7f, HeavenGui.argb(a * 0.10f, HeavenGui.GOLD));
                    }
                }
                case CLINIC -> {
                    // A heart monitor's trace running under the title.
                    float y = cy + band / 2f - 6;
                    float head = (t * 6) % (w + 60);
                    float x = 0, py = y;
                    for (int s = 0; s < w; s += 6) {
                        float k = Math.abs(s - (head - 30)) < 18 ? 1 : 0;
                        float ny = y - (s % 60 == 30 ? 10 * k : 0);
                        float glow = Math.max(0.15f, 1 - Math.abs(s - head) / 90f);
                        GuiDraw.line(g, x, py, s, ny, 1f, HeavenGui.argb(a * glow, HeavenGui.CYAN));
                        x = s;
                        py = ny;
                    }
                }
                case OFFICE -> {
                    // Ruled paper.
                    for (int y = (int) (cy - band / 2f + 6); y < cy + band / 2f; y += 8) {
                        g.fill(0, y, w, y + 1, HeavenGui.argb(a * 0.25f, 0x7F9CC8));
                    }
                }
                default -> {
                }
            }
        }
    }

    // --- a short line under the crosshair (a struggle won, a test passed) -------------------------------------------------

    /** A short message that rises and fades under the crosshair. */
    public static final class Notice extends Moment {
        private final Component text;
        private final int rgb;

        public Notice(Component text, int rgb, int life) {
            super(life);
            this.text = text;
            this.rgb = rgb;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = fade(partial, 4, 20);
            if (a <= 0.01f) return;
            float y = h / 2f + 24 - Math.min(8, time(partial) * 0.3f);
            GuiDraw.centred(g, text, w / 2f, y, 1.5f, HeavenGui.argb(a, rgb), true);
        }
    }
}
