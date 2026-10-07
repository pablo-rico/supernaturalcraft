package org.papiricoh.supernaturalcraft.ritual;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry of a ritual layout, separated from blocks so it can be unit-tested: where each
 * symbol sits relative to the altar, under each of the four rotations.
 */
public final class PatternGeometry {

    public static final char ALTAR = 'A';
    public static final char ANY = ' ';

    public record Cell(int dx, int dz, char symbol) {
    }

    private PatternGeometry() {
    }

    /** Every non-blank, non-altar cell as an offset from the altar. Throws unless exactly one altar. */
    public static List<Cell> cells(List<String> rows) {
        int ax = -1, az = -1;
        for (int z = 0; z < rows.size(); z++) {
            int x = rows.get(z).indexOf(ALTAR);
            if (x >= 0) {
                if (ax >= 0 || rows.get(z).indexOf(ALTAR, x + 1) >= 0) {
                    throw new IllegalArgumentException("ritual pattern has more than one altar");
                }
                ax = x;
                az = z;
            }
        }
        if (ax < 0) throw new IllegalArgumentException("ritual pattern has no altar ('A')");
        List<Cell> cells = new ArrayList<>();
        for (int z = 0; z < rows.size(); z++) {
            String row = rows.get(z);
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c != ANY && c != ALTAR) cells.add(new Cell(x - ax, z - az, c));
            }
        }
        return cells;
    }

    /** Rotates an offset by {@code quarterTurns} × 90° clockwise seen from above (+x east, +z south). */
    public static int[] rotate(int dx, int dz, int quarterTurns) {
        return switch (Math.floorMod(quarterTurns, 4)) {
            case 1 -> new int[]{-dz, dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{dz, -dx};
            default -> new int[]{dx, dz};
        };
    }
}
