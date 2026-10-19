package org.papiricoh.supernaturalcraft.client.abaddon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

/**
 * The Brand of the Knights as strokes (v0.21), the seal on Abaddon's palm ({@code tools/artgen/abaddon_art.py} {@code sigil}): a
 * ring, three spokes from its heart at thirds of a turn, and a blade straight down through it. One shape for every place the code
 * draws it: over a marked hunter's head, burnt into her hall's floor by the Brand, stamped on a banished knight, on her bar and on
 * her title cards.
 *
 * <p>Each stroke is a polyline in a square box, x and y in -0.5..0.5 (y down, as on a page).
 */
public final class KnightsSeal {

    private static final int RING_STEPS = 28;
    private static final float RING = 0.46f, SPOKE = 0.36f, BLADE = 0.5f;
    public static final float[][] STROKES;

    static {
        float[] ring = new float[(RING_STEPS + 1) * 2];
        for (int i = 0; i <= RING_STEPS; i++) {
            float a = -Mth.HALF_PI + Mth.TWO_PI * i / RING_STEPS;
            ring[i * 2] = Mth.cos(a) * RING;
            ring[i * 2 + 1] = Mth.sin(a) * RING;
        }
        float[][] spokes = new float[3][];
        for (int k = 0; k < 3; k++) {
            // The art's spokes run where sin(1.5 a) is 0: right, and a third of a turn either way.
            float a = k * Mth.TWO_PI / 3;
            spokes[k] = new float[]{0, 0, Mth.cos(a) * SPOKE, Mth.sin(a) * SPOKE};
        }
        // The blade: a long stroke down through the heart, a short guard across its top.
        float[] blade = {0, -BLADE, 0, BLADE * 0.9f};
        float[] guard = {-0.13f, -0.28f, 0.13f, -0.28f};
        STROKES = new float[][]{ring, blade, guard, spokes[0], spokes[1], spokes[2]};
    }

    private KnightsSeal() {
    }

    /** Receives one straight piece of the seal, in box units. */
    @FunctionalInterface
    public interface Segment {
        void accept(float x0, float y0, float x1, float y1);
    }

    public static float length() {
        float total = 0;
        for (float[] s : STROKES) {
            for (int i = 2; i < s.length; i += 2) total += segment(s, i);
        }
        return total;
    }

    private static float segment(float[] s, int i) {
        float dx = s[i] - s[i - 2], dy = s[i + 1] - s[i - 1];
        return Mth.sqrt(dx * dx + dy * dy);
    }

    /** Walks the seal's pieces in drawing order, only the first {@code fraction} of its length (0-1): the brand burning in. */
    public static void walk(float fraction, Segment out) {
        float left = Mth.clamp(fraction, 0, 1) * length();
        for (float[] s : STROKES) {
            for (int i = 2; i < s.length; i += 2) {
                if (left <= 0) return;
                float len = segment(s, i);
                float k = len <= left ? 1 : left / len;
                out.accept(s[i - 2], s[i - 1], s[i - 2] + (s[i] - s[i - 2]) * k, s[i - 1] + (s[i + 1] - s[i - 1]) * k);
                left -= len;
            }
        }
    }

    /** The seal on the screen, {@code size} pixels across, centred on (cx, cy): a dark under-stroke and a coloured one. */
    public static void gui(GuiGraphics g, float cx, float cy, float size, float fraction, float width, int argb, int underArgb) {
        if ((underArgb >>> 24) > 0) {
            walk(fraction, (x0, y0, x1, y1) -> GuiDraw.line(g, cx + x0 * size, cy + y0 * size, cx + x1 * size, cy + y1 * size,
                    width * 2.2f, underArgb));
        }
        walk(fraction, (x0, y0, x1, y1) -> GuiDraw.line(g, cx + x0 * size, cy + y0 * size, cx + x1 * size, cy + y1 * size, width, argb));
    }
}
