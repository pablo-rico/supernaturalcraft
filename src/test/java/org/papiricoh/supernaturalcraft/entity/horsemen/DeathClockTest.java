package org.papiricoh.supernaturalcraft.entity.horsemen;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathClock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathClockTest {

    private static DeathClock.Event run(DeathClock c, int ticks, boolean dead) {
        DeathClock.Event last = DeathClock.Event.NONE;
        for (int i = 0; i < ticks; i++) {
            DeathClock.Event e = c.tick(dead);
            if (e != DeathClock.Event.NONE) last = e;
        }
        return last;
    }

    @Test
    void aBlowWindsItBack() {
        DeathClock c = new DeathClock(1200, 300);
        run(c, 900, false);
        assertEquals(300, c.remaining());
        c.reset();
        assertEquals(1200, c.remaining());
    }

    @Test
    void atZeroLimboLastsFifteenSecondsThenDeath() {
        DeathClock c = new DeathClock(1200, 300);
        assertEquals(DeathClock.Event.ENTER_LIMBO, run(c, 1200, false));
        assertTrue(c.inLimbo());
        assertEquals(300, c.limboLeft());
        c.reset();
        assertTrue(c.inLimbo(), "a blow does nothing in limbo: only the light gets you out");
        assertEquals(DeathClock.Event.NONE, run(c, 299, false));
        assertEquals(DeathClock.Event.DIE, c.tick(false));
        assertFalse(c.inLimbo());
    }

    @Test
    void theLightGetsYouOut() {
        DeathClock c = new DeathClock(1200, 300);
        run(c, 1205, false);
        assertTrue(c.inLimbo());
        c.escape();
        assertFalse(c.inLimbo());
        assertEquals(1200, c.remaining());
    }

    @Test
    void theWorldOfTheDeadRunsItDouble() {
        DeathClock c = new DeathClock(1200, 300);
        assertEquals(DeathClock.Event.ENTER_LIMBO, run(c, 600, true));
    }

    @Test
    void reapersAreSeenWhenTimeIsShort() {
        DeathClock c = new DeathClock(1200, 300);
        assertFalse(c.reapersSeen());
        run(c, 1200 - DeathClock.REAPERS_SEEN_BELOW + 1, false);
        assertTrue(c.reapersSeen());
        c.steal(10_000);
        assertEquals(1, c.remaining(), "a reaper's touch never empties the clock by itself");
    }
}
