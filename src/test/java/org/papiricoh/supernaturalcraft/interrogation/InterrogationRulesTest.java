package org.papiricoh.supernaturalcraft.interrogation;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.interrogation.InterrogationRules.Tool;

import static org.junit.jupiter.api.Assertions.*;

class InterrogationRulesTest {

    @Test
    void theWeakPointWearsTwiceAndRepeatsWearLess() {
        Tool weak = Tool.PAIN;
        assertEquals(Tool.PAIN.wear * 2, InterrogationRules.wear(Tool.PAIN, weak, 0));
        assertEquals(Tool.FAITH.wear, InterrogationRules.wear(Tool.FAITH, weak, 0));
        assertTrue(InterrogationRules.wear(Tool.FAITH, weak, 1) < InterrogationRules.wear(Tool.FAITH, weak, 0));
        assertTrue(InterrogationRules.wear(Tool.WORDS, weak, 20) >= 1);
    }

    @Test
    void wordsNeverHurtAndTheBreakingPointHolds() {
        assertEquals(0f, InterrogationRules.strain(Tool.WORDS));
        assertTrue(InterrogationRules.strain(Tool.STEEL) > InterrogationRules.strain(Tool.FAITH));
        assertTrue(InterrogationRules.overdone(InterrogationRules.BREAKING_POINT - 0.01f));
        assertFalse(InterrogationRules.overdone(InterrogationRules.BREAKING_POINT + 0.01f));
        assertEquals(0, InterrogationRules.band(InterrogationRules.RESISTANCE));
        assertEquals(2, InterrogationRules.band(1));
    }

    @Test
    void everyToolIsSomeonesWeakness() {
        java.util.Set<Tool> seen = java.util.EnumSet.noneOf(Tool.class);
        for (long s = 0; s < 200; s++) seen.add(InterrogationRules.weakness(s * 7919));
        assertEquals(Tool.values().length, seen.size());
    }
}
