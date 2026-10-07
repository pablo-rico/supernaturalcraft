package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetatronBalanceTest {

    @Test
    void fourPhasesInQuarters() {
        assertEquals(4, MetatronBalance.PHASES);
        assertEquals(0.75f, MetatronBalance.threshold(1), 1e-6);
        assertEquals(0.5f, MetatronBalance.threshold(2), 1e-6);
        assertEquals(0.25f, MetatronBalance.threshold(3), 1e-6);
    }

    @Test
    void eighteenHundredTrueHealthAlone() {
        float scale = MetatronBalance.healthScale(3.0, 0.5, 1);
        assertEquals(1800, MetatronBalance.BASE_HEALTH * scale, 1e-3);
        assertEquals(2700, MetatronBalance.BASE_HEALTH * MetatronBalance.healthScale(3.0, 0.5, 2), 1e-3);
        assertTrue(MetatronBalance.BASE_HEALTH <= 1024, "vanilla health must stay under the cap");
    }

    @Test
    void theConstructsAndTheLecternComeInOrder() {
        assertFalse(MetatronBalance.hasHand(1));
        assertTrue(MetatronBalance.hasHand(2) && !MetatronBalance.hasBook(2) && !MetatronBalance.onLectern(2));
        assertTrue(MetatronBalance.hasHand(3) && MetatronBalance.hasBook(3) && MetatronBalance.onLectern(3));
        assertTrue(MetatronBalance.onLectern(4));
        for (int p = 1; p < 4; p++) assertTrue(MetatronBalance.attackGap(p + 1) < MetatronBalance.attackGap(p));
    }
}
