package org.papiricoh.supernaturalcraft.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The per-player request pace (v0.19): a burst, then the refill rate, kept apart per player and per kind. */
class RateLimiterTest {

    @Test
    void aBurstThenThePace() {
        UUID p = UUID.randomUUID();
        int ok = 0;
        for (int i = 0; i < 10; i++) if (RateLimiter.allow(p, "x", 2, 4, 100)) ok++;
        assertEquals(4, ok, "the burst");
        assertFalse(RateLimiter.allow(p, "x", 2, 4, 105), "a quarter second refills half a token");
        assertTrue(RateLimiter.allow(p, "x", 2, 4, 110), "half a second refills one");
        assertFalse(RateLimiter.allow(p, "x", 2, 4, 110));
        RateLimiter.forget(p);
    }

    @Test
    void neverAboveTheBurst() {
        UUID p = UUID.randomUUID();
        RateLimiter.allow(p, "x", 2, 4, 0);
        int ok = 0;
        for (int i = 0; i < 10; i++) if (RateLimiter.allow(p, "x", 2, 4, 100_000)) ok++;
        assertEquals(4, ok, "a long wait refills to the burst, not beyond");
        RateLimiter.forget(p);
    }

    @Test
    void playersAndKindsAreApart() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        for (int i = 0; i < 4; i++) RateLimiter.allow(a, "x", 1, 4, 0);
        assertFalse(RateLimiter.allow(a, "x", 1, 4, 0));
        assertTrue(RateLimiter.allow(a, "y", 1, 4, 0), "another kind has its own bucket");
        assertTrue(RateLimiter.allow(b, "x", 1, 4, 0), "another player too");
        RateLimiter.forget(a);
        RateLimiter.forget(b);
    }
}
