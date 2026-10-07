package org.papiricoh.supernaturalcraft.entity.horsemen;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.PlagueStacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlagueStacksTest {

    @Test
    void dosesStackUpToTheCap() {
        int amp = -1;
        amp = PlagueStacks.add(amp, 1);
        assertEquals(1, PlagueStacks.stacks(amp));
        amp = PlagueStacks.add(amp, 2);
        assertEquals(3, PlagueStacks.stacks(amp));
        amp = PlagueStacks.add(amp, 10);
        assertEquals(PlagueStacks.MAX_STACKS, PlagueStacks.stacks(amp));
    }

    @Test
    void moreStacksHurtMore() {
        assertEquals(0, PlagueStacks.damage(-1), 1e-6);
        assertTrue(PlagueStacks.damage(3) > PlagueStacks.damage(0));
        assertEquals(PlagueStacks.MAX_STACKS * PlagueStacks.HEALTH_PER_STACK, PlagueStacks.healthLost(PlagueStacks.MAX_STACKS - 1), 1e-6);
    }

    @Test
    void curedMeansNoStacks() {
        assertEquals(0, PlagueStacks.stacks(-1));
    }

    @Test
    void naturalHealingStopsButPotionsWork() {
        assertFalse(PlagueStacks.healAllowed(1f));
        assertTrue(PlagueStacks.healAllowed(4f));
    }
}
