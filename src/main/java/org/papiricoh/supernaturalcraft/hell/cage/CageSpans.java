package org.papiricoh.supernaturalcraft.hell.cage;

import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;

import static org.papiricoh.supernaturalcraft.hell.cage.CageKit.*;
import static org.papiricoh.supernaturalcraft.hell.cage.CageLayout.*;

/**
 * The four ways out (pure): arched bridges from the island's rim, a forecourt before each gatehouse, the gatehouse's carved
 * portal and its tunnel through the Pit's wall, and the anchor plates in the wall above where the great chains are made fast.
 * Drawn once in a spoke's own frame (running east) and turned to each spoke; the west bridge has a broken span patched with
 * iron plates, the north one a balustrade with gaps.
 */
final class CageSpans {

    private CageSpans() {
    }

    static final int DECK = ISLAND_Y;
    /** Where the bridge leaves the rim, its two piers, and where the forecourt begins. */
    static final int FROM = ISLAND_RADIUS + 1, PIER_A = 38, PIER_B = 47, COURT = 56;
    /** The balcony midway along the bridge. */
    static final int BALCONY = 42;
    static final int FACADE = GATE_START, FACADE_HALF = 10;

    static void draw(Canvas c) {
        for (int q = 0; q < 4; q++) {
            Spoke s = new Spoke(c, q);
            bridge(s, q == 2, q == 3);
            forecourt(s);
            gatehouse(s);
            anchorPlate(s);
            c.anchor("gate_" + Dir.HORIZONTAL[(q + 1) % 4].id(), s.x(GATE_END - 2, 0), DECK + 1, s.z(GATE_END - 2, 0));
        }
    }

    // --- bridges -------------------------------------------------------------------------------------------------------------

    /** The underside's lowest level at {@code t}: a segmental arch between the rim, the piers and the forecourt. */
    static int intrados(int t) {
        int a, b;
        if (t <= PIER_A) {
            a = RIM_RADIUS;
            b = PIER_A;
        } else if (t <= PIER_B) {
            a = PIER_A;
            b = PIER_B;
        } else {
            a = PIER_B;
            b = COURT;
        }
        double u = Math.min(1, Math.abs(t - (a + b) / 2.0) / ((b - a) / 2.0));
        return DECK - 3 - (int) Math.round(7 * u * u);
    }

    private static void bridge(Spoke s, boolean broken, boolean gapped) {
        for (int t = FROM; t < COURT; t++) {
            boolean breach = broken && t >= 50 && t <= 53;
            for (int w = -4; w <= 4; w++) {
                int aw = Math.abs(w);
                boolean lost = breach && aw >= 2 && (aw >= 3 || s.hash(t, 0, w, 3) < 0.6);
                if (lost) continue;
                if (aw <= 3) {
                    String deck;
                    if (breach && aw <= 1) deck = FRAME;
                    else if (aw == 3) deck = BRICKS;
                    else if (t % 6 == 0) deck = w == 0 ? SEAL : PB_BRICKS;
                    else deck = aw == 2 ? PB_BRICKS : FLAG;
                    s.set(t, DECK, w, deck);
                    s.set(t, DECK - 1, w, breach && aw <= 1 ? FRAME : masonry(s.x(t, w), DECK - 1, s.z(t, w)));
                    s.set(t, DECK - 2, w, aw == 3 ? FRAME : masonry(s.x(t, w), DECK - 2, s.z(t, w)));
                    if (!breach && t > RIM_RADIUS) arch(s, t, w);
                } else if (t > RIM_RADIUS) {
                    s.set(t, DECK - 1, w, stairs(w > 0 ? Dir.NORTH : Dir.SOUTH, true));
                }
            }
            if (!breach) parapet(s, t, gapped);
        }
        piers(s);
        balcony(s);
        // Chains with lanterns under the middle of each span, a gibbet under the longest.
        for (int mid : new int[]{(RIM_RADIUS + PIER_A) / 2, (PIER_A + PIER_B) / 2, (PIER_B + COURT) / 2}) {
            int y = intrados(mid) - 1;
            if (mid == (PIER_B + COURT) / 2 && !broken) {
                hangSpoke(s, mid, y, y - 5, 0, false);
                gibbet(s.c(), s.x(mid, 0), y - 6, s.z(mid, 0), 2);
            } else {
                hangSpoke(s, mid, y, y - 3 - (int) (s.hash(mid, 1, 0, 5) * 6), 0, true);
            }
        }
        if (broken) {
            // Loose ends at the breach: chains dangling from what is left of the span.
            for (int t : new int[]{50, 53}) for (int w : new int[]{-1, 1}) hangSpoke(s, t, DECK - 3, DECK - 6 - (int) (s.hash(t, 2, w, 9) * 6), w, false);
        }
    }

