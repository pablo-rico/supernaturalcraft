package org.papiricoh.supernaturalcraft.client.book.journal;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.ClientHunterLog;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.journal.JournalBlock;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.JournalEntry;
import org.papiricoh.supernaturalcraft.journal.JournalLayout;
import org.papiricoh.supernaturalcraft.journal.Unlock;
import org.papiricoh.supernaturalcraft.network.JournalActionPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Hunter's Journal. The index lists the chapters on the left page and the chosen chapter's
 * pages on the right (locked ones as "???", unread ones with a gold dot). An open entry runs over
 * both pages, spread after spread, laid out by {@link JournalLayout}: arrows (or the arrow keys)
 * turn the pages, the ribbon bookmarks it. Everything here is drawn by hand: no vanilla widgets,
 * so the arrow keys turn pages instead of moving the focus.
 */
public class JournalSection extends BookSection {

    protected static final int PAGE_L = BookStyle.LEFT_X, PAGE_R = BookStyle.RIGHT_X, PAGE_W = BookStyle.PAGE_W, PAGE_TOP = BookStyle.PAGE_Y;

    // --- layout (book space) -----------------------------------------------------------------------
    private static final int L = BookStyle.LEFT_X, R = BookStyle.RIGHT_X, PW = BookStyle.PAGE_W, TOP = BookStyle.PAGE_Y;
    /** Index rows: the chapters, and a chapter's pages (which scroll). */
    private static final int LIST_Y = 42, CHAPTER_ROW = 20, ENTRY_ROW = 18, LIST_BOTTOM = 248;
    /** An entry's faces run from the page top to just above the page-turning row. */
    private static final int FACE_H = 214, NAV_Y = 237;
    private static final int GAP = 7;
    /** Where each page's column of blocks starts (the column is {@link PageBlocks#W} wide). */
    private static final int COL_L = L + 5, COL_R = R + 2;
    /** The bookmark ribbon hangs from the right page's top edge, right of the column. */
    private static final int RIBBON_X = R + PW - 7, RIBBON_Y = 8;

    private enum Mode { INDEX, ENTRY }

    private Mode mode = Mode.INDEX;
    /** The shelf (chapter) the index shows: an index into {@link #shelves()}. */
    private int shelf;
    private boolean chapterChosen;
    private double scroll;

    // The open entry.
    @Nullable
    private JournalPages.Page page;
    private PageBlocks blocks;
    private List<List<JournalLayout.Slice>> faces = List.of();
    private List<FormattedCharSequence> titleLines = List.of();
    private int titleHeight, spread;
    /** The ribbon as clicked, until the server's next word on the bookmarks. */
    @Nullable
    private Boolean bookmarkShown;
    private List<ResourceLocation> bookmarksSeen = List.of();

    // --- public API ----------------------------------------------------------------------------------

    /** Opens an entry (from a bookmark, the dashboard or a roadmap node). A locked or unknown one opens its chapter instead. */
    public void openEntry(ResourceLocation entry) {
        JournalPages.Page p = JournalPages.find(entry);
        if (p == null) {
            openIndex();
            return;
        }
        int at = shelfOf(p.id());
        if (at >= 0) shelf = at;
        chapterChosen = true;
        if (!p.unlocked() || at < 0) {
            openIndex();
            return;
        }
        open(p);
    }

    /** Back to the chapter index. */
    public void openIndex() {
        mode = Mode.INDEX;
        page = null;
        blocks = null;
    }

    /** The index, at one chapter. */
    public void openChapter(JournalChapter c) {
        openShelf(c.id());
    }

