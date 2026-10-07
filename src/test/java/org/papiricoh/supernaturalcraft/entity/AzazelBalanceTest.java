package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzazelBalanceTest {

    @Test
    void twoPhasesSplitAtHalf() {
        assertEquals(2, AzazelBalance.PHASES);
        assertEquals(0.5f, AzazelBalance.threshold(1), 1e-6);
    }

    @Test
    void healthGrowsWithChallengers() {
        assertEquals(400, AzazelBalance.health(400, 0.5, 1), 1e-9);
        assertEquals(600, AzazelBalance.health(400, 0.5, 2), 1e-9);
        assertEquals(400, AzazelBalance.health(400, 0.5, 0), 1e-9, "no challengers counts as one");
    }

    @Test
    void theSecondPhaseIsFaster() {
        assertTrue(AzazelBalance.attackGap(2) < AzazelBalance.attackGap(1));
    }

    @Test
    void theRailsMakeHimVulnerable() {
        assertEquals(1f, AzazelBalance.vulnerability(false, false, 1.5), 1e-6);
        assertEquals(1.5f, AzazelBalance.vulnerability(false, true, 1.5), 1e-6);
        assertEquals(1.25f * 1.5f, AzazelBalance.vulnerability(true, true, 1.5), 1e-6);
    }
}
