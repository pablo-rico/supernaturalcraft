package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmaraBalanceTest {

    @Test
    void everyLitWellMakesHerWeaker() {
        for (int w = 0; w < AmaraBalance.WELLS; w++) {
            assertTrue(AmaraBalance.damageMultiplier(w + 1, false, false) > AmaraBalance.damageMultiplier(w, false, false));
        }
        assertEquals(1.0f, AmaraBalance.damageMultiplier(4, false, false) / 0.6f, 1e-5);
    }

    @Test
    void holyAndLightStack() {
        float base = AmaraBalance.damageMultiplier(2, false, false);
        assertEquals(base * 2.5f, AmaraBalance.damageMultiplier(2, true, false), 1e-5);
        assertEquals(base * 2.5f * 1.25f, AmaraBalance.damageMultiplier(2, true, true), 1e-5);
    }

    @Test
    void lightHealsTheDarkRots() {
        assertTrue(AmaraBalance.consumptionPerSecond(15, false) < 0);
        assertEquals(0f, AmaraBalance.consumptionPerSecond(5, false));
        assertEquals(2f, AmaraBalance.consumptionPerSecond(0, false));
        assertEquals(7f, AmaraBalance.consumptionPerSecond(0, true));
        assertEquals(-5f, AmaraBalance.consumptionPerSecond(12, true));
    }

    @Test
    void healthScalesWithChallengers() {
        assertEquals(1400f, AmaraBalance.scaled(1400, 0.5, 1), 1e-3);
        assertEquals(2100f, AmaraBalance.scaled(1400, 0.5, 2), 1e-3);
        assertEquals(1400f, AmaraBalance.scaled(1400, 0.5, 0), 1e-3);
    }
}
