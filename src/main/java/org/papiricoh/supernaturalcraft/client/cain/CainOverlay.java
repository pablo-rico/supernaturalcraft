package org.papiricoh.supernaturalcraft.client.cain;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.mark.MarkGlyph;

import java.util.Random;

/**
 * Cain's title cards (v0.21): each phase's card and his fall's, over his camera shots (the layer is let through camera sequences,
 * {@code CameraDirector.showDuringSequences}; it draws no game HUD of its own). The Beekeeper's is a band of dusk honey with a
 * drifting honeycomb and a few bees crossing it; the Mark's and the Father of Murder's a band of blood with the Mark burning
 * itself in beside the numeral, embers rising; his fall's dark, the rune cooling to ash, the title in pale gold.
 */
public final class CainOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("cain");
    static final int DUSK = 0x2A1A0A, HONEY = 0xE8B54A, HONEY_PALE = 0xFFE7B0, BLOOD = 0x6A0A0C, BLOOD_DEEP = 0x1E0406,
            EMBER = 0xFF5A2A, MARK_HOT = 0xFFC27A, ASH = 0xC9B8A4, PALE_GOLD = 0xF2DDA8;

    private static TitleCard card;

    /** Shows a card (a new one replaces the last). */
    public static void title(int phase, boolean fall, int duration) {
        card = new TitleCard(phase, fall, duration);
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
    }

    static void clear() {
        card = null;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        TitleCard c = card;
        if (mc.player == null || c == null) return;
        c.render(g, delta.getGameTimeDeltaPartialTick(false), g.guiWidth(), g.guiHeight());
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

    /** One card. */
    static final class TitleCard {
        final int phase, life;
        final boolean fall;
        final Component title, sub;
        final long seed = new Random().nextLong();
        int age;

        TitleCard(int phase, boolean fall, int duration) {
            this.phase = Mth.clamp(phase, 1, 3);
            this.fall = fall;
            this.life = Mth.clamp(duration, 70, 150);
            String key = fall ? "title.supernaturalcraft.cain.death" : "title.supernaturalcraft.cain.phase" + this.phase;
            title = Component.translatable(key);
            sub = Component.translatable(key + ".sub");
        }

        private float fade(float t, int in, int out) {
            return Mth.clamp(Math.min(t / in, (life - t) / out), 0, 1);
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial, a = fade(t, 8, 25);
            if (a <= 0.01f) return;
            boolean honey = !fall && phase == 1;
            int band = honey ? DUSK : fall ? 0x120806 : BLOOD_DEEP, bandLight = honey ? 0x4A3010 : fall ? 0x2A1A14 : BLOOD;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 100);
            int bh = 50;
            for (int i = 0; i < bh; i += 2) {
                float k = 1 - Math.abs(i - bh / 2f) / (bh / 2f);
                g.fill(0, (int) (cy - bh / 2f + i), w, (int) (cy - bh / 2f + i + 2), argb(a * 0.66f * k, i % 4 == 0 ? band : bandLight));
            }
            Random r = new Random(seed);
            if (honey) {
                comb(g, w, cy, bh, t, a);
                bees(g, r, w, cy, t, a);
            } else {
                embers(g, r, w, cy, t, a, fall);
            }
            // The numeral, and beside it (once the Mark has him) the rune burning itself in; on his fall, the rune alone, cooling.
            float numY = cy - 36;
            if (!fall) {
                GuiDraw.centred(g, Component.literal(roman(phase)), cx, numY, 2.6f, argb(a, honey ? HONEY_PALE : MARK_HOT), true);
            }
            if (!honey) {
                float burn = Mth.clamp((t - 4) / 26f, 0, 1);
                float rh = fall ? 26 : 20, rx = fall ? cx - rh * MarkGlyph.ASPECT / 2 : cx + 20, ry = fall ? numY - 6 : numY - 2;
                float flick = 0.8f + 0.2f * Mth.sin(t * 0.9f);
                int hot = fall ? lerpRgb(MARK_HOT, ASH, Mth.clamp((t - 30) / 40f, 0, 1)) : MARK_HOT;
                MarkGlyph.gui(g, rx, ry, rh, burn, 1.2f, argb(a * flick, hot), argb(a * 0.55f, EMBER));
                if (!fall) MarkGlyph.gui(g, cx - 20 - rh * MarkGlyph.ASPECT, ry, rh, burn, 1.2f, argb(a * flick, hot), argb(a * 0.55f, EMBER));
            }
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            int titleRgb = honey ? HONEY_PALE : fall ? PALE_GOLD : 0xFFE6DC;
            GuiDraw.centred(g, title, cx, cy - 10, scale, argb(a, titleRgb), true);
            float fy = cy - 10 + 9 * scale + 3, grow = Mth.clamp((t - 6) / 14f, 0, 1);
            int rule = honey ? HONEY : fall ? ASH : EMBER;
            GuiDraw.line(g, cx - 100 * grow, fy + 3, cx + 100 * grow, fy + 3, 1.2f, argb(a * 0.8f, rule));
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, fy + 10, ss, argb(sa, honey ? HONEY : fall ? ASH : 0xE8B0A0), true);
        }

        /** A honeycomb drifting slowly across the band, fading at its edges. */
        private static void comb(GuiGraphics g, int w, float cy, int bh, float t, float a) {
            float size = 9, hx = size * 1.732f, drift = (t * 0.25f) % hx;
            for (int row = -3; row <= 3; row++) {
                float y = cy + row * size * 1.5f;
                float k = 1 - Math.abs(y - cy) / (bh / 2f);
                if (k <= 0) continue;
                float off = (row & 1) == 0 ? 0 : hx / 2;
                for (float x = -hx + off - drift; x < w + hx; x += hx) {
                    float edge = Math.min(1, Math.min(x, w - x) / (w * 0.25f));
                    if (edge <= 0) continue;
                    int col = argb(a * 0.22f * k * edge, HONEY);
                    for (int s = 0; s < 6; s++) {
                        float a0 = Mth.PI / 6 + s * Mth.PI / 3, a1 = a0 + Mth.PI / 3;
                        GuiDraw.line(g, x + Mth.cos(a0) * size, y + Mth.sin(a0) * size, x + Mth.cos(a1) * size, y + Mth.sin(a1) * size, 1f, col);
                    }
                }
            }
        }

        /** A few bees crossing the band, each a dark dot with a golden glint and wing flicker. */
        private static void bees(GuiGraphics g, Random r, int w, float cy, float t, float a) {
            for (int i = 0; i < 14; i++) {
                float speed = 0.8f + r.nextFloat() * 1.4f, y0 = cy + (r.nextFloat() - 0.5f) * 44, ph = r.nextFloat() * 100;
                float x = ((r.nextFloat() * w + t * speed * (r.nextBoolean() ? 1 : -1)) % (w + 40) + w + 40) % (w + 40) - 20;
                float y = y0 + 4 * Mth.sin(t * 0.3f + ph) + 1.2f * Mth.sin(t * 2.1f + ph);
                g.fill((int) x - 1, (int) y - 1, (int) x + 2, (int) y + 1, argb(a * 0.9f, 0x1A1206));
                g.fill((int) x, (int) y - 1, (int) x + 1, (int) y, argb(a * 0.9f, HONEY));
                if (Mth.sin(t * 8 + ph) > 0) g.fill((int) x - 1, (int) y - 2, (int) x + 2, (int) y - 1, argb(a * 0.4f, 0xFFF4D8));
            }
        }

        /** Embers rising through the band (on his fall, ash drifting down). */
        private static void embers(GuiGraphics g, Random r, int w, float cy, float t, float a, boolean fall) {
            for (int i = 0; i < 40; i++) {
                float x0 = r.nextFloat() * w, sp = 0.3f + r.nextFloat() * 0.6f, ph = r.nextFloat() * 60, life = 60;
                float k = ((t * sp + ph) % life) / life;
                float y = fall ? cy - 25 + k * 50 : cy + 25 - k * 50, x = x0 + 6 * Mth.sin(t * 0.05f + ph);
                float ea = a * Mth.sin(Mth.PI * k) * 0.8f;
                int col = fall ? ASH : (i % 3 == 0 ? MARK_HOT : EMBER);
                g.fill((int) x, (int) y, (int) x + 1 + (i % 4 == 0 ? 1 : 0), (int) y + 1, argb(ea, col));
            }
        }

        private static int lerpRgb(int a, int b, float k) {
            int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
            return Math.round(ar + ((b >> 16 & 0xFF) - ar) * k) << 16 | Math.round(ag + ((b >> 8 & 0xFF) - ag) * k) << 8
                    | Math.round(ab + ((b & 0xFF) - ab) * k);
        }
    }
}
