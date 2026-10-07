package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.RailTrapLayout;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.RailTrapLayout.Cell;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.RailTrapLayout.Shape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Samuel Colt's devil's trap in rails: a closed circle with a star inside, within its radius, the centre left free. */
class RailTrapLayoutTest {

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    @Test
    void railsRunTheRightWay() {
        assertEquals(Shape.EW, Shape.along(0));
        assertEquals(Shape.DIAG_A, Shape.along(45));
        assertEquals(Shape.NS, Shape.along(90));
        assertEquals(Shape.DIAG_B, Shape.along(135));
        assertEquals(Shape.EW, Shape.along(180));
        assertEquals(Shape.NS, Shape.along(-90));
    }

    @Test
    void everyRailIsInsideTheTrapAndTheCentreIsFree() {
        List<Cell> cells = RailTrapLayout.cells();
        assertTrue(cells.size() > 30, "too few rails: " + cells.size());
        Set<Long> seen = new HashSet<>();
        for (Cell c : cells) {
            assertTrue(RailTrapLayout.inside(c.dx(), c.dz()), "rail outside the trap at " + c);
            assertTrue(seen.add(key(c.dx(), c.dz())), "two rails in one cell at " + c);
        }
        assertFalse(seen.contains(key(0, 0)), "the altar's cell must stay free");
    }

    @Test
    void theCircleIsClosed() {
        int r = RailTrapLayout.RADIUS;
        Map<Long, Cell> ring = new HashMap<>();
        for (Cell c : RailTrapLayout.cells()) {
            if (Math.abs(Math.hypot(c.dx(), c.dz()) - r) <= 0.5) ring.put(key(c.dx(), c.dz()), c);
        }
        for (Cell c : ring.values()) {
            int neighbours = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx != 0 || dz != 0) && ring.containsKey(key(c.dx() + dx, c.dz() + dz))) neighbours++;
                }
            }
            assertTrue(neighbours >= 2, "the circle is broken at " + c);
        }
    }

    @Test
    void theStarHasFivePointsOnTheCircle() {
        Set<Long> cells = new HashSet<>();
        for (Cell c : RailTrapLayout.cells()) cells.add(key(c.dx(), c.dz()));
        int r = RailTrapLayout.RADIUS;
        for (int k = 0; k < 5; k++) {
            double a = Math.toRadians(-90 + k * 72);
            int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
            assertTrue(cells.contains(key(x, z)), "star point " + k + " missing");
        }
        // The star's lines cross the inside of the circle, not only its rim.
        long inner = RailTrapLayout.cells().stream().filter(c -> Math.hypot(c.dx(), c.dz()) < r - 1.5).count();
        assertTrue(inner >= 10, "too few rails inside the circle: " + inner);
    }

    @Test
    void theLayoutIsSymmetricLeftToRight() {
        Set<Long> cells = new HashSet<>();
        for (Cell c : RailTrapLayout.cells()) cells.add(key(c.dx(), c.dz()));
        for (Cell c : RailTrapLayout.cells()) assertTrue(cells.contains(key(-c.dx(), c.dz())), "no mirror for " + c);
    }
}
