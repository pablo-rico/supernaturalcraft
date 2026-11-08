package org.papiricoh.supernaturalcraft.client.amara;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.lucifer.Gothic;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraHudSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Amara's title cards, voices, flashes and cracks (v0.42), over her camera shots: the layer is let through camera sequences and
 * draws no game HUD.
 * <ul>
 *   <li>A card ({@code TITLE}, {@link #TITLES}): ink bars bleed in at the top and bottom, her seal (an eclipse with a closed eye,
 *   {@link AmaraHudSheet#SEAL}) opens out of a violet glow, the phase's numeral above, the title resolving glyph by glyph in the
 *   Cage's gothic capitals with a stroke of ink under it, the subtitle in italics. Her rage burns red and violet; the dawn is
 *   gold, with rays.</li>
 *   <li>A voice ({@code VOICE}): {@code message.supernaturalcraft.amara.voice.<n>}, word by word low on the screen, the speaker
 *   ({@link #SPEAKERS}) named over it: Amara in violet-white with a smoke shadow, Chuck in warm white, Casifer in red-tinged
 *   white.</li>
 *   <li>A flash ({@code FLASH}): black, white or violet.</li>
 *   <li>Cracks ({@code CHUCK_WOUND}): ink cracks run across the screen with God's light bleeding white-gold through them.</li>
 * </ul>
 * Keys: {@code cinematic.supernaturalcraft.amara.title.<n>} and {@code .subtitle.<n>} (n 0 intro, 1-6 phases, 7 rage, 8 dawn),
 * {@code message.supernaturalcraft.amara.voice.<n>}, {@code message.supernaturalcraft.amara.speaker.<who>}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AmaraOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("amara");
    /** Cards: 0 intro, 1-6 the phases, 7 her rage, 8 the dawn. */
    public static final int TITLES = 9, RAGE = 7, DAWN = 8;
    /** Who speaks each voiced line (index = line): 0 Amara, 1 Chuck, 2 Casifer. */
    static final int[] SPEAKERS = {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1};
    static final String[] WHO = {"amara", "chuck", "casifer"};
    static final int VIOLET = 0xB48CFF, PALE = 0xE9E0FF, INK = 0x07040C, GOLD = 0xFFD877, RED = 0xFF3A5A, CHUCK = 0xFFF2D0,
            CASIFER = 0xFFD2CC;

    private static @Nullable Card card;
    private static @Nullable Voice voice;
    private static @Nullable Cracks cracks;
    private static float flash;
    private static int flashRgb;

    public static void title(int which, int duration) {
        card = new Card(Mth.clamp(which, 0, TITLES - 1), duration);
    }

    public static void voice(int line, int duration) {
        voice = new Voice(Math.max(0, line), duration);
    }

    /** {@code colour}: 0 black, 1 white, 2 violet. */
    public static void flash(int colour, float strength) {
        flash = Math.max(flash, Mth.clamp(strength, 0, 1));
        flashRgb = colour == 1 ? 0xFFFFFF : colour == 2 ? 0x8A5AE0 : 0x000000;
    }

    public static void cracks(int duration) {
        cracks = new Cracks(duration);
    }

    public static void clear() {
        card = null;
        voice = null;
        cracks = null;
        flash = 0;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            clear();
            return;
        }
        if (card != null && ++card.age >= card.life) card = null;
        if (voice != null && ++voice.age >= voice.life) voice = null;
        if (cracks != null && ++cracks.age >= cracks.life) cracks = null;
        flash = Math.max(0, flash - 0.06f);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (Minecraft.getInstance().player == null) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        Cracks k = cracks;
        if (k != null) k.render(g, partial, w, h);
        if (flash > 0.01f) g.fill(0, 0, w, h, MichaelGui.argb(flash * 0.8f, flashRgb));
        Voice v = voice;
        if (v != null) v.render(g, partial, w, h);
        Card c = card;
        if (c != null) c.render(g, partial, w, h);
    }

    // --- cards -----------------------------------------------------------------------------------------------------------

    static final class Card {
        final int which, life;
        final String title;
        final @Nullable Component sub;
        int age;

        Card(int which, int duration) {
            this.which = which;
            this.life = Mth.clamp(duration, 70, 200);
            title = I18n.get("cinematic.supernaturalcraft.amara.title." + which);
            String subKey = "cinematic.supernaturalcraft.amara.subtitle." + which;
            sub = I18n.exists(subKey) ? Component.translatable(subKey) : null;
        }

        int colour() {
            return which == RAGE ? RED : which == DAWN ? GOLD : PALE;
        }

        int glow() {
            return which == RAGE ? 0xB040FF : which == DAWN ? 0xFFE7A0 : VIOLET;
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial;
            float a = Mth.clamp(Math.min(t / 10f, (life - t) / 24f), 0, 1);
            if (a <= 0.01f) return;
            // Ink bars bleeding in at the top and bottom.
            int bar = Math.round(h * 0.1f * Mth.clamp(t / 14f, 0, 1) * Mth.clamp((life - t) / 18f, 0, 1));
            if (bar > 0) inkBars(g, w, h, bar, t, which == DAWN ? 0x1A1206 : INK);
            float cx = w / 2f, cy = Math.max(h * 0.34f, 90);
            // Dawn: rays behind everything.
            if (which == DAWN) {
                for (int i = 0; i < 16; i++) {
                    float ang = i * Mth.TWO_PI / 16 + t * 0.004f;
                    float len = Math.min(w, h) * 0.45f * Mth.clamp(t / 30f, 0, 1);
                    GuiDraw.line(g, cx, cy - 30, cx + Mth.cos(ang) * len, cy - 30 + Mth.sin(ang) * len, 3f,
                            MichaelGui.argb(a * 0.18f, 0xFFE7A0));
                }
            }
            // The seal opening out of its glow.
            float grow = 1 - (1 - Mth.clamp(t / 18f, 0, 1)) * (1 - Mth.clamp(t / 18f, 0, 1));
            AmaraHudSheet.Region seal = AmaraHudSheet.SEAL;
            if (MichaelGui.has(AmaraHud.SHEET)) {
                AmaraHud.region(g, AmaraHudSheet.CORONA, cx, cy - 34, 90 * grow, a * 0.5f, glow(), true);
                AmaraHud.region(g, seal, cx, cy - 34, 1.5f * seal.w() * grow, a, which == DAWN ? 0xFFF4DC : 0xFFFFFF, false);
            }
            // The numeral of a phase.
            if (which >= 1 && which <= 6) {
                String num = MichaelGui.roman(which);
                Gothic.centred(g, num, cx, cy - 4, 1f, a * Mth.clamp((t - 6) / 8f, 0, 1), glow(), false);
            }
            // The title, glyph by glyph, and a stroke of ink under it.
            float ta = a * Mth.clamp((t - 10) / 8f, 0, 1);
            float scale = Math.min(2.2f, (w - 40) / (float) Math.max(1, Gothic.width(title, false)));
            float ty = cy + 8;
            resolve(g, title, cx, ty, scale, ta, colour(), t - 10);
            float stroke = Mth.clamp((t - 14) / 16f, 0, 1);
            float half = Gothic.width(title, false) * scale / 2f * stroke + 8;
            GuiDraw.line(g, cx - half, ty + 12 * scale + 3, cx + half, ty + 12 * scale + 3, 1.5f, MichaelGui.argb(ta * 0.8f, which == DAWN ? 0xC99A3A : 0x3A1F6A));
            if (sub != null) {
                Component italic = sub.copy().withStyle(st -> st.withItalic(true));
                float ss = Math.min(1f, (w - 60) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
                float sa = a * Mth.clamp((t - 26) / 12f, 0, 1);
                GuiDraw.centred(g, italic, cx, ty + 12 * scale + 9, ss, MichaelGui.argb(sa, which == DAWN ? 0xFFF0D0 : 0xCBBCE8), true);
            }
        }
    }

    /** A gothic line whose glyphs fade in one after another; the vanilla font, whole, if the atlas lacks a letter. */
    static void resolve(GuiGraphics g, String text, float cx, float y, float scale, float alpha, int rgb, float t) {
        if (alpha <= 0.01f) return;
        if (!Gothic.covers(text)) {
            Gothic.centred(g, text, cx, y, scale, alpha, rgb, false);
            return;
        }
        String upper = Gothic.upper(text);
        float x = cx - Gothic.width(upper, false) * scale / 2f;
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            float k = Mth.clamp((t - i * 1.6f) / 6f, 0, 1);
            if (k > 0) {
                float rise = (1 - k) * 3 * scale;
                Gothic.glyph(g, c, x + scale, y + scale + rise, scale, alpha * k * 0.85f, 0x000000, false, false);
                Gothic.glyph(g, c, x, y + rise, scale, alpha * k, rgb, false, false);
                if (k < 1) Gothic.glyph(g, c, x, y + rise, scale, alpha * (1 - k), 0xFFFFFF, false, true);
            }
            x += Gothic.advance(c, false) * scale;
        }
    }

    /** Black bars with a ragged, dripping inner edge. */
    static void inkBars(GuiGraphics g, int w, int h, int bar, float t, int rgb) {
        g.fill(0, 0, w, bar, MichaelGui.argb(0.92f, rgb));
        g.fill(0, h - bar, w, h, MichaelGui.argb(0.92f, rgb));
        for (int x = 0; x < w; x += 3) {
            int drip = (int) (3 + 3 * Mth.sin(x * 0.21f + t * 0.03f) + 2 * Mth.sin(x * 0.07f));
            g.fill(x, bar, x + 3, bar + Math.max(0, drip), MichaelGui.argb(0.85f, rgb));
            g.fill(x, h - bar - Math.max(0, drip - 2), x + 3, h - bar, MichaelGui.argb(0.85f, rgb));
        }
    }

    // --- voices ----------------------------------------------------------------------------------------------------------

    static final class Voice {
        final int line, life, speaker;
        final String[] words;
        final Component who;
        int age;

        Voice(int line, int duration) {
            this.line = line;
            this.speaker = line < SPEAKERS.length ? SPEAKERS[line] : 0;
            String text = I18n.get("message.supernaturalcraft.amara.voice." + line);
            this.words = text.split(" ");
            this.life = Math.max(50, Math.max(duration, words.length * 6 + 50));
            this.who = Component.translatable("message.supernaturalcraft.amara.speaker." + WHO[speaker]);
        }

        int colour() {
            return speaker == 1 ? CHUCK : speaker == 2 ? CASIFER : PALE;
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial;
            float a = Mth.clamp(Math.min(t / 6f, (life - t) / 16f), 0, 1);
            if (a <= 0.01f) return;
            int shown = Mth.clamp((int) (t / 4) + 1, 1, words.length);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < shown; i++) {
                if (i > 0) sb.append(' ');
                sb.append(words[i]);
            }
            Component text = Component.literal(sb.toString()).withStyle(st -> st.withItalic(true));
            float scale = Math.min(1.25f, (w - 40) / (float) Math.max(1, GuiDraw.width(Component.literal(String.join(" ", words)), 1f)));
            float cy = h * 0.72f;
            if (speaker == 0) {
                // Her words smoke: dark ghosts drifting under them.
                Random r = new Random(line * 31L + (long) (t / 4));
                for (int k = 0; k < 2; k++) {
                    GuiDraw.centred(g, text, w / 2f + (r.nextFloat() - 0.5f) * 3, cy + 1 + k, scale, MichaelGui.argb(a * 0.35f, 0x2A1240), false);
                }
            }
            GuiDraw.centred(g, text, w / 2f, cy, scale, MichaelGui.argb(a, colour()), true);
            GuiDraw.centred(g, who, w / 2f, cy - 9, 0.6f, MichaelGui.argb(a * 0.7f, speaker == 0 ? VIOLET : speaker == 1 ? GOLD : RED), true);
        }
    }

    // --- cracks ----------------------------------------------------------------------------------------------------------

    static final class Cracks {
        final int life;
        final List<float[]> lines = new ArrayList<>();
        int age;

        Cracks(int duration) {
            life = Math.max(30, duration);
            Random r = new Random();
            for (int c = 0; c < 6; c++) {
                // From an edge inward, jagged, with a branch or two (coordinates as fractions of the screen).
                float x = r.nextBoolean() ? r.nextFloat() : (r.nextBoolean() ? 0 : 1), y = x == 0 || x == 1 ? r.nextFloat() : (r.nextBoolean() ? 0 : 1);
                float ang = (float) Math.atan2(0.5f - y, 0.5f - x);
                int segs = 5 + r.nextInt(5);
                for (int s = 0; s < segs; s++) {
                    ang += (r.nextFloat() - 0.5f) * 0.9f;
                    float len = 0.03f + r.nextFloat() * 0.05f;
                    float nx = x + Mth.cos(ang) * len, ny = y + Mth.sin(ang) * len;
                    lines.add(new float[]{x, y, nx, ny, s / (float) segs, 1.6f - s * 0.12f});
                    if (r.nextInt(4) == 0) {
                        float ba = ang + (r.nextBoolean() ? 0.9f : -0.9f);
                        lines.add(new float[]{nx, ny, nx + Mth.cos(ba) * len * 0.7f, ny + Mth.sin(ba) * len * 0.7f, s / (float) segs, 0.9f});
                    }
                    x = nx;
                    y = ny;
                }
            }
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial;
            float grow = Mth.clamp(t / 8f, 0, 1);
            float a = Mth.clamp((life - t) / 20f, 0, 1);
            if (a <= 0.01f) return;
            for (float[] l : lines) {
                if (l[4] > grow) continue;
                float x0 = l[0] * w, y0 = l[1] * h, x1 = l[2] * w, y1 = l[3] * h;
                GuiDraw.line(g, x0, y0, x1, y1, l[5] + 1.5f, MichaelGui.argb(a * 0.9f, INK));
                GuiDraw.line(g, x0, y0, x1, y1, Math.max(0.5f, l[5] * 0.45f), MichaelGui.argb(a * (0.6f + 0.4f * Mth.sin(t * 0.7f + l[4] * 9)), 0xFFF2C8));
            }
        }
    }
}
