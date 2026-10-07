package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaKind.*;

/**
 * Chapter four, the Scribe's library: the floor is a giant open book (pages of paper ruled with lines of ink,
 * a leather cover, an ink gutter down the spine) on dark boards, and three concentric arcs of bookshelves rise
 * round it, taller the further out (stacks, then shelves with candles, then a towering wall with hanging lanterns),
 * with piles of books between. The Author stands at the centre of the book. Pure.
 */
public final class LibraryLayout {

    private static final int SALT = 0x11B;

    private LibraryLayout() {
    }

    /** Half-width (x) and half-height (z) of the open book on the floor. */
    public static int bookHalfWidth(int r) {
        return Math.round(r * 0.76f);
    }

    public static int bookHalfHeight(int r) {
        return Math.round(r * 0.56f);
    }

    public static ArenaPlan plan(int r, long seed) {
        ArenaPlan.Builder b = new ArenaPlan.Builder(r);
        int bw = bookHalfWidth(r), bh = bookHalfHeight(r);
        // --- the floor: an open book on dark boards ----------------------------------------------------------
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!b.inside(dx, dz)) continue;
                b.set(dx, 0, dz, floor(dx, dz, bw, bh, seed));
            }
        }
        // --- three arcs of shelves -----------------------------------------------------------------------------
        arc(b, seed, r * 0.42, 4, 5, 4, 1);
        arc(b, seed, r * 0.70, 7, 9, 4, 2);
        arc(b, seed, r - 2.0, 12, 22, 3, 3);
        // --- piles of books between the arcs -----------------------------------------------------------------
        int piles = Math.max(10, r / 2);
        for (int i = 0; i < piles; i++) {
            double a = Math.PI * 2 * (i + ArenaNoise.rand(seed, SALT + 1, i, 0) * 0.7) / piles;
            boolean inner = i % 2 == 0;
            double d = inner ? r * (0.18 + ArenaNoise.rand(seed, SALT + 1, i, 1) * 0.16)
                    : r * (0.5 + ArenaNoise.rand(seed, SALT + 1, i, 1) * 0.12);
            int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
            if (x * x + z * z < 9) continue;
            int h = 1 + (int) (ArenaNoise.rand(seed, SALT + 1, i, 2) * 3);
            b.column(x, z, 1, h, BOOKSHELF);
            if (h > 1) b.column(x + 1, z, 1, h - 1, BOOKSHELF);
            if (h > 2) b.set(x, h + 1, z, CANDLES);
            else b.set(x, h + 1, z, inner ? LANTERN : CANDLES);
        }
        return b.build(0, 1, 0);
    }

    /** The book: cover, gutter, pages with lines of text; dark boards outside it. */
    static ArenaKind floor(int dx, int dz, int bw, int bh, long seed) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax > bw || az > bh) return DARK_PLANKS;
        if (ax == bw || az == bh || (az == bh - 1 && ax > 0) || (ax == bw - 1)) return COVER;
        if (dx == 0) return INK;
        // Text: a line every third row inside the margins, broken into words.
        int margin = 3;
        if (ax < margin || ax > bw - margin || az > bh - margin) return PAGE;
        int row = dz + bh;
        if (row % 3 != 0) return PAGE;
        int page = dx < 0 ? 0 : 1;
        int col = ax - margin;
        // A paragraph's first line is indented; the last line of each paragraph stops short.
        int line = row / 3;
        if (line % 5 == 0 && col < 3) return PAGE;
        int lineLength = line % 5 == 4 ? (int) ((bw - 2 * margin) * (0.3 + ArenaNoise.rand(seed, SALT + 2, line, page) * 0.4))
                : bw - 2 * margin;
        if (col > lineLength) return PAGE;
        // Words of 2-6 letters with single spaces.
        int pos = 0, w = 0;
        while (pos <= col) {
            int len = 2 + (int) (ArenaNoise.rand(seed, SALT + 3, line * 2 + page, w) * 5);
            if (col < pos + len) return INK;
            if (col == pos + len) return PAGE;
            pos += len + 1;
            w++;
        }
        return PAGE;
    }

    /**
     * An arc of shelves round the centre: runs of {@code run} cells and {@code gap}-cell aisles, {@code h} high, with
     * dark log posts at the ends and a plank cap. {@code dressing}: 1 lanterns on the cap, 2 candles on the cap,
     * 3 lanterns hanging on brackets on the inner face.
     */
    private static void arc(ArenaPlan.Builder b, long seed, double radius, int h, int run, int gap, int dressing) {
        List<ArenaShapes.RingCell> ring = ArenaShapes.ring(radius);
        int period = run + gap;
        int phase = (int) (ArenaNoise.rand(seed, SALT + 4, (int) radius, 0) * period);
        for (int i = 0; i < ring.size(); i++) {
            int k = Math.floorMod(i + phase, period);
            if (k >= run) continue;
            ArenaShapes.RingCell c = ring.get(i);
            int segment = Math.floorDiv(i + phase, period);
            int top = h + (dressing == 3 ? (int) (ArenaNoise.rand(seed, SALT + 5, segment, 0) * 3) : 0);
            boolean end = k == 0 || k == run - 1;
            b.column(c.x(), c.z(), 1, top, end ? DARK_LOG : BOOKSHELF);
            b.set(c.x(), top + 1, c.z(), end ? DARK_LOG : DARK_PLANKS);
            if (dressing == 1 && k == run / 2) b.set(c.x(), top + 2, c.z(), LANTERN);
            if (dressing == 2 && k % 3 == 1) b.set(c.x(), top + 2, c.z(), CANDLES);
            if (dressing == 3 && k % 6 == 3) {
                // A bracket one cell in from the wall, a lantern hanging under it.
                double len = Math.hypot(c.x(), c.z());
                int ix = (int) Math.round(c.x() - c.x() / len), iz = (int) Math.round(c.z() - c.z() / len);
                if (b.get(ix, 1, iz) == null) {
                    b.set(ix, 8, iz, DARK_PLANKS);
                    b.set(ix, 7, iz, HANGING_LANTERN);
                }
            }
        }
    }
}
