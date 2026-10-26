package org.papiricoh.supernaturalcraft.entity.horsemen;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorsemenBalanceTest {

    @Test
    void midBossesHave20000AndDeath45000() {
        // v0.15: the power curve.
        assertEquals(org.papiricoh.supernaturalcraft.balance.ProgressionScale.trueHealth(org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.WAR), HorsemenBalance.trueHealth(false, 0.5, 1), 1e-2);
        assertEquals(org.papiricoh.supernaturalcraft.balance.ProgressionScale.trueHealth(org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.DEATH), HorsemenBalance.trueHealth(true, 0.5, 1), 1e-2);
        assertEquals(org.papiricoh.supernaturalcraft.balance.ProgressionScale.trueHealth(org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.WAR) * 1.5f, HorsemenBalance.trueHealth(false, 0.5, 2), 1e-2, "half again per extra hunter");
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
        assertTrue(mid >= 60 && mid <= 240, "a mid Horseman takes 1-4 minutes alone: " + mid);
        assertTrue(death >= 180 && death <= 480, "Death takes 3-8 minutes alone: " + death);
        assertTrue(HorsemenBalance.estimatedSeconds(false, 0.5, 3) < mid, "more hunters, a shorter fight");
    }
}
