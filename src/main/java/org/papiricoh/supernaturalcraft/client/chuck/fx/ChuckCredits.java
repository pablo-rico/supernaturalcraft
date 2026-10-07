package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * The two credit rolls. The fake one (chapter 4): his production, rolling smugly to "THE END", until he cuts it —
 * "...no. Not like this." — and the screen tears in two. The real one, at the very end: the hunter's story, the enemies
 * they stopped, who wrote it and who rewrote it, and "Carry on." Every line is a lang key ({@code FourthWallLang}).
 */
public final class ChuckCredits {

    static final String K = "fourth_wall.supernaturalcraft.";

    private ChuckCredits() {
    }

    /** One line of a roll: text, size, colour, space above it. */
    record Line(Component text, float scale, int rgb, int gap) {
    }

    private static Component key(String k) {
        return Component.translatable(K + k);
    }

    // --- the fake credits ------------------------------------------------------------------------------------------

    private static final String[][] FAKE_ROLES = {
            {"directed_by", "god"}, {"written_by", "carver"}, {"starring", "chuck_as_himself"}, {"and", "a_hunter"},
            {"lead_developer", "kevin"}, {"texture_artist", "becky"}, {"sound_design", "gabriel"}, {"quality_assurance", "crowley"},
            {"lore_consultant", "metatron"}, {"vehicle", "impala"}, {"catering", "biggersons"}, {"music", "wayward"}};

