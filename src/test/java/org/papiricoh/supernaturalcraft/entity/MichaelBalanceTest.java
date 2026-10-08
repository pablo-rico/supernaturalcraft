package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MichaelBalanceTest {

    @Test
    void sixPhasesInSixthsWithoutJumps() {
        float last = 1f;
        for (int p = 1; p <= MichaelBalance.PHASES; p++) {
            float t = MichaelBalance.threshold(p);
            assertTrue(t < last, "phase " + p + " must end below the one before");
            assertEquals(1f / MichaelBalance.PHASES, last - t, 1e-5);
            last = t;
        }
        assertEquals(0f, last, 1e-6);
    }

    @Test
    void twoPhasesToEachHeaven() {
        int[] arenas = {0, 0, 1, 1, 2, 2};
        for (int p = 1; p <= 6; p++) assertEquals(arenas[p - 1], MichaelBalance.arenaOf(p), "phase " + p);
    }

    @Test
    void theVesselGivesWayToTheTrueFormInPhaseFive() {
        assertFalse(MichaelBalance.archangel(4));
        assertTrue(MichaelBalance.archangel(5));
        assertFalse(MichaelBalance.shadowWings(2));
        assertTrue(MichaelBalance.shadowWings(3));
        assertTrue(MichaelBalance.shadowWings(4));
        assertFalse(MichaelBalance.shadowWings(5));
    }

    @Test
    void aboutTwentyTwoHundredTrueHealthAlone() {
        assertEquals(2200, MichaelBalance.trueHealth(2.2, 0.5, 1), 0.01);
        assertEquals(3300, MichaelBalance.trueHealth(2.2, 0.5, 2), 0.01);
    }

    @Test
    void aSoloFightLastsTwelveToFifteenMinutes() {
        int s = MichaelBalance.estimatedSeconds(2.2, 0.5, 1);
        assertTrue(s >= 12 * 60 && s <= 15 * 60, "estimated " + s + " s");
    }

    @Test
    void anAnswerCountsOnlyInTime() {
        assertTrue(MichaelBalance.answerInTime(100, 100));
        assertTrue(MichaelBalance.answerInTime(100, 100 + MichaelBalance.YES_DECIDE_TICKS + MichaelBalance.YES_GRACE_TICKS));
        assertFalse(MichaelBalance.answerInTime(100, 101 + MichaelBalance.YES_DECIDE_TICKS + MichaelBalance.YES_GRACE_TICKS));
        assertFalse(MichaelBalance.answerInTime(100, 99));
    }
}
