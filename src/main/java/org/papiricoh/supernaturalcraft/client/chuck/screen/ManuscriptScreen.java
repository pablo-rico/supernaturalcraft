package org.papiricoh.supernaturalcraft.client.chuck.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.author.Chronicle;
import org.papiricoh.supernaturalcraft.author.Manuscript;

import java.util.ArrayList;
import java.util.List;

/**
 * "The End", read: the chronicle the Author wrote, typewritten on numbered pages. The first page is the title page
 * (the first lines up to the blank one, centred); the rest are the chapters, and "THE END" stands alone at the bottom
 * of the last. Turn with a click (right half forward, left half back) or the arrow keys.
 */
public class ManuscriptScreen extends PaperScreen {

    private static final int W = 230, H = 270, MARGIN = 20, TOP = 22, ROW = 10;
    private static final int ROWS = (H - TOP - 30) / ROW;

    private final List<String> title = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    private int page;

    public ManuscriptScreen(Manuscript text) {
        super(Component.translatable("screen.supernaturalcraft.manuscript"), W, H);
        var font = net.minecraft.client.Minecraft.getInstance().font;
        List<String> lines = text == null ? List.of(Chronicle.THE_END) : text.lines();
        int i = 0;
        for (; i < lines.size() && !lines.get(i).isEmpty(); i++) title.add(lines.get(i));
        List<String> rows = new ArrayList<>();
        for (i++; i < lines.size(); i++) {
            if (lines.get(i).equals(Chronicle.THE_END)) continue;
            rows.addAll(Paper.wrap(font, lines.get(i), W - MARGIN * 2));
            rows.add("");
        }
        for (int from = 0; from < rows.size(); from += ROWS) {
            List<String> p = new ArrayList<>(rows.subList(from, Math.min(rows.size(), from + ROWS)));
            while (!p.isEmpty() && p.getFirst().isEmpty()) p.removeFirst();
            pages.add(p);
        }
        if (pages.isEmpty()) pages.add(new ArrayList<>());
    }

    /** Pages including the title page. */
    public int pageCount() {
        return pages.size() + 1;
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(pageCount() - 1, page));
    }

    @Override
    protected void drawPage(GuiGraphics g, double mx, double my, float partial) {
        if (page == 0) {
            int y = H / 2 - title.size() * 14;
            for (int i = 0; i < title.size(); i++) {
                String t = title.get(i);
                Paper.type(g, font, t, (W - Paper.width(font, t)) / 2, y, t.length(), i == 0 ? Paper.RED : Paper.INK);
                y += i == 0 ? 22 : 14;
            }
        } else {
            List<String> rows = pages.get(page - 1);
            int y = TOP;
            for (String r : rows) {
                Paper.type(g, font, r, MARGIN, y, r.length(), Paper.INK);
                y += ROW;
            }
            if (page == pageCount() - 1) {
                String end = Chronicle.THE_END;
                Paper.type(g, font, end, (W - Paper.width(font, end)) / 2, H - 46, end.length(), Paper.INK);
            }
        }
        String num = "- " + (page + 1) + " -";
        Paper.type(g, font, num, (W - Paper.width(font, num)) / 2, H - 18, num.length(), Paper.FADED);
        if (page > 0) Paper.type(g, font, "<", 8, H - 18, 1, mx < W / 2.0 ? Paper.RED : Paper.FADED);
        if (page < pageCount() - 1) Paper.type(g, font, ">", W - 14, H - 18, 1, mx >= W / 2.0 ? Paper.RED : Paper.FADED);
    }

    @Override
    protected boolean clickPage(double x, double y, int button) {
        if (button != 0 || x < 0 || y < 0 || x > W || y > H) return false;
        turn(x < W / 2.0 ? -1 : 1);
        return true;
    }

    private void turn(int by) {
        int before = page;
        setPage(page + by);
        if (page != before) Paper.sound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
