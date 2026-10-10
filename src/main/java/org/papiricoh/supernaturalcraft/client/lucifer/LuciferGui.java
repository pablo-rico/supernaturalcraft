package org.papiricoh.supernaturalcraft.client.lucifer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;

/**
 * The Cage's HUD textures ({@code textures/gui/lucifer/}, laid out by {@code tools/artgen/lucifer_gui_art.py}) and the look
 * of each phase of both Lucifers: the fill that runs in the bar, the colour of the runes, of his name and of his titles,
 * and the motif at the heart of each title's seal.
 */
public final class LuciferGui {

    public static final ResourceLocation BAR_FRAME = tex("bar_frame"), BAR_RUNES = tex("bar_runes"), BAR_BARS = tex("bar_bars"),
            DEBRIS = tex("debris"), TITLE_SEAL = tex("title_seal"), TITLE_MOTIFS = tex("title_motifs"), TITLE_CHAIN = tex("title_chain");

    /** The frame and its fill's rect (the fill sits where the vanilla bar would); one cage bar's cell. */
    public static final int FRAME_W = 256, FRAME_H = 32, FILL_X = 24, FILL_Y = 12, FILL_W = 208, FILL_H = 8, BAR_W = 12, BAR_H = 32;
    /** Fill sheets: 8 frames of {@link #FILL_W}×{@link #FILL_H}. */
    public static final int FILL_FRAMES = 8;

    /**
     * One phase's look.
     *
     * @param fill  the fill sheet ({@code fill_<name>.png})
     * @param rune  the colour the runes and the medallion glow
     * @param name  the colour of his name over the bar
     * @param title the colour of the title card's seal, motif and letters
     */
    public record Style(String fill, int rune, int name, int title) {
    }

    private static final Style[] LUCIFER = {
            new Style("hellfire", 0xFF3B1F, 0xFF5A3A, 0xFF3B1F),
            new Style("embers", 0xFF7A2A, 0xFF8A3A, 0xFF6A1F),
            new Style("frost", 0x8FD8FF, 0xBFEFFF, 0x9FD8F0),
            new Style("light", 0xFFF3C4, 0xFFF3C4, 0xFFE8A0),
    };
    private static final Style[] UNCAGED = {
            new Style("chains", 0xC41A12, 0xE8452A, 0xC41A12),
            new Style("hellfire", 0xFF5A14, 0xFF7A2A, 0xFF4A12),
            new Style("frost", 0xBFEFFF, 0xBFEFFF, 0x9FD8F0),
            new Style("legion", 0xFF2A2A, 0xFF4A5A, 0xD01A2A),
            new Style("star", 0xFFE08A, 0xFFE08A, 0xFFD060),
            new Style("light", 0xFFFFFF, 0xFFF8E0, 0xFFF3C4),
    };
    /** The motif (cell of {@code title_motifs.png}) at the heart of each title: intro, phases 2.., victory last. */
    private static final int[] LUCIFER_MOTIFS = {0, 0, 1, 2, 3}, UNCAGED_MOTIFS = {4, 4, 5, 6, 7, 8, 9};
    public static final int MOTIF_PADLOCK = 10;

    private LuciferGui() {
    }

    private static ResourceLocation tex(String name) {
        return SupernaturalCraft.asResource("textures/gui/lucifer/" + name + ".png");
    }

    public static ResourceLocation fill(Style style) {
        return tex("fill_" + style.fill());
    }

    public static int maxPhase(boolean uncaged) {
        return (uncaged ? UNCAGED : LUCIFER).length;
    }

    public static Style style(boolean uncaged, int phase) {
        Style[] table = uncaged ? UNCAGED : LUCIFER;
        return table[Math.max(1, Math.min(table.length, phase)) - 1];
    }

    /** The cell of {@code title_motifs.png} for a title card ({@link LuciferFxPayload#TITLE_INTRO}, a phase or the victory). */
    public static int motif(boolean uncaged, int which) {
        if (which == LuciferFxPayload.TITLE_VICTORY) return MOTIF_PADLOCK;
        int[] table = uncaged ? UNCAGED_MOTIFS : LUCIFER_MOTIFS;
        return table[Math.max(0, Math.min(table.length - 1, which))];
    }

    /** The lang key of a title card, without its {@code .title}/{@code .subtitle}. */
    public static String titleKey(boolean uncaged, int which) {
        String base = uncaged ? "cinematic.supernaturalcraft.uncaged" : "cinematic.supernaturalcraft";
        if (which == LuciferFxPayload.TITLE_INTRO) return uncaged ? base : base + ".emerge";
        if (which == LuciferFxPayload.TITLE_VICTORY) return base + ".victory";
        return base + ".phase" + which;
    }

    /** The phase of cold (the third, for either Lucifer), where blows strike ice off the bar instead of embers. */
    public static boolean cold(int phase) {
        return phase == 3;
    }

    /** The centre x (in the frame) of cage bar {@code i}. */
    public static float barX(int i) {
        return FILL_X + FILL_W * (i + 0.5f) / CageBars.COUNT;
    }

    public static boolean has(ResourceLocation texture) {
        return MichaelGui.has(texture);
    }

    public static int argb(float alpha, int rgb) {
        return MichaelGui.argb(alpha, rgb);
    }

    public static int lerp(float k, int a, int b) {
        k = Math.max(0, Math.min(1, k));
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * k);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * k);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * k);
        return (r << 16) | (g << 8) | bl;
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                            int tw, int th, float a, int rgb) {
        if (a > 0.004f) MichaelGui.blit(g, texture, x, y, w, h, u, v, uw, vh, tw, th, a, rgb);
    }

    /** As {@link #blit}, added onto what is under it (light). */
    public static void glow(GuiGraphics g, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                            int tw, int th, float a, int rgb) {
        if (a > 0.004f) MichaelGui.glow(g, texture, x, y, w, h, u, v, uw, vh, tw, th, a, rgb);
    }

    public static String roman(int n) {
        return MichaelGui.roman(n);
    }

    /** Ease out with a bounce at the end: bars slamming down. */
    public static float bounce(float k) {
        k = Math.max(0, Math.min(1, k));
        if (k < 0.7f) return (k / 0.7f) * (k / 0.7f);
        float b = (k - 0.85f) / 0.15f;
        return 1 - 0.12f * (1 - b * b);
    }
}
