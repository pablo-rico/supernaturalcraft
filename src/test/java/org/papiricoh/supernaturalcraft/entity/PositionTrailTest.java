package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.PositionTrail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PositionTrailTest {

    @Test
    void fiveSecondsBack() {
        PositionTrail trail = PositionTrail.covering(100, 5);
        for (long t = 0; t <= 300; t += 5) trail.record(t, t, 64, -t);
        PositionTrail.Sample s = trail.back(300, 100);
        assertEquals(200, s.tick());
        assertEquals(200, s.x());
        assertEquals(-200, s.z());
    }

    @Test
    void theNewestSampleOldEnough() {
        PositionTrail trail = PositionTrail.covering(100, 5);
        for (long t = 0; t <= 300; t += 5) trail.record(t, t, 64, 0);
        // 297 - 100 = 197: the newest sample at least that old is 195.
        assertEquals(195, trail.back(297, 100).tick());
    }

    @Test
    void aYoungTrailGivesItsOldest() {
        PositionTrail trail = PositionTrail.covering(100, 5);
        assertNull(trail.back(10, 100));
        trail.record(40, 1, 2, 3);
        trail.record(45, 4, 5, 6);
        assertEquals(40, trail.back(50, 100).tick());
    }

    @Test
    void theRingForgetsTheOldest() {
        PositionTrail trail = new PositionTrail(4);
        for (long t = 0; t < 10; t++) trail.record(t, t, 0, 0);
        assertEquals(4, trail.size());
        assertEquals(9, trail.newest(0).tick());
        assertEquals(6, trail.newest(3).tick());
        assertEquals(6, trail.back(9, 100).tick(), "older than it holds: the oldest it has");
        assertEquals(3, trail.backIndex(9, 100));
    }
}
