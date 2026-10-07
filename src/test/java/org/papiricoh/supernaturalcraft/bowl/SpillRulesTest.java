package org.papiricoh.supernaturalcraft.bowl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpillRulesTest {

    @Test
    void jumpsRarelyLoseOneDose() {
        assertEquals(new SpillRules.Spill(1, 0), SpillRules.jump(3, 0.0));
        assertTrue(SpillRules.jump(3, SpillRules.JUMP_CHANCE).isNone(), "above the chance nothing spills");
        assertTrue(SpillRules.jump(0, 0.0).isNone(), "no liquid, nothing to lose");
    }

    @Test
    void shortFallsAreSafe() {
        assertTrue(SpillRules.fall(1.25f, 4, 8).isNone(), "a jump's landing");
        assertTrue(SpillRules.fall(2.99f, 4, 8).isNone());
    }

    @Test
    void longFallsSpillEverything() {
        assertEquals(new SpillRules.Spill(4, 8), SpillRules.fall(8, 4, 8));
        assertEquals(new SpillRules.Spill(2, 3), SpillRules.fall(20, 2, 3));
    }

    @Test
    void inBetweenTheShareGrowsWithHeight() {
        SpillRules.Spill low = SpillRules.fall(3, 4, 8);
        SpillRules.Spill mid = SpillRules.fall(5.5f, 4, 8);
        SpillRules.Spill high = SpillRules.fall(7.5f, 4, 8);
        assertEquals(1, low.doses(), "from 3 blocks at least one dose goes");
        assertEquals(0, low.items());
        assertTrue(mid.doses() >= low.doses() && high.doses() >= mid.doses());
        assertTrue(mid.items() > 0 && high.items() > mid.items());
        assertTrue(high.doses() <= 4 && high.items() < 8, "only from 8 blocks is everything lost");
    }

    @Test
    void blowsSpillByDamage() {
        assertEquals(1, SpillRules.hit(1, 4, 2, 1).doses(), "any blow costs a dose");
        assertEquals(2, SpillRules.hit(8, 4, 2, 1).doses());
        assertEquals(3, SpillRules.hit(30, 3, 2, 1).doses(), "never more than there is");
        assertEquals(0, SpillRules.hit(8, 4, 2, 1).items(), "a high roll keeps the ingredients");
        assertEquals(1, SpillRules.hit(8, 4, 2, 0).items(), "a low roll throws one out");
        assertEquals(0, SpillRules.hit(1, 4, 2, 0.1).items(), "a light blow rarely does");
        assertTrue(SpillRules.hit(0, 4, 2, 0).isNone());
        assertEquals(0, SpillRules.hit(8, 0, 0, 0).doses());
    }
}