    private static void hangSpoke(Spoke s, int t, int top, int bottom, int w, boolean lantern) {
        hang(s.c(), s.x(t, w), top, bottom, s.z(t, w), lantern);
    }

    /** The arch under the deck at (t, w): masonry down to the intrados, voussoirs of frame along it, a seal roundel mid-span. */
    private static void arch(Spoke s, int t, int w) {
        int bottom = intrados(t);
        for (int y = DECK - 3; y >= bottom; y--) {
            String m = masonry(s.x(t, w), y, s.z(t, w));
            if (y == bottom) m = FRAME;
            else if (y == bottom + 1 && Math.abs(w) == 3) m = PB_BRICKS;
            else if (Math.abs(w) == 3 && y == DECK - 4 && (t == (PIER_A + PIER_B) / 2 || t == (PIER_B + COURT) / 2 + 1)) m = SEAL;
            s.set(t, y, w, m);
        }
    }

    /** Bars between posts on the deck's edge, brick pilasters every eight, braziers on every other. */
    private static void parapet(Spoke s, int t, boolean gapped) {
        if (t < RIM_RADIUS) return;
        if (t == RIM_RADIUS) {
            // The bridgehead: a pier each side with a brazier, where the rim's balustrade ends.
            for (int w : new int[]{-3, 3}) {
                s.set(t, DECK + 1, w, BRICKS);
                s.set(t, DECK + 2, w, CHISELED);
                s.set(t, DECK + 3, w, BRAZIER);
            }
            return;
        }
        for (int w : new int[]{-3, 3}) {
            if (t >= BALCONY - 1 && t <= BALCONY + 1) continue;
            if (gapped && (w > 0 ? t >= 33 && t <= 35 : t >= 50 && t <= 52)) {
                if (s.hash(t, 4, w, 2) < 0.6) s.set(t, DECK + 1, w, slab("bottom"));
                continue;
            }
            if (t % 8 == 4) {
                s.set(t, DECK + 1, w, BRICKS);
                s.set(t, DECK + 2, w, CHISELED);
                if (t % 16 == 4) s.set(t, DECK + 3, w, BRAZIER);
                else s.set(t, DECK + 3, w, lantern(false));
            } else {
                boolean post = t % 4 == 0;
                s.set(t, DECK + 1, w, post ? post() : bars());
                s.set(t, DECK + 2, w, post ? post() : bars());
            }
        }
    }

