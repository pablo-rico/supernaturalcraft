package org.papiricoh.supernaturalcraft.client.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * One section of the Hunter's Book (a tab). The book keeps one instance per section, so a section
 * remembers where it was while the reader looks elsewhere. Everything is in book space
 * ({@link BookStyle}): {@link #render} draws over the open pages; widgets are added in {@link #init}
 * with {@code book.add(...)} at book coordinates; mouse coordinates arrive already in book space.
 */
public abstract class BookSection {

    protected HunterBookScreen book;
    protected Minecraft mc;
    protected Font font;

    final void attach(HunterBookScreen book) {
        this.book = book;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    /** Called each time the section is shown (and on resize): add widgets here. */
    public void init() {
    }

    /** Draw over the open book, before the widgets. */
    public abstract void render(GuiGraphics g, int mx, int my, float partial);

    /** Draw over the widgets (overlays, drag ghosts). */
    public void renderOverlay(GuiGraphics g, int mx, int my, float partial) {
    }

    public void tick() {
    }

    /** Whether the section hides the pages' text area entirely (the roadmap's canvas). */
    public boolean coversPages() {
        return false;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return false;
    }

    public boolean keyPressed(int key, int scan, int modifiers) {
        return false;
    }

    /** Called when the reader leaves the section or closes the book. */
    public void hidden() {
    }

    /**
     * A named state of this section for {@code SN_PREVIEW=book} to photograph: {@code setup} puts the
     * section in that state (open an entry, zoom the canvas...).
     */
    public record PreviewShot(String name, java.util.function.Consumer<HunterBookScreen> setup) {
    }

    /** The states {@code SN_PREVIEW=book} photographs, in order. By default, just as it opens. */
    public java.util.List<PreviewShot> previewShots() {
        return java.util.List.of(new PreviewShot("open", b -> {
        }));
    }
}
