package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorRules;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Author's words on the screen: chapter title cards, his narration, rules written across the sky, the cracks when
 * his script breaks, the backspace smear, the snap's countdown, and the credits (both). Each is a {@link Moment} that
 * ticks and draws itself until it is over.
 */
public final class ChuckOverlay implements LayeredDraw.Layer {

    private static final List<Moment> MOMENTS = new ArrayList<>();
    private static Narration narration;
    private static int lastRules;

    /** Something on screen for a while. */
    public abstract static class Moment {
        protected int age;
        protected final int life;

        protected Moment(int life) {
            this.life = Math.max(1, life);
        }

        protected void tick() {
            age++;
        }

        protected boolean over() {
            return age >= life;
        }

        protected float time(float partial) {
            return age + partial;
        }

        /** 0-1: in over {@code in} ticks, out over the last {@code out}. */
        protected float fade(float partial, int in, int out) {
            float t = time(partial);
            return Mth.clamp(Math.min(t / Math.max(1, in), (life - t) / Math.max(1, out)), 0, 1);
        }

        protected abstract void render(GuiGraphics g, float partial, int w, int h);
    }

    public static void add(Moment m) {
        MOMENTS.add(m);
    }

    /** Whether any credits roll right now (the HUD keeps quiet). */
    public static boolean creditsRolling() {
        return MOMENTS.stream().anyMatch(m -> m instanceof ChuckCredits.Roll);
    }

    public static void narrate(String key, int duration) {
        if (narration != null) MOMENTS.remove(narration);
        narration = new Narration(Component.translatable(key).getString(), Math.max(60, duration));
        MOMENTS.add(narration);
    }

