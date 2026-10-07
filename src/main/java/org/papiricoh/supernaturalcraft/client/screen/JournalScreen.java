package org.papiricoh.supernaturalcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/**
 * The Hunter's Journal, bound into the back of every grimoire: the whole progression, from the
 * first pinch of salt to the Cage, with the counters to every trick Lucifer has.
 */
public class JournalScreen extends Screen {

    public static final int PAGES = 17;
    private static final ResourceLocation BG = SupernaturalCraft.asResource("textures/gui/journal.png");
    private static final int W = 256, H = 196, INK = 0x3B2A1A, FADED = 0x7A6448;

    private final Screen parent;
    private int page;
    private int left, top;

    public JournalScreen(Screen parent) {
        super(Component.translatable("screen.supernaturalcraft.journal"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1))
                .bounds(left + 10, top + 170, 30, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page = Math.min(PAGES - 1, page + 1))
                .bounds(left + W - 40, top + 170, 30, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(left + W / 2 - 30, top + 170, 60, 18).build());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        g.blit(BG, left, top, 0, 0, W, H, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        String key = "journal.supernaturalcraft.page" + (page + 1);
        Component title = Component.translatable(key + ".title");
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 10, INK, false);
        List<FormattedCharSequence> lines = font.split(FormattedText.of(Component.translatable(key + ".body").getString()), W - 28);
        int y = top + 26;
        for (FormattedCharSequence line : lines) {
            if (y > top + 160) break;
            g.drawString(font, line, left + 14, y, INK, false);
            y += 10;
        }
        Component n = Component.literal((page + 1) + " / " + PAGES);
        g.drawString(font, n, left + (W - font.width(n)) / 2, top + 160, FADED, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
