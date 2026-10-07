package org.papiricoh.supernaturalcraft.client.book.scriptorium;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;

import java.util.function.Consumer;

/**
 * A one-line text field written in ink on the page: no box, no shadow (vanilla's {@code EditBox}
 * always shadows its text, which smears dark ink). Typing, backspace/delete, arrows, home/end and
 * paste; Enter lets go of the field.
 */
final class InkField extends AbstractWidget {

    private final Font font;
    private final int maxLength;
    private final Component hint;
    private final Consumer<String> responder;
    private String value = "";
    private int cursor, offset;

    InkField(Font font, int x, int y, int width, int maxLength, Component title, Component hint, Consumer<String> responder) {
        super(x, y, width, 10, title);
        this.font = font;
        this.maxLength = maxLength;
        this.hint = hint;
        this.responder = responder;
    }

    String value() {
        return value;
    }

    /** Replaces the text without telling the responder. */
    void setValue(String text) {
        value = text.length() > maxLength ? text.substring(0, maxLength) : text;
        cursor = value.length();
        offset = 0;
    }

    private void insert(String text) {
        String clean = StringUtil.filterText(text);
        int room = maxLength - value.length();
        if (room <= 0 || clean.isEmpty()) return;
        if (clean.length() > room) clean = clean.substring(0, room);
        value = value.substring(0, cursor) + clean + value.substring(cursor);
        cursor += clean.length();
        responder.accept(value);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!isFocused() || !StringUtil.isAllowedChatCharacter(c)) return false;
        insert(String.valueOf(c));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (!isFocused()) return false;
        if (Screen.isPaste(key)) {
            insert(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) {
                    value = value.substring(0, cursor - 1) + value.substring(cursor);
                    cursor--;
                    responder.accept(value);
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < value.length()) {
                    value = value.substring(0, cursor) + value.substring(cursor + 1);
                    responder.accept(value);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> cursor = Math.max(0, cursor - 1);
            case GLFW.GLFW_KEY_RIGHT -> cursor = Math.min(value.length(), cursor + 1);
            case GLFW.GLFW_KEY_HOME -> cursor = 0;
            case GLFW.GLFW_KEY_END -> cursor = value.length();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> setFocused(false);
            // Everything else (Escape above all) is the screen's.
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onClick(double mx, double my) {
        String shown = value.substring(Math.min(offset, value.length()));
        cursor = Math.min(value.length(), offset + font.plainSubstrByWidth(shown, (int) Math.round(mx - getX())).length());
    }

    @Override
    public void playDownSound(SoundManager sounds) {
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        if (value.isEmpty() && !isFocused()) {
            g.drawString(font, hint, getX(), getY(), 0xFF9C8868, false);
            return;
        }
        offset = Math.min(offset, cursor);
        while (offset < cursor && font.width(value.substring(offset, cursor)) > width - 2) offset++;
        String shown = font.plainSubstrByWidth(value.substring(offset), width - 2);
        g.drawString(font, shown, getX(), getY(), BookStyle.INK, false);
        if (isFocused() && (Util.getMillis() / 400) % 2 == 0) {
            int cx = getX() + font.width(value.substring(offset, cursor));
            g.fill(cx, getY() - 1, cx + 1, getY() + 9, BookStyle.INK);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("narration.edit_box", value));
    }
}
