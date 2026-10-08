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
        // v0.15: the curve's 28 000 scales every pool alike (a face holds 1750).
        float k = ChorusBalance.partScale(28_000);
        assertEquals(1750, ChorusBalance.FACE_POOL * k, 1e-2);
        assertEquals(28_000, ChorusBalance.BASE_PARTS * k + ChorusBalance.corePool(28_000), 1e-1);
    }

    @Test
    void hitsAreBoostedSoftCappedAndNeverOverkill() {
        float max = 28_000; // soft cap 280, hard cap 420
        assertEquals(10, ChorusBalance.partDamage(10, false, false, 1000, max), 1e-4);
        assertEquals(12.5, ChorusBalance.partDamage(10, true, false, 1000, max), 1e-4);
        assertEquals(18.75, ChorusBalance.partDamage(10, true, true, 1000, max), 1e-4);
        assertEquals(280 + 120 * 0.3f, ChorusBalance.partDamage(400, false, false, 1000, max), 1e-3);
        assertEquals(420, ChorusBalance.partDamage(1e6f, false, false, 1000, max), 1e-3);
        assertEquals(7, ChorusBalance.partDamage(30, false, false, 7, max), 1e-4);
    }

    @Test
    void exactDamageSkipsBoostsAndSoftCapUpToTheColtsCapAndThePart() {
        assertEquals(60, ChorusBalance.partDamage(60, true, true, true, 1000, 28_000), 1e-4);
        assertEquals(25, ChorusBalance.partDamage(60, true, false, true, 25, 28_000), 1e-4);
        assertEquals(1400, ChorusBalance.partDamage(5000, false, false, true, 5000, 28_000), 1e-3, "a round: 5% of it");
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
