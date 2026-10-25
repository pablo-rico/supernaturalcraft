package org.papiricoh.supernaturalcraft.magic.mana;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepthRulesTest {

    @Test
    void tiersAreFiveFiveThenForever() {
        assertEquals(1, DepthRules.tierFor(0));
        assertEquals(1, DepthRules.tierFor(4));
        assertEquals(2, DepthRules.tierFor(5));
        assertEquals(2, DepthRules.tierFor(9));
        assertEquals(3, DepthRules.tierFor(10));
        assertEquals(3, DepthRules.tierFor(1000));
    }

    @Test
    void restDoublesUpToEightDays() {
        assertEquals(0, DepthRules.restDays(0));
        assertEquals(1, DepthRules.restDays(1));
        assertEquals(2, DepthRules.restDays(2));
        assertEquals(4, DepthRules.restDays(3));
        assertEquals(8, DepthRules.restDays(4));
        assertEquals(8, DepthRules.restDays(500));
    }

    @Test
    void regenIsAShareOfTheBar() {
        // The base bar refills at the old two a second.
        assertEquals(2f, DepthRules.regenPerSecond(100, 0), 1e-5);
        assertEquals(9f, DepthRules.regenPerSecond(300, 1), 1e-5);
        assertEquals(12f, DepthRules.regenPerSecond(100, 10), 1e-5);
    }

    @Test
    void milestonesAndTitles() {
        assertFalse(DepthRules.milestone(0));
        assertFalse(DepthRules.milestone(9));
        assertTrue(DepthRules.milestone(10));
        assertTrue(DepthRules.milestone(70));
        assertEquals(0, DepthRules.title(9));
        assertEquals(1, DepthRules.title(10));
        assertEquals(3, DepthRules.title(39));
        assertEquals(DepthRules.TITLES, DepthRules.title(500));
    }
}
