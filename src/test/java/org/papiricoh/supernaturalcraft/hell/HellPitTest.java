package org.papiricoh.supernaturalcraft.hell;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.hell.worldgen.PitShape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Pit's shaft: air around the origin from the lava up, rock beyond its wall, closed at both ends. */
class HellPitTest {


    @Test
    void theShaftIsOpenAroundTheCage() {
        for (int y = CageLayout.ISLAND_BOTTOM; y <= CageLayout.CAGE_ROOF + 20; y += 4) {
            for (int r = 0; r <= 50; r += 5) {
                assertTrue(PitShape.at(r, y, 0) < 0, "rock in the shaft at r=" + r + " y=" + y);
                assertTrue(PitShape.at(0, y, -r) < 0, "rock in the shaft at r=" + r + " y=" + y);
            }
        }
    }

    @Test
    void farFromTheShaftNothingChanges() {
        assertEquals(PitShape.SOLID, PitShape.at(200, 100, 0));
        assertEquals(PitShape.SOLID, PitShape.at(0, 100, -150));
        assertTrue(PitShape.at(95, 100, 0) > 0, "the wall must stand by 95 blocks out");
    }

    @Test
    void theEndsAreClosed() {
        assertEquals(PitShape.SOLID, PitShape.at(0, 5, 0));
        assertEquals(PitShape.SOLID, PitShape.at(0, 250, 0));
    }

    @Test
    void theWayOutIsCutThroughTheWall() {
        // Along the four bridges, the wall stands between where the way is cleared and where the gates' tunnels end.
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2 * k;
            for (int y = CageLayout.ISLAND_Y; y <= CageLayout.ISLAND_Y + CageLayout.GATE_HEIGHT; y++) {
                double r = PitShape.wallRadius(PitShape.RADIUS, a, y);
                assertTrue(r > CageLayout.CLEAR_START + 2, "wall at " + r + " cuts the bridge short");
                assertTrue(r < CageLayout.GATE_END - 8, "wall at " + r + " is past the end of the tunnel");
            }
        }
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 360; i++) min = Math.min(min, PitShape.wallRadius(PitShape.RADIUS, Math.toRadians(i), CageLayout.ISLAND_Y));
        assertTrue(min > CageLayout.ISLAND_RADIUS + 20, "the Pit must leave room around the island");
    }
}
