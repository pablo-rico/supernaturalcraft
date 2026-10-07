package org.papiricoh.supernaturalcraft.entity.chuck.arena;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BlankPageErosion;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BlankPageErosion.Column;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.LavaIslands;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Blank Page erasing itself, and the islands spared when the floor is lava. */
class BlankPageErosionTest {

    @Test
    void theSafeDiscIsNeverErased() {
        assertTrue(BlankPageErosion.SAFE_RADIUS >= 7);
        for (int r : new int[]{16, 34, 48}) {
            List<Column> order = BlankPageErosion.order(r, 42);
            int area = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) if (dx * dx + dz * dz <= r * r) area++;
            }
            int safe = 0;
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) if (dx * dx + dz * dz <= 49) safe++;
            }
            assertEquals(area - safe, order.size(), "every other column goes");
            for (Column c : order) {
                assertTrue(c.dx() * c.dx() + c.dz() * c.dz() > 49, "erases the safe disc at " + c);
                assertTrue(c.at() > 0 && c.at() <= 1, "moment out of range: " + c);
            }
            assertEquals(0, BlankPageErosion.due(order, 0.0), "nothing goes before the erosion starts");
            assertEquals(order.size(), BlankPageErosion.due(order, 1.0), "all gone by the end");
            assertEquals(BlankPageErosion.SAFE_RADIUS, BlankPageErosion.intactRadius(order, order.size(), r));
        }
    }

    @Test
    void itIsDeterministicAndSeeded() {
        assertEquals(BlankPageErosion.order(34, 7), BlankPageErosion.order(34, 7));
        assertNotEquals(BlankPageErosion.order(34, 7), BlankPageErosion.order(34, 8));
    }

    @Test
    void theEdgesGoFirstInARaggedFront() {
        int r = 34;
        List<Column> order = BlankPageErosion.order(r, 99);
        double rim = 0, inner = 0;
        int nRim = 0, nInner = 0;
        for (Column c : order) {
            double d = Math.hypot(c.dx(), c.dz());
            if (d > r - 3) {
                rim += c.at();
                nRim++;
            } else if (d < BlankPageErosion.SAFE_RADIUS + 4) {
                inner += c.at();
                nInner++;
            }
        }
        assertTrue(rim / nRim + 0.4 < inner / nInner, "rim " + rim / nRim + " vs inner " + inner / nInner);
        // Ragged: halfway through, the front is not a circle (the distance of what's gone varies).
        int half = BlankPageErosion.due(order, 0.5);
        double min = Double.MAX_VALUE, max = 0;
        for (int i = 0; i < half; i++) {
            Column c = order.get(i);
            double d = Math.hypot(c.dx(), c.dz());
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        assertTrue(max - min > 8, "front too even: " + min + ".." + max);
        int intact = BlankPageErosion.intactRadius(order, half, r);
        assertTrue(intact >= BlankPageErosion.SAFE_RADIUS && intact < r, "intact radius " + intact);
    }

    @Test
    void progressRunsFromTheDelayToTheEnd() {
        assertEquals(0, BlankPageErosion.progress(0));
        assertEquals(0, BlankPageErosion.progress(BlankPageErosion.DELAY));
        assertEquals(1, BlankPageErosion.progress(BlankPageErosion.DELAY + BlankPageErosion.DURATION));
        List<Column> order = BlankPageErosion.order(34, 1);
        int last = 0;
        for (int t = 0; t <= BlankPageErosion.DELAY + BlankPageErosion.DURATION; t += 100) {
            int due = BlankPageErosion.due(order, BlankPageErosion.progress(t));
            assertTrue(due >= last, "the page grew back at " + t);
            last = due;
        }
    }

    @Test
    void theFloorIsLavaButSparesIslands() {
        for (int radius : new int[]{4, 8, 12}) {
            List<LavaIslands.Island> islands = LavaIslands.islands(radius, 1234);
            assertTrue(islands.size() >= 2);
            int spared = 0, all = 0;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    all++;
                    if (LavaIslands.spared(islands, dx, dz)) spared++;
                }
            }
            assertTrue(spared >= 5 && spared < all * 0.7, "radius " + radius + ": " + spared + "/" + all + " spared");
            assertTrue(Math.hypot(islands.getFirst().x(), islands.getFirst().z()) <= radius * 0.5, "no island near the centre");
            assertEquals(islands, LavaIslands.islands(radius, 1234));
        }
    }
}
