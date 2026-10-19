package org.papiricoh.supernaturalcraft.client.witchboss;

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
 * The witch bosses' title cards (v0.21, P6), over their camera shots (the layer is let through camera sequences,
 * {@code CameraDirector.showDuringSequences}; it draws no game HUD of its own).
 * <ul>
 *   <li><b>Rowena</b>: a band of emerald velvet (its nap catching the light in slow vertical sheens) between two gold filigree
 *   rules that unfurl from the middle and curl at their ends; gold motes drift up through it. Her recitation's card turns violet,
 *   letters of the spell rising through the band; her fall's is dark green, the gold tarnished, the letters falling as ash.</li>
 *   <li><b>The Grand Witch</b>: a band of plum with silver rules, the staff's crystal cut above the title (three silver threads
 *   converge into it while her coven shields her; frost and embers flank it as she hexes; red threads, one per taglock, hang
 *   from it in the last phase); her fall's crystal cracks and its shards drift away.</li>
 * </ul>
 */
public final class WitchBossOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("witch_boss");

    static final int EMERALD_DEEP = 0x07170E, EMERALD = 0x0F3A22, EMERALD_LIGHT = 0x1E6B40, GOLD = 0xE2B44A, GOLD_PALE = 0xFFE9B0,
            GOLD_DIM = 0x8A6A2A, RECITE = 0x9B4FD8, RECITE_PALE = 0xE6CCFF, PLUM_DEEP = 0x14061A, PLUM = 0x3A1446, PLUM_LIGHT = 0x6A2E7E,
            SILVER = 0xD8DCE6, SILVER_DIM = 0x8C90A0, CRYSTAL = 0xC8A8FF, FROST = 0xBFE8FF, EMBER = 0xFF7A3A, TAGLOCK = 0xD0203A;

    public enum Who {ROWENA, GRAND_WITCH}

    private static TitleCard card;

    /** Shows a card (a new one replaces the last). */
    public static void title(Who who, int phase, boolean fall, int duration) {
        card = new TitleCard(who, phase, fall, duration);
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

    static int lerpRgb(int a, int b, float k) {
        k = Mth.clamp(k, 0, 1);
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        return Math.round(ar + ((b >> 16 & 0xFF) - ar) * k) << 16 | Math.round(ag + ((b >> 8 & 0xFF) - ag) * k) << 8
                | Math.round(ab + ((b & 0xFF) - ab) * k);
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "";
        };
    }

    /** A faceted gem (the staff's crystal) centred on (cx, cy), {@code h} tall; {@code crack} 0-1 splits it. */
    static void crystal(GuiGraphics g, float cx, float cy, float h, float alpha, float glow, float crack) {
        float w = h * 0.42f, top = cy - h / 2, bot = cy + h / 2, mid = cy - h * 0.12f;
        // Halo.
        for (int i = 3; i >= 1; i--) {
            float r = h * (0.35f + i * 0.22f);
            g.fill((int) (cx - r), (int) (cy - r * 0.6f), (int) (cx + r), (int) (cy + r * 0.6f), argb(alpha * glow * 0.06f, CRYSTAL));
        }
        float split = crack * 3.5f;
        // The facets: left and right halves (a crack pushes them apart).
        int[] cols = {0x9C7CE0, 0xE2D2FF, 0x6A4AB8, 0xC8A8FF};
        float[][] tri = {
                {cx, top, cx - w, mid, cx, mid}, {cx, top, cx + w, mid, cx, mid},
                {cx - w, mid, cx, bot, cx, mid}, {cx + w, mid, cx, bot, cx, mid}};
        for (int i = 0; i < 4; i++) {
            float[] t = tri[i];
            float off = (i % 2 == 0 ? -split : split);
            fillTri(g, t[0] + off, t[1] + (i > 1 ? split * 0.4f : 0), t[2] + off, t[3], t[4] + off, t[5], argb(alpha, cols[i]));
        }
        // Outline.
        int edge = argb(alpha, SILVER);
        GuiDraw.line(g, cx - split, top, cx - w - split, mid, 1f, edge);
        GuiDraw.line(g, cx + split, top, cx + w + split, mid, 1f, edge);
        GuiDraw.line(g, cx - w - split, mid, cx - split, bot, 1f, edge);
        GuiDraw.line(g, cx + w + split, mid, cx + split, bot, 1f, edge);
        if (crack > 0.05f) {
            // The crack itself: a jagged white line down its middle.
            float j = h / 6;
            GuiDraw.line(g, cx, top, cx + j * 0.4f, top + j, 0.8f, argb(alpha * crack, 0xFFFFFF));
            GuiDraw.line(g, cx + j * 0.4f, top + j, cx - j * 0.5f, top + 2.5f * j, 0.8f, argb(alpha * crack, 0xFFFFFF));
            GuiDraw.line(g, cx - j * 0.5f, top + 2.5f * j, cx + j * 0.3f, top + 4 * j, 0.8f, argb(alpha * crack, 0xFFFFFF));
            GuiDraw.line(g, cx + j * 0.3f, top + 4 * j, cx, bot, 0.8f, argb(alpha * crack, 0xFFFFFF));
        } else {
            // A glint travelling across it.
            g.fill((int) (cx - w * 0.35f), (int) (top + h * 0.22f), (int) (cx - w * 0.15f), (int) (top + h * 0.28f), argb(alpha * glow, 0xFFFFFF));
        }
    }

    /** A filled triangle (two windings of a degenerate quad, so it never culls). */
    static void fillTri(GuiGraphics g, float x0, float y0, float x1, float y1, float x2, float y2, int argb) {
        var vc = g.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        var m = g.pose().last().pose();
        vc.addVertex(m, x0, y0, 0).setColor(argb);
        vc.addVertex(m, x1, y1, 0).setColor(argb);
        vc.addVertex(m, x2, y2, 0).setColor(argb);
        vc.addVertex(m, x2, y2, 0).setColor(argb);
        vc.addVertex(m, x2, y2, 0).setColor(argb);
        vc.addVertex(m, x1, y1, 0).setColor(argb);
        vc.addVertex(m, x0, y0, 0).setColor(argb);
        vc.addVertex(m, x0, y0, 0).setColor(argb);
        g.flush();
    }

    /** One card. */
    static final class TitleCard {
        final Who who;
        final int phase, life;
        final boolean fall;
        final Component title, sub;
        final long seed = new Random().nextLong();
        int age;

        TitleCard(Who who, int phase, boolean fall, int duration) {
            this.who = who;
            this.phase = Mth.clamp(phase, 1, 3);
            this.fall = fall;
            this.life = Mth.clamp(duration, 70, 150);
            String base = who == Who.ROWENA ? "title.supernaturalcraft.rowena." : "title.supernaturalcraft.grand_witch.";
            String key = fall ? base + "death" : base + "phase" + this.phase;
            title = Component.translatable(key);
            sub = Component.translatable(key + ".sub");
        }

        private float fade(float t, int in, int out) {
            return Mth.clamp(Math.min(t / in, (life - t) / out), 0, 1);
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial, a = fade(t, 8, 25);
            if (a <= 0.01f) return;
            if (who == Who.ROWENA) rowena(g, t, a, w, h);
            else grandWitch(g, t, a, w, h);
        }

        // --- Rowena -------------------------------------------------------------------------------------------------------

        private void rowena(GuiGraphics g, float t, float a, int w, int h) {
            boolean recite = !fall && phase == 3;
            int deep = fall ? 0x050B07 : recite ? 0x12061E : EMERALD_DEEP, body = fall ? 0x0B1A10 : recite ? 0x2A0E40 : EMERALD,
                    sheen = fall ? 0x1A2A1E : recite ? 0x5A2A88 : EMERALD_LIGHT;
            int gold = fall ? GOLD_DIM : GOLD, pale = fall ? 0xC8B88A : recite ? RECITE_PALE : GOLD_PALE;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 100);
            int bh = 54;
            float top = cy - bh / 2f;
            // The velvet: dark at its edges, its nap catching slow vertical sheens.
            for (int i = 0; i < bh; i += 2) {
                float k = 1 - Math.abs(i - bh / 2f) / (bh / 2f);
                g.fill(0, (int) (top + i), w, (int) (top + i + 2), argb(a * 0.72f * (0.35f + 0.65f * k), lerpRgb(deep, body, k)));
            }
            for (int x = 0; x < w; x += 3) {
                float s = 0.5f + 0.5f * Mth.sin(x * 0.045f + t * 0.03f) * Mth.sin(x * 0.013f - t * 0.02f + (seed & 7));
                if (s < 0.6f) continue;
                g.fillGradient(x, (int) top + 4, x + 3, (int) (top + bh) - 4, argb(a * (s - 0.6f) * 0.35f, sheen), argb(0, sheen));
            }
            // Gold filigree rules above and below, unfurling from the middle.
            float grow = Mth.clamp((t - 2) / 18f, 0, 1);
            filigree(g, cx, top + 2, w * 0.42f * grow, t, a, gold, false);
            filigree(g, cx, top + bh - 2, w * 0.42f * grow, t, a, gold, true);
            Random r = new Random(seed);
            if (recite) letters(g, r, w, cy, bh, t, a, false);
            else if (fall) letters(g, r, w, cy, bh, t, a, true);
            else motes(g, r, w, cy, bh, t, a, gold);
            // The numeral flanked by two little gold diamonds.
            float numY = cy - 40;
            if (!fall) {
                GuiDraw.centred(g, Component.literal(roman(phase)), cx, numY, 2.4f, argb(a, pale), true);
                for (int s = -1; s <= 1; s += 2) {
                    float dx = cx + s * 22, dy = numY + 9;
                    fillTri(g, dx, dy - 3, dx - 3, dy, dx + 3, dy, argb(a, gold));
                    fillTri(g, dx - 3, dy, dx, dy + 3, dx + 3, dy, argb(a, gold));
                }
            }
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            GuiDraw.centred(g, title, cx, cy - 12, scale, argb(a, fall ? 0xD8E8D0 : 0xF4EED8), true);
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, cy - 12 + 9 * scale + 6, ss, argb(sa, recite ? RECITE_PALE : fall ? 0xA8B8A0 : GOLD), true);
        }

        /** A gold rule with a little curl at each end and a lozenge at its middle; {@code under} flips the curls. */
        private void filigree(GuiGraphics g, float cx, float y, float half, float t, float a, int gold, boolean under) {
            if (half < 2) return;
            int c = argb(a * 0.9f, gold);
            GuiDraw.line(g, cx - half, y, cx + half, y, 1.1f, c);
            GuiDraw.line(g, cx - half * 0.7f, y + (under ? -2 : 2), cx + half * 0.7f, y + (under ? -2 : 2), 0.6f, argb(a * 0.45f, gold));
            float dir = under ? -1 : 1;
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * half;
                // A curl: a few segments round a small circle.
                float px = ex, py = y;
                for (int i = 1; i <= 7; i++) {
                    float ang = i / 7f * Mth.PI * 1.4f;
                    float qx = ex + s * (Mth.sin(ang) * 4.5f), qy = y + dir * (1 - Mth.cos(ang)) * 4.5f;
                    GuiDraw.line(g, px, py, qx, qy, 0.9f, c);
                    px = qx;
                    py = qy;
                }
                // Little leaves along the rule.
                for (float f = 0.25f; f < 0.95f; f += 0.22f) {
                    float lx = cx + s * half * f, ly = y + dir * 2.5f;
                    GuiDraw.line(g, lx, y, lx + s * 2.5f, ly, 0.7f, argb(a * 0.6f, gold));
                }
            }
            float pulse = 0.75f + 0.25f * Mth.sin(t * 0.15f);
            fillTri(g, cx, y - 3, cx - 4, y, cx + 4, y, argb(a * pulse, gold));
            fillTri(g, cx - 4, y, cx, y + 3, cx + 4, y, argb(a * pulse, gold));
        }

        /** Gold motes drifting up through the velvet. */
        private void motes(GuiGraphics g, Random r, int w, float cy, int bh, float t, float a, int gold) {
            for (int i = 0; i < 46; i++) {
                float x0 = r.nextFloat() * w, sp = 0.2f + r.nextFloat() * 0.45f, ph = r.nextFloat() * 80, life = 80;
                float k = ((t * sp + ph) % life) / life;
                float y = cy + bh / 2f - 4 - k * (bh - 8), x = x0 + 5 * Mth.sin(t * 0.04f + ph);
                float ea = a * Mth.sin(Mth.PI * k) * 0.85f;
                g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, argb(ea, i % 4 == 0 ? 0xFFF4D0 : gold));
            }
        }

        /** Letters of her spell rising (or, on her fall, sinking like ash) through the band. */
        private void letters(GuiGraphics g, Random r, int w, float cy, int bh, float t, float a, boolean ash) {
            String abc = "abcdefghiklmnopqrstuvxyz";
            for (int i = 0; i < 26; i++) {
                float x0 = r.nextFloat() * w, sp = 0.15f + r.nextFloat() * 0.3f, ph = r.nextFloat() * 90, life = 90;
                char ch = abc.charAt(r.nextInt(abc.length()));
                float k = ((t * sp + ph) % life) / life;
                float y = ash ? cy - bh / 2f + 2 + k * (bh - 10) : cy + bh / 2f - 10 - k * (bh - 10);
                float x = x0 + 4 * Mth.sin(t * 0.05f + ph);
                float ea = a * Mth.sin(Mth.PI * k) * (ash ? 0.5f : 0.7f);
                GuiDraw.text(g, Component.literal(String.valueOf(ch)).withStyle(s -> s.withItalic(true)), x, y, 0.8f,
                        argb(ea, ash ? 0x8A9A88 : RECITE_PALE), false);
            }
        }

        // --- the Grand Witch ----------------------------------------------------------------------------------------------

        private void grandWitch(GuiGraphics g, float t, float a, int w, int h) {
            float cx = w / 2f, cy = Math.max(h * 0.3f, 100);
            int bh = 56;
            float top = cy - bh / 2f;
            int deep = fall ? 0x0A060C : PLUM_DEEP, body = fall ? 0x1E1422 : PLUM, light = fall ? 0x3A3040 : PLUM_LIGHT;
            for (int i = 0; i < bh; i += 2) {
                float k = 1 - Math.abs(i - bh / 2f) / (bh / 2f);
                g.fill(0, (int) (top + i), w, (int) (top + i + 2), argb(a * 0.7f * (0.35f + 0.65f * k), lerpRgb(deep, i % 4 == 0 ? body : light, k * 0.7f)));
            }
            // Silver rules: double lines with small studs.
            float grow = Mth.clamp((t - 2) / 16f, 0, 1), half = w * 0.44f * grow;
            for (int e = 0; e < 2; e++) {
                float y = e == 0 ? top + 2 : top + bh - 2;
                GuiDraw.line(g, cx - half, y, cx + half, y, 1f, argb(a * 0.9f, SILVER));
                GuiDraw.line(g, cx - half * 0.8f, y + (e == 0 ? 2 : -2), cx + half * 0.8f, y + (e == 0 ? 2 : -2), 0.5f, argb(a * 0.5f, SILVER_DIM));
                for (float f = -1; f <= 1.01f; f += 0.125f) {
                    float sx = cx + f * half;
                    g.fill((int) sx - 1, (int) y - 1, (int) sx + 1, (int) y + 1, argb(a, SILVER));
                }
            }
            Random r = new Random(seed);
            // Silver dust drifting across the band.
            for (int i = 0; i < 40; i++) {
                float x0 = r.nextFloat() * w, y0 = cy + (r.nextFloat() - 0.5f) * (bh - 8), sp = 0.15f + r.nextFloat() * 0.35f;
                float x = (x0 + t * sp) % w, tw = 0.5f + 0.5f * Mth.sin(t * 0.2f + i);
                g.fill((int) x, (int) y0, (int) x + 1, (int) y0 + 1, argb(a * 0.6f * tw, fall ? 0x8C8090 : SILVER));
            }
            float gemY = cy - 42, gemH = 20;
            if (!fall) {
                switch (phase) {
                    case 1 -> {
                        // Three threads converging into the crystal (her coven's channel).
                        for (int i = 0; i < 3; i++) {
                            float ang = Mth.PI * (0.15f + 0.35f * i), len = 70;
                            float sx = cx + Mth.cos(ang) * len * (i == 1 ? 0 : (i == 0 ? 1 : -1)) * 1.4f, sy = gemY + (i == 1 ? -26 : 14);
                            if (i == 1) sx = cx;
                            float flow = (t * 0.05f + i * 0.33f) % 1f;
                            GuiDraw.line(g, sx, sy, cx, gemY, 1f, argb(a * 0.45f, CRYSTAL));
                            float fx = Mth.lerp(flow, sx, cx), fy = Mth.lerp(flow, sy, gemY);
                            g.fill((int) fx - 1, (int) fy - 1, (int) fx + 2, (int) fy + 2, argb(a, 0xFFFFFF));
                        }
                    }
                    case 2 -> {
                        // Frost on one side, embers on the other.
                        for (int i = 0; i < 24; i++) {
                            float k = ((t * (0.3f + r.nextFloat() * 0.4f) + r.nextFloat() * 50) % 50) / 50;
                            float fx = cx - 30 - r.nextFloat() * 60, fy = gemY + 12 - k * 30;
                            g.fill((int) fx, (int) fy, (int) fx + 1, (int) fy + 1, argb(a * Mth.sin(Mth.PI * k), FROST));
                            float ex = cx + 30 + r.nextFloat() * 60, ey = gemY + 12 - k * 34;
                            g.fill((int) ex, (int) ey, (int) ex + 1, (int) ey + 1, argb(a * Mth.sin(Mth.PI * k), i % 3 == 0 ? 0xFFD08A : EMBER));
                        }
                    }
                    default -> {
                        // Red threads hanging from the crystal, one per taglock, swaying.
                        for (int i = 0; i < 7; i++) {
                            float sx = cx + (i - 3) * 16, sway = 3 * Mth.sin(t * 0.07f + i);
                            float len = 14 + 8 * r.nextFloat();
                            GuiDraw.line(g, cx, gemY + gemH * 0.4f, sx + sway, gemY + gemH * 0.4f + len, 0.7f, argb(a * 0.7f, TAGLOCK));
                            g.fill((int) (sx + sway) - 1, (int) (gemY + gemH * 0.4f + len), (int) (sx + sway) + 2,
                                    (int) (gemY + gemH * 0.4f + len) + 3, argb(a, 0x5A2A1A));
                        }
                    }
                }
                GuiDraw.centred(g, Component.literal(roman(phase)), cx - 34, gemY - 6, 2.0f, argb(a, SILVER), true);
                GuiDraw.centred(g, Component.literal(roman(phase)), cx + 34, gemY - 6, 2.0f, argb(a, SILVER), true);
                crystal(g, cx, gemY, gemH, a, 0.6f + 0.4f * Mth.sin(t * 0.2f), 0);
            } else {
                float crack = Mth.clamp((t - 10) / 30f, 0, 1);
                crystal(g, cx, gemY, gemH, a * (1 - crack * 0.4f), 0.3f, crack);
                // Its shards drifting off.
                for (int i = 0; i < 10; i++) {
                    float k = crack, ang = r.nextFloat() * Mth.TWO_PI, d = k * (20 + r.nextFloat() * 40);
                    float sx = cx + Mth.cos(ang) * d, sy = gemY + Mth.sin(ang) * d * 0.6f + k * k * 10;
                    g.fill((int) sx, (int) sy, (int) sx + 2, (int) sy + 1, argb(a * (1 - k) * 0.9f, CRYSTAL));
                }
            }
            float scale = Math.min(2.4f, (w - 40) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            GuiDraw.centred(g, title, cx, cy - 12, scale, argb(a, fall ? 0xCFC6D6 : 0xF2EEF8), true);
            float sa = a * Mth.clamp((t - 14) / 10f, 0, 1);
            Component italic = sub.copy().withStyle(s -> s.withItalic(true));
            float ss = Math.min(1.15f, (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
            GuiDraw.centred(g, italic, cx, cy - 12 + 9 * scale + 6, ss, argb(sa, fall ? 0x9A90A0 : 0xC8B8D8), true);
        }
    }
}
