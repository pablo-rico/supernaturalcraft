package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import java.util.ArrayList;
import java.util.List;

/**
 * Metatron's library of Heaven, laid out round the arena's centre: two broken rings of shelves (with
 * gaps, and a clear corridor north and south to the stairs), reading lecterns and piles of books; and,
 * once he takes to it, the dais with its two flights of stairs. Pure geometry; offsets from the centre,
 * {@code dy} counted up from the first block above the floor.
 */
public final class ScriptoriumLayout {

    public enum Kind { SHELF, CHISELED, LECTERN, DAIS, DAIS_TOP, STAIR }

    /** One block. For {@link Kind#STAIR}, {@code north} says which way the flight climbs. */
    public record Place(int dx, int dy, int dz, Kind kind, boolean north) {
    }

    public static final int INNER_RING = 9, OUTER_RING = 15, DAIS_HALF = 2, DAIS_HEIGHT = 3;
    /** Half-width of the north-south corridor kept clear for the stairs. */
    public static final int CORRIDOR = 1;

    private ScriptoriumLayout() {
    }

    /** The library: shelves, lecterns and piles of books (never inside the dais's footprint or the corridor). */
    public static List<Place> library() {
        List<Place> out = new ArrayList<>();
        ring(out, INNER_RING, 2, 5, 3);
        ring(out, OUTER_RING, 3, 6, 3);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            out.add(new Place((int) Math.round(Math.cos(a) * 12), 0, (int) Math.round(Math.sin(a) * 12), Kind.LECTERN, false));
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 8 + k * Math.PI / 4;
            int x = (int) Math.round(Math.cos(a) * 18), z = (int) Math.round(Math.sin(a) * 18);
            if (Math.abs(x) <= CORRIDOR) continue;
            out.add(new Place(x, 0, z, Kind.CHISELED, false));
            if (k % 2 == 0) out.add(new Place(x, 1, z, Kind.CHISELED, false));
        }
        return out;
    }

    /** A ring of shelf runs {@code run} cells long with {@code gap}-cell gaps, {@code height} high. */
    private static void ring(List<Place> out, int radius, int height, int run, int gap) {
        int cells = (int) Math.round(2 * Math.PI * radius);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int i = 0; i < cells; i++) {
            if (i % (run + gap) >= run) continue;
            double a = 2 * Math.PI * i / cells;
            int x = (int) Math.round(Math.cos(a) * radius), z = (int) Math.round(Math.sin(a) * radius);
            if (Math.abs(x) <= CORRIDOR || !seen.add(((long) x << 32) | (z & 0xFFFFFFFFL))) continue;
            for (int y = 0; y < height; y++) out.add(new Place(x, y, z, Kind.SHELF, false));
        }
    }

    /** The dais: a block of scripture stone with two flights of stairs, north and south. */
    public static List<Place> dais() {
        List<Place> out = new ArrayList<>();
        for (int dx = -DAIS_HALF; dx <= DAIS_HALF; dx++) {
            for (int dz = -DAIS_HALF; dz <= DAIS_HALF; dz++) {
                for (int y = 0; y < DAIS_HEIGHT; y++) {
                    out.add(new Place(dx, y, dz, y == DAIS_HEIGHT - 1 ? Kind.DAIS_TOP : Kind.DAIS, false));
                }
            }
        }
        for (int side : new int[]{-1, 1}) {
            for (int step = 1; step <= DAIS_HEIGHT; step++) {
                int dz = side * (DAIS_HALF + step);
                int top = DAIS_HEIGHT - step;  // this step's tread is at dy = top
                for (int dx = -CORRIDOR; dx <= CORRIDOR; dx++) {
                    for (int y = 0; y < top; y++) out.add(new Place(dx, y, dz, Kind.DAIS, false));
                    out.add(new Place(dx, top, dz, Kind.STAIR, side < 0));
                }
            }
        }
        return out;
    }

    /** Where he stands on the dais: its centre, on top. */
    public static int lecternDy() {
        return DAIS_HEIGHT;
    }

    /** Whether a cell belongs to the dais or its stairs (rewrites keep away from it). */
    public static boolean daisFootprint(int dx, int dz) {
        if (Math.abs(dx) <= DAIS_HALF && Math.abs(dz) <= DAIS_HALF) return true;
        return Math.abs(dx) <= CORRIDOR && Math.abs(dz) <= DAIS_HALF + DAIS_HEIGHT;
    }
}
