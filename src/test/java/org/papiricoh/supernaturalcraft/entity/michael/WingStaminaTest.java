package org.papiricoh.supernaturalcraft.entity.michael;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.reward.michael.HeavenLedger;
import org.papiricoh.supernaturalcraft.reward.michael.WingStamina;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WingStaminaTest {

    @Test
    void flightDrainsTheBarAndTheWingsGiveOut() {
        WingStamina s = new WingStamina(20);
        assertEquals(400, s.max());
        int gaveOut = -1;
        for (int t = 1; t <= 500; t++) {
            if (s.tick(true, false)) {
                gaveOut = t;
                break;
            }
        }
        assertEquals(400, gaveOut, "twenty seconds of flight");
        assertFalse(s.canFly());
        // In the air it does not come back.
        for (int t = 0; t < 100; t++) s.tick(false, false);
        assertFalse(s.canFly());
    }

    @Test
    void theGroundGivesItBack() {
        WingStamina s = new WingStamina(20);
        while (!s.tick(true, false)) {
        }
        s.tick(false, true);
        assertFalse(s.canFly(), "a moment on the ground is not enough");
        for (int t = 0; t < 20; t++) s.tick(false, true);
        assertTrue(s.canFly(), "a second on the ground lifts you again");
        for (int t = 0; t < 200; t++) s.tick(false, true);
        assertEquals(1f, s.share(), 1e-6, "full in five seconds");
    }

    @Test
    void theArmourComesOnePieceAVictoryNeverTwice() {
        HeavenLedger l = HeavenLedger.NONE;
        Set<Integer> got = new HashSet<>();
        for (int v = 0; v < HeavenLedger.PIECES; v++) {
            int piece = l.nextPiece();
            assertTrue(got.add(piece), "piece " + piece + " given twice before the set was whole");
            l = l.give(piece);
        }
        assertEquals(HeavenLedger.PIECES, l.given());
        // A fifth victory starts the round again.
        assertEquals(0, l.nextPiece());
        l = l.give(l.nextPiece());
        assertEquals(1, l.given());
        assertTrue(l.withGrace(true).grace() && !l.flown() && l.withFlown(true).flown());
    }
}
