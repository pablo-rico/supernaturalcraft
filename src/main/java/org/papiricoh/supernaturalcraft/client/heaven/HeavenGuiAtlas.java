package org.papiricoh.supernaturalcraft.client.heaven;

/**
 * Where each piece of Heaven's HUD sits in {@code textures/gui/heaven.png} ({@code HeavenAssets.GUI_ATLAS}, drawn by
 * {@code tools/artgen/heaven_gui_art.py}). Pure: regions only. {@link HeavenGui} draws them, or a plain stand-in of fills while
 * {@link #USE_ATLAS} is off or the sheet is missing, so swapping the art in is one flag and these numbers.
 * <p>Every region is a nine-slice with a {@link Region#border} (0 = drawn at its own size or stretched whole).
 */
public final class HeavenGuiAtlas {

    /** Whether the sheet's regions below are the art's (they are: {@code heaven_gui_art.py}); off draws every piece with fills. */
    public static final boolean USE_ATLAS = true;
    /** The sheet's size. */
    public static final int SHEET_W = 256, SHEET_H = 512;

    /** A rectangle of the sheet. */
    public record Region(int u, int v, int w, int h, int border) {
    }

    /** The QTE: its frame (the strap's track at x 4-179, y 13-22), the bar's fill, and the fill once time runs short. */
    public static final Region QTE_FRAME = new Region(0, 172, 184, 30, 6), QTE_FILL = new Region(0, 202, 176, 10, 0),
            QTE_DANGER = new Region(0, 212, 176, 10, 0);
    /** The form card (a sheet of Heaven's stationery). */
    public static final Region FORM_CARD = new Region(0, 224, 104, 64, 5);
    /** The stamps, 72x22. */
    public static final Region APPROVED = new Region(104, 224, 72, 22, 0), DENIED = new Region(104, 246, 72, 22, 0),
            OVERDUE = new Region(176, 224, 72, 22, 0);
    /** The docket panel (a clipboard) and its strike-through line (stretched across an entry), the lit row. */
    public static final Region DOCKET = new Region(0, 288, 136, 112, 6), STRIKE = new Region(136, 288, 112, 6, 0),
            DOCKET_ROW = new Region(136, 296, 112, 14, 0);
    /** The memory toast's frame, 160x32 like vanilla's. */
    public static final Region TOAST = new Region(0, 140, 160, 32, 0);
    /** Ash's menu panel (bar wood and brass). */
    public static final Region ASH_PANEL = new Region(0, 0, 200, 140, 8);
    /** The plot's progress bar: frame and fill. */
    public static final Region PLOT_BAR = new Region(136, 312, 104, 10, 3), PLOT_FILL = new Region(136, 322, 100, 6, 0);

    /** A cabinet's numeral I-IV, 16x16. */
    public static Region numeral(int n) {
        return new Region(176 + 16 * (Math.max(1, Math.min(4, n)) - 1), 246, 16, 16, 0);
    }

    private HeavenGuiAtlas() {
    }
}