    /**
     * The piers: no ground to stand on this far up, so they end in pendants over the lava, anchors on their flanks with chains
     * and lanterns hanging from them.
     */
    private static void piers(Spoke s) {
        for (int p : new int[]{PIER_A, PIER_B}) {
            for (int dt = -1; dt <= 1; dt++) {
                for (int w = -3; w <= 3; w++) {
                    int aw = Math.abs(w), t = p + dt;
                    if (s.get(t, DECK, w) == null) continue;
                    int bottom = dt == 0 ? (aw <= 1 ? DECK - 17 : aw <= 2 ? DECK - 15 : DECK - 12) : aw <= 2 ? DECK - 12 : DECK - 11;
                    for (int y = intrados(t) - 1; y >= bottom; y--) s.set(t, y, w, masonry(s.x(t, w), y, s.z(t, w)));
                    if (aw == 3 && dt == 0) s.set(t, DECK - 6, w, FRAME);
                    s.set(t, bottom, w, dt == 0 && aw == 0 ? SEAL : FRAME);
                }
            }
            s.set(p, DECK - 18, 0, BEDROCK);
            hangSpoke(s, p, DECK - 19, DECK - 22 - (int) (s.hash(p, 0, 0, 1) * 6), 0, true);
            for (int w : new int[]{-4, 4}) {
                s.set(p, DECK - 7, w, anchor(w > 0 ? Dir.SOUTH : Dir.NORTH));
                hangSpoke(s, p, DECK - 8, DECK - 12 - (int) (s.hash(p, 3, w, 1) * 4), w, true);
            }
        }
    }

    /** A balcony out over the drop on each side, a watcher in it looking at the road, braziers at its outer corners. */
    private static void balcony(Spoke s) {
        for (int side : new int[]{-1, 1}) {
            Dir in = side > 0 ? Dir.NORTH : Dir.SOUTH;
            for (int t = BALCONY - 2; t <= BALCONY + 2; t++) {
                for (int k = 4; k <= 6; k++) {
                    int w = side * k;
                    boolean edge = k == 6 || t == BALCONY - 2 || t == BALCONY + 2;
                    s.set(t, DECK, w, edge ? BRICKS : (t + k) % 2 == 0 ? FLAG : PB_BRICKS);
                    s.set(t, DECK - 1, w, k == 6 ? stairs(in, true) : BRICKS);
                    if (k == 4) s.set(t, DECK - 2, w, stairs(in, true));
                    if (!edge) continue;
                    boolean corner = k == 6 && (t == BALCONY - 2 || t == BALCONY + 2);
                    if (corner) {
                        s.set(t, DECK + 1, w, BRICKS);
                        s.set(t, DECK + 2, w, BRAZIER);
                    } else {
                        s.set(t, DECK + 1, w, t == BALCONY && k == 6 ? post() : bars());
                    }
                }
            }
            s.set(BALCONY, DECK + 1, side * 5, watcher(in));
        }
    }

    // --- the forecourt -----------------------------------------------------------------------------------------------------------

    /** A landing before the gate, as wide as the facade, on corbelled masonry, with braziers and a balustrade. */
    private static void forecourt(Spoke s) {
        int h = FACADE_HALF - 1;
        for (int t = COURT; t < FACADE; t++) {
            for (int w = -h; w <= h; w++) {
                int aw = Math.abs(w);
                String f = aw == h || t == COURT ? PB_BRICKS : aw <= 3 ? (t == COURT + 3 && w == 0 ? SEAL : FLAG)
                        : (t + aw) % 3 == 0 ? CHISELED : FLAG;
                if (aw == 4 && t > COURT) f = PB_BRICKS;
                s.set(t, DECK, w, f);
                for (int y = DECK - 1; y >= DECK - 3; y--) s.set(t, y, w, masonry(s.x(t, w), y, s.z(t, w)));
                // Corbelled underneath, stepping in toward the wall.
                int depth = (h - aw) / 2 + 3;
                for (int y = DECK - 4; y >= DECK - 3 - depth; y--) s.set(t, y, w, y == DECK - 3 - depth ? foot(t, w) : masonry(s.x(t, w), y, s.z(t, w)));
            }
            for (int w : new int[]{-h, h}) {
                boolean pier = t == COURT || t == FACADE - 1;
                if (pier) {
                    s.set(t, DECK + 1, w, BRICKS);
                    s.set(t, DECK + 2, w, CHISELED);
                    s.set(t, DECK + 3, w, BRAZIER);
                } else {
                    s.set(t, DECK + 1, w, t % 2 == 0 ? post() : bars());
                    s.set(t, DECK + 2, w, t % 2 == 0 ? post() : bars());
                }
            }
        }
        for (int w = -h + 1; w <= h - 1; w++) {
            int aw = Math.abs(w);
            if (aw <= 3) continue;
            if (aw == 4) {
                s.set(COURT, DECK + 1, w, BRICKS);
                s.set(COURT, DECK + 2, w, CHISELED);
                s.set(COURT, DECK + 3, w, BRAZIER);
            } else {
                s.set(COURT, DECK + 1, w, aw == 6 ? post() : bars());
            }
        }
        // Nothing of the wall may stand over the road.
        for (int t = CLEAR_START; t < FACADE; t++) {
            for (int w = -h; w <= h; w++) {
                for (int y = DECK + 1; y <= DECK + GATE_HEIGHT + 4; y++) if (s.get(t, y, w) == null && s.rock(t, y, w)) s.carve(t, y, w);
            }
        }
    }

