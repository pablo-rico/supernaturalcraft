package org.papiricoh.supernaturalcraft.client.crowley;

import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.lucifer.Gothic;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;

import java.util.Random;

import static org.papiricoh.supernaturalcraft.client.crowley.CrowleyHud.SHEET;
import static org.papiricoh.supernaturalcraft.client.crowley.CrowleyHud.TH;
import static org.papiricoh.supernaturalcraft.client.crowley.CrowleyHud.TW;

/**
 * Crowley's title cards (v0.35), over his camera shots (the layer is let through camera sequences; it draws no game HUD): a
 * contract unrolling across the screen, the title in gothic ink on it, the phase's seal stamped beside it (I, II, III), red
 * smoke curling along its edges. His fall's: the seal cracks in two and the contract burns in from both ends, embers rising.
 * Keys: {@code title.supernaturalcraft.crowley.phase<n>} or {@code .fall}, each with {@code .sub} (shown only if it exists).
 */
public final class CrowleyOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("crowley");
    static final int RED_SMOKE = 0x6A0A0E, EMBER = 0xFF6A1A, EMBER_HOT = 0xFFE0A0;

    private static Card card;

    /** Shows a card (a new one replaces the last). */
    public static void title(int phase, boolean fall, int duration) {
        card = new Card(phase, fall, duration);
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
        Card c = card;
        if (Minecraft.getInstance().player == null || c == null || !MichaelGui.has(SHEET)) return;
        c.render(g, delta.getGameTimeDeltaPartialTick(false), g.guiWidth(), g.guiHeight());
    }

    static final class Card {
        final int phase, life;
        final boolean fall;
        final String title;
        final Component sub;
        final long seed = new Random().nextLong();
        int age;

        Card(int phase, boolean fall, int duration) {
            this.phase = Mth.clamp(phase, 1, 3);
            this.fall = fall;
            this.life = Mth.clamp(duration, 70, 160);
            String key = fall ? "title.supernaturalcraft.crowley.fall" : "title.supernaturalcraft.crowley.phase" + this.phase;
            title = I18n.get(key);
            sub = I18n.exists(key + ".sub") ? Component.translatable(key + ".sub") : null;
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial;
            float a = Mth.clamp(Math.min(t / 6f, (life - t) / 20f), 0, 1);
            if (a <= 0.01f) return;
            float cx = w / 2f, cy = Math.max(h * 0.3f, 90);
            // The contract unrolling from the middle: the parchment at twice its size.
            float scale = Math.min(2f, (w - 20) / 256f);
            float pw = 256 * scale, ph = 40 * scale;
            float open = Mth.clamp((t - 2) / 12f, 0, 1);
            open = 1 - (1 - open) * (1 - open);
            // Burning in from both ends on his fall.
            float burn = fall ? Mth.clamp((t - 30) / 60f, 0, 0.42f) : 0;
            float shownU = 256 * open * (1 - 2 * burn);
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(scale, scale, 1);
            MichaelGui.blit(g, SHEET, -shownU / 2, -20, Math.round(shownU), 40, 128 - shownU / 2, 0, Math.round(shownU), 40, TW, TH, a, 0xFFFFFF);
            if (fall && burn > 0) {
                // The charred, burning edges where the paper is eaten.
                for (int side = -1; side <= 1; side += 2) {
                    float ex = side * shownU / 2;
                    for (int i = 0; i < 2; i++) {
                        MichaelGui.blit(g, SHEET, ex - (side < 0 ? 0 : 12) + side * i * 3, -20 + i * 20, 12, 20,
                                ((int) (t / 2) + i * 3 + (side > 0 ? 4 : 0)) % 8 * 12, 64, 12, 20, TW, TH, a, 0xFFFFFF);
                    }
                }
            }
            g.pose().popPose();
            if (open < 1) return;
            float ta = a * Mth.clamp((t - 10) / 8f, 0, 1);
            float textScale = Math.min(2f, (pw - 90) / Math.max(1, Gothic.width(title, false)));
            Gothic.centred(g, title, cx, cy - ph * 0.3f, textScale, ta, CrowleyHud.INK, false);
            if (sub != null) {
                Component italic = sub.copy().withStyle(s -> s.withItalic(true));
                float ss = Math.min(1f, (pw - 60) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
                GuiDraw.centred(g, italic, cx, cy + ph * 0.18f, ss, MichaelGui.argb(ta, 0x5A1A12), false);
            }
            // The seal stamped at the right end: it lands (a squash), then (his fall) cracks and splits.
            float stamp = Mth.clamp((t - 16) / 5f, 0, 1);
            if (stamp > 0) {
                float sx = cx + pw * 0.5f * (1 - 2 * burn) - 30 * scale, sy = cy + ph * 0.5f - 4;
                float squash = 1 + 0.5f * (1 - stamp);
                int state = fall ? (t < 40 ? 1 : 2) : 0;
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().mulPose(Axis.ZP.rotationDegrees(-10));
                g.pose().scale(scale * 1.3f * squash, scale * 1.3f * squash, 1);
                MichaelGui.blit(g, SHEET, -12, -12, 24, 24, state * 24, 88, 24, 24, TW, TH, ta * stamp, 0xFFFFFF);
                g.pose().popPose();
                if (!fall) GuiDraw.centred(g, Component.literal(roman(phase)), sx, sy + 13 * scale, 1f, MichaelGui.argb(ta * stamp, 0xF2D2A0), true);
            }
            smoke(g, w, cx, cy, pw, ph, t, a);
            if (fall) embers(g, cx, cy, pw * (1 - 2 * burn), ph, t, a);
        }

        private static String roman(int n) {
            return n == 1 ? "I" : n == 2 ? "II" : "III";
        }

        /** Red smoke curling along the contract's top and bottom edges. */
        private void smoke(GuiGraphics g, int w, float cx, float cy, float pw, float ph, float t, float a) {
            Random r = new Random(seed);
            for (int i = 0; i < 26; i++) {
                float u = r.nextFloat(), sp = 0.15f + r.nextFloat() * 0.3f, ph0 = r.nextFloat() * 100;
                float x = cx - pw / 2 - 30 + ((u * (pw + 60) + t * sp * 6) % (pw + 60));
                boolean top = i % 2 == 0;
                float y = cy + (top ? -ph / 2 : ph / 2) + 4 * Mth.sin(t * 0.06f + ph0) + (top ? -3 : 3);
                int s = 6 + r.nextInt(10);
                for (int k = s; k > 1; k -= 3) {
                    g.fill((int) (x - k), (int) (y - k / 2f), (int) (x + k), (int) (y + k / 2f), MichaelGui.argb(a * 0.035f, RED_SMOKE));
                }
            }
        }

        private void embers(GuiGraphics g, float cx, float cy, float pw, float ph, float t, float a) {
            Random r = new Random(seed ^ 31);
            for (int i = 0; i < 40; i++) {
                float side = i % 2 == 0 ? -1 : 1, sp = 0.4f + r.nextFloat() * 0.6f, ph0 = r.nextFloat() * 50, life = 50;
                float k = ((t * sp + ph0) % life) / life;
                float x = cx + side * pw / 2 + (r.nextFloat() - 0.5f) * 14 + 5 * Mth.sin(t * 0.07f + i);
                float y = cy + (r.nextFloat() - 0.5f) * ph - k * 40;
                g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, MichaelGui.argb(a * Mth.sin(Mth.PI * k), i % 3 == 0 ? EMBER_HOT : EMBER));
            }
        }
    }
}
