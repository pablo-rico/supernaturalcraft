package org.papiricoh.supernaturalcraft.client.raphael;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Raphael's moments on the screen (v0.16): each phase's title card (a band of storm cloud, lightning forking across it, the
 * roman numeral, the title and its line) and the white of the lightning flashes. Each is a {@link Moment} that ticks and draws
 * itself until it is over (the pattern of Michael's overlay).
 */
public final class RaphaelOverlay implements LayeredDraw.Layer {

    static final int STORM = 0x1A2030, STORM_LIGHT = 0x3A4660, BOLT = 0xDDEBFF, PALE = 0xB9D8FF, GOLD = 0xFFE3A5;
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
        if (mc.player == null || MOMENTS.isEmpty()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        for (Moment m : List.copyOf(MOMENTS)) m.render(g, partial, w, h);
    }

    static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "";
        };
    }

    // --- the title card --------------------------------------------------------------------------------------------------

    /** A phase begins (or he falls): storm, forked lightning, the numeral, the title and its line. */
    public static final class TitleCard extends Moment {
        private final int phase;
        private final boolean fall;
        private final Component title, sub;
        private final long seed = new Random().nextLong();

        public TitleCard(int phase, boolean fall, int duration) {
            super(Mth.clamp(duration, 70, 150));
            this.phase = phase;
            this.fall = fall;
            String key = fall ? "title.supernaturalcraft.raphael.death" : "title.supernaturalcraft.raphael.phase" + Mth.clamp(phase, 1, 3);
            title = Component.translatable(key);
            sub = Component.translatable(key + ".sub");
        }

        public int phase() {
            return phase;
        }

        public boolean fall() {
            return fall;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = fade(partial, 8, 25), t = time(partial);
            if (a <= 0.01f) return;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 100);
            // A band of storm cloud across the screen, lighter at its heart.
            int band = 46;
            for (int i = 0; i < band; i += 2) {
                float k = 1 - Math.abs(i - band / 2f) / (band / 2f);
                g.fill(0, (int) (cy - band / 2f + i), w, (int) (cy - band / 2f + i + 2), argb(a * 0.62f * k, i % 4 == 0 ? STORM : STORM_LIGHT));
            }
            // Lightning forks across it, now and then (each strike lasts a few ticks).
            int strike = (int) (t / 9);
            float life = (t % 9) / 9f;
            if (life < 0.45f) bolt(g, new Random(seed + strike), cx, cy, w, a * (1 - life / 0.45f));
            if (!fall) GuiDraw.centred(g, Component.literal(roman(phase)), cx, cy - 34, 2.6f, argb(a, PALE), true);
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            GuiDraw.centred(g, title, cx, cy - 10, scale, argb(a, fall ? GOLD : 0xF4F9FF), true);
            float fy = cy - 10 + 9 * scale + 3;
            float grow = Mth.clamp((t - 6) / 14f, 0, 1);
            GuiDraw.line(g, cx - 100 * grow, fy + 3, cx + 100 * grow, fy + 3, 1.2f, argb(a * 0.8f, PALE));
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, fy + 10, ss, argb(sa, PALE), true);
        }

        /** One forked bolt from somewhere above the band to somewhere below it. */
        private static void bolt(GuiGraphics g, Random r, float cx, float cy, int w, float a) {
            float x = cx + (r.nextFloat() - 0.5f) * w * 0.8f, y = cy - 40;
            for (int i = 0; i < 9; i++) {
                float nx = x + (r.nextFloat() - 0.5f) * 22, ny = y + 9;
                GuiDraw.line(g, x, y, nx, ny, 3f, argb(a * 0.35f, PALE));
                GuiDraw.line(g, x, y, nx, ny, 1.2f, argb(a, BOLT));
                if (r.nextFloat() < 0.25f) {
                    float bx = nx + (r.nextFloat() - 0.5f) * 30, by = ny + 10;
                    GuiDraw.line(g, nx, ny, bx, by, 0.8f, argb(a * 0.7f, BOLT));
                }
                x = nx;
                y = ny;
            }
        }
    }

    // --- the flash -------------------------------------------------------------------------------------------------------

    /** The screen goes white at once and fades (a lightning flash; stronger the nearer it fell). */
    public static final class Flash extends Moment {
        private final float strength;

        public Flash(int life, float strength) {
            super(life);
            this.strength = strength;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = time(partial);
            float a = t < 1 ? t : 1 - (t - 1) / Math.max(1, life - 1);
            // A second, fainter flicker, as lightning does.
            if (t > life * 0.45f && t < life * 0.6f) a = Math.max(a, 0.6f);
            if (a <= 0.01f) return;
            g.fill(0, 0, w, h, argb(a * strength, 0xF2F7FF));
        }
    }
}
