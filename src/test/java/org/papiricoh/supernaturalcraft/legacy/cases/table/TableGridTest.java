package org.papiricoh.supernaturalcraft.legacy.cases.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The war table's grid: offsets, turns and mirrors that stay on the grid, and one deterministic window per 3×3 of loose pieces. */
class TableGridTest {

    @Test
    void partsAndOffsetsRoundTrip() {
        assertEquals(TableGrid.CENTER, TableGrid.of(0, 0));
        assertEquals("nw", TableGrid.name(TableGrid.of(-1, -1)));
        assertEquals("se", TableGrid.name(TableGrid.of(1, 1)));
        assertEquals(-1, TableGrid.of(2, 0));
        for (int p = 0; p < TableGrid.PARTS; p++) assertEquals(p, TableGrid.of(TableGrid.dx(p), TableGrid.dz(p)));
        for (TablePart t : TablePart.values()) assertEquals(t, TablePart.of(t.part()));
        assertEquals("single", TablePart.SINGLE.getSerializedName());
        assertEquals("center", TablePart.CENTER.getSerializedName());
    }

    @Test
    void turnsMatchMinecraftClockwise() {
        // CLOCKWISE_90 takes (x, z) to (-z, x): the north-west corner goes north-east, north goes east.
        assertEquals(TableGrid.of(1, -1), TableGrid.rotate(TableGrid.of(-1, -1), 1));
        assertEquals(TableGrid.of(1, 0), TableGrid.rotate(TableGrid.of(0, -1), 1));
        assertEquals(TableGrid.CENTER, TableGrid.rotate(TableGrid.CENTER, 3));
        for (int p = 0; p < TableGrid.PARTS; p++) {
            assertEquals(p, TableGrid.rotate(p, 4));
            assertEquals(p, TableGrid.rotate(TableGrid.rotate(p, 1), -1));
            assertEquals(p, TableGrid.mirror(TableGrid.mirror(p, true), true));
            assertEquals(p, TableGrid.mirror(TableGrid.mirror(p, false), false));
        }
        assertEquals(TableGrid.of(1, -1), TableGrid.mirror(TableGrid.of(-1, -1), true));
        assertEquals(TableGrid.of(-1, 1), TableGrid.mirror(TableGrid.of(-1, -1), false));
    }

    @Test
    void outerEdgesAreTheRim() {
        assertTrue(TableGrid.outer(TableGrid.of(-1, -1), -1, 0));
        assertTrue(TableGrid.outer(TableGrid.of(-1, -1), 0, -1));
        assertFalse(TableGrid.outer(TableGrid.of(-1, -1), 1, 0));
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) assertFalse(TableGrid.outer(TableGrid.CENTER, d[0], d[1]));
    }

    @Test
    void aFull3x3FormsWhereverTheLastPieceGoes() {
        for (int px = -1; px <= 1; px++) {
            for (int pz = -1; pz <= 1; pz++) {
                // A 3×3 of loose pieces centred at (0,0), the last one set down at (px, pz).
                int fx = px, fz = pz;
                int[] w = TableGrid.window((x, z) -> Math.abs(x + fx) <= 1 && Math.abs(z + fz) <= 1);
                assertNotNull(w);
                assertEquals(-px, w[0]);
                assertEquals(-pz, w[1]);
            }
        }
    }

    @Test
    void nothingFormsShortOfNine() {
        assertNull(TableGrid.window((x, z) -> Math.abs(x) <= 1 && Math.abs(z) <= 1 && !(x == 1 && z == 1)));
        assertNull(TableGrid.window((x, z) -> x >= -1 && x <= 1 && z >= 0 && z <= 1));
    }

    @Test
    void largerGridsPickTheNorthWestWindow() {
        // A 4×4 of loose pieces from (-1,-1) to (2,2): the piece at (0,0) sits in four full windows.
        int[] w = TableGrid.window((x, z) -> x >= -1 && x <= 2 && z >= -1 && z <= 2);
        assertNotNull(w);
        assertArrayEquals(new int[]{0, 0}, w);
        // A 4-wide row: the window furthest west.
        int[] row = TableGrid.window((x, z) -> x >= -2 && x <= 1 && Math.abs(z) <= 1);
        assertArrayEquals(new int[]{-1, 0}, row);
    }
}
