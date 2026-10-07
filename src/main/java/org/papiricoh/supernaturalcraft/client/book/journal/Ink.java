package org.papiricoh.supernaturalcraft.client.book.journal;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;

/** Small drawing helpers shared by the journal and the dashboard: headings, clipped lines, silhouettes. */
public final class Ink {

    /** Ink colour as shader colour, for silhouettes. */
    private static final float SIL_R = 0.23f, SIL_G = 0.16f, SIL_B = 0.10f;
    public static final String ELLIPSIS = "…";

    private Ink() {
    }

    /** A heading centred over a page {@code w} wide, with the flourish under it: 20 high in all. */
    public static void heading(GuiGraphics g, Font font, Component text, int x, int y, int w, int color) {
        FormattedCharSequence line = fit(font, text, w);
        g.drawString(font, line, x + (w - font.width(line)) / 2, y, color, false);
        BookAtlas.FLOURISH.draw(g, x + (w - BookAtlas.FLOURISH.w()) / 2, y + 10);
    }

    /** The text, cut short with an ellipsis if it is wider than {@code w}. */
    public static FormattedCharSequence fit(Font font, Component text, int w) {
        if (font.width(text) <= w) return text.getVisualOrderText();
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, w - font.width(ELLIPSIS)));
        return Component.literal(cut.stripTrailing() + ELLIPSIS).withStyle(text.getStyle()).getVisualOrderText();
    }

    public static void left(GuiGraphics g, Font font, Component text, int x, int y, int w, int color) {
        g.drawString(font, fit(font, text, w), x, y, color, false);
    }

    public static void centred(GuiGraphics g, Font font, Component text, int cx, int y, int w, int color) {
        FormattedCharSequence line = fit(font, text, w);
        g.drawString(font, line, cx - font.width(line) / 2, y, color, false);
    }

    public static void right(GuiGraphics g, Font font, Component text, int x1, int y, int color) {
        g.drawString(font, text, x1 - font.width(text), y, color, false);
    }

    /** Text shrunk (never grown) to fit {@code w}, centred on {@code cx}. */
    public static void centredFitted(GuiGraphics g, Font font, Component text, int cx, int y, int w, int color) {
        int tw = font.width(text);
        if (tw <= w) {
            g.drawString(font, text, cx - tw / 2, y, color, false);
            return;
        }
        float s = w / (float) tw;
        g.pose().pushPose();
        g.pose().translate(cx - w / 2f, y + (1 - s) * 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** An item icon as a flat inked shape: what a locked entry shows. */
    public static void silhouette(GuiGraphics g, ItemStack stack, int x, int y) {
        g.flush();
        RenderSystem.setShaderColor(SIL_R, SIL_G, SIL_B, 1);
        g.renderFakeItem(stack, x, y);
        g.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static ItemStack item(ResourceLocation id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    public static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** A row highlight under the mouse. */
    public static void hover(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BookStyle.HOVER);
    }

    /** A thin rule across a page. */
    public static void rule(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }
}
