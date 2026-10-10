package org.papiricoh.supernaturalcraft.client.lucifer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.io.Reader;
import java.util.Locale;

/**
 * The Cage's gothic capitals: two atlases ({@code glyphs_large}, {@code glyphs_small}) and the advances in
 * {@code glyphs.json}, all from {@code tools/artgen/lucifer_gui_art.py}. Text is drawn in capitals; a string with any
 * character the atlas lacks (another language's letters) is drawn whole in the vanilla font instead, never mixed.
 */
public final class Gothic {

    private static final ResourceLocation JSON = SupernaturalCraft.asResource("textures/gui/lucifer/glyphs.json");

    /** One atlas: its texture, cell size, columns, the cap's baseline in a cell and each glyph's advance. */
    public record Atlas(ResourceLocation texture, int cellW, int cellH, int cols, int baseline, int[] advance, int texW, int texH) {
    }

    private static boolean loaded;
    private static @Nullable String chars;
    private static @Nullable Atlas large, small;

    private Gothic() {
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(JSON);
            if (resource.isEmpty()) return;
            try (Reader reader = resource.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                String set = root.get("chars").getAsString();
                large = atlas(root.getAsJsonObject("large"), "glyphs_large", set.length());
                small = atlas(root.getAsJsonObject("small"), "glyphs_small", set.length());
                chars = set;
            }
        } catch (Exception e) {
            SupernaturalCraft.LOGGER.warn("Lucifer's glyphs could not be read; his titles fall back to the vanilla font", e);
            chars = null;
        }
    }

    private static Atlas atlas(JsonObject o, String name, int count) {
        JsonArray cell = o.getAsJsonArray("cell");
        int cw = cell.get(0).getAsInt(), ch = cell.get(1).getAsInt(), cols = o.get("cols").getAsInt();
        JsonArray adv = o.getAsJsonArray("advance");
        int[] advance = new int[count];
        for (int i = 0; i < count; i++) advance[i] = adv.get(i).getAsInt();
        int rows = (count + cols - 1) / cols;
        return new Atlas(SupernaturalCraft.asResource("textures/gui/lucifer/" + name + ".png"), cw, ch, cols, o.get("baseline").getAsInt(),
                advance, cw * cols, ch * rows);
    }

    /** Forgets the glyphs (after a resource reload). */
    public static void forget() {
        loaded = false;
        chars = null;
        large = small = null;
    }

    public static @Nullable Atlas atlas(boolean big) {
        load();
        Atlas a = big ? large : small;
        return a != null && LuciferGui.has(a.texture()) ? a : null;
    }

    /** Whether every character of {@code text} (in capitals) has a glyph. */
    public static boolean covers(String text) {
        load();
        if (chars == null || atlas(true) == null) return false;
        String upper = upper(text);
        for (int i = 0; i < upper.length(); i++) if (chars.indexOf(upper.charAt(i)) < 0) return false;
        return true;
    }

    public static String upper(String text) {
        return text.toUpperCase(Locale.ROOT);
    }

    /** The advance of one character (already in capitals), in atlas pixels; 0 for one it lacks. */
    public static int advance(char c, boolean big) {
        Atlas a = atlas(big);
        int i = chars == null ? -1 : chars.indexOf(c);
        return a == null || i < 0 ? 0 : a.advance()[i];
    }

    /** The width of {@code text} in atlas pixels (unscaled), or in vanilla font pixels if it is not covered. */
    public static int width(String text, boolean big) {
        if (!covers(text)) return Minecraft.getInstance().font.width(text);
        String upper = upper(text);
        int w = 0;
        for (int i = 0; i < upper.length(); i++) w += advance(upper.charAt(i), big);
        return w;
    }

    /**
     * One glyph, its cell's top-left at (x, y), at {@code scale}, alpha {@code a}, tinted {@code rgb}; {@code light} adds it
     * onto what is under it.
     */
    public static void glyph(GuiGraphics g, char c, float x, float y, float scale, float a, int rgb, boolean big, boolean light) {
        Atlas atlas = atlas(big);
        int i = chars == null ? -1 : chars.indexOf(c);
        if (atlas == null || i < 0 || c == ' ') return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        int u = (i % atlas.cols()) * atlas.cellW(), v = (i / atlas.cols()) * atlas.cellH();
        if (light) {
            LuciferGui.glow(g, atlas.texture(), 0, 0, atlas.cellW(), atlas.cellH(), u, v, atlas.cellW(), atlas.cellH(), atlas.texW(), atlas.texH(), a, rgb);
        } else {
            LuciferGui.blit(g, atlas.texture(), 0, 0, atlas.cellW(), atlas.cellH(), u, v, atlas.cellW(), atlas.cellH(), atlas.texW(), atlas.texH(), a, rgb);
        }
        g.pose().popPose();
    }

    /**
     * {@code text} centred on {@code cx}, the cells' top at {@code y}, with a dark shadow; in the vanilla font (scaled to the
     * same cap height) if the atlas does not cover it.
     */
    public static void centred(GuiGraphics g, String text, float cx, float y, float scale, float a, int rgb, boolean big) {
        if (!covers(text)) {
            Atlas atlas = atlas(big);
            float s = scale * (big ? 16 : 8) / 7f;
            float top = y + scale * (atlas != null ? atlas.baseline() - (big ? 16 : 8) : 0);
            GuiDraw.centred(g, Component.literal(text), cx, top, s, LuciferGui.argb(a, rgb), true);
            return;
        }
        String upper = upper(text);
        float x = cx - width(upper, big) * scale / 2f;
        for (int pass = 0; pass < 2; pass++) {
            float px = x;
            for (int i = 0; i < upper.length(); i++) {
                char c = upper.charAt(i);
                if (pass == 0) glyph(g, c, px + scale, y + scale, scale, a * 0.85f, 0x000000, big, false);
                else glyph(g, c, px, y, scale, a, rgb, big, false);
                px += advance(c, big) * scale;
            }
        }
    }
}
