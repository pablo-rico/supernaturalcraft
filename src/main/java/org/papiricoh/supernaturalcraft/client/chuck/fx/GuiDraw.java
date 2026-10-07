package org.papiricoh.supernaturalcraft.client.chuck.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Screen-space drawing the vanilla GUI lacks: slanted strokes, scaled centred text. */
public final class GuiDraw {

    private GuiDraw() {
    }

    /** A stroke from (x0,y0) to (x1,y1), {@code width} GUI pixels wide (both windings, so it never culls away). */
    public static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0, dy = y1 - y0, len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1e-3f) return;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        vc.addVertex(m, x0 + nx, y0 + ny, 0).setColor(argb);
        vc.addVertex(m, x0 - nx, y0 - ny, 0).setColor(argb);
        vc.addVertex(m, x1 - nx, y1 - ny, 0).setColor(argb);
        vc.addVertex(m, x1 + nx, y1 + ny, 0).setColor(argb);
        vc.addVertex(m, x1 + nx, y1 + ny, 0).setColor(argb);
        vc.addVertex(m, x1 - nx, y1 - ny, 0).setColor(argb);
        vc.addVertex(m, x0 - nx, y0 - ny, 0).setColor(argb);
        vc.addVertex(m, x0 + nx, y0 + ny, 0).setColor(argb);
        g.flush();
    }

    /** {@code text} at {@code scale}, its left edge at {@code x} (unscaled GUI pixels), top at {@code y}. */
    public static void text(GuiGraphics g, Component text, float x, float y, float scale, int argb, boolean shadow) {
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, argb, shadow);
        g.pose().popPose();
    }

    public static int width(Component text, float scale) {
        return Math.round(Minecraft.getInstance().font.width(text) * scale);
    }

    /** {@code text} centred on {@code cx}. */
    public static void centred(GuiGraphics g, Component text, float cx, float y, float scale, int argb, boolean shadow) {
        text(g, text, cx - Minecraft.getInstance().font.width(text) * scale / 2f, y, scale, argb, shadow);
    }

    /** A sheet of paper behind text: cream, with faint blue rules and a red margin. */
    public static void paper(GuiGraphics g, int x0, int y0, int x1, int y1, float alpha, int paperRgb) {
        if (alpha <= 0.01f) return;
        g.fill(x0, y0, x1, y1, ChuckText.argb(alpha * 0.86f, paperRgb));
        for (int y = y0 + 10; y < y1 - 2; y += 11) g.fill(x0, y, x1, y + 1, ChuckText.argb(alpha * 0.22f, 0x7F9CC8));
        int margin = x0 + Math.min(28, (x1 - x0) / 8);
        g.fill(margin, y0, margin + 1, y1, ChuckText.argb(alpha * 0.35f, 0xC0504A));
        g.fill(x0, y0, x1, y0 + 1, ChuckText.argb(alpha * 0.3f, 0x000000));
        g.fill(x0, y1 - 1, x1, y1, ChuckText.argb(alpha * 0.3f, 0x000000));
    }
}
