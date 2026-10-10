package org.papiricoh.supernaturalcraft.client.lucifer;

import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.client.lucifer.LuciferGui.*;

/**
 * The title cards of both Lucifers' fights, branded on the screen. The screen darkens; behind, a seal of runes turns with
 * the moment's motif at its heart; a chain is drawn across, pulls taut and bursts, and where it was the title is burned in
 * letter by letter, white-hot, cooling to the phase's colour and shedding embers; the roman numeral above, the line
 * beneath. In each fight's last phase the seal burns out to white and the card turns to light and gold; at the victory the
 * bars of the Cage slam down over the title and the padlock shines at the heart of the seal.
 */
public final class LuciferOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("lucifer");
    private static final List<TitleCard> CARDS = new ArrayList<>();

    public static void add(TitleCard card) {
        // One title at a time: a new one takes the old one's place.
        CARDS.clear();
        CARDS.add(card);
    }

    /** Whether a title card is on screen (previews). */
    public static boolean showing() {
        return !CARDS.isEmpty();
    }

    static void tick() {
        if (Minecraft.getInstance().level == null) {
            CARDS.clear();
            return;
        }
        for (TitleCard c : List.copyOf(CARDS)) {
            c.tick();
            if (c.age >= c.life) CARDS.remove(c);
        }
    }

    static void clear() {
        CARDS.clear();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (Minecraft.getInstance().player == null || CARDS.isEmpty()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        for (TitleCard c : List.copyOf(CARDS)) c.render(g, partial, g.guiWidth(), g.guiHeight());
    }

    /** A band of {@code rgb} round the edges of the screen, {@code depth} of its smaller side deep. */
    static void edges(GuiGraphics g, int w, int h, float a, int rgb, float depth) {
        int e = (int) (Math.min(w, h) * depth);
        if (e <= 0 || a <= 0.01f) return;
        g.fillGradient(0, 0, w, e, argb(a, rgb), 0);
        g.fillGradient(0, h - e, w, h, 0, argb(a, rgb));
        for (int i = 0; i < e; i += 2) {
            int c = argb(a * (1 - i / (float) e), rgb);
            g.fill(i, 0, i + 2, h, c);
            g.fill(w - i - 2, 0, w - i, h, c);
        }
    }

    // --- the title card ----------------------------------------------------------------------------------------------

    public static final class TitleCard {
        /** When the chain comes in and bursts (ticks), and the ticks between letters. */
        static final int CHAIN_IN = 2, SNAP = 18;
        static final float LETTER_TICKS = 1.6f;
        /** Ticks after the burst before the first letter burns in. */
        static final int CLEAR_TICKS = 7;
        /** Where a boss bar of the Cage ends, from the top of the screen. */
        static final int BOSS_BARS_BOTTOM = 50;

        final boolean uncaged, last, victory;
        final int which, life, motif;
        final Style style;
        final String title;
        final Component sub;
        final int lettersAt;
        final Debris debris = new Debris();
        int age;
        private boolean snapped;
        private int burned, landed, shake;

        public TitleCard(boolean uncaged, int which, int duration) {
            this.uncaged = uncaged;
            this.which = which;
            this.life = Mth.clamp(duration, 90, 170);
            victory = which == LuciferFxPayload.TITLE_VICTORY;
            last = which == maxPhase(uncaged);
            style = victory ? style(uncaged, maxPhase(uncaged)) : style(uncaged, Math.max(1, which));
            motif = LuciferGui.motif(uncaged, which);
            String key = titleKey(uncaged, which);
            title = Component.translatable(key + ".title").getString();
            sub = Component.translatable(key + ".subtitle");
            // At the victory there is no chain: the title is there at once, and the Cage comes down on it. Otherwise the
            // letters wait for the burst chain to clear the line they are burned into.
            lettersAt = victory ? 4 : SNAP + CLEAR_TICKS;
        }

        public int which() {
            return which;
        }

        public boolean uncaged() {
            return uncaged;
        }

        void tick() {
            age++;
            if (shake > 0) shake--;
            debris.tick();
        }

        private float fade(float t) {
            return Mth.clamp(Math.min(t / 6f, (life - t) / 24f), 0, 1);
        }

        /** The colour a letter cools through: white-hot, then orange, then the phase's own. */
        private int heat(float h, int settled) {
            if (h > 0.7f) return lerp((1 - h) / 0.3f, 0xFFFFFF, 0xFFD27A);
            if (h > 0.4f) return lerp((0.7f - h) / 0.3f, 0xFFD27A, 0xFF6A1A);
            return lerp((0.4f - h) / 0.4f, 0xFF6A1A, settled);
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial, a = fade(t);
            if (a <= 0.01f) return;
            float cx = w / 2f, cy = Math.max(h * 0.4f, 112);
            g.pose().pushPose();
            if (shake > 0) {
                float s = (shake - partial) * 0.5f;
                g.pose().translate(Mth.sin(t * 9.1f) * s, Mth.cos(t * 7.3f) * s, 0);
            }
            backdrop(g, w, h, cx, cy, t, a);
            seal(g, h, cx, cy, t, a);
            if (!victory) chain(g, w, cx, cy, t, a);
            float flash = snapped && !victory ? Mth.clamp(1 - (t - SNAP) / 10f, 0, 1) : 0;
            if (flash > 0) g.fill(0, 0, w, h, argb(flash * a * (last ? 0.85f : 0.4f), last ? 0xFFFFFF : style.title()));
            // What flies off goes behind the title, never over it.
            debris.render(g, partial, a);
            letters(g, w, h, cx, cy, t, a, partial);
            g.pose().popPose();
        }

        private void backdrop(GuiGraphics g, int w, int h, float cx, float cy, float t, float a) {
            // The world darkens (or, in his last light, washes gold), the edges of sight burn, a band holds the title.
            g.fill(0, 0, w, h, argb(a * (last ? 0.12f : 0.42f), 0x000000));
            edges(g, w, h, a * 0.75f, last ? 0xFFE8A0 : 0x3A0404, 0.24f);
            int band = 64, bandRgb = last ? 0xFFF3C4 : 0x000000;
            float ba = a * (last ? 0.22f : 0.6f);
            g.fillGradient(0, (int) cy - band, w, (int) cy, 0, argb(ba, bandRgb));
            g.fillGradient(0, (int) cy, w, (int) cy + band, argb(ba, bandRgb), 0);
        }

        private void seal(GuiGraphics g, int h, float cx, float cy, float t, float a) {
            float in = Mth.clamp(t / 16f, 0, 1);
            in = 1 - (1 - in) * (1 - in);
            // In the last phase the seal burns out to white as the chain bursts.
            float burn = last && snapped ? Mth.clamp((t - SNAP) / 12f, 0, 1) : 0;
            int rgb = lerp(burn, style.title(), 0xFFFFFF);
            // As large as fits between the boss bars at the top and the hotbar, never past 320.
            float full = Mth.clamp(2 * Math.min(cy - BOSS_BARS_BOTTOM, h - 40 - cy), 110, 320);
            float size = full * (1.35f - 0.35f * in);
            float sa = a * in * (0.42f + 0.4f * burn * Math.max(0, 1 - (t - SNAP) / 40f));
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(Axis.ZP.rotation(t * 0.006f));
            glow(g, TITLE_SEAL, -size / 2, -size / 2, (int) size, (int) size, 0, 0, 256, 256, 256, 256, sa, rgb);
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(Axis.ZP.rotation(-t * 0.011f));
            glow(g, TITLE_SEAL, -size * 0.29f, -size * 0.29f, (int) (size * 0.58f), (int) (size * 0.58f), 0, 0, 256, 256, 256, 256, sa * 0.55f, rgb);
            g.pose().popPose();
            // The motif at its heart, breathing.
            float ms = full * 0.42f * (1.2f - 0.2f * in);
            float ma = a * in * (0.42f + 0.12f * Mth.sin(t * 0.18f)) * (victory && landed == (1 << CageBars.COUNT) - 1 ? 1.6f : 1);
            int u = (motif % 4) * 128, v = (motif / 4) * 128;
            glow(g, TITLE_MOTIFS, cx - ms / 2, cy - ms / 2, (int) ms, (int) ms, u, v, 128, 128, 512, 384, Math.min(1, ma), rgb);
        }

        private void chain(GuiGraphics g, int w, float cx, float cy, float t, float a) {
            if (t < SNAP) {
                // It whips in from the left, pulls taut, glows with the strain.
                float p = Mth.clamp((t - CHAIN_IN) / 8f, 0, 1);
                p = 1 - (1 - p) * (1 - p) * (1 - p);
                float strain = Mth.clamp((t - 10) / (SNAP - 10f), 0, 1);
                float jitter = strain * 1.8f * Mth.sin(t * 13.7f);
                float y = cy - 12 + jitter;
                int end = (int) (w * p);
                for (int x = 0; x < end; x += 256) {
                    int seg = Math.min(256, end - x);
                    int u = 256 - seg;
                    blit(g, TITLE_CHAIN, x, y, seg, 24, u, 0, seg, 24, 256, 24, a, 0xFFFFFF);
                    glow(g, TITLE_CHAIN, x, y, seg, 24, u, 0, seg, 24, 256, 24, a * strain * 0.9f, style.title());
                }
            } else if (!snapped) {
                snapped = true;
                shake = 8;
                // The chain bursts: its links are flung out from the middle and up, off the title's line.
                for (int x = 6; x < w; x += 16) {
                    float side = (x - cx) / Math.max(1f, w / 2f);
                    float away = side < 0 ? -1 : 1;
                    debris.add(Debris.LINK, x + debris.random.nextFloat() * 8, cy, side * 6f + away * (2f + debris.random.nextFloat() * 2.5f),
                            -4.5f - debris.random.nextFloat() * 4f, 1.6f + debris.random.nextFloat() * 0.8f, 20 + debris.random.nextInt(12), 0xFFFFFF);
                }
                debris.embers(40, cx, cy, w * 0.7f, style.title());
            }
        }

        private void letters(GuiGraphics g, int w, int h, float cx, float cy, float t, float a, float partial) {
            boolean gothic = Gothic.covers(title);
            String upper = Gothic.upper(title);
            Gothic.Atlas atlas = Gothic.atlas(true);
            float tw = Math.max(1, Gothic.width(upper, true));
            // Growing with the screen (a fifth of its height for the cells), never wider than it.
            float scale = Math.min(Math.min(3f, h / 120f), (w - 48) / tw);
            float cellH = atlas != null ? atlas.cellH() : 24, base = atlas != null ? atlas.baseline() : 19;
            float top = cy - (base - 8) * scale;
            float x0 = cx - tw * scale / 2f;
            int settled = last || victory ? 0xFFE8A0 : style.title();
            // The numeral of the phase, over the title.
            if (which >= 1 && which <= maxPhase(uncaged) && !victory) {
                float na = a * Mth.clamp((t - (lettersAt - 6)) / 8f, 0, 1);
                Gothic.centred(g, roman(which), cx, top - 26 * Math.min(1.2f, scale * 0.75f), Math.min(1.2f, scale * 0.75f), na, style.title(), true);
            }
            if (!gothic) {
                // A title the atlas cannot write: in the vanilla font, burned in as a whole.
                float local = t - lettersAt;
                if (local > 0) {
                    float hh = Mth.clamp(1 - local / 24f, 0, 1);
                    float s = Math.min(3f, (w - 40) / (float) Math.max(1, GuiDraw.width(Component.literal(title), 1f)));
                    GuiDraw.centred(g, Component.literal(title), cx, cy - 4.5f * s, s, argb(a * Mth.clamp(local / 3f, 0, 1), heat(hh, settled)), true);
                }
            } else {
                float x = x0;
                for (int i = 0; i < upper.length(); i++) {
                    char c = upper.charAt(i);
                    float adv = Gothic.advance(c, true) * scale;
                    float local = t - (lettersAt + i * LETTER_TICKS);
                    if (local > 0 && c != ' ') {
                        if (i >= burned) {
                            burned = i + 1;
                            debris.embers(4, x + adv / 2, top + 10 * scale, adv, 0xFF8A3A);
                        }
                        float hh = Mth.clamp(1 - local / 24f, 0, 1);
                        float pop = 1 + 0.3f * Math.max(0, 1 - local / 4f);
                        float la = a * Mth.clamp(local / 2f, 0, 1);
                        float gx = x + adv / 2 - adv * pop / 2, gy = top + base * scale * (1 - pop) * 0.5f;
                        // A dark outline and a drop shadow, so the title reads over any sky.
                        for (int k = 0; k < 4; k++) {
                            float ox = (k == 0 ? -1 : k == 1 ? 1 : 0) * scale, oy = (k == 2 ? -1 : k == 3 ? 1 : 0) * scale;
                            Gothic.glyph(g, c, gx + ox, gy + oy, scale * pop, la * 0.75f, 0x100404, true, false);
                        }
                        Gothic.glyph(g, c, x + 2 * scale, top + 2 * scale, scale, la * 0.6f, 0x000000, true, false);
                        Gothic.glyph(g, c, gx, gy, scale * pop, la, heat(hh, settled), true, false);
                        // The heat round it, and the glow it keeps.
                        Gothic.glyph(g, c, gx, gy, scale * pop, la * (0.25f + 0.75f * hh), hh > 0.5f ? 0xFFE0A0 : style.title(), true, true);
                    }
                    x += adv;
                }
            }
            // The line beneath.
            float subAt = lettersAt + upper.length() * LETTER_TICKS + 4;
            float sa = a * Mth.clamp((t - subAt) / 10f, 0, 1);
            if (sa > 0.01f && !sub.getString().isEmpty()) {
                Component italic = sub.copy().withStyle(s -> s.withItalic(true));
                float ss = Math.min(Math.max(1.2f, scale * 0.6f), (w - 30) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
                GuiDraw.centred(g, italic, cx, top + cellH * scale + 6, ss, argb(sa, last ? 0xFFF8E0 : 0xD8C8B4), true);
            }
            if (victory) cage(g, x0, tw * scale, top, scale, t, a);
        }

        /** The victory: the bars of the Cage slam down over the title, one after another. */
        private void cage(GuiGraphics g, float x0, float width, float top, float scale, float t, float a) {
            float barsAt = lettersAt + title.length() * LETTER_TICKS + 10;
            float bs = Math.max(1.2f, scale);
            float bh = BAR_H * bs, y = top + 12 * scale - bh / 2;
            float span = Math.max(width + 24 * bs, 8 * 14 * bs);
            for (int i = 0; i < CageBars.COUNT; i++) {
                float local = t - (barsAt + Math.min(i, CageBars.COUNT - 1 - i) * 3);
                if (local < 0) continue;
                float bx = x0 + width / 2 - span / 2 + span * (i + 0.5f) / CageBars.COUNT - 5 * bs;
                float by = y - (1 - bounce(local / 10f)) * (y + bh + 10);
                if (local >= 7 && (landed & (1 << i)) == 0) {
                    // It lands: the screen jolts, dust kicks up.
                    landed |= 1 << i;
                    shake = Math.max(shake, 4);
                    for (int d = 0; d < 6; d++) {
                        debris.add(Debris.DUST, bx + 6 * bs, y + bh, (debris.random.nextFloat() - 0.5f) * 3f, -debris.random.nextFloat(),
                                2f, 16, 0x8A8078);
                    }
                }
                g.pose().pushPose();
                g.pose().translate(bx, by, 0);
                g.pose().scale(bs, bs, 1);
                blit(g, BAR_BARS, 0, 0, BAR_W, BAR_H, 0, 0, BAR_W, BAR_H, BAR_W * 8, BAR_H, a, 0xFFFFFF);
                g.pose().popPose();
            }
        }
    }
}
