package org.papiricoh.supernaturalcraft.client.book;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.client.book.home.HomeSection;
import org.papiricoh.supernaturalcraft.client.book.journal.JournalSection;
import org.papiricoh.supernaturalcraft.client.book.roadmap.RoadmapSection;
import org.papiricoh.supernaturalcraft.client.book.scriptorium.ScriptoriumSection;

import java.util.EnumMap;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * The Hunter's Book: the grimoire opened wide. A double-page spread with ribbon tabs down its edge
 * for each {@link Tab}. The book is laid out in a fixed book space ({@link BookStyle#W}×{@link BookStyle#H})
 * and drawn at the largest whole number of real pixels per book pixel that fits the window, so the
 * pixel art stays crisp at any GUI scale. Mouse input is mapped back into book space before the
 * sections and widgets see it.
 */
public class HunterBookScreen extends Screen {

    public enum Tab {
        HOME("supernaturalcraft:grimoire"),
        JOURNAL("minecraft:writable_book"),
        SCRIPTORIUM("supernaturalcraft:spell_scroll"),
        ROADMAP("minecraft:filled_map"),
        /** v0.17: the Men of Letters' Archive, only for members of the order. */
        ARCHIVE("supernaturalcraft:aquarian_star"),
        /** v0.18: the hunter's memories, as their Heaven keeps them (once there is one). */
        MEMORIES("minecraft:feather");

        final String icon;

        Tab(String icon) {
            this.icon = icon;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component title() {
            return Component.translatable("screen.supernaturalcraft.book.tab." + id());
        }

        /** Whether the reader has this tab at all (the Archive exists only for the Men of Letters). */
        public boolean shown() {
            if (this == MEMORIES) {
                return !org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven.log().entries().isEmpty()
                        || org.papiricoh.supernaturalcraft.client.heaven.ClientHeaven.standing().plotIndex() >= 0;
            }
            return this != ARCHIVE || org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy.member();
        }
    }

    /** A page the book opens at next time (research just finished): its tab and its id. */
    @Nullable
    private static Tab pendingTab;
    @Nullable
    private static ResourceLocation pendingPage;

    /** The next time the book opens, it opens at this page of this tab. */
    public static void openNextAt(Tab t, @Nullable ResourceLocation page) {
        pendingTab = t;
        pendingPage = page;
    }

    /** The tab the book was last left open at. */
    private static Tab lastTab = Tab.HOME;

    private final Map<Tab, BookSection> sections = new EnumMap<>(Tab.class);
    private Tab tab;
    private float scale = 1;
    private int left, top;
    private boolean drawingBook;

    public HunterBookScreen() {
        this(pendingTab != null && pendingTab.shown() ? pendingTab : lastTab);
        if (pendingTab != null && pendingTab.shown() && pendingPage != null) openEntry(pendingPage);
        pendingTab = null;
        pendingPage = null;
    }

    public HunterBookScreen(Tab tab) {
        super(Component.translatable("screen.supernaturalcraft.book"));
        this.tab = tab.shown() ? tab : Tab.HOME;
        put(Tab.HOME, new HomeSection());
        put(Tab.JOURNAL, new JournalSection());
        put(Tab.SCRIPTORIUM, new ScriptoriumSection());
        put(Tab.ROADMAP, new RoadmapSection());
        put(Tab.ARCHIVE, new org.papiricoh.supernaturalcraft.client.book.archive.ArchiveSection());
        put(Tab.MEMORIES, new org.papiricoh.supernaturalcraft.client.book.memories.MemoriesSection());
    }

    private void put(Tab t, BookSection s) {
        s.attach(this);
        sections.put(t, s);
    }

    // --- sections -------------------------------------------------------------------------------

    public Tab tab() {
        return tab;
    }

    public BookSection section() {
        return sections.get(tab);
    }

    public HomeSection home() {
        return (HomeSection) sections.get(Tab.HOME);
    }

    public JournalSection journal() {
        return (JournalSection) sections.get(Tab.JOURNAL);
    }

    public ScriptoriumSection scriptorium() {
        return (ScriptoriumSection) sections.get(Tab.SCRIPTORIUM);
    }

    public RoadmapSection roadmap() {
        return (RoadmapSection) sections.get(Tab.ROADMAP);
    }

    public org.papiricoh.supernaturalcraft.client.book.archive.ArchiveSection archive() {
        return (org.papiricoh.supernaturalcraft.client.book.archive.ArchiveSection) sections.get(Tab.ARCHIVE);
    }

    public org.papiricoh.supernaturalcraft.client.book.memories.MemoriesSection memories() {
        return (org.papiricoh.supernaturalcraft.client.book.memories.MemoriesSection) sections.get(Tab.MEMORIES);
    }

    /** Opens a page in the tab that holds it: the Archive's (Men of Letters) pages there, every other in the Journal. */
    public void openEntry(ResourceLocation id) {
        var p = org.papiricoh.supernaturalcraft.client.book.journal.JournalPages.find(id);
        boolean archive = p != null && p.entry().chapter() == org.papiricoh.supernaturalcraft.journal.JournalChapter.ARCHIVE;
        if (archive && Tab.ARCHIVE.shown()) {
            if (tab != Tab.ARCHIVE) show(Tab.ARCHIVE);
            archive().openEntry(id);
        } else {
            if (tab != Tab.JOURNAL) show(Tab.JOURNAL);
            journal().openEntry(id);
        }
    }

    /** Turns to another tab (rebuilding the widgets). */
    public void show(Tab t) {
        if (!t.shown()) t = Tab.HOME;
        if (t != tab) section().hidden();
        tab = t;
        lastTab = t;
        if (minecraft != null) rebuildWidgets();
    }

    /** Re-adds the current section's widgets (after it changed what it shows). */
    public void refresh() {
        rebuildWidgets();
    }

    /** Adds a widget at book coordinates. */
    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable & NarratableEntry> T add(T widget) {
        return addRenderableWidget(widget);
    }

    // --- book space -----------------------------------------------------------------------------

    @Override
    protected void init() {
        double gs = minecraft.getWindow().getGuiScale();
        // Whole real pixels per book pixel, as many as fit; a window too small shrinks the book.
        int k = (int) Math.floor(Math.min((width - 12) * gs / BookStyle.W, (height - 12) * gs / BookStyle.H));
        scale = k >= 1 ? (float) (k / gs) : (float) Math.min((width - 12) / (double) BookStyle.W, (height - 12) / (double) BookStyle.H);
        left = Math.round((width - BookStyle.W * scale) / 2);
        top = Math.round((height - BookStyle.H * scale) / 2);
        section().init();
    }

    public float scale() {
        return scale;
    }

    public double toBookX(double screenX) {
        return (screenX - left) / scale;
    }

    public double toBookY(double screenY) {
        return (screenY - top) / scale;
    }

    public int toScreenX(double bookX) {
        return (int) Math.round(left + bookX * scale);
    }

    public int toScreenY(double bookY) {
        return (int) Math.round(top + bookY * scale);
    }

    /**
     * Clips drawing to a rectangle in book space. Vanilla's scissor ignores the pose, so a section
     * must clip through here. Pair with {@code g.disableScissor()}.
     */
    public void scissor(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.enableScissor(toScreenX(x0), toScreenY(y0), toScreenX(x1), toScreenY(y1));
    }

    /** Shows a tooltip at the mouse, at the normal GUI size, after this frame. */
    public void tooltip(List<Component> lines) {
        if (lines.isEmpty()) return;
        Component all = lines.getFirst().copy();
        for (int i = 1; i < lines.size(); i++) all = Component.empty().append(all).append("\n").append(lines.get(i));
        setTooltipForNextRenderPass(Tooltip.splitTooltip(minecraft, all));
    }

    // --- drawing --------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        int mx = (int) Math.floor(toBookX(mouseX)), my = (int) Math.floor(toBookY(mouseY));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(left, top, 0);
        pose.scale(scale, scale, 1);
        drawTabs(g, mx, my, false);
        g.blit(BookAtlas.SPREAD, 0, 0, 0, 0, BookStyle.BOOK_W, BookStyle.H, BookAtlas.SPREAD_SHEET, BookAtlas.SPREAD_SHEET);
        drawTabs(g, mx, my, true);
        BookSection s = section();
        s.render(g, mx, my, partial);
        drawingBook = true;
        super.render(g, mx, my, partial);
        drawingBook = false;
        s.renderOverlay(g, mx, my, partial);
        pose.popPose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Screen.render draws the background again before the widgets: once (outside the book) is enough.
        if (!drawingBook) super.renderBackground(g, mouseX, mouseY, partial);
    }

    /** Inactive tabs tuck under the cover; the open one is drawn over it. */
    private void drawTabs(GuiGraphics g, int mx, int my, boolean activeOnly) {
        for (Tab t : Tab.values()) {
            if (!t.shown()) continue;
            boolean active = t == tab;
            if (active != activeOnly) continue;
            int x = BookStyle.TAB_X + (active ? 2 : 0), y = tabY(t);
            drawTab(g, t, active, x, y);
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(t.icon)));
            g.renderItem(icon, x + 10, y + 3);
            if (overTab(t, mx, my)) tooltip(List.of(t.title()));
        }
    }

    /** Whether book_widgets.png has a leather tab of the Memories' own (its sixth column); until then it borrows a tinted one. */
    private static final boolean MEMORIES_TAB_ART = false;

    private static void drawTab(GuiGraphics g, Tab t, boolean active, int x, int y) {
        if (t != Tab.MEMORIES || MEMORIES_TAB_ART) {
            BookAtlas.tab(t.ordinal(), active).draw(g, x, y);
            return;
        }
        // The roadmap's ochre leather, paled toward Heaven's ivory.
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(1f, 0.97f, 0.84f, 1f);
        BookAtlas.tab(Tab.ROADMAP.ordinal(), active).draw(g, x, y);
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static int tabY(Tab t) {
        return BookStyle.TAB_Y + t.ordinal() * BookStyle.TAB_GAP;
    }

    private static boolean overTab(Tab t, double mx, double my) {
        int y = tabY(t);
        return mx >= BookStyle.TAB_X + 6 && mx < BookStyle.TAB_X + BookAtlas.TAB_W + 2 && my >= y && my < y + BookAtlas.TAB_H;
    }

    @Override
    public void tick() {
        section().tick();
    }

    // --- input, mapped into book space ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = toBookX(mouseX), my = toBookY(mouseY);
        for (Tab t : Tab.values()) {
            if (t.shown() && overTab(t, mx, my)) {
                if (t != tab) {
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0f));
                    show(t);
                }
                return true;
            }
        }
        if (super.mouseClicked(mx, my, button)) return true;
        return section().mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mx = toBookX(mouseX), my = toBookY(mouseY);
        boolean handled = section().mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button) || handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double mx = toBookX(mouseX), my = toBookY(mouseY);
        if (section().mouseDragged(mx, my, button, dragX / scale, dragY / scale)) return true;
        return super.mouseDragged(mx, my, button, dragX / scale, dragY / scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = toBookX(mouseX), my = toBookY(mouseY);
        if (section().mouseScrolled(mx, my, scrollX, scrollY)) return true;
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(toBookX(mouseX), toBookY(mouseY));
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (super.keyPressed(key, scan, modifiers)) return true;
        return section().keyPressed(key, scan, modifiers);
    }

    @Override
    public void removed() {
        section().hidden();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
