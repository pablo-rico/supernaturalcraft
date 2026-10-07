package org.papiricoh.supernaturalcraft.client.chuck.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A screen that draws one sheet of paper in its own space ({@link #pageW} × {@link #pageH}) scaled to fit whatever GUI
 * scale and window it gets, never larger than 1:1. Subclasses draw in page space ({@link #drawPage}) and receive clicks
 * in page space ({@link #clickPage}).
 */
abstract class PaperScreen extends Screen {

    protected int pageW, pageH;
    protected float scale = 1;
    protected int left, top;
    protected int age;

    PaperScreen(Component title, int pageW, int pageH) {
        super(title);
        this.pageW = pageW;
        this.pageH = pageH;
    }

    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 12f) / (pageW + 4), (height - 12f) / (pageH + 5)));
        left = Math.round((width - pageW * scale) / 2);
        top = Math.round((height - pageH * scale) / 2);
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1);
        Paper.sheet(g, 0, 0, pageW, pageH, getClass().getName().hashCode());
        drawPage(g, pageX(mx), pageY(my), partial);
        g.pose().popPose();
    }

    protected double pageX(double mx) {
        return (mx - left) / scale;
    }

    protected double pageY(double my) {
        return (my - top) / scale;
    }

    /** Draws the page's contents; the mouse is given in page space. */
    protected abstract void drawPage(GuiGraphics g, double mx, double my, float partial);

    /** A click in page space. */
    protected boolean clickPage(double x, double y, int button) {
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (clickPage(pageX(mx), pageY(my), button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
