package org.papiricoh.supernaturalcraft.client.chuck.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Typewritten paper for the Author's screens, drawn without a texture: a cream sheet with grain, a soft shadow and a
 * dog-eared corner, and text struck letter by letter in the typewriter font (the mod's own if the art ships one,
 * Minecraft's monospaced {@code uniform} otherwise), each letter a touch darker or lighter, as keys strike.
 */
final class Paper {

    static final int PAPER = 0xFFF1E9D6, PAPER_EDGE = 0xFFE0D4B8, SHADOW = 0x55000000, GRAIN = 0x14000000;
    static final int INK = 0xFF1C1814, INK_LIGHT = 0xFF3A322A, FADED = 0xFF8A7E6A, RED = 0xFF8A1A10;
    private static final ResourceLocation OWN_FONT = SupernaturalCraft.asResource("typewriter");
    private static final ResourceLocation UNIFORM = ResourceLocation.withDefaultNamespace("uniform");
    private static ResourceLocation font;

    private Paper() {
    }

    static Style style() {
        if (font == null) {
            boolean own = Minecraft.getInstance().getResourceManager().getResource(SupernaturalCraft.asResource("font/typewriter.json")).isPresent();
            font = own ? OWN_FONT : UNIFORM;
        }
        return Style.EMPTY.withFont(font);
    }

    /** A sheet at (x, y), w × h, in the current pose. */
    static void sheet(GuiGraphics g, int x, int y, int w, int h, long seed) {
        g.fill(x + 3, y + 4, x + w + 3, y + h + 4, SHADOW);
        g.fill(x, y, x + w, y + h, PAPER);
        g.fill(x, y, x + w, y + 1, PAPER_EDGE);
        g.fill(x, y + h - 1, x + w, y + h, PAPER_EDGE);
        g.fill(x, y, x + 1, y + h, PAPER_EDGE);
        g.fill(x + w - 1, y, x + w, y + h, PAPER_EDGE);
        // Grain: specks scattered by a fixed hash, the same every frame.
        long s = seed * 0x9E3779B97F4A7C15L;
        int specks = w * h / 90;
        for (int i = 0; i < specks; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int px = x + 2 + (int) ((s >>> 33) % (w - 4));
            int py = y + 2 + (int) ((s >>> 13) % (h - 4));
            g.fill(px, py, px + 1, py + 1, GRAIN);
        }
        // The dog-eared corner (bottom right).
        for (int i = 0; i < 8; i++) {
            g.fill(x + w - 8 + i, y + h - 1 - i, x + w, y + h - i, i == 0 ? PAPER_EDGE : 0xFFD9CCAE);
        }
        // A faint ring where a glass stood.
        ring(g, x + w - 30, y + 26, 13, 0x10603010);
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int a = 0; a < 90; a++) {
            if (a % 7 == 3) continue;
            double t = a * Math.PI * 2 / 90;
            int px = cx + (int) Math.round(Math.cos(t) * r), py = cy + (int) Math.round(Math.sin(t) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** {@code text} wrapped to {@code width} in the typewriter font. */
    static List<String> wrap(Font font, String text, int width) {
        List<String> out = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) {
                out.add("");
                continue;
            }
            for (FormattedText line : font.getSplitter().splitLines(para, width, style())) out.add(line.getString());
        }
        return out;
    }

    /** Strikes {@code line} letter by letter at (x, y); only the first {@code shown} letters. @return the x after it */
    static int type(GuiGraphics g, Font font, String line, int x, int y, int shown, int color) {
        Style st = style();
        int cx = x;
        for (int i = 0; i < line.length() && i < shown; i++) {
            String c = String.valueOf(line.charAt(i));
            int h = (line.hashCode() * 31 + i * 7919) & 0xFF;
            int ink = color == INK ? (h % 5 == 0 ? INK_LIGHT : INK) : color;
            g.drawString(font, Component.literal(c).withStyle(st), cx, y + (h % 11 == 0 ? 1 : 0), ink, false);
            cx += font.width(Component.literal(c).withStyle(st));
        }
        return cx;
    }

    static int width(Font font, String s) {
        return font.width(Component.literal(s).withStyle(style()));
    }

    /** The blinking block cursor. */
    static void cursor(GuiGraphics g, int x, int y, int lineHeight) {
        if ((System.currentTimeMillis() / 450) % 2 == 0) g.fill(x, y, x + 4, y + lineHeight - 1, INK);
    }

    static void sound(SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }
}