    /** The underside of corbelled masonry: frame and bedrock in a checker. */
    private static String foot(int t, int w) {
        return (t + w & 1) == 0 ? FRAME : BEDROCK;
    }

    // --- the gatehouse -------------------------------------------------------------------------------------------------------------

    /** The tunnel's open profile (feet at {@code DECK + 1}): seven wide, then stepping in to a pointed top at {@code DECK + 8}. */
    static boolean open(int w, int y) {
        int aw = Math.abs(w), h = y - DECK;
        return h >= 1 && (h <= 6 && aw <= GATE_HALF || h == 7 && aw <= GATE_HALF - 1 || h == 8 && aw <= GATE_HALF - 2);
    }

    /** The arch's haunches: stairs upside down, stepping in over the opening. */
    static Dir haunch(int w, int y) {
        int aw = Math.abs(w), h = y - DECK;
        if (aw == 0 || h < 7 || h > 9 || aw != GATE_HALF + 7 - h) return null;
        return w > 0 ? Dir.SOUTH : Dir.NORTH;
    }

    private static void gatehouse(Spoke s) {
        int h = FACADE_HALF;
        int roof = DECK + 16;
        for (int t = FACADE; t <= GATE_END; t++) {
            for (int w = -h - 1; w <= h + 1; w++) {
                int aw = Math.abs(w);
                for (int y = DECK - 10; y <= roof + 1; y++) {
                    boolean lining = aw <= GATE_HALF + 1 && y >= DECK - 1 && y <= DECK + 10;
                    if (open(w, y)) {
                        s.carve(t, y, w);
                        continue;
                    }
                    Dir hn = haunch(w, y);
                    if (hn != null) {
                        s.set(t, y, w, stairs(hn, true));
                        continue;
                    }
                    if (!lining && s.depth(t, y, w) > 0.25) continue;
                    String m = body(s, t, y, w, roof);
                    if (m != null) s.set(t, y, w, m);
                }
            }
            // The tunnel: a seal down the middle of its floor, ribs of frame, lanterns hanging from the crown.
            if (t > FACADE) {
                if (t % 6 == 2) {
                    s.set(t, DECK, 0, SEAL);
                    s.set(t, DECK + 8, 0, lantern(true));
                }
                if (t % 6 == 2 || t == GATE_END) {
                    // Braziers in niches low in both walls.
                    for (int w : new int[]{-GATE_HALF - 1, GATE_HALF + 1}) {
                        s.set(t, DECK + 1, w, BRAZIER);
                        s.carve(t, DECK + 2, w);
                    }
                }
                if (t % 6 == 5) {
                    for (int w = -GATE_HALF - 1; w <= GATE_HALF + 1; w++) {
                        for (int y = DECK; y <= DECK + 10; y++) {
                            String cur = s.get(t, y, w);
                            if (cur != null && !cur.equals("minecraft:air") && !cur.contains("stairs") && y > DECK) s.set(t, y, w, FRAME);
                        }
                    }
                }
            }
        }
        facade(s);
    }