    /** The index, at the shelf with this id. */
    public void openShelf(String id) {
        List<Shelf> all = shelves();
        for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(id)) shelf = i;
        chapterChosen = true;
        scroll = 0;
        openIndex();
    }

    // --- opening an entry ----------------------------------------------------------------------------

    public void open(JournalPages.Page p) {
        page = p;
        mode = Mode.ENTRY;
        spread = 0;
        bookmarkShown = null;
        bookmarksSeen = ClientHunterLog.bookmarks();
        layout(p);
        // Only real pages are marked read (not the preview's made-up one).
        if (!ClientHunterLog.isRead(p.id()) && JournalPages.find(p.id()) != null && JournalPages.READ_HERE.add(p.id())) {
            PacketDistributor.sendToServer(new JournalActionPayload(JournalActionPayload.MARK_READ, p.id()));
        }
    }

    private void layout(JournalPages.Page p) {
        blocks = new PageBlocks(p.entry().blocks(), lines -> book.tooltip(lines));
        titleLines = font.split(p.title(), PW);
        if (titleLines.size() > 2) titleLines = titleLines.subList(0, 2);
        titleHeight = 2 + titleLines.size() * 10 + 10 + (p.entry().creature().isPresent() ? 11 : 0);
        faces = JournalLayout.layout(blocks.measured(), FACE_H, titleHeight, GAP);
        spread = Mth.clamp(spread, 0, spreads() - 1);
    }

    private int spreads() {
        return (faces.size() + 1) / 2;
    }

    private void turn(int by) {
        int to = Mth.clamp(spread + by, 0, spreads() - 1);
        if (to == spread) return;
        spread = to;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    private boolean bookmarked() {
        if (bookmarkShown != null && ClientHunterLog.bookmarks() != bookmarksSeen) bookmarkShown = null;
        return bookmarkShown != null ? bookmarkShown : page != null && ClientHunterLog.bookmarks().contains(page.id());
    }

    private void toggleBookmark() {
        if (page == null) return;
        boolean now = !bookmarked();
        bookmarksSeen = ClientHunterLog.bookmarks();
        bookmarkShown = now;
        PacketDistributor.sendToServer(new JournalActionPayload(JournalActionPayload.TOGGLE_BOOKMARK, page.id()));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, now ? 1.4f : 0.8f));
    }

    // --- lifecycle -----------------------------------------------------------------------------------

    @Override
    public void init() {
        if (!chapterChosen) {
            // First time: the first chapter with something unread, or else the first one.
            List<Shelf> all = shelves();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).pages().get().stream().anyMatch(JournalPages.Page::unread)) {
                    shelf = i;
                    break;
                }
            }
            chapterChosen = true;
        }
        // The resource packs may have changed under an open entry (F3+T): lay it out again.
        if (mode == Mode.ENTRY && page != null) blocks = null;
    }

    @Override
    public void tick() {
        if (blocks != null) blocks.tick();
    }

    @Override
    public void hidden() {
        // Let go of the creatures; the entry re-opens from its id when the reader comes back.
        if (page != null) blocks = null;
    }

    // --- drawing -------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        decorate(g, mode == Mode.INDEX || page == null);
        if (mode == Mode.ENTRY && page != null) {
            if (blocks == null) layout(page);
            renderEntry(g, mx, my);
        } else {
            renderIndex(g, mx, my);
        }
    }

    private void renderIndex(GuiGraphics g, int mx, int my) {
        // Left page: the chapters.
        Ink.heading(g, font, contents(), L, TOP + 2, PW, BookStyle.INK);
        List<Shelf> all = shelves();
        shelf = Mth.clamp(shelf, 0, Math.max(0, all.size() - 1));
        for (int i = 0; i < all.size(); i++) {
            Shelf c = all.get(i);
            int y = LIST_Y + i * CHAPTER_ROW;
            List<JournalPages.Page> pages = c.pages().get();
            long open = pages.stream().filter(JournalPages.Page::unlocked).count();
            boolean selected = i == shelf, over = Ink.over(mx, my, L, y, PW, CHAPTER_ROW - 1);
            if (selected) {
                g.fill(L, y, L + PW, y + CHAPTER_ROW - 1, 0x2AB07D18);
                g.fill(L, y, L + 2, y + CHAPTER_ROW - 1, BookStyle.GOLD);
            } else if (over) {
                Ink.hover(g, L, y, PW, CHAPTER_ROW - 1);
            }
            g.renderFakeItem(c.icon(), L + 4, y + 1);
            if (pages.stream().anyMatch(JournalPages.Page::unread)) BookAtlas.DOT_GOLD.draw(g, L + 16, y);
            Component count = Component.literal(open + "/" + pages.size());
            int countW = font.width(count);
            Ink.left(g, font, c.title(), L + 24, y + 6, PW - 24 - countW - 8,
                    selected ? BookStyle.INK : over ? BookStyle.INK : BookStyle.FADED);
            Ink.right(g, font, count, L + PW - 4, y + 6, open == pages.size() && open > 0 ? BookStyle.GOLD : BookStyle.FADED);
        }

        // Right page: the chosen chapter's pages.
        if (all.isEmpty()) return;
        Ink.heading(g, font, all.get(shelf).title(), R, TOP + 2, PW, BookStyle.INK);
        List<JournalPages.Page> pages = all.get(shelf).pages().get();
        if (pages.isEmpty()) {
            Ink.centred(g, font, Component.translatable("screen.supernaturalcraft.book.journal.empty_chapter").withStyle(ChatFormatting.ITALIC),
                    R + PW / 2, LIST_Y + 10, PW, BookStyle.FADED);
            return;
        }
        int max = maxScroll(pages.size());
        scroll = Mth.clamp(scroll, 0, max);
        book.scissor(g, R, LIST_Y, R + PW, LIST_BOTTOM);
        int y0 = LIST_Y - (int) scroll;
        boolean inList = my >= LIST_Y && my < LIST_BOTTOM;
        for (int i = 0; i < pages.size(); i++) {
            int y = y0 + i * ENTRY_ROW;
            if (y + ENTRY_ROW < LIST_Y || y > LIST_BOTTOM) continue;
            JournalPages.Page p = pages.get(i);
            boolean over = inList && Ink.over(mx, my, R, y, PW - 4, ENTRY_ROW - 1);
            if (p.unlocked()) {
                if (over) Ink.hover(g, R, y, PW - 4, ENTRY_ROW - 1);
                g.renderFakeItem(p.icon(), R + 3, y + 1);
                boolean unread = p.unread();
                Ink.left(g, font, p.title(), R + 23, y + 5, PW - 23 - (unread ? 16 : 6), BookStyle.INK);
                if (unread) BookAtlas.DOT_GOLD.draw(g, R + PW - 14, y + 6);
            } else {
                Ink.silhouette(g, p.icon(), R + 3, y + 1);
                g.drawString(font, "???", R + 23, y + 5, BookStyle.FADED, false);
                if (over) book.tooltip(List.of(Component.translatable("screen.supernaturalcraft.book.journal.locked")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            }
        }
        g.disableScissor();
        if (max > 0) {
            // A thin scroll bar down the page's edge.
            int track = LIST_BOTTOM - LIST_Y, content = pages.size() * ENTRY_ROW;
            int thumb = Math.max(12, track * track / content);
            int ty = LIST_Y + (int) ((track - thumb) * (scroll / max));
            g.fill(R + PW - 2, LIST_Y, R + PW - 1, LIST_BOTTOM, 0x223B2A1A);
            g.fill(R + PW - 3, ty, R + PW, ty + thumb, BookStyle.FADED);
        }
    }

    private static int maxScroll(int rows) {
        return Math.max(0, rows * ENTRY_ROW - (LIST_BOTTOM - LIST_Y));
    }

    private void renderEntry(GuiGraphics g, int mx, int my) {
        JournalPages.Page p = page;
        // The title, at the top of the entry's first page.
        if (spread == 0) {
            int y = TOP + 2;
            for (FormattedCharSequence line : titleLines) {
                g.drawString(font, line, L + (PW - font.width(line)) / 2, y, BookStyle.INK, false);
                y += 10;
            }
            BookAtlas.FLOURISH.draw(g, L + (PW - BookAtlas.FLOURISH.w()) / 2, y);
            y += 10;
            if (p.entry().creature().isPresent()) {
                int kills = ClientHunterLog.kills(p.entry().creature().get());
                Ink.centred(g, font, Component.translatable("screen.supernaturalcraft.book.journal.slain", kills), L + PW / 2, y + 1, PW,
                        kills > 0 ? BookStyle.BLOOD : BookStyle.FADED);
            }
        }
        for (int side = 0; side < 2; side++) {
            int f = spread * 2 + side;
            if (f >= faces.size()) break;
            int x = side == 0 ? COL_L : COL_R;
            for (JournalLayout.Slice s : faces.get(f)) {
                blocks.parts.get(s.block()).draw(g, x, TOP + s.y(), s.first(), s.last(), mx, my);
            }
        }

        // Turning the pages.
        int pages = spreads();
        if (spread > 0) {
            boolean over = Ink.over(mx, my, L, NAV_Y, 18, 12);
            (over ? BookAtlas.ARROW_PREV_HOVER : BookAtlas.ARROW_PREV).draw(g, L, NAV_Y);
        }
        if (spread < pages - 1) {
            boolean over = Ink.over(mx, my, R + PW - 18, NAV_Y, 18, 12);
            (over ? BookAtlas.ARROW_NEXT_HOVER : BookAtlas.ARROW_NEXT).draw(g, R + PW - 18, NAV_Y);
        }
        Component back = Component.translatable("screen.supernaturalcraft.book.journal.back");
        boolean overBack = overBack(mx, my);
        Ink.centred(g, font, back, L + PW / 2, NAV_Y + 2, PW - 50, overBack ? BookStyle.GOLD : BookStyle.FADED);
        if (overBack) Ink.rule(g, L + (PW - font.width(back)) / 2, NAV_Y + 11, font.width(back), BookStyle.GOLD);
        if (pages > 1) {
            Ink.centred(g, font, Component.literal((spread + 1) + " / " + pages), R + PW / 2, NAV_Y + 2, PW, BookStyle.FADED);
        }

        // The bookmark ribbon on the right page's top edge.
        boolean marked = bookmarked();
        (marked ? BookAtlas.RIBBON_ON : BookAtlas.RIBBON_OFF).draw(g, RIBBON_X, RIBBON_Y);
        if (Ink.over(mx, my, RIBBON_X, RIBBON_Y, 10, 24)) {
            book.tooltip(List.of(Component.translatable(marked ? "screen.supernaturalcraft.book.journal.unbookmark"
                    : "screen.supernaturalcraft.book.journal.bookmark")));
        }
    }

    private boolean overBack(double mx, double my) {
        int w = font.width(Component.translatable("screen.supernaturalcraft.book.journal.back"));
        return Ink.over(mx, my, L + (PW - w) / 2 - 2, NAV_Y, w + 4, 12);
    }

    // --- what this section holds (the Archive tab overrides these) ----------------------------------------

    /** The index's rows: every Journal chapter but the Men of Letters' Archive (it has its own tab). */
    protected List<Shelf> shelves() {
        List<Shelf> out = new ArrayList<>();
        for (JournalChapter c : JournalChapter.values()) {
            if (c == JournalChapter.ARCHIVE) continue;
            out.add(new Shelf(c.id(), Component.translatable(c.titleKey()), Ink.item(ResourceLocation.parse(c.icon)), () -> JournalPages.chapter(c)));
        }
        return out;
    }

    /** The index's heading. */
    protected Component contents() {
        return Component.translatable("screen.supernaturalcraft.book.journal.contents");
    }

    /** Drawn over the open book before the index or the entry (the Archive's lamp light and brass). */
    protected void decorate(GuiGraphics g, boolean index) {
    }

    /** The shelf holding a page, or -1 if this section has no such page. */
    protected int shelfOf(ResourceLocation id) {
        List<Shelf> all = shelves();
        for (int i = 0; i < all.size(); i++) {
            for (JournalPages.Page p : all.get(i).pages().get()) if (p.id().equals(id)) return i;
        }
        return -1;
    }

    // --- input ---------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (mode == Mode.ENTRY && page != null) {
            if (spread > 0 && Ink.over(mx, my, L, NAV_Y, 18, 12)) {
                turn(-1);
                return true;
            }
            if (spread < spreads() - 1 && Ink.over(mx, my, R + PW - 18, NAV_Y, 18, 12)) {
                turn(1);
                return true;
            }
            if (overBack(mx, my)) {
                click();
                openIndex();
                return true;
            }
            if (Ink.over(mx, my, RIBBON_X, RIBBON_Y, 10, 24)) {
                toggleBookmark();
                return true;
            }
            return false;
        }
        List<Shelf> all = shelves();
        for (int c = 0; c < all.size(); c++) {
            if (Ink.over(mx, my, L, LIST_Y + c * CHAPTER_ROW, PW, CHAPTER_ROW - 1)) {
                if (c != shelf) {
                    shelf = c;
                    scroll = 0;
                    click();
                }
                return true;
            }
        }
        if (my >= LIST_Y && my < LIST_BOTTOM && Ink.over(mx, my, R, LIST_Y, PW - 4, LIST_BOTTOM - LIST_Y)) {
            int i = (int) Math.floor((my - LIST_Y + scroll) / ENTRY_ROW);
            List<JournalPages.Page> pages = all.isEmpty() ? List.of() : all.get(Mth.clamp(shelf, 0, all.size() - 1)).pages().get();
            if (i >= 0 && i < pages.size() && pages.get(i).unlocked()) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                open(pages.get(i));
                return true;
            }
        }
        return false;
    }

    private void click() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mode == Mode.ENTRY) {
            if (sy != 0) turn(sy < 0 ? 1 : -1);
            return true;
        }
        if (Ink.over(mx, my, R, LIST_Y, PW, LIST_BOTTOM - LIST_Y)) {
            List<Shelf> all = shelves();
            int rows = all.isEmpty() ? 0 : all.get(Mth.clamp(shelf, 0, all.size() - 1)).pages().get().size();
            scroll = Mth.clamp(scroll - sy * ENTRY_ROW, 0, maxScroll(rows));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (mode != Mode.ENTRY) return false;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> turn(-1);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> turn(1);
            case GLFW.GLFW_KEY_HOME -> turn(-spread);
            case GLFW.GLFW_KEY_END -> turn(spreads());
            case GLFW.GLFW_KEY_BACKSPACE -> openIndex();
            default -> {
                return false;
            }
        }
        return true;
    }

    // --- preview -------------------------------------------------------------------------------------

    @Override
    public List<PreviewShot> previewShots() {
        return List.of(
                new PreviewShot("index", b -> {
                    // The chapter with the most pages.
                    JournalChapter best = JournalChapter.BASICS;
                    for (JournalChapter c : JournalChapter.values()) {
                        if (JournalPages.chapter(c).size() > JournalPages.chapter(best).size()) best = c;
                    }
                    b.journal().openChapter(best);
                }),
                new PreviewShot("entry", b -> {
                    ResourceLocation pick = SupernaturalCraft.asResource("family_business");
                    for (JournalPages.Page p : JournalPages.all()) {
                        if (p.unlocked() && p.entry().blocks().stream().anyMatch(x -> x instanceof JournalBlock.Entity)) {
                            pick = p.id();
                            break;
                        }
                    }
                    b.journal().openEntry(pick);
                }),
                new PreviewShot("bowl_spell", b -> JournalPages.chapter(JournalChapter.BOWL).stream()
                        .filter(p -> p.id().getPath().startsWith("bowl_spell/")).findFirst()
                        .ifPresentOrElse(p -> b.journal().open(p), () -> b.journal().openChapter(JournalChapter.BOWL))),
                new PreviewShot("ghost", b -> b.journal().openEntry(SupernaturalCraft.asResource("ghosts"))),
                new PreviewShot("hellhound", b -> b.journal().openEntry(SupernaturalCraft.asResource("hellhounds"))),
                new PreviewShot("sample", b -> b.journal().open(sample())),
                new PreviewShot("sample_next", b -> {
                    b.journal().open(sample());
                    b.journal().turn(1);
                }));
    }

    /** A made-up entry with one of every kind of block, for {@code SN_PREVIEW=book} to show how they look. */
    private static JournalPages.Page sample() {
        ResourceLocation id = SupernaturalCraft.asResource("preview_sample");
        List<JournalBlock> blocks = new ArrayList<>();
        blocks.add(new JournalBlock.Entity(SupernaturalCraft.asResource("black_eyed_demon"), Optional.of("entity.supernaturalcraft.black_eyed_demon"), 1f));
        blocks.add(new JournalBlock.Text("journal.supernaturalcraft.entry.family_business.0"));
        blocks.add(new JournalBlock.Recipe(SupernaturalCraft.asResource("grimoire"), Optional.of("item.supernaturalcraft.grimoire")));
        blocks.add(new JournalBlock.Recipe(SupernaturalCraft.asResource("ritual/binding"), Optional.empty()));
        blocks.add(new JournalBlock.Entity(SupernaturalCraft.asResource("azazel"), Optional.of("entity.supernaturalcraft.azazel"), 1.4f));
        blocks.add(new JournalBlock.Items(List.of(SupernaturalCraft.asResource("salt"), SupernaturalCraft.asResource("holy_water"),
                SupernaturalCraft.asResource("demon_blood")), Optional.of("journal.supernaturalcraft.entry.family_business.cap0")));
        JournalEntry entry = new JournalEntry(JournalChapter.DEMONS, 0, SupernaturalCraft.asResource("demon_blood"), Unlock.ALWAYS,
                Optional.of(SupernaturalCraft.asResource("black_eyed_demon")), blocks);
        return new JournalPages.Page(id, entry, Component.translatable("entity.supernaturalcraft.black_eyed_demon"), Ink.item(entry.icon()));
    }
}
