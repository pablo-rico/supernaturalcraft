package org.papiricoh.supernaturalcraft.hell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TormentTest {

    @Test
    void growsInHellFasterOnTheRack() {
        float plain = Torment.next(0.2f, 0.06f, true, false, false), rack = Torment.next(0.2f, 0.06f, true, true, false);
        assertTrue(plain > 0.2f && rack > plain);
        assertEquals(0.201f, plain, 1e-4);
    }

    @Test
    void saltEasesAndLeavingFades() {
        assertTrue(Torment.next(0.5f, 0.06f, true, false, true) < 0.5f);
        assertTrue(Torment.next(0.5f, 0.06f, false, false, false) < 0.5f);
    }

    @Test
    void staysBetweenZeroAndOne() {
        assertEquals(1f, Torment.next(0.9999f, 1f, true, true, false));
        assertEquals(0f, Torment.next(0.001f, 0.06f, false, false, false));
    }
}