    /** What the gatehouse holds at (t, y, w) behind the facade (null: nothing). */
    private static String body(Spoke s, int t, int y, int w, int roof) {
        int aw = Math.abs(w);
        if (y > roof) return aw == FACADE_HALF && t % 2 == 0 && t > FACADE ? BRICKS : null;
        if (y == roof) return aw == FACADE_HALF ? FRAME : (t + w) % 4 == 0 ? PB_BRICKS : FLAG;
        if (aw > FACADE_HALF) {
            // Buttresses down the gatehouse's flanks.
            if (t % 4 != 1 || t == FACADE || y < DECK - 4) return null;
            return y == roof - 1 ? stairs(w > 0 ? Dir.NORTH : Dir.SOUTH, false) : masonry(s.x(t, w), y, s.z(t, w));
        }
        // Corbelled beneath, stepping in.
        if (y < DECK - 3) {
            int depth = (FACADE_HALF - aw) / 2 + 3;
            if (y < DECK - 3 - depth) return null;
            return y == DECK - 3 - depth ? foot(t, w) : masonry(s.x(t, w), y, s.z(t, w));
        }
        if (y == DECK && aw <= GATE_HALF) return aw == GATE_HALF ? PB_BRICKS : FLAG;
        if (aw == FACADE_HALF && y > DECK && y % 4 == 0) return FRAME;
        return masonry(s.x(t, w), y, s.z(t, w));
    }

    /**
     * The portal: an archivolt of frame with a seal for keystone, twin Enochian pilasters each side with a watcher in the niche
     * between them, a frieze of seals, a cornice, and a stepped pediment with a medallion and a brazier at its apex.
     */
    private static void facade(Spoke s) {
        int f = FACADE, front = f - 1;
        // Archivolt round the opening.
        for (int w = -GATE_HALF - 1; w <= GATE_HALF + 1; w++) {
            for (int y = DECK + 1; y <= DECK + 10; y++) {
                if (open(w, y) || haunch(w, y) != null) continue;
                boolean edge = false;
                for (int dw = -1; dw <= 1; dw++) for (int dy = -1; dy <= 1; dy++) edge |= open(w + dw, y + dy) || haunch(w + dw, y + dy) != null;
                if (edge) s.set(front, y, w, w == 0 ? SEAL : FRAME);
            }
        }
        for (int side : new int[]{-1, 1}) {
            Dir in = side > 0 ? Dir.NORTH : Dir.SOUTH;
            for (int k : new int[]{6, 8}) {
                int w = side * k;
                s.set(front, DECK + 1, w, capital(false));
                for (int y = DECK + 2; y <= DECK + 12; y++) s.set(front, y, w, pillar());
                s.set(front, DECK + 13, w, capital(true));
            }
            // The niche between the pilasters, a watcher kneeling in it, a seal above.
            int w = side * 7;
            for (int y = DECK + 1; y <= DECK + 3; y++) s.carve(f, y, w);
            s.set(f, DECK + 1, w, watcher(Dir.WEST));
            s.set(f, DECK + 4, w, stairs(Dir.EAST, true));
            s.set(f, DECK + 6, w, SEAL);
            s.set(front, DECK + 11, side * 5, anchor(Dir.WEST));
            hangSpoke(s, front, DECK + 10, DECK + 8, side * 5, false);
            // Flanking quoins of frame on the facade's corners.
            for (int y = DECK + 1; y <= DECK + 15; y++) if (y % 3 == 0) s.set(f, y, side * FACADE_HALF, FRAME);
            s.set(front, DECK + 1, side * 10, BRICKS);
            s.set(front, DECK + 2, side * 10, stairs(Dir.EAST, false));
        }
        // Frieze and cornice.
        for (int w = -FACADE_HALF; w <= FACADE_HALF; w++) {
            int aw = Math.abs(w);
            if (aw <= FACADE_HALF - 1) s.set(front, DECK + 14, w, FRAME);
            if (aw <= FACADE_HALF - 1) s.set(front, DECK + 15, w, aw % 3 == 0 ? SEAL : BRICKS);
            s.set(front - 1, DECK + 16, w, stairs(Dir.EAST, true));
            s.set(front, DECK + 16, w, BRICKS);
        }
        // The stepped pediment, its medallion, a pair of anchors and the apex brazier.
        int base = DECK + 17;
        for (int k = 0; k <= 9; k++) {
            int y = base + k, half = 9 - k;
            for (int w = -half; w <= half; w++) {
                String m = masonry(s.x(f, w), y, s.z(f, w));
                if (Math.abs(w) == half) m = half == 0 ? CHISELED : stairs(w > 0 ? Dir.NORTH : Dir.SOUTH, false);
                s.set(f, y, w, m);
                if (Math.abs(w) < half) s.set(f + 1, y, w, masonry(s.x(f + 1, w), y, s.z(f + 1, w)));
            }
        }
        s.set(f, base + 10, 0, BRAZIER);
        int my = base + 4;
        for (int w = -3; w <= 3; w++) {
            for (int dy = -3; dy <= 3; dy++) {
                double d = Math.sqrt(w * w + dy * dy);
                if (d >= 2.5 && d < 3.5) s.set(front, my + dy, w, FRAME);
                else if (d < 1) s.set(front, my + dy, w, SEAL);
                else if (d < 1.5) s.set(front, my + dy, w, BRICKS);
            }
        }
        for (int side : new int[]{-1, 1}) {
            s.set(front, base + 2, side * 5, anchor(Dir.WEST));
            hangSpoke(s, front, base + 1, base, side * 5, false);
        }
    }

