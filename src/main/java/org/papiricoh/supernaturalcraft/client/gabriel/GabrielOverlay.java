package org.papiricoh.supernaturalcraft.client.gabriel;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.QuizBank;
import org.papiricoh.supernaturalcraft.trickster.PrankRules;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.client.gabriel.ClientGabriel.*;
import static org.papiricoh.supernaturalcraft.client.gabriel.GabrielGui.*;

/**
 * TV Land on the screen (v0.14), a moderate fourth wall: the static and rolling bars of a channel flip and the green
 * "CH n" in the corner; the studio's LAUGH / APPLAUSE / ON AIR signs, lit or dark, under the boss bar; the game show's
 * question with its three coloured answers and the buzzer's countdown; the heart monitor's trace above the hotbar (the
 * band in the middle is when a blow lands on the beat); a prank's quiet hint; and the channels' title cards. Nothing here
 * covers the screen for long or stands in for the game's own interface.
 */
public final class GabrielOverlay implements LayeredDraw.Layer {

    private static final List<Moment> MOMENTS = new ArrayList<>();
    /** Pixels of the monitor's trace per tick. */
    private static final float ECG_PX = 2.6f;

    // --- moments (title cards) -----------------------------------------------------------------------------------------

    /** Something on screen for a while (the pattern of Michael's overlay). */
    public abstract static class Moment {
        protected int age;
        protected final int life;

        protected Moment(int life) {
            this.life = Math.max(1, life);
        }

        protected boolean over() {
            return age >= life;
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
        for (Moment m : List.copyOf(MOMENTS)) {
            m.age++;
            if (m.over()) MOMENTS.remove(m);
        }
    }

    static void clear() {
        MOMENTS.clear();
    }

    // --- the layer -------------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        float top = GabrielHud.bottom() + 3;
        top = signs(g, w, top, partial);
        quiz(g, w, top, partial);
        monitor(g, w, h, partial);
        prankHint(g, w, h, partial);
        for (Moment m : List.copyOf(MOMENTS)) m.render(g, partial, w, h);
        flip(g, w, h, partial);
    }

    // --- the flip ------------------------------------------------------------------------------------------------------

    private static void flip(GuiGraphics g, int w, int h, float partial) {
        if (flipChannel < 0) return;
        float t = ticks - flipAt + partial;
        if (t < 0 || t > Math.max(flipLength, OSD_TICKS)) return;
        if (t < flipLength) {
            float a = t < 3 ? t / 3f : t > flipLength * 0.55f ? 1 - (t - flipLength * 0.55f) / (flipLength * 0.45f) : 1;
            a = Mth.clamp(a, 0, 1) * 0.94f;
            staticNoise(g, w, h, (long) (ticks - flipAt), a);
            rollingBars(g, w, h, t, a);
        }
        // The OSD: up once the picture settles, for a few seconds.
        float osdT = t - Math.min(6, flipLength * 0.4f);
        if (osdT > 0 && osdT < OSD_TICKS) {
            float a = Mth.clamp((OSD_TICKS - osdT) / 20f, 0, 1);
            Channel c = Channel.values()[Mth.clamp(flipChannel, 0, Channel.values().length - 1)];
            osd(g, c.number, w - 10, 8, 2f, a);
        }
    }

    /** Snow: the static tile, tiled and jumping each tick (or grey cells while the art is missing). */
    static void staticNoise(GuiGraphics g, int w, int h, long frame, float a) {
        long seed = frame * 0x9E3779B97F4A7C15L;
        if (has(STATIC)) {
            int f = (int) Math.floorMod(frame, 4);
            float u = (f % 2) * 64, v = (f / 2) * 64;
            int size = 128;
            int ox = (int) Math.floorMod(seed >>> 7, 64), oy = (int) Math.floorMod(seed >>> 23, 64);
            for (int y = -oy; y < h; y += size) {
                for (int x = -ox; x < w; x += size) blit(g, STATIC, x, y, size, size, u, v, 64, 64, 128, 128, a, 0xE8ECF0);
            }
        } else {
            int cell = 2;
            for (int y = 0; y < h; y += cell) {
                for (int x = 0; x < w; x += cell) {
                    long k = (x * 73856093L) ^ (y * 19349663L) ^ seed;
                    k ^= k >>> 29;
                    k *= 0xBF58476D1CE4E5B9L;
                    k ^= k >>> 32;
                    int grey = (int) ((k & 0xFF) * 0.9);
                    g.fill(x, y, x + cell, y + cell, argb(a, grey << 16 | grey << 8 | grey));
                }
            }
        }
    }

