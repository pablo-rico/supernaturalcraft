package org.papiricoh.supernaturalcraft.client.mark;

import net.minecraft.client.gui.GuiGraphics;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

/**
 * The Mark of Cain as strokes (v0.21), the same rune as his rig's ({@code tools/artgen/cain_art.py} {@code MARK_RUNE}, 7 × 11): a
 * hook at the top curling down into a stem, a bar across it, and the stem sweeping out to the right at its foot. One shape for
 * every place it is drawn by code: on the ground (his flare), hovering (the offer), on a bearer's forearm and on the HUD.
 *
 * <p>Each stroke is a polyline in the glyph's box: x 0-1 left to right, y 0-1 top to bottom; the box is {@link #ASPECT} as wide as
 * it is tall.
 */
public final class MarkGlyph {

    /** Width over height of the rune's box. */
    public static final float ASPECT = 7f / 11f;

    /** The strokes, in texel units of the 7 × 11 rune (converted to 0-1 below). */
    private static final float[][] RAW = {
            // the hook: up from the left, over the top, down and back in to the stem
            {1.0f, 1.9f, 1.6f, 0.8f, 3.0f, 0.4f, 4.6f, 0.7f, 5.4f, 1.6f, 5.2f, 2.6f, 4.2f, 3.3f, 3.0f, 4.0f},
            // the bar across
            {1.0f, 5.5f, 6.0f, 5.5f},
            // the stem, sweeping out to the right at its foot
            {3.0f, 4.0f, 2.6f, 5.5f, 2.6f, 7.6f, 3.2f, 8.6f, 4.4f, 9.4f, 5.6f, 9.9f, 6.6f, 10.7f},
    };

    public static final float[][] STROKES = new float[RAW.length][];

    static {
        for (int s = 0; s < RAW.length; s++) {
            STROKES[s] = new float[RAW[s].length];
            for (int i = 0; i < RAW[s].length; i += 2) {
                STROKES[s][i] = RAW[s][i] / 7f;
                STROKES[s][i + 1] = RAW[s][i + 1] / 11f;
            }
        }
    }

    private MarkGlyph() {
    }

    /** Total length of every stroke (in box units, x scaled by {@link #ASPECT}), for drawing it on bit by bit. */
    public static float length() {
        float total = 0;
        for (float[] s : STROKES) {
            for (int i = 2; i < s.length; i += 2) total += segment(s, i);
        }
        return total;
    }

    private static float segment(float[] s, int i) {
        float dx = (s[i] - s[i - 2]) * ASPECT, dy = s[i + 1] - s[i - 1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** Receives one straight piece of the rune, in box units. */
    @FunctionalInterface
    public interface Segment {
        void accept(float x0, float y0, float x1, float y1);
    }

    /**
     * Walks the rune's pieces in drawing order, only the first {@code fraction} of its length (0-1): the rune being burnt in. The
     * piece where it stops is cut short.
     */
    public static void walk(float fraction, Segment out) {
        float left = Math.max(0, Math.min(1, fraction)) * length();
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

    /** The rune on the screen, {@code height} pixels tall with its top-left at (x, y): a dark under-stroke and a coloured one. */
    public static void gui(GuiGraphics g, float x, float y, float height, float fraction, float width, int argb, int underArgb) {
        float w = height * ASPECT;
        if ((underArgb >>> 24) > 0) {
            walk(fraction, (x0, y0, x1, y1) -> GuiDraw.line(g, x + x0 * w, y + y0 * height, x + x1 * w, y + y1 * height, width * 2.2f,
                    underArgb));
        }
        walk(fraction, (x0, y0, x1, y1) -> GuiDraw.line(g, x + x0 * w, y + y0 * height, x + x1 * w, y + y1 * height, width, argb));
    }
}
