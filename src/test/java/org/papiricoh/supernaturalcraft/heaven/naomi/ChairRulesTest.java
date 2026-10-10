package org.papiricoh.supernaturalcraft.heaven.naomi;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.ChairRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The reprogramming chair's struggle (v0.18): presses by phase and side, the time, the server's budget, the drill. */
class ChairRulesTest {

    @Test
    void pressesByPhaseAndSide() {
        assertEquals(18, ChairRules.presses(1, Faction.DEMON));
        assertEquals(24, ChairRules.presses(2, Faction.DEMON));
        assertEquals(14, ChairRules.presses(1, Faction.HUMAN), "a human's will is their own: 18 x 0.75 = 13.5");
        assertEquals(18, ChairRules.presses(2, Faction.HUMAN));
        assertEquals(23, ChairRules.presses(1, Faction.ANGEL), "an angel was made to sit: 18 x 1.25 = 22.5");
        assertEquals(30, ChairRules.presses(2, Faction.ANGEL));
        assertEquals(80, ChairRules.ticks(1));
        assertEquals(70, ChairRules.ticks(2));
    }

    @Test
    void everyStruggleCanBeWonAtTheRateTheServerBelieves() {
        for (Faction f : Faction.values()) {
            for (int phase = 1; phase <= 2; phase++) {
                float rate = ChairRules.presses(phase, f) / (ChairRules.ticks(phase) / 20f);
                assertTrue(rate < ChairRules.MAX_PER_SECOND * 0.8f, f + " in phase " + phase + " needs " + rate + "/s");
            }
        }
        assertFalse(ChairRules.breaksFree(17, 18));
        assertTrue(ChairRules.breaksFree(18, 18));
    }

    @Test
    void theServerBelievesAtMostTwelveASecond() {
        ChairRules.Budget b = new ChairRules.Budget(0);
        assertEquals(0, b.accept(10, 0), "nothing at the moment of strapping");
        int total = 0;
        // A cheating client claims 50 presses every 5 ticks for 4 seconds.
        for (long t = 5; t <= 80; t += 5) total += b.accept(50, t);
        assertTrue(total <= 12 * 4 && total >= 12 * 4 - 1, "about 12 a second: " + total);
        // An honest one mashing 3 every 5 ticks keeps all of them.
        ChairRules.Budget honest = new ChairRules.Budget(0);
        int kept = 0;
        for (long t = 5; t <= 80; t += 5) kept += honest.accept(3, t);
        assertEquals(48, kept);
        // A late packet still counts what was earned (up to half a second).
        ChairRules.Budget late = new ChairRules.Budget(0);
        assertEquals(6, late.accept(10, 20), "a second's silence holds only half a second of credit");
        assertEquals(0, late.accept(-5, 25), "negative reports count for nothing");
    }

    @Test
    void theDrillTakesAShareOfMaxHealthDeeperIntoADemon() {
        assertEquals(1.2f, ChairRules.drillDamage(20f, false), 1e-5);
        assertEquals(1.2f * 1.15f, ChairRules.drillDamage(20f, true), 1e-5);
        assertEquals(3, ChairRules.ALLY_HITS);
        assertEquals(160, ChairRules.FAIL_CONDITIONED);
    }
}
