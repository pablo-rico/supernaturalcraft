package org.papiricoh.supernaturalcraft.client.book.chain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas.Sprite;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * A panel over the chain's spread (the officer's record, the front): the spread dimmed behind a folio of the side's binding,
 * scrolling columns of rows, clickable areas and tooltips. Book space throughout; it closes with Esc, its X or a click
 * outside it.
 */
abstract class ChainPanel {

    static final String K = "screen.supernaturalcraft.book.chain.";
    /** The folio, over the spread. */
    static final int X0 = BookStyle.SPREAD_X + 10, Y0 = BookStyle.SPREAD_Y + 4, X1 = BookStyle.SPREAD_X + BookStyle.SPREAD_W - 10,
            Y1 = BookStyle.SPREAD_Y + BookStyle.SPREAD_H - 4;
    /** Over the tree's cards, and under the screen's tooltips (z 400). */
    static final float Z = 270;
    /** Inks on each binding (Pit, Host, Fallen): text, faded, accent. */
    static final int[] INK = {0xFFEADCC4, BookStyle.INK, 0xFFE4DED8};
    static final int[] FADED = {0xFFB09478, BookStyle.FADED, 0xFFA8A09A};
    static final int[] ACCENT = {0xFFF0794A, BookStyle.GOLD, 0xFFD2C298};
    /** Button labels on each side's plate. */
    static final int[] LABEL = {0xFFF6E2CC, BookStyle.INK, 0xFFF4F0EA};

    protected final HunterBookScreen book;
    protected final Font font;
    protected final ChainSection section;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> next = new ArrayList<>();

    ChainPanel(ChainSection section, HunterBookScreen book) {
        this.section = section;
        this.book = book;
        this.font = Minecraft.getInstance().font;
    }

    /** The binding: 0 the Pit, 1 the Host, 2 the Fallen. */
    abstract int side();

    abstract void draw(GuiGraphics g, int mx, int my, float partial);

    /** Gone (its officer no longer known): the section closes it. */
    boolean stale() {
        return false;
    }

