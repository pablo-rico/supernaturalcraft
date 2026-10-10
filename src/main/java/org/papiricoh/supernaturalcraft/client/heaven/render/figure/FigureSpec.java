package org.papiricoh.supernaturalcraft.client.heaven.render.figure;

/**
 * What to draw for a figure (v0.18): a memory's or a training copy's. {@code figure} follows the scenes' convention
 * ({@code memory.scenes.Figure}): an entity type id ({@code minecraft:wolf}, {@code supernaturalcraft:lucifer}), {@code @owner}
 * (the viewer, in their own skin) or {@code @ally:dean|sam|castiel|bobby}; the client also knows {@code @rival:0-2} (a rival
 * hunter's look, Naomi's hostile copies).
 *
 * @param figure who
 * @param pose   a {@link FigurePose} name
 * @param scale  size (1 = natural)
 * @param argb   the colour it is washed in (white = none; alpha below 255 makes it translucent where its renderer allows)
 * @param glow   the least block light it is lit with (0-15)
 * @param armed  a hostile copy shows its weapon
 */
public record FigureSpec(String figure, String pose, float scale, int argb, int glow, boolean armed) {

    public static FigureSpec of(String figure, String pose) {
        return new FigureSpec(figure, pose, 1f, 0xFFFFFFFF, 0, false);
    }

    public boolean owner() {
        return figure.equals("@owner");
    }

    /** {@code @ally:dean} → {@code dean}; null if not an ally. */
    public String ally() {
        return figure.startsWith("@ally:") ? figure.substring(6) : null;
    }

    /** {@code @rival:1} → 1; -1 if not a rival hunter's look. */
    public int rival() {
        if (!figure.startsWith("@rival")) return -1;
        try {
            return figure.length() > 7 ? Integer.parseInt(figure.substring(7)) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
