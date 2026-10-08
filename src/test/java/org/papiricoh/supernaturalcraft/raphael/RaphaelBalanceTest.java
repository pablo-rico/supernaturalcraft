package org.papiricoh.supernaturalcraft.raphael;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Raphael's numbers (v0.16): thirds, the threads, the smite, the snap's safe band, the field. */
class RaphaelBalanceTest {

    @Test
    void threePhasesOfAThirdEach() {
        assertEquals(2f / 3f, RaphaelBalance.threshold(1), 1e-6);
        assertEquals(1f / 3f, RaphaelBalance.threshold(2), 1e-6);
        assertEquals(0f, RaphaelBalance.threshold(3), 1e-6);
        assertTrue(RaphaelBalance.attackGap(3) < RaphaelBalance.attackGap(2) && RaphaelBalance.attackGap(2) < RaphaelBalance.attackGap(1),
                "the storm quickens");
    }

    @Test
    void threadsHealAShareOfTrueHealthPerThread() {
        assertEquals(0f, RaphaelBalance.tetherHeal(0, 0.004, 26_000f));
        assertEquals(104f, RaphaelBalance.tetherHeal(1, 0.004, 26_000f), 1e-3);
        assertEquals(416f, RaphaelBalance.tetherHeal(4, 0.004, 26_000f), 1e-3);
        assertEquals(624f, RaphaelBalance.tetherHeal(4, 0.004, 39_000f), 1e-3, "two hunters: a share of the larger health");
    }

    @Test
    void aHunterInTheThreadCutsIt() {
        // An angel at (0, 0), Raphael at (10, 0).
        assertTrue(RaphaelBalance.inBeam(5, 0, 0, 0, 10, 0), "on the line");
        assertTrue(RaphaelBalance.inBeam(5, 0.7, 0, 0, 10, 0), "just beside it");
        assertFalse(RaphaelBalance.inBeam(5, 1.2, 0, 0, 10, 0), "a step away");
        assertFalse(RaphaelBalance.inBeam(0.2, 0, 0, 0, 10, 0), "the angel's own feet");
        assertFalse(RaphaelBalance.inBeam(9.8, 0, 0, 0, 10, 0), "his feet");
        assertFalse(RaphaelBalance.inBeam(-3, 0, 0, 0, 10, 0), "behind the angel");
        assertTrue(RaphaelBalance.inBeam(3, 3.2, 0, 0, 6, 6), "a diagonal thread");
    }

    @Test
    void theSmiteBreaksOnAShareOfTrueHealthOrAShieldInTime() {
        assertFalse(RaphaelBalance.interrupts(129f, 26_000f));
        assertTrue(RaphaelBalance.interrupts(130f, 26_000f));
        assertTrue(RaphaelBalance.parries(0) && RaphaelBalance.parries(RaphaelBalance.PARRY_WINDOW));
        assertFalse(RaphaelBalance.parries(-1), "no shield");
        assertFalse(RaphaelBalance.parries(RaphaelBalance.PARRY_WINDOW + 1), "a shield held up all along");
    }

    @Test
    void theSnapSparesOnlyItsBand() {
        int r = RaphaelBalance.SNAP_RADIUS, w = RaphaelBalance.SNAP_SAFE_WIDTH;
        assertTrue(RaphaelBalance.snapHits(1, r, w), "close to him");
        assertFalse(RaphaelBalance.snapHits(r / 2.0, r, w), "the band's middle");
        assertFalse(RaphaelBalance.snapHits(RaphaelBalance.snapSafeInner(r, w), r, w), "the band's inner edge");
        assertTrue(RaphaelBalance.snapHits(RaphaelBalance.snapSafeOuter(r, w) + 0.1, r, w), "just past it");
        assertFalse(RaphaelBalance.snapHits(r + 0.5, r, w), "out of reach");
        assertTrue(RaphaelBalance.snapSafeInner(r, w) > 2 && RaphaelBalance.snapSafeOuter(r, w) < r - 2, "the band is inside the burst");
    }

    @Test
    void theFieldStrikesEveryOtherCell() {
        int struck = 0;
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                boolean a = RaphaelBalance.fieldStrikes(i, j, 0), b = RaphaelBalance.fieldStrikes(i, j, 1);
                assertTrue(a != b, "each cell is struck in exactly one of the two volleys");
                if (a) struck++;
                if (a) {
                    assertFalse(RaphaelBalance.fieldStrikes(i + 1, j, 0), "a struck cell's neighbour is safe");
                }
            }
        }
        assertEquals(25, struck);
    }
}
