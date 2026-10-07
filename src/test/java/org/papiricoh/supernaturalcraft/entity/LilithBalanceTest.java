package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LilithBalanceTest {

    @Test
    void threePhasesInThirds() {
        assertEquals(3, LilithBalance.PHASES);
        assertEquals(2f / 3f, LilithBalance.threshold(1), 1e-6);
        assertEquals(1f / 3f, LilithBalance.threshold(2), 1e-6);
    }

    @Test
    void sheIsHarderThanAzazel() {
        assertEquals(500, LilithBalance.health(500, 0.5, 1), 1e-9);
        assertEquals(750, LilithBalance.health(500, 0.5, 2), 1e-9);
        for (int p = 1; p < 3; p++) {
            assertTrue(LilithBalance.attackGap(p + 1) < LilithBalance.attackGap(p), "faster each phase");
            assertTrue(LilithBalance.contractSeconds(p + 1) < LilithBalance.contractSeconds(p), "shorter contracts each phase");
            assertTrue(LilithBalance.whiteLightEvery(p + 1) < LilithBalance.whiteLightEvery(p), "more light each phase");
        }
        assertEquals(2, LilithBalance.contractsAtOnce(3));
    }

    @Test
    void holyWoundsPayDouble() {
        assertEquals(10f, LilithBalance.contractCredit(10f, false), 1e-6);
        assertEquals(20f, LilithBalance.contractCredit(10f, true), 1e-6);
    }

    @Test
    void sheIsOpenAfterHerLight() {
        assertEquals(1f, LilithBalance.vulnerability(false, false), 1e-6);
        assertEquals(LilithBalance.EMPTY_VULNERABILITY, LilithBalance.vulnerability(false, true), 1e-6);
        assertEquals(1.25f * LilithBalance.EMPTY_VULNERABILITY, LilithBalance.vulnerability(true, true), 1e-6);
    }
}