    // --- the anchor plates -------------------------------------------------------------------------------------------------------

    /** Where a great chain is made fast above the gate: a plate sunk in the wall, two anchors on it, a corbel under it. */
    private static void anchorPlate(Spoke s) {
        int front = plateFront(s.q());
        for (int w = -PLATE_HALF; w <= PLATE_HALF; w++) {
            for (int dy = -PLATE_RISE; dy <= PLATE_RISE; dy++) {
                int aw = Math.abs(w), ady = Math.abs(dy), y = ANCHOR_Y + dy;
                if (aw + ady > PLATE_HALF + PLATE_RISE - 2) continue;
                boolean border = aw == PLATE_HALF || ady == PLATE_RISE || aw + ady == PLATE_HALF + PLATE_RISE - 2;
                int back = wallAt(s.q(), w, y) + 1;
                for (int t = front; t <= back; t++) {
                    String m = masonry(s.x(t, w), y, s.z(t, w));
                    if (t == front) {
                        if (border) m = FRAME;
                        else if (w == 0 && dy == 0) m = SEAL;
                        else if (w == 0 || dy == 0) m = PB_BRICKS;
                        else if (aw == CHAIN_W && ady <= 1) m = FRAME;
                    }
                    s.set(t, y, w, m);
                }
            }
        }
        for (int w = -PLATE_HALF + 2; w <= PLATE_HALF - 2; w++) s.set(front - 1, ANCHOR_Y - PLATE_RISE, w, stairs(Dir.EAST, true));
        for (int w : new int[]{-CHAIN_W, CHAIN_W}) s.set(front - 1, ANCHOR_Y, w, anchor(Dir.WEST));
        for (int w : new int[]{-PLATE_HALF + 1, PLATE_HALF - 1}) {
            s.set(front - 1, ANCHOR_Y + PLATE_RISE - 1, w, BRICKS);
            s.set(front - 1, ANCHOR_Y + PLATE_RISE, w, BRAZIER);
        }
    }
}
