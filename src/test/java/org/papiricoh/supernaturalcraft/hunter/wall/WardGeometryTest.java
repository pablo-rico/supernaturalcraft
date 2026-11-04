package org.papiricoh.supernaturalcraft.hunter.wall;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WardGeometryTest {

    @Test
    void aWardGuardsItsSphereAndASupremeOneTwice() {
        assertTrue(WardGeometry.inside(0.5 + WardGeometry.RADIUS - 0.1, 0.5, 0.5, 0, 0, 0, WardGeometry.radius(false)));
        assertFalse(WardGeometry.inside(0.5 + WardGeometry.RADIUS + 0.1, 0.5, 0.5, 0, 0, 0, WardGeometry.radius(false)));
        assertEquals(2 * WardGeometry.RADIUS, WardGeometry.radius(true));
    }

    @Test
    void outwardPointsAwayFromTheCentre() {
        double[] o = WardGeometry.outward(5.5, 0.5, 0, 0);
        assertEquals(1, o[0], 1e-9);
        assertEquals(0, o[1], 1e-9);
        double[] c = WardGeometry.outward(0.5, 0.5, 0, 0);
        assertEquals(1, Math.hypot(c[0], c[1]), 1e-9);
    }
}
