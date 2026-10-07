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
        ROADMAP("minecraft:filled_map");

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
    }

    /** The tab the book was last left open at. */
    private static Tab lastTab = Tab.HOME;

    private final Map<Tab, BookSection> sections = new EnumMap<>(Tab.class);
    private Tab tab;
    private float scale = 1;
    private int left, top;
    private boolean drawingBook;

    public HunterBookScreen() {
        this(lastTab);
    }

    public HunterBookScreen(Tab tab) {
        super(Component.translatable("screen.supernaturalcraft.book"));
        this.tab = tab;
        put(Tab.HOME, new HomeSection());
        put(Tab.JOURNAL, new JournalSection());
        put(Tab.SCRIPTORIUM, new ScriptoriumSection());
        put(Tab.ROADMAP, new RoadmapSection());
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

    /** Turns to another tab (rebuilding the widgets). */
    public void show(Tab t) {
        if (t != tab) section().hidden();
        tab = t;
        lastTab = t;
        rebuildWidgets();
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
            boolean active = t == tab;
            if (active != activeOnly) continue;
            int x = BookStyle.TAB_X + (active ? 2 : 0), y = tabY(t);
            BookAtlas.tab(t.ordinal(), active).draw(g, x, y);
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(t.icon)));
            g.renderItem(icon, x + 10, y + 3);
            if (overTab(t, mx, my)) tooltip(List.of(t.title()));
        }
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
            if (overTab(t, mx, my)) {
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
