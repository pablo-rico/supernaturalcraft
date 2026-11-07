package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Godstiel's numbers (v0.39): five phases without gaps, the looks by phase, the inner fight, every windup on its clip. */
class GodstielBalanceTest {

    @Test
    void fivePhasesAFifthEachWithoutGaps() {
        assertEquals(5, GodstielBalance.PHASES);
        float last = 1f;
        for (int p = 1; p <= GodstielBalance.PHASES; p++) {
            float t = GodstielBalance.threshold(p);
            assertTrue(t < last, "phase " + p + " ends below where it began");
            assertEquals(1f / GodstielBalance.PHASES, last - t, 1e-5, "phase " + p + " is a fifth");
            last = t;
        }
        assertEquals(0f, GodstielBalance.threshold(GodstielBalance.PHASES));
        for (int p = 1; p < GodstielBalance.PHASES; p++) assertTrue(GodstielBalance.attackGap(p + 1) <= GodstielBalance.attackGap(p), "faster each phase");
    }

    @Test
    void theLooksFollowThePhases() {
        assertTrue(GodstielBalance.halo(1) && GodstielBalance.halo(2) && !GodstielBalance.halo(3));
        assertEquals(0, GodstielBalance.cracks(2));
        assertEquals(1, GodstielBalance.cracks(3));
        assertEquals(3, GodstielBalance.cracks(5));
        assertTrue(GodstielBalance.chestMouth(GodstielBalance.LEVIATHAN_PHASE) && !GodstielBalance.chestMouth(2));
        assertTrue(GodstielBalance.tentacles(4) && !GodstielBalance.tentacles(3));
        assertTrue(GodstielBalance.faceMouth(5) && !GodstielBalance.faceMouth(4));
        assertEquals(GodstielBalance.PHASES, GodstielBalance.RESERVOIR_PHASE);
    }

    @Test
    void theInnerFightComesTwiceInEachOfTheLastPhases() {
        float[] s = GodstielBalance.struggles();
        assertEquals((GodstielBalance.PHASES - GodstielBalance.LEVIATHAN_PHASE + 1) * 2, s.length);
        assertEquals(GodstielBalance.threshold(GodstielBalance.LEVIATHAN_PHASE - 1), s[0], 1e-5, "as phase III begins");
        for (int i = 1; i < s.length; i++) assertTrue(s[i] < s[i - 1], "in order, highest health first");
        assertTrue(s[s.length - 1] > 0, "never at zero (he kneels then)");
        assertEquals(GodstielBalance.STRUGGLE_TICKS, GodstielBalance.struggleExtended(0));
        assertTrue(GodstielBalance.struggleExtended(1) > GodstielBalance.STRUGGLE_TICKS);
        assertEquals(GodstielBalance.STRUGGLE_MAX, GodstielBalance.struggleExtended(99), "capped");
        assertTrue(GodstielBalance.STRUGGLE_VULNERABILITY > 1);
    }

    @Test
    void everyWindupEndsOnItsClipsMoment() {
        // A windup at least as long as the clip's moment, so the clip's blow lands at its end (RaphaelAttacks.Cued).
        assertTrue(GodstielBalance.SMITE_WINDUP >= GodstielBalance.cue("snap"));
        assertTrue(GodstielBalance.PALM_WINDUP >= GodstielBalance.cue("smite"));
        assertTrue(GodstielBalance.RADIANCE_WINDUP >= GodstielBalance.cue("radiance"));
        assertTrue(GodstielBalance.JUDGMENT_WINDUP >= GodstielBalance.cue("judgment"));
        assertTrue(GodstielBalance.BITE_WINDUP >= GodstielBalance.cue("bite"));
        assertTrue(GodstielBalance.RETCH_WINDUP >= GodstielBalance.cue("retch"));
        assertTrue(GodstielBalance.SWEEP_WINDUP >= GodstielBalance.cue("sweep"));
        assertTrue(GodstielBalance.SLAM_WINDUP >= GodstielBalance.cue("slam"));
        assertTrue(GodstielBalance.SURGE_WINDUP >= GodstielBalance.cue("surge"));
        for (String clip : GodstielAssets.HIT_TICKS.keySet()) {
            assertTrue(GodstielAssets.HIT_TICKS.get(clip) < GodstielAssets.CLIP_TICKS.get(clip), clip + "'s moment lands inside it");
        }
        assertFalse(GodstielAssets.TRIGGERED.contains("idle"));
    }
}
