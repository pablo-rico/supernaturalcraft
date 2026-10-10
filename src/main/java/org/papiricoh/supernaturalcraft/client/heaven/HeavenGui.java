package org.papiricoh.supernaturalcraft.client.heaven;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;

/**
 * Heaven's HUD drawing (v0.18): its panels from {@link HeavenGuiAtlas} when the sheet is there, a stand-in of fills in the same
 * place otherwise, and the palette every piece shares (the light of Heaven, the clinic's cyan, the office's paper).
 */
public final class HeavenGui {

    public static final ResourceLocation ATLAS = SupernaturalCraft.asResource(HeavenAssets.GUI_ATLAS);

    /** Heaven's warm white and its gold. */
    public static final int LIGHT = 0xFFF8E8, GOLD = 0xE8C46A, GOLD_DEEP = 0xA9822E, INK = 0x3A3226;
    /** The clinic: sterile white, cyan instruments, alarm red. */
    public static final int CLINIC = 0xEAF6FA, CYAN = 0x5FD0E6, ALARM = 0xE8463C;
    /** The office: paper, the carbon's grey-blue, the red of DENIED, the green of APPROVED. */
    public static final int PAPER = 0xF4EEDC, CARBON = 0x4C5870, DENIED = 0xC0392B, APPROVED = 0x3E9A4B;
    /** The bar: dark wood and brass. */
    public static final int WOOD = 0x2B1C12, WOOD_LIGHT = 0x4A3220, BRASS = 0xC9A24A;

    /** What a panel stands in for when the sheet is missing. */
    public enum Style {
        /** Heaven's own: warm white with a gold rule. */
        HEAVEN(LIGHT, GOLD, INK),
        /** Naomi's clinic: white enamel, cyan edge. */
        CLINIC(HeavenGui.CLINIC, CYAN, 0x1E3A44),
        /** Zachariah's paperwork: cream paper, carbon edge. */
        OFFICE(PAPER, CARBON, 0x2A2A30),
        /** Ash's bar: wood and brass. */
        BAR(WOOD, BRASS, 0xF0E2C0);

        public final int fill, edge, text;

        Style(int fill, int edge, int text) {
            this.fill = fill;
            this.edge = edge;
            this.text = text;
        }
    }

    private HeavenGui() {
    }

    public static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Whether the atlas can be drawn (its regions are the art's and the sheet is loaded). */
    public static boolean atlas() {
        return HeavenGuiAtlas.USE_ATLAS && GeoGuard.exists(ATLAS);
    }

    /** A panel: the atlas region as a nine-slice stretched to the rectangle, or a stand-in in {@code style}. */
    public static void panel(GuiGraphics g, HeavenGuiAtlas.Region r, int x0, int y0, int x1, int y1, Style style, float alpha) {
        if (alpha <= 0.01f) return;
        if (atlas()) {
            nineSlice(g, r, x0, y0, x1 - x0, y1 - y0, alpha);
            return;
        }
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, argb(alpha * 0.35f, 0x000000));
        g.fill(x0, y0, x1, y1, argb(alpha * 0.9f, style.fill));
        g.fill(x0, y0, x1, y0 + 1, argb(alpha, style.edge));
        g.fill(x0, y1 - 1, x1, y1, argb(alpha, style.edge));
        g.fill(x0, y0, x0 + 1, y1, argb(alpha, style.edge));
        g.fill(x1 - 1, y0, x1, y1, argb(alpha, style.edge));
        // An inner hairline, a shade lighter: the stand-in's bevel.
        g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 3, argb(alpha * 0.35f, style.edge));
    }

    /** A sprite region drawn whole at (x, y), tinted; nothing (false) if the atlas is not in use. */
    public static boolean sprite(GuiGraphics g, HeavenGuiAtlas.Region r, int x, int y, int w, int h, float alpha, int rgb) {
        if (!atlas()) return false;
        RenderSystem.enableBlend();
        g.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
        g.blit(ATLAS, x, y, w, h, r.u(), r.v(), r.w(), r.h(), HeavenGuiAtlas.SHEET_W, HeavenGuiAtlas.SHEET_H);
        g.setColor(1, 1, 1, 1);
        return true;
    }

    private static void nineSlice(GuiGraphics g, HeavenGuiAtlas.Region r, int x, int y, int w, int h, float alpha) {
        RenderSystem.enableBlend();
        g.setColor(1, 1, 1, alpha);
        int b = r.border(), s = HeavenGuiAtlas.SHEET_W, sh = HeavenGuiAtlas.SHEET_H;
        if (b <= 0) {
            g.blit(ATLAS, x, y, w, h, r.u(), r.v(), r.w(), r.h(), s, sh);
        } else {
            int mw = r.w() - 2 * b, mh = r.h() - 2 * b, iw = Math.max(0, w - 2 * b), ih = Math.max(0, h - 2 * b);
            int u = r.u(), v = r.v();
            g.blit(ATLAS, x, y, u, v, b, b, s, sh);
            g.blit(ATLAS, x + w - b, y, u + r.w() - b, v, b, b, s, sh);
            g.blit(ATLAS, x, y + h - b, u, v + r.h() - b, b, b, s, sh);
            g.blit(ATLAS, x + w - b, y + h - b, u + r.w() - b, v + r.h() - b, b, b, s, sh);
            g.blit(ATLAS, x + b, y, iw, b, u + b, v, mw, b, s, sh);
            g.blit(ATLAS, x + b, y + h - b, iw, b, u + b, v + r.h() - b, mw, b, s, sh);
            g.blit(ATLAS, x, y + b, b, ih, u, v + b, b, mh, s, sh);
            g.blit(ATLAS, x + w - b, y + b, b, ih, u + r.w() - b, v + b, b, mh, s, sh);
            g.blit(ATLAS, x + b, y + b, iw, ih, u + b, v + b, mw, mh, s, sh);
        }
        g.setColor(1, 1, 1, 1);
    }

    /** A bar filled to {@code fraction}: the atlas fill, or a flat one in {@code rgb} with a lighter top line. */
    public static void bar(GuiGraphics g, HeavenGuiAtlas.Region fill, int x0, int y0, int x1, int y1, float fraction, int rgb, float alpha) {
        int w = Math.round((x1 - x0) * Mth.clamp(fraction, 0, 1));
        if (w <= 0 || alpha <= 0.01f) return;
        if (atlas()) {
            g.enableScissor(x0, y0, x0 + w, y1);
            sprite(g, fill, x0, y0, x1 - x0, y1 - y0, alpha, 0xFFFFFF);
            g.disableScissor();
            return;
        }
        g.fill(x0, y0, x0 + w, y1, argb(alpha, rgb));
        g.fill(x0, y0, x0 + w, y0 + 1, argb(alpha * 0.6f, 0xFFFFFF));
    }

    /** {@code 1:05}, {@code 0:09}: ticks as minutes and seconds (rounded up, so the last second shows until it is over). */
    public static String clock(int ticks) {
        int s = Math.max(0, (ticks + 19) / 20);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    public static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
