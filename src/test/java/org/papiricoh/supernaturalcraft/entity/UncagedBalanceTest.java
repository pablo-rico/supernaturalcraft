package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UncagedBalanceTest {

    @Test
    void sixEqualPhases() {
        float prev = 1f;
        for (int p = 1; p < UncagedBalance.PHASES; p++) {
            float t = UncagedBalance.threshold(p);
            assertEquals(1f / 6f, prev - t, 1e-6, "phase " + p + " is not a sixth");
            prev = t;
        }
        assertEquals(1f / 6f, prev, 1e-6);
    }

    @Test
    void tenTimesLuciferAlone() {
        assertEquals(10f, UncagedBalance.healthScale(10, 0.5, 1), 1e-6);
        assertEquals(15f, UncagedBalance.healthScale(10, 0.5, 2), 1e-6);
        assertEquals(10f, UncagedBalance.healthScale(10, 0.5, 0), 1e-6);
    }

    @Test
    void growsWithEveryPhaseAndSpeedsUp() {
        for (int p = 2; p <= UncagedBalance.PHASES; p++) {
            assertTrue(UncagedBalance.scale(p) > UncagedBalance.scale(p - 1));
            assertTrue(UncagedBalance.attackGap(p) < UncagedBalance.attackGap(p - 1));
            assertTrue(UncagedBalance.wingPairs(p) >= UncagedBalance.wingPairs(p - 1));
        }
        assertEquals(3, UncagedBalance.wingPairs(6));
        assertTrue(UncagedBalance.chained(1) && !UncagedBalance.chained(2));
    }
}
