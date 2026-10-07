package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChorusBalanceTest {

    @Test
    void partsAndCoreAddUpToTheBaseHealth() {
        assertEquals(1600, ChorusBalance.BASE_PARTS + ChorusBalance.corePool(1600), 1e-3);
        assertEquals(1.0, ChorusBalance.partScale(1600), 1e-6);
        assertEquals(2400, ChorusBalance.scaled(1600, 0.5, 2), 1e-3);
    }

    @Test
    void hitsAreBoostedCappedAndNeverOverkill() {
        assertEquals(10, ChorusBalance.partDamage(10, false, false, 100), 1e-4);
        assertEquals(12.5, ChorusBalance.partDamage(10, true, false, 100), 1e-4);
        assertEquals(18.75, ChorusBalance.partDamage(10, true, true, 100), 1e-4);
        assertEquals(ChorusBalance.HIT_CAP, ChorusBalance.partDamage(500, false, false, 100), 1e-4);
        assertEquals(7, ChorusBalance.partDamage(30, false, false, 7), 1e-4);
    }

    @Test
    void exactDamageSkipsBoostsAndCapButNotThePart() {
        assertEquals(60, ChorusBalance.partDamage(60, true, true, true, 100), 1e-4);
        assertEquals(25, ChorusBalance.partDamage(60, true, false, true, 25), 1e-4);
        assertEquals(ChorusBalance.HIT_CAP, ChorusBalance.partDamage(60, true, false, false, 100), 1e-4);
    }

    @Test
    void theGazeJudgesSightAndAttention() {
        assertEquals(ChorusBalance.Gaze.NONE, ChorusBalance.gaze(false, 1.0));
        assertEquals(ChorusBalance.Gaze.BURN, ChorusBalance.gaze(true, 0.2));
        assertEquals(ChorusBalance.Gaze.FULL, ChorusBalance.gaze(true, 0.95));
    }

    @Test
    void echoesDeepenTheHymn() {
        assertEquals(12, ChorusBalance.hymnDamage(0), 1e-4);
        assertEquals(12 * 1.3, ChorusBalance.hymnDamage(2), 1e-4);
    }
}