    /** Dark bands rolling down the picture, and the bright line of a lost vertical hold. */
    static void rollingBars(GuiGraphics g, int w, int h, float t, float a) {
        int band = Math.max(8, h / 7);
        for (int i = 0; i < 3; i++) {
            float y = ((t * 7f + i * h / 3f) % (h + band)) - band;
            g.fillGradient(0, (int) y, w, (int) (y + band / 2f), 0, argb(a * 0.55f, 0x000000));
            g.fillGradient(0, (int) (y + band / 2f), w, (int) (y + band), argb(a * 0.55f, 0x000000), 0);
        }
        float line = (t * 13f) % h;
        g.fill(0, (int) line, w, (int) line + 1, argb(a * 0.7f, 0xFFFFFF));
        // Scanlines.
        for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, argb(a * 0.18f, 0x000000));
    }

    // --- the studio signs ----------------------------------------------------------------------------------------------

    /** @return the y below the signs */
    private static float signs(GuiGraphics g, int w, float top, float partial) {
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < SIGN_UNTIL.length; i++) if (ticks < SIGN_UNTIL[i]) shown.add(i);
        if (shown.isEmpty()) return top;
        float scale = 0.75f;
        int sw = Math.round(SIGN_W * scale), sh = Math.round(SIGN_H * scale), gap = 6;
        float x = w / 2f - (shown.size() * sw + (shown.size() - 1) * gap) / 2f;
        for (int i : shown) {
            sign(g, i, x, top, sw, sh, partial);
            x += sw + gap;
        }
        return top + sh + 4;
    }

    private static void sign(GuiGraphics g, int which, float x, float y, int sw, int sh, float partial) {
        float since = ticks - SIGN_LIT_AT[which] + partial;
        // A tube sign stutters as it lights.
        boolean lit = SIGN_LIT[which] && !(since < 8 && ((int) since == 1 || (int) since == 4));
        float pulse = 0.85f + 0.15f * Mth.sin((ticks + partial) * 0.35f);
        float fade = SIGN_UNTIL[which] == Long.MAX_VALUE ? 1 : Mth.clamp((SIGN_UNTIL[which] - ticks - partial) / 15f, 0, 1);
        if (has(SIGNS[which])) {
            if (lit) {
                // The glow round it: a soft red halo, then the sign, then the light added on top.
                g.fill((int) x - 3, (int) y - 3, (int) x + sw + 3, (int) y + sh + 3, argb(0.22f * pulse * fade, SIGN_RED));
                blit(g, SIGNS[which], x, y, sw, sh, 0, 0, SIGN_W, SIGN_H, SIGN_W, SIGN_H, fade, 0xFFFFFF);
                glow(g, SIGNS[which], x, y, sw, sh, 0, 0, SIGN_W, SIGN_H, SIGN_W, SIGN_H, 0.45f * pulse * fade, 0xFFFFFF);
            } else {
                blit(g, SIGNS[which], x, y, sw, sh, 0, 0, SIGN_W, SIGN_H, SIGN_W, SIGN_H, 0.85f * fade, 0x4A4A4A);
            }
        } else {
            g.fill((int) x, (int) y, (int) x + sw, (int) y + sh, argb(0.9f * fade, 0x1A0E0C));
            int edge = argb(fade, lit ? 0xFFB0A0 : 0x4A3030);
            g.fill((int) x, (int) y, (int) x + sw, (int) y + 1, edge);
            g.fill((int) x, (int) y + sh - 1, (int) x + sw, (int) y + sh, edge);
            g.fill((int) x, (int) y, (int) x + 1, (int) y + sh, edge);
            g.fill((int) x + sw - 1, (int) y, (int) x + sw, (int) y + sh, edge);
            Component word = Component.translatable("hud.supernaturalcraft.gabriel.sign." + GabrielAssets.SIGNS.get(which));
            float scale = Math.min(1.6f, (sw - 8) / (float) Math.max(1, GuiDraw.width(word, 1f)));
            float ty = y + (sh - 8 * scale) / 2f;
            if (lit) {
                g.fill((int) x - 3, (int) y - 3, (int) x + sw + 3, (int) y + sh + 3, argb(0.2f * pulse * fade, SIGN_RED));
                GuiDraw.centred(g, word, x + sw / 2f, ty, scale, argb(fade, 0xFFE6DC), false);
                GuiDraw.centred(g, word, x + sw / 2f + 0.5f, ty + 0.5f, scale, argb(0.5f * pulse * fade, SIGN_RED), false);
            } else {
                GuiDraw.centred(g, word, x + sw / 2f, ty, scale, argb(fade, 0x5A2A26), false);
            }
        }
    }

    // --- the quiz ------------------------------------------------------------------------------------------------------

    private static void quiz(GuiGraphics g, int w, float top, float partial) {
        if (quizQuestion < 0) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        QuizBank.Question q = QuizBank.QUESTIONS.get(quizQuestion);
        float t = ticks - quizAt + partial;
        float in = Mth.clamp(t / 6f, 0, 1);
        float out = quizResult >= 0 ? Mth.clamp(1 - (ticks - quizResultAt + partial - (QUIZ_RESULT_TICKS - 12)) / 12f, 0, 1) : 1;
        float a = in * out;
        if (a <= 0.01f) return;
        float scale = Math.min(Math.min(1f, (w - 16) / (float) QUIZ_W), g.guiHeight() * 0.34f / QUIZ_H);
        float x0 = w / 2f - QUIZ_W * scale / 2f, y0 = top + (1 - in) * -12;
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale(scale, scale, 1);
        // The panel.
        if (has(QUIZ_PANEL)) {
            blit(g, QUIZ_PANEL, 0, 0, QUIZ_W, QUIZ_H, 0, 0, QUIZ_W, QUIZ_H, QUIZ_W, QUIZ_H, a, 0xFFFFFF);
        } else {
            g.fill(0, 0, QUIZ_W, QUIZ_H, argb(0.88f * a, 0x10123A));
            g.fill(0, 0, QUIZ_W, 2, argb(a, GOLD));
            g.fill(0, QUIZ_H - 2, QUIZ_W, QUIZ_H, argb(a, GOLD));
            g.fill(0, 0, 2, QUIZ_H, argb(a, GOLD));
            g.fill(QUIZ_W - 2, 0, QUIZ_W, QUIZ_H, argb(a, GOLD));
            for (int p = 0; p < 3; p++) {
                int sx = 4 + p * 84;
                g.fill(sx, 52, sx + 80, 88, argb(0.9f * a, PLATFORM[p]));
                g.fill(sx + 2, 54, sx + 78, 86, argb(0.55f * a, 0x000000));
            }
        }
        // The question, up to three lines.
        Component question = Component.translatable(q.key());
        List<FormattedCharSequence> lines = font.split(question, 232);
        float qs = lines.size() > 3 ? 0.75f : 1f;
        if (lines.size() > 3) lines = font.split(question, (int) (232 / qs));
        float qy = 8 + (32 - Math.min(lines.size(), 4) * 9 * qs) / 2f;
        for (int i = 0; i < Math.min(lines.size(), 4); i++) {
            g.pose().pushPose();
            g.pose().translate(QUIZ_W / 2f - font.width(lines.get(i)) * qs / 2f, qy + i * 9 * qs, 0);
            g.pose().scale(qs, qs, 1);
            g.drawString(font, lines.get(i), 0, 0, argb(a, CREAM), true);
            g.pose().popPose();
        }
        // The three answers, one per platform.
        int right = QuizBank.rightPlatform(quizDeal);
        for (int p = 0; p < 3; p++) {
            int sx = 4 + p * 84;
            Component label = Component.translatable("hud.supernaturalcraft.gabriel.quiz.platform." + p);
            GuiDraw.centred(g, label, sx + 40, 54.5f, 0.5f, has(QUIZ_PANEL) ? argb(a * 0.8f, 0xFFFFFF) : argb(a * 0.9f, PLATFORM[p]), false);
            Component answer = Component.translatable(q.answerKey(quizDeal[p]));
            List<FormattedCharSequence> al = font.split(answer, 74);
            float as = al.size() > 2 ? 0.7f : 0.85f;
            if (al.size() > 2) al = font.split(answer, (int) (74 / as));
            int n = Math.min(al.size(), 3);
            float ay = 61 + (26 - n * 9 * as) / 2f;
            boolean dim = quizResult >= 0 && p != right;
            for (int i = 0; i < n; i++) {
                g.pose().pushPose();
                g.pose().translate(sx + 40 - font.width(al.get(i)) * as / 2f, ay + i * 9 * as, 0);
                g.pose().scale(as, as, 1);
                g.drawString(font, al.get(i), 0, 0, argb(a * (dim ? 0.35f : 1f), 0xFFFFFF), true);
                g.pose().popPose();
            }
            if (quizResult >= 0 && p == right) {
                float pulse = 0.6f + 0.4f * Mth.sin((ticks + partial) * 0.6f);
                int c = argb(a * pulse, 0xFFFFFF);
                g.fill(sx - 1, 51, sx + 81, 52, c);
                g.fill(sx - 1, 88, sx + 81, 89, c);
                g.fill(sx - 1, 51, sx, 89, c);
                g.fill(sx + 80, 51, sx + 81, 89, c);
            }
        }
        // The buzzer's countdown.
        if (quizResult < 0) {
            float left = Mth.clamp(1 - t / quizLength, 0, 1);
            int c = left > 0.5f ? 0x5CFF6A : left > 0.2f ? 0xFFD040 : 0xFF4030;
            g.fill(8, QUIZ_H + 3, QUIZ_W - 16, QUIZ_H + 7, argb(0.7f * a, 0x000000));
            g.fill(8, QUIZ_H + 3, 8 + Math.round((QUIZ_W - 24) * left), QUIZ_H + 7, argb(a, c));
            int secs = (int) Math.ceil(left * quizLength / 20f);
            Component s = Component.literal(String.valueOf(secs));
            GuiDraw.text(g, s, QUIZ_W - 6, QUIZ_H + 1, 0.8f, argb(a, c), true);
        } else {
            // Right or wrong: a big tick or cross over the panel.
            float rt = ticks - quizResultAt + partial;
            float pop = 1 + 0.3f * Mth.clamp(1 - rt / 6f, 0, 1);
            boolean ok = quizResult == 1;
            int c = argb(a, ok ? 0x5CFF6A : 0xFF4030);
            float cx = QUIZ_W / 2f, cy = 30, r = 16 * pop;
            if (ok) {
                GuiDraw.line(g, cx - r, cy, cx - r * 0.3f, cy + r * 0.7f, 6, argb(a, 0x000000));
                GuiDraw.line(g, cx - r * 0.3f, cy + r * 0.7f, cx + r, cy - r * 0.8f, 6, argb(a, 0x000000));
                GuiDraw.line(g, cx - r, cy, cx - r * 0.3f, cy + r * 0.7f, 4, c);
                GuiDraw.line(g, cx - r * 0.3f, cy + r * 0.7f, cx + r, cy - r * 0.8f, 4, c);
            } else {
                GuiDraw.line(g, cx - r, cy - r, cx + r, cy + r, 6, argb(a, 0x000000));
                GuiDraw.line(g, cx - r, cy + r, cx + r, cy - r, 6, argb(a, 0x000000));
                GuiDraw.line(g, cx - r, cy - r, cx + r, cy + r, 4, c);
                GuiDraw.line(g, cx - r, cy + r, cx + r, cy - r, 4, c);
            }
            Component word = Component.translatable(ok ? "hud.supernaturalcraft.gabriel.quiz.right" : "hud.supernaturalcraft.gabriel.quiz.wrong");
            GuiDraw.centred(g, word, cx, QUIZ_H + 4, 1.4f, c, true);
        }
        g.pose().popPose();
    }

    // --- the heart monitor ---------------------------------------------------------------------------------------------

    private static void monitor(GuiGraphics g, int w, int h, float partial) {
        if (BEEPS.isEmpty()) return;
        float now = ticks + partial;
        long last = BEEPS.getLast();
        float since = now - last;
        if (since > BEAT_HOLD + 20) return;
        float a = Mth.clamp((BEAT_HOLD + 20 - since) / 20f, 0, 1);
        int sw = 182, sh = 26;
        float x0 = w / 2f - sw / 2f, y0 = h - 52 - sh;
        int ix = (int) x0, iy = (int) y0;
        g.fill(ix - 1, iy - 1, ix + sw + 1, iy + sh + 1, argb(0.8f * a, 0x0E2A14));
        g.fill(ix, iy, ix + sw, iy + sh, argb(0.82f * a, 0x020A04));
        for (int gx = 0; gx < sw; gx += 10) g.fill(ix + gx, iy, ix + gx + 1, iy + sh, argb(0.12f * a, 0x5CFF6A));
        for (int gy = 0; gy < sh; gy += 6) g.fill(ix, iy + gy, ix + sw, iy + gy + 1, argb(0.12f * a, 0x5CFF6A));
        // "Now" sits two thirds of the way along: what has beeped scrolls off to the left, the next beep comes in from the right.
        float nowX = x0 + sw * 0.66f;
        // The window a blow lands on the beat: lit when a beep is inside it.
        float win = GabrielBalance.BEAT_WINDOW * ECG_PX;
        float next = last + beatPeriod;
        float toBeat = Math.min(Math.abs(now - last), Math.abs(next - now));
        boolean onBeat = toBeat <= GabrielBalance.BEAT_WINDOW;
        g.fill((int) (nowX - win), iy + 1, (int) (nowX + win), iy + sh - 1, argb((onBeat ? 0.4f : 0.12f) * a, onBeat ? 0xFF5050 : 0x5CFF6A));
        g.fill((int) nowX, iy, (int) nowX + 1, iy + sh, argb(0.6f * a, 0xFFFFFF));
        // The trace.
        float mid = y0 + sh * 0.62f, amp = sh * 0.48f;
        float px = x0, py = mid - amp * ecg(now + (x0 - nowX) / ECG_PX, last);
        for (int i = 2; i <= sw; i += 2) {
            float x = x0 + i;
            float tt = now + (x - nowX) / ECG_PX;
            float y = mid - amp * ecg(tt, last);
            boolean ahead = x > nowX;
            GuiDraw.line(g, px, py, x, y, 1.2f, argb(a * (ahead ? 0.3f : 1f), 0x6CFF7A));
            px = x;
            py = y;
        }
        // Beats per minute, and the heart.
        int bpm = Math.round(1200f / Math.max(1, beatPeriod));
        float beatGlow = Mth.clamp(1 - since / 6f, 0, 1);
        Component heart = Component.literal("❤").withStyle(ChatFormatting.BOLD);
        GuiDraw.text(g, heart, ix + 3, iy + 2, 0.8f + 0.25f * beatGlow, argb(a * (0.5f + 0.5f * beatGlow), 0xFF5050), false);
        Component label = Component.translatable("hud.supernaturalcraft.gabriel.monitor.bpm", bpm);
        GuiDraw.text(g, label, ix + sw - GuiDraw.width(label, 0.6f) - 3, iy + 2, 0.6f, argb(a, 0x6CFF7A), false);
        if (onBeat) {
            Component hit = Component.translatable("hud.supernaturalcraft.gabriel.monitor.hit");
            GuiDraw.centred(g, hit, nowX, iy - 8, 0.7f, argb(a, 0xFF7060), true);
        }
    }

    /** The trace at time {@code t}: a spike at each beep (past ones heard, future ones on the beat), flat between. */
    private static float ecg(float t, long last) {
        float v = 0;
        for (long b : BEEPS) v += spike(t - b);
        // Beats still to come, on the current period.
        for (int k = 1; k <= 4; k++) {
            float b = last + k * beatPeriod;
            if (b - t > 6) break;
            v += spike(t - b);
        }
        return v;
    }

    /** One heartbeat's shape, {@code d} ticks from the beep: a little dip, the tall spike, the dip under, the T wave. */
    private static float spike(float d) {
        if (d < -4 || d > 9) return 0;
        if (d < -2) return 0.08f * (1 - Math.abs(d + 3));
        if (d < -0.5f) return -0.12f * (d + 2) / 1.5f;
        if (d < 0.5f) return -0.12f + 1.12f * (d + 0.5f);
        if (d < 1.5f) return 1 - 1.45f * (d - 0.5f);
        if (d < 2.5f) return -0.45f + 0.45f * (d - 1.5f);
        if (d < 8) return 0.16f * Mth.sin((d - 2.5f) / 5.5f * Mth.PI);
        return 0;
    }

    // --- a prank's hint ------------------------------------------------------------------------------------------------

    private static void prankHint(GuiGraphics g, int w, int h, float partial) {
        if (prank < 0) return;
        float t = ticks - prankAt + partial;
        if (t > PRANK_TICKS) return;
        float a = Mth.clamp(Math.min(t / 10f, (PRANK_TICKS - t) / 30f), 0, 1);
        String id = PrankRules.Prank.values()[prank].id();
        Component line = Component.translatable("hud.supernaturalcraft.gabriel.prank." + id).withStyle(ChatFormatting.ITALIC);
        GuiDraw.centred(g, line, w / 2f, h - 92, 1f, argb(a * 0.85f, 0xE8D9A8), true);
    }

    // --- title cards ---------------------------------------------------------------------------------------------------

    /** A channel's opening card ("CH 2 — THE SITCOM"), or the episode's end: a little screen that tunes in and switches off. */
    public static final class TitleCard extends Moment {
        private final Channel channel;
        private final boolean end;
        private final Component title, sub;

        public TitleCard(Channel channel, boolean end, int duration) {
            super(Mth.clamp(duration <= 0 ? 100 : duration, 60, 160));
            this.channel = channel;
            this.end = end;
            title = Component.translatable(end ? "hud.supernaturalcraft.gabriel.title.end" : "hud.supernaturalcraft.gabriel.title." + channel.id());
            sub = Component.translatable(end ? "hud.supernaturalcraft.gabriel.title.end." + channel.id()
                    : "hud.supernaturalcraft.gabriel.title." + channel.id() + ".sub");
        }

        public Channel channel() {
            return channel;
        }

        public boolean end() {
            return end;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial;
            int colour = end ? GOLD : channelColour(channel);
            float cw = Math.min(w - 24, 260), ch = 70;
            float cx = w / 2f, cy = Math.max(h * 0.36f, GabrielHud.bottom() + ch / 2f + 10);
            // Tuning in: a bright line opens into the picture. Switching off: back to a line, then a dot.
            float open = Mth.clamp(t / 7f, 0, 1), close = Mth.clamp((life - t) / 10f, 0, 1);
            float sy = Math.min(open * open, close < 1 ? Math.max(0.02f, close * close) : 1);
            float sx = close < 0.35f ? Math.max(0.02f, close / 0.35f) : 1;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(sx, sy, 1);
            float x0 = -cw / 2, y0 = -ch / 2;
            g.fill((int) x0 - 2, (int) y0 - 2, (int) -x0 + 2, (int) -y0 + 2, argb(0.9f, 0x0A0A0A));
            g.fill((int) x0, (int) y0, (int) -x0, (int) -y0, argb(0.86f, 0x15121C));
            g.fill((int) x0, (int) y0, (int) -x0, (int) y0 + 1, argb(1, colour));
            g.fill((int) x0, (int) -y0 - 1, (int) -x0, (int) -y0, argb(1, colour));
            for (int y = (int) y0; y < -y0; y += 2) g.fill((int) x0, y, (int) -x0, y + 1, argb(0.12f, 0x000000));
            if (!end) osd(g, channel.number, -x0 - 6, y0 + 5, 1f, 1);
            float scale = Math.min(2.4f, (cw - 24) / (float) Math.max(1, GuiDraw.width(title, 1f)));
            GuiDraw.centred(g, title, 1, y0 + 20 + 1, scale, argb(1, 0x000000), false);
            GuiDraw.centred(g, title, 0, y0 + 20, scale, argb(1, colour), false);
            float ss = Math.min(1f, (cw - 16) / (float) Math.max(1, GuiDraw.width(sub, 1f)));
            GuiDraw.centred(g, sub, 0, -y0 - 14, ss, argb(0.9f, CREAM), true);
            g.pose().popPose();
            // The flash as it tunes in.
            if (t < 7) {
                float f = 1 - t / 7f;
                g.fill((int) (cx - cw / 2), (int) (cy - 1), (int) (cx + cw / 2), (int) (cy + 1), argb(f, 0xFFFFFF));
            }
        }
    }
}