    public static ChuckOverlay.Moment fake(int cutAt) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(key("fake.title"), 3f, 0xF2E8D0, 0));
        lines.add(new Line(key("fake.production"), 1.2f, 0xC8B8A0, 8));
        for (String[] role : FAKE_ROLES) {
            lines.add(new Line(key("fake." + role[0]), 1f, 0x9C8F78, 22));
            lines.add(new Line(key("fake." + role[1]), 1.5f, 0xF2E8D0, 3));
        }
        lines.add(new Line(key("fake.disclaimer"), 0.9f, 0x8C806A, 30));
        lines.add(new Line(key("the_end"), 4f, 0xFFFFFF, 60));
        return new FakeRoll(lines, Math.max(120, cutAt));
    }

    /** The real credits, {@code duration} ticks long (at least 40 s). */
    public static ChuckOverlay.Moment real(int duration) {
        List<Line> lines = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        Component hunter = mc.player == null ? key("credits.a_hunter") : mc.player.getName();
        lines.add(new Line(key("credits.title"), 3f, 0xF2E8D0, 0));
        lines.add(new Line(key("credits.story"), 1.1f, 0xC8B8A0, 10));
        lines.add(new Line(hunter, 2f, 0xFFE59A, 4));
        lines.add(new Line(key("credits.defeated"), 1f, 0x9C8F78, 40));
        for (String boss : new String[]{"azazel", "lilith", "lucifer", "broken_chorus", "metatron", "amara", "lucifer_uncaged"}) {
            lines.add(new Line(Component.translatable("entity.supernaturalcraft." + boss), 1.4f, 0xF2E8D0, 6));
        }
        lines.add(new Line(key("credits.and_the_author"), 1.4f, 0xFFE59A, 10));
        lines.add(new Line(key("credits.allies"), 1f, 0x9C8F78, 40));
        lines.add(new Line(key("credits.dean"), 1.4f, 0xF2E8D0, 6));
        lines.add(new Line(key("credits.sam"), 1.4f, 0xF2E8D0, 4));
        lines.add(new Line(key("credits.castiel"), 1.4f, 0xF2E8D0, 4));
        lines.add(new Line(key("credits.written_by"), 1f, 0x9C8F78, 40));
        lines.add(new Line(key("credits.chuck"), 1.6f, 0xF2E8D0, 4));
        lines.add(new Line(key("credits.chuck_note"), 0.9f, 0x8C806A, 2));
        lines.add(new Line(key("credits.rewritten_by"), 1f, 0x9C8F78, 26));
        lines.add(new Line(hunter, 1.6f, 0xFFE59A, 4));
        lines.add(new Line(key("credits.thanks"), 1f, 0xC8B8A0, 40));
        lines.add(new Line(key("credits.mod"), 0.9f, 0x8C806A, 30));
        return new RealRoll(lines, Math.max(800, duration));
    }

    /** The base: a dark curtain and the lines rising; {@code until} is when the last line reaches the middle. */
    abstract static class Roll extends ChuckOverlay.Moment {
        final List<Line> lines;
        final int until;

        Roll(List<Line> lines, int until, int life) {
            super(life);
            this.lines = lines;
            this.until = until;
        }

        int height() {
            int hgt = 0;
            for (Line l : lines) hgt += l.gap + (int) (9 * l.scale);
            return hgt;
        }

        /** Draws the roll with its last line centred at {@code progress} = 1 (0 = its first line just below the screen). */
        void roll(GuiGraphics g, float progress, float alpha, int w, int h) {
            int total = height();
            Line last = lines.getLast();
            float endTop = h / 2f - 9 * last.scale / 2f - (total - 9 * last.scale);
            float startTop = h + 10;
            float y = Mth.lerp(Mth.clamp(progress, 0, 1), startTop, endTop);
            for (Line l : lines) {
                y += l.gap;
                float lh = 9 * l.scale;
                if (y > -lh && y < h + lh) {
                    float edge = Math.min(Mth.clamp((y + lh) / (h * 0.15f), 0, 1), Mth.clamp((h - y) / (h * 0.15f), 0, 1));
                    GuiDraw.centred(g, ChuckText.typed(l.text.getString()), w / 2f, y, l.scale, ChuckText.argb(alpha * edge, l.rgb), true);
                }
                y += lh;
            }
        }
    }

    static final class FakeRoll extends Roll {
        private final int cut;
        private final ChuckText.Typist no;

        FakeRoll(List<Line> lines, int cut) {
            super(lines, cut - 40, cut + 75);
            this.cut = cut;
            no = new ChuckText.Typist(Component.translatable(K + "not_like_this").getString(), 0.8f, cut + 4, false);
        }

        @Override
        protected void tick() {
            super.tick();
            no.tick(age);
            if (age == cut + 32) ChuckText.sound(AllSounds.CHUCK_PAGE_TEAR.get(), 0.9f, 1f);
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = time(partial);
            float tear = Mth.clamp((t - cut - 32) / 30f, 0, 1);
            float curtain = Mth.clamp(t / 15f, 0, 1) * (1 - tear);
            if (curtain <= 0.01f && tear >= 1) return;
            float progress = t / until;
            if (tear <= 0) {
                g.fill(0, 0, w, h, ChuckText.argb(0.92f * curtain, 0x050407));
                roll(g, progress, curtain, w, h);
                if (t >= cut) {
                    // He stops it: the roll freezes, dims, and he types over it.
                    g.fill(0, 0, w, h, ChuckText.argb(0.5f, 0x050407));
                    Component line = ChuckText.typedItalic(no.visible(t));
                    Component full = ChuckText.typedItalic(no.text);
                    float x = w / 2f - GuiDraw.width(full, 2f) / 2f;
                    GuiDraw.text(g, line, x, h * 0.62f, 2f, ChuckText.argb(1, 0xC8261E), true);
                }
                return;
            }
            // The tear: the black page rips along a jagged line and both halves fall away.
            float slide = tear * tear * w * 0.6f;
            int steps = 24;
            for (int half = 0; half < 2; half++) {
                for (int i = 0; i < steps; i++) {
                    int y0 = h * i / steps, y1 = h * (i + 1) / steps;
                    float jag = w / 2f + ((i * 7919) % 23 - 11) * 1.4f;
                    int x0 = half == 0 ? 0 : (int) jag, x1 = half == 0 ? (int) jag : w;
                    int dx = (int) (half == 0 ? -slide : slide);
                    int dy = (int) (tear * tear * h * 0.15f * (half == 0 ? 1 : -1));
                    g.fill(x0 + dx, y0 + dy, x1 + dx, y1 + dy, ChuckText.argb(0.92f * (1 - tear), 0x050407));
                    int ex = half == 0 ? x1 + dx - 1 : x0 + dx;
                    g.fill(ex, y0 + dy, ex + 1, y1 + dy, ChuckText.argb(0.8f * (1 - tear), ChuckText.PAPER));
                }
            }
        }
    }

    static final class RealRoll extends Roll {
        private final ChuckText.Typist carry;

        RealRoll(List<Line> lines, int duration) {
            super(lines, duration - 160, duration);
            carry = new ChuckText.Typist(Component.translatable(K + "credits.carry_on").getString(), 0.35f, duration - 120, true);
        }

        @Override
        protected void tick() {
            super.tick();
            carry.tick(age);
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = time(partial);
            float curtain = fade(partial, 40, 40);
            g.fill(0, 0, w, h, ChuckText.argb(0.78f * curtain, 0x050407));
            float rollOut = Mth.clamp(1 - (t - until) / 30f, 0, 1);
            roll(g, t / until, curtain * rollOut, w, h);
            if (t > life - 130) {
                Component full = ChuckText.typed(carry.text);
                float x = w / 2f - GuiDraw.width(full, 3f) / 2f;
                GuiDraw.text(g, ChuckText.typed(carry.visible(t)), x, h / 2f - 13, 3f, ChuckText.argb(curtain, 0xF2E8D0), true);
            }
        }
    }
}
