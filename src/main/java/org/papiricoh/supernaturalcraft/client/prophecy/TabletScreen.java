package org.papiricoh.supernaturalcraft.client.prophecy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.TabletGuessPayload;
import org.papiricoh.supernaturalcraft.network.TabletPayload;
import org.papiricoh.supernaturalcraft.prophecy.Tablet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A chapter of a tablet of the Word (v0.36): the sentence in glyphs, word by word, each with the letter the reader has put
 * under it. Click a glyph, type its letter (Backspace or Delete clears it); the same glyph takes the same letter everywhere.
 * Known letters (gold) are fixed. "Read" sends the try; a wrong one says how many glyphs were wrong.
 */
public class TabletScreen extends Screen {

    private static final ResourceLocation BG = SupernaturalCraft.asResource("textures/gui/tablet_screen.png");
    private static final ResourceLocation GLYPHS = SupernaturalCraft.asResource("textures/gui/word_glyphs.png");
    static final int W = 248, H = 200, CELL = 14, GLYPH = 12, LINE = 26, TEXT_W = 166, FIELD_X = 10, FIELD_Y = 10, KEY_X = 192;

    private final int book;
    private final List<Integer> glyphs;
    private final int[] known = new int[26], guess = new int[26];
    private boolean prophet;
    private int status, selected = -1;
    private boolean read;
    /** Laid-out cells: x, y, glyph (relative to the panel). */
    private final List<int[]> cells = new ArrayList<>();

    public TabletScreen(TabletPayload p) {
        super(Component.translatable("item.supernaturalcraft." + Tablet.byOrdinal(p.book() / 16).id() + "_tablet"));
        this.book = p.book();
        this.glyphs = List.copyOf(p.glyphs());
        Arrays.fill(guess, -1);
        update(p);
        layout();
    }

    public int book() {
        return book;
    }

    public void update(TabletPayload p) {
        for (int g = 0; g < 26; g++) {
            known[g] = g < p.known().size() ? p.known().get(g) : -1;
            if (known[g] >= 0) guess[g] = known[g];
        }
        prophet = p.prophet();
        status = p.status();
    }

    public void solved() {
        read = true;
        status = 0;
    }

    private void layout() {
        cells.clear();
        List<List<Integer>> words = new ArrayList<>();
        List<Integer> word = new ArrayList<>();
        for (int g : glyphs) {
            if (g < 0) {
                if (!word.isEmpty()) words.add(word);
                word = new ArrayList<>();
            } else word.add(g);
        }
        if (!word.isEmpty()) words.add(word);
        int x = 0, y = 0;
        for (List<Integer> w : words) {
            int width = w.size() * CELL;
            if (x > 0 && x + width > TEXT_W) {
                x = 0;
                y += LINE;
            }
            for (int g : w) {
                cells.add(new int[]{x, y, g});
                x += CELL;
            }
            x += CELL / 2;
        }
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H - 24) / 2;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.supernaturalcraft.tablet.read"), b -> send())
                .bounds(left() + W / 2 - 40, top() + H + 4, 80, 20).build());
    }

    private void send() {
        if (read) {
            onClose();
            return;
        }
        List<Integer> out = new ArrayList<>();
        for (int g : guess) out.add(g);
        PacketDistributor.sendToServer(new TabletGuessPayload(book, out));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int l = left(), t = top();
        int ox = l + FIELD_X + 4, oy = t + FIELD_Y + 22;
        g.drawCenteredString(font, Component.translatable("gui.supernaturalcraft.tablet.chapter", title, book % 16), l + FIELD_X + 87, t + FIELD_Y + 6, 0xE8D9A0);
        // The key strip: what the reader knows, glyph and letter, in two columns.
        g.drawCenteredString(font, Component.translatable("gui.supernaturalcraft.tablet.known"), l + KEY_X + 23, t + FIELD_Y + 4, 0xB8A878);
        int k = 0;
        for (int glyph = 0; glyph < 26; glyph++) {
            if (known[glyph] < 0) continue;
            int kx = l + KEY_X + 3 + (k % 2) * 22, ky = t + FIELD_Y + 16 + (k / 2) * 13;
            g.blit(GLYPHS, kx, ky, 10, 10, (glyph % 13) * 16, (glyph / 13) * 16, 16, 16, 208, 32);
            g.drawString(font, String.valueOf((char) ('A' + known[glyph])), kx + 12, ky + 1, 0xFFD27A);
            k++;
        }
        for (int[] c : cells) {
            int x = ox + c[0], y = oy + c[1], glyph = c[2];
            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + LINE - 4;
            if (glyph == selected) g.fill(x - 1, y - 1, x + GLYPH + 1, y + LINE - 6, 0x55E8D9A0);
            else if (hover) g.fill(x - 1, y - 1, x + GLYPH + 1, y + LINE - 6, 0x33FFFFFF);
            g.blit(GLYPHS, x, y, GLYPH, GLYPH, (glyph % 13) * 16, (glyph / 13) * 16, 16, 16, 208, 32);
            int letter = guess[glyph];
            if (letter >= 0) {
                int color = known[glyph] >= 0 ? 0xFFD27A : 0xFFFFFF;
                g.drawCenteredString(font, String.valueOf((char) ('A' + letter)), x + GLYPH / 2, y + GLYPH + 2, color);
            } else {
                g.drawCenteredString(font, "_", x + GLYPH / 2, y + GLYPH + 2, 0x777777);
            }
        }
        Component line = read ? Component.translatable("gui.supernaturalcraft.tablet.solved")
                : status > 0 ? Component.translatable("gui.supernaturalcraft.tablet.wrong", status)
                : Component.translatable(prophet ? "gui.supernaturalcraft.tablet.hint_prophet" : "gui.supernaturalcraft.tablet.hint");
        g.drawCenteredString(font, line, l + FIELD_X + 87, t + H - 22, read ? 0xFFD27A : status > 0 ? 0xFF6060 : 0xBBBBBB);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        g.blit(BG, left(), top(), 0, 0, W, H, 256, 256);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int ox = left() + FIELD_X + 4, oy = top() + FIELD_Y + 22;
        for (int[] c : cells) {
            int x = ox + c[0], y = oy + c[1];
            if (mx >= x && mx < x + CELL && my >= y && my < y + LINE - 4) {
                selected = known[c[2]] >= 0 ? -1 : c[2];
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (selected >= 0 && (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE)) {
            guess[selected] = -1;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        char u = Character.toUpperCase(c);
        if (selected >= 0 && u >= 'A' && u <= 'Z' && !read) {
            int letter = u - 'A';
            // One glyph per letter: taking a letter frees it from any other glyph that was guessed with it.
            for (int g = 0; g < 26; g++) if (guess[g] == letter && known[g] < 0) guess[g] = -1;
            guess[selected] = letter;
            selected = next(selected);
            return true;
        }
        return super.charTyped(c, mods);
    }

    /** The next glyph along the sentence still without a letter, or none. */
    private int next(int from) {
        boolean past = false;
        for (int[] c : cells) {
            if (c[2] == from) past = true;
            else if (past && guess[c[2]] < 0) return c[2];
        }
        return -1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
