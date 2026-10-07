package org.papiricoh.supernaturalcraft.client.book;

/**
 * The Hunter's Book's layout and inks. Everything is laid out in book space: a fixed
 * {@link #W}×{@link #H} canvas the screen scales to fit (see {@link HunterBookScreen}).
 */
public final class BookStyle {

    /** Book space: the open book (416 wide) plus the tabs that stick out of its right edge. */
    public static final int W = 440, H = 272;
    /** The open book itself. */
    public static final int BOOK_W = 416;

    /** The left page's writable area. */
    public static final int LEFT_X = 22, PAGE_Y = 18, PAGE_W = 180, PAGE_H = 232;
    /** The right page's writable area. */
    public static final int RIGHT_X = 214;
    /** Both pages, gutter included (the roadmap's canvas). */
    public static final int SPREAD_X = 16, SPREAD_Y = 12, SPREAD_W = 384, SPREAD_H = 248;
    /** Where the tabs hang, one under the other. */
    public static final int TAB_X = BOOK_W - 8, TAB_Y = 24, TAB_GAP = 26;

    public static final int INK = 0xFF3B2A1A;
    public static final int FADED = 0xFF7A6448;
    public static final int GOLD = 0xFFB07D18;
    public static final int BLOOD = 0xFF8A1C12;
    public static final int MANA = 0xFF3D5FA8;
    /** Highlight under the mouse. */
    public static final int HOVER = 0x33B07D18;

    private BookStyle() {
    }
}
