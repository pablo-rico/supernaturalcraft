package org.papiricoh.supernaturalcraft.legacy.cases.table;

import org.jetbrains.annotations.Nullable;

/**
 * The war table's 3×3 grid (v0.19), pure: a formed piece is one of nine parts, row-major from the north-west corner (0) to the
 * south-east (8), 4 the centre (the devil's trap's scheme). Turns follow Minecraft's {@code Rotation.CLOCKWISE_90}:
 * (dx, dz) → (−dz, dx).
 */
public final class TableGrid {

    public static final int CENTER = 4;
    public static final int PARTS = 9;
    private static final String[] NAMES = {"nw", "n", "ne", "w", "center", "e", "sw", "s", "se"};

    /** Whether the cell at (x, z), relative to the piece just placed, holds a loose piece. */
    @FunctionalInterface
    public interface Cells {
        boolean loose(int x, int z);
    }

    private TableGrid() {
    }

    /** Offset of {@code part} from the centre, east. */
    public static int dx(int part) {
        return part % 3 - 1;
    }

    /** Offset of {@code part} from the centre, south. */
    public static int dz(int part) {
        return part / 3 - 1;
    }

    /** The part at offset (dx, dz) from the centre, or −1 off the grid. */
    public static int of(int dx, int dz) {
        if (dx < -1 || dx > 1 || dz < -1 || dz > 1) return -1;
        return (dz + 1) * 3 + dx + 1;
    }

    public static String name(int part) {
        return NAMES[part];
    }

    /** {@code part} after {@code quarter} clockwise quarter turns. */
    public static int rotate(int part, int quarter) {
        int x = dx(part), z = dz(part);
        for (int i = 0; i < Math.floorMod(quarter, 4); i++) {
            int t = x;
            x = -z;
            z = t;
        }
        return of(x, z);
    }

    /** {@code part} mirrored east–west ({@code flipX}) or north–south. */
    public static int mirror(int part, boolean flipX) {
        return flipX ? of(-dx(part), dz(part)) : of(dx(part), -dz(part));
    }

    /** Whether {@code part} has no neighbour of the table towards (sx, sz) — an outer edge on that side. */
    public static boolean outer(int part, int sx, int sz) {
        return of(dx(part) + sx, dz(part) + sz) < 0;
    }

    /**
     * The 3×3 window a piece just placed at (0, 0) completes: of the nine windows holding it, those whose nine cells are all
     * loose; of those, the one whose centre lies furthest north, then west, whatever the order the pieces went down in.
     *
     * @return the window's centre relative to the piece, {@code {cx, cz}}, or null
     */
    public static int @Nullable [] window(Cells cells) {
        for (int cz = -1; cz <= 1; cz++) {
            for (int cx = -1; cx <= 1; cx++) {
                if (full(cells, cx, cz)) return new int[]{cx, cz};
            }
        }
        return null;
    }

    private static boolean full(Cells cells, int cx, int cz) {
        for (int p = 0; p < PARTS; p++) if (!cells.loose(cx + dx(p), cz + dz(p))) return false;
        return true;
    }
}
