package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GhostBalanceTest {

    @Test
    void nightByTheClock() {
        assertFalse(GhostBalance.isNight(6000));
        assertTrue(GhostBalance.isNight(13000));
        assertTrue(GhostBalance.isNight(18000 + 24000L * 5));
        assertFalse(GhostBalance.isNight(23000));
        assertFalse(GhostBalance.isNight(-1000));
    }

    @Test
    void flickersAreBriefRareAndDeterministic() {
        for (int id = 1; id < 40; id++) {
            int on = 0, runs = 0;
            boolean last = false;
            for (long t = 0; t < 20000; t++) {
                boolean f = GhostBalance.flickers(id, t);
                assertEquals(f, GhostBalance.flickers(id, t));
                if (f) on++;
                if (f && !last) runs++;
                last = f;
            }
            double share = on / 20000.0;
            assertTrue(share > 0.005 && share < 0.06, "ghost " + id + " shows " + share + " of the time");
            assertTrue(runs > 0 && on / (double) runs <= GhostBalance.FLICKER_LENGTH, "flickers should be short");
        }
        int together = 0;
        for (long t = 0; t < 20000; t++) if (GhostBalance.flickers(1, t) && GhostBalance.flickers(2, t)) together++;
        assertTrue(together < 100, "two ghosts should not blink in step");
    }

    @Test
    void dispersalAndFadeTiming() {
        assertTrue(GhostBalance.stillDispersed(100, 100));
        assertTrue(GhostBalance.stillDispersed(100, 100 + GhostBalance.DISPERSE_TICKS - 1));
        assertFalse(GhostBalance.stillDispersed(100, 100 + GhostBalance.DISPERSE_TICKS));
        assertFalse(GhostBalance.stillDispersed(-1, 5));
        assertEquals(0f, GhostBalance.fadeProgress(50, 50));
        assertEquals(1f, GhostBalance.fadeProgress(50, 50 + GhostBalance.FADE_TICKS + 3));
    }
}
