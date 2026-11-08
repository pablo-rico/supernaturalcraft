package org.papiricoh.supernaturalcraft.dyingsun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DyingSunRulesTest {

    @Test
    void theSunDiesSlowlyForeverAndNeverAllTheWayByItself() {
        assertEquals(0f, DyingSunRules.darkness(0));
        float last = 0f;
        for (int day = 1; day < 400; day++) {
            float d = DyingSunRules.darkness(day);
            assertTrue(d > last, "darker every day");
            assertTrue(d < DyingSunRules.CEILING, "never past the ceiling by itself");
            last = d;
        }
        assertEquals(DyingSunRules.CEILING / 2, DyingSunRules.darkness(DyingSunRules.HALF_DAYS), 1e-6);
    }

    @Test
    void onlyAmarasLastPhaseTakesItAllTheWay() {
        float base = DyingSunRules.darkness(3);
        assertEquals(base, DyingSunRules.fightDarkness(base, 1f), 1e-6);
        assertEquals(DyingSunRules.FIGHT_CEILING, DyingSunRules.fightDarkness(base, 0f), 1e-6);
    }

    @Test
    void itsEffectsGrowWithIt() {
        assertFalse(DyingSunRules.undeadSpared(0.1f));
        assertTrue(DyingSunRules.undeadSpared(DyingSunRules.UNDEAD_SPARED));
        assertTrue(DyingSunRules.cropDenyChance(0.8f) > DyingSunRules.cropDenyChance(0.2f));
        assertEquals(1f, DyingSunRules.spawnBonus(0f));
        assertEquals(0.5f, DyingSunRules.quantise(0.501f), 1f / DyingSunRules.STEPS);
    }
}
