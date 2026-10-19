package org.papiricoh.supernaturalcraft.client.abaddon;

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
 * Abaddon's title cards (v0.21): each phase's card and her fall's, over her camera shots (the layer is let through camera sequences,
 * {@code CameraDirector.showDuringSequences}; it draws no game HUD of its own). A band of smoke-black between two black-iron chains
 * running in from the edges; black smoke drifting through it. The Knight of Hell's is lit by hellfire embers; Possession's by
 * crimson glints in the smoke, which thickens and coils; the Brand's has the seal burning itself in either side of the numeral.
 * Her fall's: the seal cracking and cooling to ash, the chains slack, ash falling, the title in pale gold.
 */
public final class AbaddonOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("abaddon");
    static final int SMOKE = 0x0A0607, SMOKE_LIGHT = 0x1E1012, IRON = 0x2A2628, IRON_LIGHT = 0x5A5458, CRIMSON = 0xB0101A,
            CRIMSON_HOT = 0xFF3A2A, HELLFIRE = 0xFF5A1E, HELL_HOT = 0xFFD08A, ASH = 0xB8AEA6, PALE_GOLD = 0xF2DDA8, VIOLET = 0x8A3A9A;

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

    static int lerpRgb(int a, int b, float k) {
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        return Math.round(ar + ((b >> 16 & 0xFF) - ar) * k) << 16 | Math.round(ag + ((b >> 8 & 0xFF) - ag) * k) << 8
                | Math.round(ab + ((b & 0xFF) - ab) * k);
    }

    /**
     * A chain of black iron links along y from x0 to x1 (drawn in from the side {@code fromLeft} over {@code k} 0-1), sagging by
     * {@code sag} pixels in the middle: oval links seen side-on and edge-on, alternately.
     */
    static void chain(GuiGraphics g, float x0, float x1, float y, float sag, float k, float alpha, boolean fromLeft) {
        float link = 7;
        int n = (int) ((x1 - x0) / link);
        for (int i = 0; i < n; i++) {
            float u = (i + 0.5f) / n;
            if (fromLeft ? u > k : 1 - u > k) continue;
            float x = x0 + (i + 0.5f) * link, yy = y + sag * 4 * u * (1 - u);
            if (i % 2 == 0) {
                // Side-on: an open oval.
                g.fill((int) (x - 4), (int) yy - 2, (int) (x + 4), (int) yy - 1, argb(alpha, IRON_LIGHT));
                g.fill((int) (x - 4), (int) yy + 1, (int) (x + 4), (int) yy + 2, argb(alpha, IRON));
                g.fill((int) (x - 5), (int) yy - 1, (int) (x - 4), (int) yy + 1, argb(alpha, IRON));
                g.fill((int) (x + 4), (int) yy - 1, (int) (x + 5), (int) yy + 1, argb(alpha, IRON));
            } else {
                // Edge-on: a bar.
                g.fill((int) (x - 4), (int) yy - 1, (int) (x + 4), (int) yy + 1, argb(alpha, IRON));
                g.fill((int) (x - 3), (int) yy - 1, (int) (x + 3), (int) yy, argb(alpha * 0.8f, IRON_LIGHT));
            }
        }
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
            String key = fall ? "title.supernaturalcraft.abaddon.death" : "title.supernaturalcraft.abaddon.phase" + this.phase;
            title = Component.translatable(key);
            sub = Component.translatable(key + ".sub");
        }

        private float fade(float t, int in, int out) {
            return Mth.clamp(Math.min(t / in, (life - t) / out), 0, 1);
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial, a = fade(t, 8, 25);
            if (a <= 0.01f) return;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 100);
            int bh = 54;
            int bandLight = fall ? 0x1A1210 : phase == 2 ? 0x1C0A16 : SMOKE_LIGHT;
            for (int i = 0; i < bh; i += 2) {
                float k = 1 - Math.abs(i - bh / 2f) / (bh / 2f);
                g.fill(0, (int) (cy - bh / 2f + i), w, (int) (cy - bh / 2f + i + 2), argb(a * 0.72f * k, i % 4 == 0 ? SMOKE : bandLight));
            }
            Random r = new Random(seed);
            smoke(g, r, w, cy, bh, t, a, phase == 2 && !fall);
            // The chains running in from either edge along the band's top and bottom (slack on her fall).
            float run = Mth.clamp((t - 2) / 16f, 0, 1);
            float sag = fall ? 7 + 4 * Mth.clamp((t - 20) / 30f, 0, 1) : 2;
            chain(g, 0, w / 2f, cy - bh / 2f + 2, sag, run, a, true);
            chain(g, w / 2f, w, cy - bh / 2f + 2, sag, run, a, false);
            chain(g, 0, w / 2f, cy + bh / 2f - 2, -sag * 0.5f, run, a, true);
            chain(g, w / 2f, w, cy + bh / 2f - 2, -sag * 0.5f, run, a, false);
            if (fall) ash(g, r, w, cy, t, a);
            else if (phase != 2) embers(g, r, w, cy, t, a);
            else glints(g, r, w, cy, t, a);
            // The numeral, flanked (in the Brand's phase, and on her fall alone) by the seal burning itself in.
            float numY = cy - 40;
            if (!fall) {
                int num = phase == 1 ? HELL_HOT : phase == 2 ? 0xF0C8D0 : HELL_HOT;
                GuiDraw.centred(g, Component.literal(roman(phase)), cx, numY, 2.6f, argb(a, num), true);
            }
            if (phase == 3 || fall) {
                float burn = Mth.clamp((t - 4) / 26f, 0, 1), flick = 0.8f + 0.2f * Mth.sin(t * 0.9f);
                float cool = fall ? Mth.clamp((t - 30) / 40f, 0, 1) : 0;
                int hot = lerpRgb(HELLFIRE, ASH, cool), under = lerpRgb(CRIMSON, 0x302A28, cool);
                float size = fall ? 26 : 18, sy = numY + 9;
                if (fall) {
                    KnightsSeal.gui(g, cx, sy, size, burn, 1.3f, argb(a * flick, hot), argb(a * 0.6f, under));
                    // Cracking through as it cools.
                    if (cool > 0) {
                        Random cr = new Random(seed ^ 77);
                        for (int i = 0; i < 4; i++) {
                            float ang = cr.nextFloat() * Mth.TWO_PI, len = size * 0.6f * cool;
                            GuiDraw.line(g, cx, sy, cx + Mth.cos(ang) * len, sy + Mth.sin(ang) * len, 1f, argb(a * 0.9f, 0x0A0606));
                        }
                    }
                } else {
                    KnightsSeal.gui(g, cx - 30, sy, size, burn, 1.2f, argb(a * flick, hot), argb(a * 0.55f, under));
                    KnightsSeal.gui(g, cx + 30, sy, size, burn, 1.2f, argb(a * flick, hot), argb(a * 0.55f, under));
                }
            }
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            int titleRgb = fall ? PALE_GOLD : phase == 2 ? 0xF2D6DC : 0xFFE6DC;
            GuiDraw.centred(g, title, cx, cy - 10, scale, argb(a, titleRgb), true);
            float fy = cy - 10 + 9 * scale + 3, grow = Mth.clamp((t - 6) / 14f, 0, 1);
            int rule = fall ? ASH : phase == 2 ? CRIMSON : HELLFIRE;
            GuiDraw.line(g, cx - 100 * grow, fy + 3, cx + 100 * grow, fy + 3, 1.2f, argb(a * 0.8f, rule));
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, fy + 10, ss, argb(sa, fall ? ASH : phase == 2 ? 0xD8A8B4 : 0xE8B0A0), true);
        }

        /** Black smoke drifting through the band (thicker, coiling, in Possession). */
        private static void smoke(GuiGraphics g, Random r, int w, float cy, int bh, float t, float a, boolean thick) {
            int puffs = thick ? 26 : 16;
            for (int i = 0; i < puffs; i++) {
                float sp = 0.2f + r.nextFloat() * 0.5f, ph = r.nextFloat() * 400, y0 = cy + (r.nextFloat() - 0.5f) * bh * 0.8f;
                float x = ((r.nextFloat() * w + t * sp) % (w + 80)) - 40;
                float y = y0 + (thick ? 6 : 3) * Mth.sin(t * 0.04f + ph);
                int size = 10 + r.nextInt(thick ? 16 : 10);
                float k = 1 - Math.abs(y - cy) / (bh / 2f);
                if (k <= 0) continue;
                for (int s = size; s > 2; s -= 3) {
                    g.fill((int) (x - s), (int) (y - s / 2f), (int) (x + s), (int) (y + s / 2f), argb(a * 0.07f * k, 0x000000));
                }
            }
        }

        /** Hellfire embers rising through the band. */
        private static void embers(GuiGraphics g, Random r, int w, float cy, float t, float a) {
            for (int i = 0; i < 44; i++) {
                float x0 = r.nextFloat() * w, sp = 0.3f + r.nextFloat() * 0.6f, ph = r.nextFloat() * 60, life = 60;
                float k = ((t * sp + ph) % life) / life;
                float y = cy + 26 - k * 52, x = x0 + 6 * Mth.sin(t * 0.05f + ph);
                float ea = a * Mth.sin(Mth.PI * k) * 0.85f;
                int col = i % 3 == 0 ? HELL_HOT : i % 3 == 1 ? HELLFIRE : CRIMSON_HOT;
                g.fill((int) x, (int) y, (int) x + 1 + (i % 4 == 0 ? 1 : 0), (int) y + 1, argb(ea, col));
            }
        }

        /** Crimson glints winking in the smoke (her eyes, many of them). */
        private static void glints(GuiGraphics g, Random r, int w, float cy, float t, float a) {
            for (int i = 0; i < 18; i++) {
                float x = r.nextFloat() * w + 10 * Mth.sin(t * 0.03f + i), y = cy + (r.nextFloat() - 0.5f) * 40, ph = r.nextFloat() * 100;
                float on = Mth.clamp(Mth.sin(t * 0.08f + ph) * 2 - 1, 0, 1);
                if (on <= 0) continue;
                g.fill((int) x - 2, (int) y, (int) x - 1, (int) y + 1, argb(a * on, CRIMSON_HOT));
                g.fill((int) x + 1, (int) y, (int) x + 2, (int) y + 1, argb(a * on, CRIMSON_HOT));
                g.fill((int) x - 3, (int) y - 1, (int) x + 3, (int) y + 2, argb(a * on * 0.25f, CRIMSON));
            }
            embers(g, r, w, cy, t, a * 0.4f);
        }

        /** Ash drifting down. */
        private static void ash(GuiGraphics g, Random r, int w, float cy, float t, float a) {
            for (int i = 0; i < 40; i++) {
                float x0 = r.nextFloat() * w, sp = 0.2f + r.nextFloat() * 0.4f, ph = r.nextFloat() * 80, life = 80;
                float k = ((t * sp + ph) % life) / life;
                float y = cy - 26 + k * 52, x = x0 + 8 * Mth.sin(t * 0.04f + ph);
                g.fill((int) x, (int) y, (int) x + 1 + (i % 5 == 0 ? 1 : 0), (int) y + 1, argb(a * Mth.sin(Mth.PI * k) * 0.8f, ASH));
            }
        }
    }
}
