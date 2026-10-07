package org.papiricoh.supernaturalcraft.client.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A button inked on the page: a parchment nine-slice, ink text, no shadow. */
public class BookButton extends AbstractButton {

    private final Runnable onPress;

    public BookButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        BookAtlas.panel(g, !active ? BookAtlas.BUTTON_OFF : isHoveredOrFocused() ? BookAtlas.BUTTON_HOVER : BookAtlas.BUTTON,
                getX(), getY(), width, height);
        var font = Minecraft.getInstance().font;
        int color = active ? BookStyle.INK : BookStyle.FADED;
        g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, color, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
