package org.papiricoh.supernaturalcraft.client.leviathan;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.Random;

/**
 * Dick Roman's title cards (v0.25): each phase's card and his fall's, over his camera shots (the layer is let through camera
 * sequences, {@code CameraDirector.showDuringSequences}; it draws no game HUD of its own). The first is a boardroom's: a cold
 * white band with steel rules and a stock ticker scrolling along it; from the Maw on, a band of black goo with an oil-blue sheen
 * running across it and drops falling off its lower edge; his fall's, the goo drying to borax white, flakes drifting down.
 */
public final class DickOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("dick_roman");
    static final int OFFICE = 0x1A222C, STEEL = 0x8C9AA8, WHITE = 0xF2F6FA, GOO = 0x05070B, GOO_LIGHT = 0x121A26, OIL = 0x4A7AB8,
            OIL_PALE = 0x9CC4F0, BORAX = 0xF4F4EE, ASH = 0xBFC4C8, TIE = 0xB01A22;

    private static TitleCard card;
    /** The local hunter inside him: ticks left and how long it lasts (0 when out). */
    private static int swallowed, swallowedLife;

    private DickOverlay() {
    }

    /** The layer itself (registered once). */
    static DickOverlay layer() {
        return new DickOverlay();
    }

    /** Shows a card (a new one replaces the last). */
    public static void title(int phase, boolean fall, int duration) {
        card = new TitleCard(phase, fall, duration);
    }

    /** The local hunter has been swallowed for {@code ticks} (or, with 0, spat out). */
    public static void swallowed(int ticks) {
        swallowed = Math.max(0, ticks);
        swallowedLife = swallowed;
    }

    public static boolean showing() {
        return card != null;
    }

    static void tick() {
        if (Minecraft.getInstance().level == null) {
            card = null;
            return;
        }
        if (card != null && ++card.age >= card.life) card = null;
        if (swallowed > 0) swallowed--;
    }

    static void clear() {
        card = null;
        swallowed = 0;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        if (swallowed > 0) gut(g, partial, g.guiWidth(), g.guiHeight());
        TitleCard c = card;
        if (c != null) c.render(g, partial, g.guiWidth(), g.guiHeight());
    }

    /** Inside him: black, a slow oil-blue sheen breathing round the edges, closing in and opening at the end. */
    private static void gut(GuiGraphics g, float partial, int w, int h) {
        float left = swallowed - partial, since = swallowedLife - left;
        float a = Mth.clamp(Math.min(since / 4f, left / 6f), 0, 1);
        g.fill(0, 0, w, h, argb(a * 0.94f, GOO));
        float breathe = 0.5f + 0.5f * Mth.sin(since * 0.25f);
        int steps = 10;
        for (int i = 0; i < steps; i++) {
            float k = 1 - i / (float) steps;
            int inset = i * 3;
            int col = argb(a * 0.06f * k * (0.6f + 0.4f * breathe), OIL);
            g.fill(inset, inset, w - inset, inset + 3, col);
            g.fill(inset, h - inset - 3, w - inset, h - inset, col);
            g.fill(inset, inset + 3, inset + 3, h - inset - 3, col);
            g.fill(w - inset - 3, inset + 3, w - inset, h - inset - 3, col);
        }
    }

    static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "";
        };
    }

    /** One card. */
    static final class TitleCard {
        final int phase, life;
        final boolean fall;
        final Component title, sub;
        final long seed = new Random().nextLong();
        int age;

        TitleCard(int phase, boolean fall, int duration) {
            this.phase = Mth.clamp(phase, 1, 4);
            this.fall = fall;
            this.life = Mth.clamp(duration, 70, 150);
            String key = fall ? "title.supernaturalcraft.dick_roman.death" : "title.supernaturalcraft.dick_roman.phase" + this.phase;
            title = Component.translatable(key);
            sub = Component.translatable(key + ".sub");
        }

        private float fade(float t, int in, int out) {
            return Mth.clamp(Math.min(t / in, (life - t) / out), 0, 1);
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial, a = fade(t, 8, 25);
            if (a <= 0.01f) return;
            boolean office = !fall && phase == 1;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 100);
            int bh = 50;
            // The band: office glass, the goo, or the goo drying white.
            float dry = fall ? Mth.clamp((t - 10) / 50f, 0, 1) : 0;
            int band = office ? OFFICE : lerpRgb(GOO, 0x2A2C2E, dry), bandLight = office ? 0x2C3846 : lerpRgb(GOO_LIGHT, 0x4A4C4E, dry);
            for (int i = 0; i < bh; i += 2) {
                float k = 1 - Math.abs(i - bh / 2f) / (bh / 2f);
                g.fill(0, (int) (cy - bh / 2f + i), w, (int) (cy - bh / 2f + i + 2), argb(a * (office ? 0.62f : 0.78f) * k, i % 4 == 0 ? band : bandLight));
            }
            Random r = new Random(seed);
            if (office) {
                ticker(g, r, w, cy, bh, t, a);
            } else {
                sheen(g, w, cy, bh, t, a * (1 - dry));
                drips(g, r, w, cy, bh, t, a * (1 - dry), phase >= 4);
                if (fall) flakes(g, r, w, cy, t, a * dry);
            }
            // The numeral, flanked by two short steel rules (a tie-red pin between them in the office).
            float numY = cy - 36;
            if (!fall) {
                int numRgb = office ? WHITE : OIL_PALE;
                GuiDraw.centred(g, Component.literal(roman(phase)), cx, numY, 2.6f, argb(a, numRgb), true);
                float grow = Mth.clamp((t - 2) / 12f, 0, 1);
                GuiDraw.line(g, cx - 18 - 40 * grow, numY + 9, cx - 18, numY + 9, 1f, argb(a * 0.8f, office ? STEEL : OIL));
                GuiDraw.line(g, cx + 18, numY + 9, cx + 18 + 40 * grow, numY + 9, 1f, argb(a * 0.8f, office ? STEEL : OIL));
                if (office) g.fill((int) cx - 1, (int) numY + 22, (int) cx + 2, (int) numY + 25, argb(a, TIE));
            }
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            int titleRgb = office ? WHITE : fall ? lerpRgb(OIL_PALE, BORAX, dry) : 0xE6F0FA;
            // The goo's title wobbles a little, as if under a skin.
            float wob = office || fall ? 0 : 0.6f * Mth.sin(t * 0.35f);
            GuiDraw.centred(g, title, cx + wob, cy - 10, scale, argb(a, titleRgb), true);
            float fy = cy - 10 + 9 * scale + 3, grow = Mth.clamp((t - 6) / 14f, 0, 1);
            int rule = office ? STEEL : fall ? lerpRgb(OIL, ASH, dry) : OIL;
            GuiDraw.line(g, cx - 100 * grow, fy + 3, cx + 100 * grow, fy + 3, 1.2f, argb(a * 0.8f, rule));
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, fy + 10, ss, argb(sa, office ? 0xC8D4E0 : fall ? ASH : 0x8AA6C8), true);
        }

        /** A stock ticker scrolling along the band's lower edge: arrows and figures, most of them going up. */
        private static void ticker(GuiGraphics g, Random r, int w, float cy, int bh, float t, float a) {
            String[] syms = {"RMN", "BRX", "SUC", "LVT", "CHT", "PRT", "BIG", "MRS"};
            float y = cy + bh / 2f - 9, x = -((t * 0.9f) % 360);
            for (int i = 0; x < w; i++) {
                boolean up = r.nextFloat() < 0.8f;
                String s = syms[i % syms.length] + (up ? " ▲" : " ▼") + String.format(" %.2f", 1 + r.nextFloat() * 40);
                Component c = Component.literal(s);
                GuiDraw.text(g, c, x, y, 0.6f, argb(a * 0.55f, up ? 0x8AD8A0 : 0xE07A7A), false);
                x += GuiDraw.width(c, 0.6f) + 14;
            }
            // Thin steel rules top and bottom, like a glass desk's edges.
            g.fill(0, (int) (cy - bh / 2f + 2), w, (int) (cy - bh / 2f + 3), argb(a * 0.5f, STEEL));
            g.fill(0, (int) (cy + bh / 2f - 3), w, (int) (cy + bh / 2f - 2), argb(a * 0.35f, STEEL));
        }

        /** A slow oil-blue sheen sliding along the goo. */
        private static void sheen(GuiGraphics g, int w, float cy, int bh, float t, float a) {
            float sx = ((t * 3.2f) % (w + 160)) - 80;
            for (int i = -40; i <= 40; i += 2) {
                float k = 1 - Math.abs(i) / 40f;
                int x = (int) (sx + i);
                g.fill(x, (int) (cy - bh / 2f + 6 + i * 0.1f), x + 2, (int) (cy - bh / 2f + 8 + i * 0.1f), argb(a * 0.5f * k, OIL_PALE));
                g.fill(x, (int) (cy + bh / 2f - 10 - i * 0.1f), x + 2, (int) (cy + bh / 2f - 9 - i * 0.1f), argb(a * 0.25f * k, OIL));
            }
        }

        /** Drops of goo stretching off the band's lower edge and falling (more and fatter in the Flood). */
        private static void drips(GuiGraphics g, Random r, int w, float cy, int bh, float t, float a, boolean flood) {
            int n = flood ? 26 : 14;
            float edge = cy + bh / 2f - 4;
            for (int i = 0; i < n; i++) {
                float x = r.nextFloat() * w, period = 40 + r.nextFloat() * 40, ph = r.nextFloat() * period;
                float k = ((t + ph) % period) / period;
                int width = flood ? 3 : 2;
                if (k < 0.6f) {
                    // Stretching: a bead growing under the edge.
                    float len = 2 + 8 * (k / 0.6f);
                    g.fill((int) x, (int) edge, (int) x + width, (int) (edge + len), argb(a * 0.9f, GOO));
                    g.fill((int) x, (int) edge, (int) x + 1, (int) (edge + len - 1), argb(a * 0.35f, OIL));
                } else {
                    // Falling, fading.
                    float fall = (k - 0.6f) / 0.4f, y = edge + 10 + fall * fall * 40;
                    g.fill((int) x, (int) y, (int) x + width, (int) y + 3, argb(a * (1 - fall) * 0.9f, GOO));
                    g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, argb(a * (1 - fall) * 0.6f, OIL_PALE));
                }
            }
        }

        /** Borax-white flakes drifting down through the band (his fall). */
        private static void flakes(GuiGraphics g, Random r, int w, float cy, float t, float a) {
            for (int i = 0; i < 40; i++) {
                float x0 = r.nextFloat() * w, sp = 0.3f + r.nextFloat() * 0.5f, ph = r.nextFloat() * 60, life = 60;
                float k = ((t * sp + ph) % life) / life;
                float y = cy - 25 + k * 50, x = x0 + 5 * Mth.sin(t * 0.06f + ph);
                g.fill((int) x, (int) y, (int) x + 1 + (i % 3 == 0 ? 1 : 0), (int) y + 1, argb(a * Mth.sin(Mth.PI * k) * 0.85f, i % 4 == 0 ? ASH : BORAX));
            }
        }

        static int lerpRgb(int a, int b, float k) {
            int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
            return Math.round(ar + ((b >> 16 & 0xFF) - ar) * k) << 16 | Math.round(ag + ((b >> 8 & 0xFF) - ag) * k) << 8
                    | Math.round(ab + ((b & 0xFF) - ab) * k);
        }
    }
}
