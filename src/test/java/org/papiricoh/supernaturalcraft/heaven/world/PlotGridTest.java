package org.papiricoh.supernaturalcraft.heaven.world;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotLayout;
import org.papiricoh.supernaturalcraft.heaven.plot.PlotGrid;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The square spiral that lays Heaven's plots out (v0.18). */
class PlotGridTest {

    @Test
    void theRoadhouseIsAtTheOrigin() {
        assertArrayEquals(new int[]{0, 0}, PlotGrid.cell(PlotGrid.HUB));
        assertEquals(PlotGrid.HUB, PlotGrid.index(0, 0));
        assertEquals(0, PlotGrid.ring(0));
    }

    @Test
    void theFirstRingSurroundsIt() {
        Set<String> seen = new HashSet<>();
        for (int i = 1; i <= 8; i++) {
            int[] c = PlotGrid.cell(i);
            assertEquals(1, Math.max(Math.abs(c[0]), Math.abs(c[1])), "plot " + i + " is not in ring 1");
            assertTrue(seen.add(c[0] + "," + c[1]), "plot " + i + " repeats a cell");
            assertEquals(1, PlotGrid.ring(i));
        }
        assertEquals(2, PlotGrid.ring(9));
        assertEquals(2, PlotGrid.ring(24));
        assertEquals(3, PlotGrid.ring(25));
    }

    @Test
    void indexAndCellAreInverse() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            int[] c = PlotGrid.cell(i);
            assertEquals(i, PlotGrid.index(c[0], c[1]), "round trip of plot " + i);
            assertTrue(seen.add(c[0] + "," + c[1]), "plot " + i + " repeats a cell");
        }
        // Every cell of the first rings has an index below the ring's end.
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                int i = PlotGrid.index(x, z);
                assertArrayEquals(new int[]{x, z}, PlotGrid.cell(i));
                assertTrue(i < 121, "cell " + x + "," + z + " out of the 11x11 square");
            }
        }
    }

    @Test
    void consecutivePlotsAreNeighbours() {
        for (int i = 1; i < 5000; i++) {
            int[] a = PlotGrid.cell(i), b = PlotGrid.cell(i + 1);
            int step = Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
            // Inside a ring the walk moves one cell; from the end of a ring to the next one's start it steps out.
            if (PlotGrid.ring(i) == PlotGrid.ring(i + 1)) assertEquals(1, step, "plots " + i + " and " + (i + 1));
            else assertTrue(step <= 2, "plots " + i + " and " + (i + 1));
        }
    }

    @Test
    void originsAreSpacedAndNearestFindsThem() {
        int s = HeavenDimension.PLOT_SPACING;
        assertArrayEquals(new int[]{0, 0}, PlotGrid.origin(0, s));
        for (int i = 0; i < 200; i++) {
            int[] o = PlotGrid.origin(i, s);
            assertEquals(0, Math.floorMod(o[0], s));
            assertEquals(0, Math.floorMod(o[1], s));
            assertEquals(i, PlotGrid.nearest(o[0], o[1], s));
            assertEquals(i, PlotGrid.nearest(o[0] + HeavenPlotLayout.RADIUS, o[1] - HeavenPlotLayout.RADIUS, s));
            assertEquals(i, PlotGrid.nearest(o[0] - s / 2.0 + 1, o[1] + s / 2.0 - 1, s));
        }
    }

    @Test
    void plotsNeverOverlap() {
        // A plot (and its office, which reaches past the island) stays well inside its cell.
        int reach = HeavenPlotLayout.RADIUS + 48;
        assertTrue(2 * reach < HeavenDimension.PLOT_SPACING);
        assertTrue(PlotGrid.within(reach, -reach, reach));
        assertFalse(PlotGrid.within(reach + 1, 0, reach));
    }
}
