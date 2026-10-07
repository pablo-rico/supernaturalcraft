package org.papiricoh.supernaturalcraft.ritual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternGeometryTest {

    @Test
    void cellsAreRelativeToTheAltar() {
        List<PatternGeometry.Cell> cells = PatternGeometry.cells(List.of("c#c", "#A#", "c #"));
        assertEquals(7, cells.size(), "blank cells and the altar itself are not cells");
        assertTrue(cells.contains(new PatternGeometry.Cell(-1, -1, 'c')));
        assertTrue(cells.contains(new PatternGeometry.Cell(1, 1, '#')));
    }

    @Test
    void rotationIsClockwiseSeenFromAbove() {
        assertArrayEquals(new int[]{0, 1}, PatternGeometry.rotate(1, 0, 1), "east turns south");
        assertArrayEquals(new int[]{-1, 0}, PatternGeometry.rotate(1, 0, 2), "east turns west");
        assertArrayEquals(new int[]{0, -1}, PatternGeometry.rotate(1, 0, 3), "east turns north");
        assertArrayEquals(new int[]{2, -3}, PatternGeometry.rotate(2, -3, 4), "four turns is identity");
    }

    @Test
    void patternNeedsExactlyOneAltar() {
        assertThrows(IllegalArgumentException.class, () -> PatternGeometry.cells(List.of("###")));
        assertThrows(IllegalArgumentException.class, () -> PatternGeometry.cells(List.of("A#A")));
    }
}
