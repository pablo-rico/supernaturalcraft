package org.papiricoh.supernaturalcraft.entity.horsemen;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorsemenBalanceTest {

    @Test
    void midBossesHave20000AndDeath45000() {
        // v0.15: the power curve.
        assertEquals(20_000, HorsemenBalance.trueHealth(false, 0.5, 1), 1e-2);
        assertEquals(45_000, HorsemenBalance.trueHealth(true, 0.5, 1), 1e-2);
        assertEquals(30_000, HorsemenBalance.trueHealth(false, 0.5, 2), 1e-2, "half again per extra hunter");
        assertTrue(HorsemenBalance.DEATH_BASE_HEALTH <= 1024, "vanilla health stays under the cap");
    }

    @Test
    void thresholdsSplitTheHealthEvenly() {
        assertEquals(2 / 3f, HorsemenBalance.threshold(3, 1), 1e-6);
        assertEquals(1 / 3f, HorsemenBalance.threshold(3, 2), 1e-6);
        assertEquals(0.25f, HorsemenBalance.threshold(4, 3), 1e-6);
    }

    @Test
    void theyGetFasterEachPhase() {
        for (int phases : new int[]{3, 4}) {
            for (int p = 2; p <= phases; p++) {
                assertTrue(HorsemenBalance.attackGap(phases, p) < HorsemenBalance.attackGap(phases, p - 1));
            }
        }
    }

    @Test
    void estimatedFightLengths() {
        int mid = HorsemenBalance.estimatedSeconds(false, 0.5, 1);
        int death = HorsemenBalance.estimatedSeconds(true, 0.5, 1);
        assertTrue(mid >= 180 && mid <= 480, "a mid Horseman takes 3-8 minutes alone: " + mid);
        assertTrue(death >= 360 && death <= 900, "Death takes 6-15 minutes alone: " + death);
        assertTrue(HorsemenBalance.estimatedSeconds(false, 0.5, 3) < mid, "more hunters, a shorter fight");
    }
}
