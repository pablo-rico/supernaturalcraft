package org.papiricoh.supernaturalcraft.heaven.zachariah;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.FormRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Heaven's paperwork (v0.18): the quarter, the approval, the deadline, the right cabinet. */
class FormRulesTest {

    @Test
    void anUnfiledFormWeighsDownYourBlowsAndAnApprovalLiftsThem() {
        assertEquals(1f, FormRules.damageFactor(false, false), 1e-6);
        assertEquals(0.25f, FormRules.damageFactor(true, false), 1e-6);
        assertEquals(1.5f, FormRules.damageFactor(false, true), 1e-6);
        assertEquals(0.375f, FormRules.damageFactor(true, true), 1e-6, "a new form over an old approval");
    }

    @Test
    void aFormIsOverdueWhenItsTimeRunsOut() {
        assertFalse(FormRules.overdue(100, 699, 600));
        assertTrue(FormRules.overdue(100, 700, 600));
        assertEquals(600, FormRules.ticksLeft(100, 100, 600));
        assertEquals(1, FormRules.ticksLeft(100, 699, 600));
        assertEquals(0, FormRules.ticksLeft(100, 5000, 600));
        assertFalse(FormRules.stale(100, 1900, 600));
        assertTrue(FormRules.stale(100, 1901, 600), "a forgotten form crumbles");
        assertTrue(FormRules.stale(100, 50, 600), "from another world's clock");
    }

    @Test
    void onlyTheRightCabinet() {
        assertTrue(FormRules.matches(3, 3));
        assertFalse(FormRules.matches(3, 2));
        assertFalse(FormRules.matches(0, 0), "no form, no cabinet");
        assertFalse(FormRules.matches(5, 5));
    }

    @Test
    void aNewFormNeverNamesTheLastCabinet() {
        for (int prev = 0; prev <= 4; prev++) {
            boolean[] seen = new boolean[5];
            for (int roll = -20; roll < 40; roll++) {
                int n = FormRules.numberFor(roll, prev);
                assertTrue(FormRules.valid(n), "cabinet " + n);
                if (prev > 0) assertNotEquals(prev, n, "never the last one");
                seen[n] = true;
            }
            for (int n = 1; n <= 4; n++) if (n != prev) assertTrue(seen[n], "every other cabinet comes up (" + n + " after " + prev + ")");
        }
    }

    @Test
    void approvalsLapse() {
        assertTrue(FormRules.approved(300, 299));
        assertFalse(FormRules.approved(300, 300));
        assertEquals(200, FormRules.APPROVED_TICKS);
    }

    @Test
    void romanNumeralsOnTheCabinets() {
        assertEquals("I", FormRules.roman(1));
        assertEquals("IV", FormRules.roman(4));
        assertEquals("?", FormRules.roman(9));
    }
}
