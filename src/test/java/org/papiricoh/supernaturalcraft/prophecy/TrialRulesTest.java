package org.papiricoh.supernaturalcraft.prophecy;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.prophecy.trials.TrialRules;

import static org.junit.jupiter.api.Assertions.*;

class TrialRulesTest {

    @Test
    void eachTrialTakesMoreAndTheEndKeepsSome() {
        assertEquals(0, TrialRules.toll(TrialRules.NONE, 0));
        for (int t = TrialRules.HOUND; t <= TrialRules.CURE; t++) assertEquals(t * TrialRules.HEARTS_PER_TRIAL, TrialRules.toll(t, 0));
        assertEquals(TrialRules.HEARTS_KEPT, TrialRules.toll(TrialRules.DONE, 0));
        assertEquals(TrialRules.SOUL, TrialRules.next(TrialRules.HOUND));
        assertEquals(TrialRules.DONE, TrialRules.next(TrialRules.DONE));
    }

    @Test
    void stoppingGivesItBackDayByDay() {
        int owed = TrialRules.owedOnStop(TrialRules.CURE);
        assertEquals(TrialRules.CURE * TrialRules.HEARTS_PER_TRIAL, owed);
        assertEquals(owed, TrialRules.toll(TrialRules.STOPPED, owed));
        assertEquals(owed - TrialRules.HEARTS_PER_TRIAL, TrialRules.owedAfter(owed, 1));
        assertEquals(0, TrialRules.owedAfter(owed, 100));
    }

    @Test
    void injectionsWaitAndTheLastAsks() {
        assertTrue(TrialRules.mayInject(0, 0));
        assertFalse(TrialRules.mayInject(1, TrialRules.INJECTION_GAP - 1));
        assertTrue(TrialRules.mayInject(1, TrialRules.INJECTION_GAP));
        assertFalse(TrialRules.last(TrialRules.INJECTIONS - 1));
        assertTrue(TrialRules.last(TrialRules.INJECTIONS));
        assertTrue(TrialRules.sealed(TrialRules.SEAL_RADIUS - 1, 0));
        assertFalse(TrialRules.sealed(TrialRules.SEAL_RADIUS, 1));
    }
}
