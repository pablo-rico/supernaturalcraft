package org.papiricoh.supernaturalcraft.client.eve;

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
 * Eve's title cards (v0.39): each phase's card and her fall's, over her camera shots (the layer is let through camera sequences,
 * {@code CameraDirector.showDuringSequences}; it draws no game HUD of its own). A band of the glade's dusk green, roots crawling in
 * from both edges toward the title and a few roaches running along its rules; amber in the last phase; her fall's, the roots
 * withering grey and ash drifting down.
 */
public final class EveOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("eve");
    static final int DUSK = 0x10160C, DUSK_LIGHT = 0x1E2814, ROOT = 0x4E3A26, ROOT_LIGHT = 0x7A5E3E, MOSS = 0x9AB86A, AMBER = 0xF2A23A,
            PALE = 0xEDE4C8, ASH = 0xA8A49A, ROACH = 0x3A2412;

    private static TitleCard card;

    private EveOverlay() {
    }

    public static EveOverlay layer() {
        return new EveOverlay();
    }

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
        if (Minecraft.getInstance().player == null) return;
        TitleCard c = card;
        if (c != null) c.render(g, delta.getGameTimeDeltaPartialTick(false), g.guiWidth(), g.guiHeight());
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
            String key = fall ? "title.supernaturalcraft.eve.death" : "title.supernaturalcraft.eve.phase" + this.phase;
            title = Component.translatable(key);
            sub = Component.translatable(key + ".sub");
        }

        private float fade(float t, int in, int out) {
            return Mth.clamp(Math.min(t / in, (life - t) / out), 0, 1);
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial, a = fade(t, 8, 25);
            if (a <= 0.01f) return;
            boolean last = !fall && phase >= 4;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 100);
            int bh = 54;
            float wither = fall ? Mth.clamp((t - 10) / 50f, 0, 1) : 0;
            for (int i = 0; i < bh; i += 2) {
                float k = 1 - Math.abs(i - bh / 2f) / (bh / 2f);
                g.fill(0, (int) (cy - bh / 2f + i), w, (int) (cy - bh / 2f + i + 2), argb(a * 0.78f * k, i % 4 == 0 ? DUSK : DUSK_LIGHT));
            }
            Random r = new Random(seed);
            int rootRgb = fall ? lerpRgb(ROOT_LIGHT, ASH, wither) : last ? lerpRgb(ROOT_LIGHT, AMBER, 0.5f + 0.5f * Mth.sin(t * 0.2f)) : ROOT_LIGHT;
            // Roots crawl in from both edges along the band, toward the title.
            float grow = Mth.clamp(t / 30f, 0, 1) * (1 - wither * 0.6f);
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 5; k++) {
                    float sy = cy - bh / 2f + 6 + k * (bh - 12) / 4f;
                    float sx = side < 0 ? 0 : w;
                    EveHud.root(g, r, sx, sy, -side * 1.0f, (r.nextFloat() - 0.5f) * 0.3f, 30 + r.nextInt(30), grow, t, rootRgb, 2.0f);
                }
            }
            // Roaches running along the band's rules.
            if (!fall) {
                for (int i = 0; i < (last ? 14 : 8); i++) {
                    float sp = 0.8f + r.nextFloat(), x = (r.nextFloat() * w + t * sp * 2) % w;
                    float y = r.nextBoolean() ? cy - bh / 2f + 2 : cy + bh / 2f - 4;
                    g.fill((int) x, (int) y, (int) x + 3, (int) y + 2, argb(a * 0.9f, ROACH));
                }
            } else {
                for (int i = 0; i < 40; i++) {
                    float x0 = r.nextFloat() * w, sp = 0.3f + r.nextFloat() * 0.5f, ph = r.nextFloat() * 60;
                    float k = ((t * sp + ph) % 60) / 60f;
                    float y = cy - 25 + k * 50, x = x0 + 5 * Mth.sin(t * 0.06f + ph);
                    g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, argb(a * wither * Mth.sin(Mth.PI * k), ASH));
                }
            }
            float numY = cy - 38;
            if (!fall) {
                int numRgb = last ? AMBER : MOSS;
                GuiDraw.centred(g, Component.literal(roman(phase)), cx, numY, 2.6f, argb(a, numRgb), true);
                float gr = Mth.clamp((t - 2) / 12f, 0, 1);
                GuiDraw.line(g, cx - 18 - 40 * gr, numY + 9, cx - 18, numY + 9, 1f, argb(a * 0.8f, ROOT_LIGHT));
                GuiDraw.line(g, cx + 18, numY + 9, cx + 18 + 40 * gr, numY + 9, 1f, argb(a * 0.8f, ROOT_LIGHT));
            }
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            int titleRgb = fall ? lerpRgb(PALE, ASH, wither) : last ? 0xFFE2A8 : PALE;
            GuiDraw.centred(g, title, cx, cy - 10, scale, argb(a, titleRgb), true);
            float fy = cy - 10 + 9 * scale + 3, rule = Mth.clamp((t - 6) / 14f, 0, 1);
            GuiDraw.line(g, cx - 100 * rule, fy + 3, cx + 100 * rule, fy + 3, 1.2f, argb(a * 0.8f, last ? AMBER : ROOT_LIGHT));
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, fy + 10, ss, argb(sa, fall ? ASH : 0xB8C89A), true);
        }

        static int lerpRgb(int a, int b, float k) {
            int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
            return Math.round(ar + ((b >> 16 & 0xFF) - ar) * k) << 16 | Math.round(ag + ((b >> 8 & 0xFF) - ag) * k) << 8
                    | Math.round(ab + ((b & 0xFF) - ab) * k);
        }
    }
}