    final void render(GuiGraphics g, int mx, int my, float partial) {
        next.clear();
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, Z);
        g.fill(BookStyle.SPREAD_X, BookStyle.SPREAD_Y, BookStyle.SPREAD_X + BookStyle.SPREAD_W, BookStyle.SPREAD_Y + BookStyle.SPREAD_H, 0xB4120A08);
        // A soft shadow under the folio.
        g.fill(X0 + 3, Y0 + 4, X1 + 3, Y1 + 3, 0x50000000);
        ChainAtlas.folio(g, side(), X0, Y0, X1 - X0, Y1 - Y0);
        draw(g, mx, my, partial);
        boolean overClose = inside(mx, my, X1 - 18, Y0 + 5, 12, 12);
        ChainAtlas.close(overClose).draw(g, X1 - 18, Y0 + 5);
        if (overClose) book.tooltip(List.of(Component.translatable(K + "close")));
        pose.popPose();
        hits.clear();
        hits.addAll(next);
        for (Hit h : hits) if (h.tip != null && inside(mx, my, h.x, h.y, h.w, h.h)) book.tooltip(h.tip);
    }

    // --- input -----------------------------------------------------------------------------------

    boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return true;
        if (inside(mx, my, X1 - 18, Y0 + 5, 12, 12) || !inside(mx, my, X0, Y0, X1 - X0, Y1 - Y0)) {
            click(SoundEvents.BOOK_PAGE_TURN);
            section.closePanel();
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.click != null && inside(mx, my, h.x, h.y, h.w, h.h)) {
                click(SoundEvents.UI_BUTTON_CLICK.value());
                h.click.run();
                return true;
            }
        }
        return true;
    }

    boolean mouseScrolled(double mx, double my, double sy) {
        return true;
    }

    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            section.closePanel();
            return true;
        }
        return false;
    }

    static void click(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f));
    }

    /** A clickable or hoverable area of this frame (clicks use the last frame's). */
    void hit(int x, int y, int w, int h, @Nullable Runnable click, @Nullable List<Component> tip) {
        next.add(new Hit(x, y, w, h, click, tip));
    }

    private record Hit(int x, int y, int w, int h, @Nullable Runnable click, @Nullable List<Component> tip) {
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // --- rows ------------------------------------------------------------------------------------

    /** One line of a column: text (with an icon), a gap, a rule or a button (1 live, 2 disabled). */
    record Row(FormattedCharSequence text, int colour, int indent, int height, @Nullable Sprite icon, @Nullable Runnable click,
               @Nullable List<Component> tip, int button, boolean rule) {
    }

    void text(List<Row> rows, Component c, int colour, int width, int indent) {
        for (FormattedCharSequence s : font.split(c, width - indent)) rows.add(new Row(s, colour, indent, 10, null, null, null, 0, false));
    }

    /** Text after an icon (wrapped beside it), clickable and with a tooltip if given. */
    void link(List<Row> rows, @Nullable Sprite icon, Component c, int colour, int width, int indent, @Nullable Runnable click,
              @Nullable List<Component> tip) {
        int pad = icon == null ? 0 : icon.w() + 3;
        List<FormattedCharSequence> lines = font.split(c, width - indent - pad);
        for (int i = 0; i < lines.size(); i++) {
            rows.add(new Row(lines.get(i), colour, indent + (i == 0 ? 0 : pad), 10, i == 0 ? icon : null, click, tip, 0, false));
        }
    }

    void gap(List<Row> rows, int h) {
        rows.add(new Row(FormattedCharSequence.EMPTY, 0, 0, h, null, null, null, 0, false));
    }

    void rule(List<Row> rows) {
        rows.add(new Row(FormattedCharSequence.EMPTY, 0, 0, 9, null, null, null, 0, true));
    }

    void button(List<Row> rows, Component label, int indent, @Nullable Runnable click, @Nullable List<Component> tip) {
        rows.add(new Row(label.getVisualOrderText(), 0, indent, 16, null, click, tip, click == null ? 2 : 1, false));
    }

    /** A scrolling column: its rows clipped to its box, a thin bar when they overflow. */
    final class Column {
        float scroll;
        int x, y, w, h, content;

        void place(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean over(double mx, double my) {
            return inside(mx, my, x, y, w, h);
        }

        void scroll(double sy) {
            scroll = Mth.clamp(scroll - (float) sy * 14, 0, Math.max(0, content - h));
        }

        void draw(GuiGraphics g, List<Row> rows, int mx, int my, int pageSide, boolean onPage) {
            content = 0;
            for (Row r : rows) content += r.height;
            scroll = Mth.clamp(scroll, 0, Math.max(0, content - h));
            book.scissor(g, x, y, x + w, y + h);
            boolean mouseIn = over(mx, my);
            int ry = y - Math.round(scroll);
            int hoverInk = onPage ? BookStyle.BLOOD : ACCENT[pageSide];
            for (Row r : rows) {
                if (ry + r.height > y && ry < y + h) {
                    int rx = x + r.indent, rw = w - r.indent - 4;
                    if (r.rule) {
                        ChainAtlas.rule(g, pageSide, rx + 6, ry + 2, rw - 12);
                    } else if (r.button > 0) {
                        boolean hot = r.button == 1 && mouseIn && inside(mx, my, rx, ry, rw, 14);
                        ChainAtlas.button(g, pageSide, r.button == 2 ? 2 : hot ? 1 : 0, rx, ry, rw);
                        int col = r.button == 2 ? 0xFF8A7E70 : LABEL[pageSide];
                        g.drawString(font, r.text, rx + (rw - font.width(r.text)) / 2, ry + 3, col, false);
                        if (mouseIn) hit(rx, Math.max(ry, y), rw, Math.min(14, y + h - ry), r.click, r.tip);
                    } else {
                        int tx = rx;
                        boolean hot = r.click != null && mouseIn && inside(mx, my, rx, ry - 1, rw, r.height);
                        if (r.icon != null) {
                            r.icon.draw(g, rx, ry - 1);
                            tx += r.icon.w() + 3;
                        }
                        int col = hot ? hoverInk : r.colour;
                        g.drawString(font, r.text, tx, ry, col, false);
                        if (hot) g.fill(tx, ry + 9, tx + font.width(r.text), ry + 10, col);
                        if (mouseIn && (r.click != null || r.tip != null)) hit(rx, ry - 1, rw, r.height, r.click, r.tip);
                    }
                }
                ry += r.height;
            }
            g.disableScissor();
            if (content > h) {
                float share = h / (float) content, at = scroll / Math.max(1, content - h);
                int barH = Math.max(8, Math.round(h * share)), barY = y + Math.round((h - barH) * at);
                int track = onPage ? 0x22000000 : 0x30FFFFFF, thumb = onPage ? 0x88000000 | (BookStyle.FADED & 0xFFFFFF) : ACCENT[pageSide];
                g.fill(x + w - 2, y, x + w - 1, y + h, track);
                g.fill(x + w - 3, barY, x + w, barY + barH, thumb);
            }
        }
    }
}