    /** The rules changed to {@code mask}: each newly written rule gets its line. */
    public static void rules(int mask, int duration) {
        int fresh = mask & ~lastRules;
        lastRules = mask;
        int row = 0;
        for (int rule : AuthorRules.ALL) {
            if (AuthorRules.has(fresh, rule)) MOMENTS.add(new RuleLine(Component.translatable(AuthorRules.key(rule)).getString(), row++));
        }
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MOMENTS.clear();
            narration = null;
            lastRules = 0;
            return;
        }
        for (Moment m : List.copyOf(MOMENTS)) {
            m.tick();
            if (m.over()) MOMENTS.remove(m);
        }
        if (narration != null && narration.over()) narration = null;
    }

    static void clear() {
        MOMENTS.clear();
        narration = null;
        lastRules = 0;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (MOMENTS.isEmpty()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        for (Moment m : List.copyOf(MOMENTS)) m.render(g, partial, w, h);
    }

    // --- chapter titles --------------------------------------------------------------------------------------------

    /** "Chapter Three" typed on a strip of paper across the screen, then the title, big; the bell; it fades. */
    public static final class TitleCard extends Moment {
        private final ChuckText.Typist number, title;
        private final boolean blank;

        public TitleCard(Chapter chapter) {
            super(0);
            number = new ChuckText.Typist(Component.translatable(chapter.titleKey()).getString(), 0.7f, 8, false);
            title = new ChuckText.Typist(Component.translatable(chapter.titleKey() + ".title").getString(), 0.6f, number.length() + 6, true);
            blank = chapter == Chapter.BLANK;
        }

        @Override
        protected boolean over() {
            return age >= title.length() + 70;
        }

        @Override
        protected void tick() {
            super.tick();
            number.tick(age);
            title.tick(age);
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            int total = title.length() + 70;
            float t = time(partial);
            float a = Mth.clamp(Math.min(t / 8, (total - t) / 20), 0, 1);
            if (a <= 0.01f) return;
            int cy = (int) (h * 0.3f), half = 36;
            GuiDraw.paper(g, 0, cy - half, w, cy + half, a, blank ? 0xFFFFFF : ChuckText.PAPER);
            int ink = ChuckText.argb(a, ChuckText.INK);
            Component full = ChuckText.typed(number.text), shown = ChuckText.typed(number.visible(t));
            float x = w / 2f - GuiDraw.width(full, 1.5f) / 2f;
            GuiDraw.text(g, shown, x, cy - half + 9, 1.5f, ink, false);
            Component fullTitle = ChuckText.typed(title.text), shownTitle = ChuckText.typed(title.visible(t));
            float scale = Math.min(3f, (w - 40) / (float) Math.max(1, GuiDraw.width(fullTitle, 1f)));
            float tx = w / 2f - GuiDraw.width(fullTitle, scale) / 2f;
            GuiDraw.text(g, shownTitle, tx, cy - 4, scale, ink, false);
            // The cursor, blinking where the next letter will land.
            if (((int) t / 6) % 2 == 0) {
                boolean onTitle = number.done(t);
                Component sofar = onTitle ? shownTitle : shown;
                float s = onTitle ? scale : 1.5f;
                float cx = (onTitle ? tx : x) + GuiDraw.width(sofar, s);
                float cyl = onTitle ? cy - 4 + 8 * s : cy - half + 9 + 8 * s;
                g.fill((int) cx, (int) cyl, (int) (cx + 5 * s), (int) (cyl + Math.max(1, s)), ink);
            }
        }
    }

    // --- narration -------------------------------------------------------------------------------------------------

    /** His voice: an italic typed line on a slip of paper above the hotbar. */
    static final class Narration extends Moment {
        private final ChuckText.Typist line;

        Narration(String text, int life) {
            super(life);
            line = new ChuckText.Typist(text, 1.1f, 2, false);
        }

        @Override
        protected void tick() {
            super.tick();
            line.tick(age);
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = fade(partial, 5, 15);
            if (a <= 0.01f) return;
            float t = time(partial);
            Component full = ChuckText.typedItalic(line.text);
            float scale = Math.min(1.25f, (w - 30) / (float) Math.max(1, GuiDraw.width(full, 1f)));
            int tw = GuiDraw.width(full, scale);
            int y = h - 84;
            int x0 = (w - tw) / 2 - 10, x1 = (w + tw) / 2 + 10;
            g.fill(x0, y - 5, x1, y + (int) (9 * scale) + 4, ChuckText.argb(a * 0.8f, ChuckText.PAPER));
            g.fill(x0, y + (int) (9 * scale) + 4, x1, y + (int) (9 * scale) + 5, ChuckText.argb(a * 0.35f, 0x000000));
            GuiDraw.text(g, ChuckText.typedItalic(line.visible(t)), (w - tw) / 2f, y, scale, ChuckText.argb(a, ChuckText.INK), false);
        }
    }

    // --- rules -----------------------------------------------------------------------------------------------------

    /** A rule rewritten: typed fast in red ink high on the screen, underlined by a stroke of the pen. */
    static final class RuleLine extends Moment {
        private final ChuckText.Typist line;
        private final int row;

        RuleLine(String text, int row) {
            super(110);
            line = new ChuckText.Typist(text, 1.6f, row * 12, false);
            this.row = row;
        }

        @Override
        protected void tick() {
            super.tick();
            line.tick(age);
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float a = fade(partial, 3, 25);
            if (a <= 0.01f) return;
            float t = time(partial);
            Component full = ChuckText.typedItalic(line.text);
            float scale = Math.min(2f, (w - 40) / (float) Math.max(1, GuiDraw.width(full, 1f)));
            int tw = GuiDraw.width(full, scale);
            float x = (w - tw) / 2f, y = h * 0.16f + row * 26;
            GuiDraw.text(g, ChuckText.typedItalic(line.visible(t)), x + 1, y + 1, scale, ChuckText.argb(a * 0.5f, 0x000000), false);
            GuiDraw.text(g, ChuckText.typedItalic(line.visible(t)), x, y, scale, ChuckText.argb(a, 0xC8261E), false);
            float under = Mth.clamp((t - line.length()) / 10f, 0, 1);
            if (under > 0) {
                float uy = y + 9 * scale + 2;
                GuiDraw.line(g, x - 4, uy + 1, x - 4 + (tw + 8) * under, uy - 1, 1.6f, ChuckText.argb(a * 0.9f, 0xC8261E));
            }
        }
    }

    // --- cracks ----------------------------------------------------------------------------------------------------

    /** The script breaks: white cracks across the screen from one point, a flash, then they fade. */
    public static final class Cracks extends Moment {
        private final List<float[]> segments = new ArrayList<>();

        public Cracks(int duration) {
            super(Mth.clamp(duration / 2, 30, 70));
            Random rnd = new Random();
            float ox = 0.3f + rnd.nextFloat() * 0.4f, oy = 0.3f + rnd.nextFloat() * 0.4f;
            int branches = 6 + rnd.nextInt(4);
            for (int b = 0; b < branches; b++) {
                float a = Mth.TWO_PI * b / branches + (rnd.nextFloat() - 0.5f) * 0.6f;
                grow(rnd, ox, oy, a, 0.9f, 0);
            }
        }

        private void grow(Random rnd, float x, float y, float a, float reach, int depth) {
            int steps = 5 + rnd.nextInt(4);
            for (int s = 0; s < steps; s++) {
                a += (rnd.nextFloat() - 0.5f) * 0.9f;
                float len = reach / steps * (0.6f + rnd.nextFloat() * 0.8f);
                float nx = x + Mth.cos(a) * len, ny = y + Mth.sin(a) * len;
                segments.add(new float[]{x, y, nx, ny, depth});
                if (depth < 2 && rnd.nextFloat() < 0.22f) grow(rnd, nx, ny, a + (rnd.nextBoolean() ? 0.8f : -0.8f), reach * 0.4f, depth + 1);
                x = nx;
                y = ny;
            }
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = time(partial);
            float flash = Math.max(0, 1 - t / 5f);
            if (flash > 0) g.fill(0, 0, w, h, ChuckText.argb(flash * 0.55f, 0xFFFFFF));
            float a = fade(partial, 1, life * 2 / 3);
            float grow = Mth.clamp(t / 4f, 0, 1);
            for (float[] s : segments) {
                float x0 = s[0] * w, y0 = s[1] * h;
                float x1 = x0 + (s[2] - s[0]) * w * grow, y1 = y0 + (s[3] - s[1]) * h * grow;
                float width = 2.4f - s[4] * 0.7f;
                GuiDraw.line(g, x0, y0, x1, y1, width * 3, ChuckText.argb(a * 0.18f, 0xFFF3C4));
                GuiDraw.line(g, x0, y0, x1, y1, width, ChuckText.argb(a, 0xFFFFFF));
            }
        }
    }

    // --- backspace -------------------------------------------------------------------------------------------------

    /** Pulled back through the page: streaks rushing right to left, and a line of letters deleting itself. */
    public static final class Smear extends Moment {
        private final long seed = new Random().nextLong();

        public Smear() {
            super(20);
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            float t = time(partial), a = fade(partial, 1, 12);
            Random rnd = new Random(seed);
            for (int i = 0; i < 26; i++) {
                int y = rnd.nextInt(h), bh = 1 + rnd.nextInt(4);
                float len = w * (0.2f + rnd.nextFloat() * 0.5f);
                float x = w - ((t * (25 + rnd.nextInt(30)) + rnd.nextInt(w)) % (w + len));
                g.fill((int) x, y, (int) (x + len), y + bh, ChuckText.argb(a * (0.25f + rnd.nextFloat() * 0.3f), rnd.nextBoolean() ? 0x1E1A3B : 0xF2E8D0));
            }
            String letters = Component.translatable("fourth_wall.supernaturalcraft.backspace").getString();
            int keep = Math.max(0, letters.length() - (int) (t * 2.2f));
            Component line = ChuckText.typed(letters.substring(0, keep) + "_");
            GuiDraw.centred(g, line, w / 2f, h * 0.62f, 1.4f, ChuckText.argb(a, ChuckText.PAPER), true);
        }
    }

    // --- the snap --------------------------------------------------------------------------------------------------

    /** While the local hunter stands inside a snap's frame: the count, big, and the edges of sight going red. */
    public static final class SnapWarning extends Moment {
        private final Vec3 point;
        private final float radius;

        public SnapWarning(Vec3 point, float radius, int duration) {
            super(duration);
            this.point = point;
            this.radius = radius;
        }

        @Override
        protected void render(GuiGraphics g, float partial, int w, int h) {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            Vec3 p = player.position();
            if (Math.abs(p.x - point.x) > radius || Math.abs(p.z - point.z) > radius) return;
            float left = (life - time(partial)) / 20f;
            int secs = Math.max(1, (int) Math.ceil(left));
            float pulse = 1 - (left - (float) Math.floor(left));
            int edge = (int) (Math.min(w, h) * 0.12f);
            int red = ChuckText.argb(0.25f + 0.25f * pulse, 0xC8261E);
            g.fillGradient(0, 0, w, edge, red, 0);
            g.fillGradient(0, h - edge, w, h, 0, red);
            GuiDraw.centred(g, ChuckText.typed(String.valueOf(secs)), w / 2f, h * 0.3f - 12 * (1 + pulse * 0.4f), 3f + pulse * 1.2f,
                    ChuckText.argb(0.9f, 0xC8261E), true);
        }
    }
}
