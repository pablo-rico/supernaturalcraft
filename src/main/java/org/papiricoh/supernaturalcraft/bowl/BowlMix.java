package org.papiricoh.supernaturalcraft.bowl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Pure mixing rules: the colour a bowl's liquids make together, and recipe matching on liquids. */
public final class BowlMix {

    /** Colour of an empty bowl's floor showing through (unused by renderers when empty). */
    public static final int EMPTY_COLOR = 0x3A2A1A;

    private BowlMix() {
    }

    /** The average of the doses' colours (0xRRGGBB), each dose weighing the same. */
    public static int mixColor(List<Integer> colors) {
        if (colors.isEmpty()) return EMPTY_COLOR;
        long r = 0, g = 0, b = 0;
        for (int c : colors) {
            r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;
            b += c & 0xFF;
        }
        int n = colors.size();
        return (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
    }

    /** The average of the doses' colours, each counted {@code weights[i]} times. */
    public static int mixColor(List<Integer> colors, List<Integer> weights) {
        if (colors.isEmpty()) return EMPTY_COLOR;
        long r = 0, g = 0, b = 0, n = 0;
        for (int i = 0; i < colors.size(); i++) {
            int c = colors.get(i), w = Math.max(1, weights.get(i));
            r += (long) ((c >> 16) & 0xFF) * w;
            g += (long) ((c >> 8) & 0xFF) * w;
            b += (long) (c & 0xFF) * w;
            n += w;
        }
        return (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
    }

    /** Whether two lists hold the same liquids, counted, in any order. */
    public static boolean sameLiquids(List<BowlLiquid> a, List<BowlLiquid> b) {
        if (a.size() != b.size()) return false;
        return count(a).equals(count(b));
    }

    /**
     * Height of the liquid's surface above a bowl's floor (blocks) for {@code doses} doses: from
     * 1.2 px for a splash up to 4.4 px for a full bowl, just under the lip.
     */
    public static float liquidHeight(int doses) {
        float t = Math.max(0, Math.min(doses, BowlContents.MAX_DOSES)) / (float) BowlContents.MAX_DOSES;
        return (1.2f + (4.4f - 1.2f) * t) / 16f;
    }

    private static Map<BowlLiquid, Integer> count(List<BowlLiquid> list) {
        Map<BowlLiquid, Integer> m = new EnumMap<>(BowlLiquid.class);
        for (BowlLiquid l : list) m.merge(l, 1, Integer::sum);
        return m;
    }
}
