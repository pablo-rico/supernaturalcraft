package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.cain.layout.CainFarmLayout;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAssets;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainBalance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cain's numbers (v0.21 P4, pure): three phases of a third each with no gaps, quickening; every windup long enough for its
 * clip's blow; Abel's blood ring with its safe middle; the flare's shelters; his yard big enough for his attacks; his own blade.
 */
class CainBalanceTest {

    @Test
    void threePhasesOfAThirdEachWithNoGaps() {
        assertEquals(3, CainBalance.PHASES);
        assertEquals(1f, CainBalance.threshold(0), 1e-6);
        assertEquals(2f / 3f, CainBalance.threshold(1), 1e-6);
        assertEquals(1f / 3f, CainBalance.threshold(2), 1e-6);
        assertEquals(0f, CainBalance.threshold(3), 1e-6);
        assertEquals(0f, CainBalance.threshold(4), 1e-6);
        for (int p = 1; p < CainBalance.PHASES; p++) {
            // Phase p+1 begins exactly where phase p ends.
            assertEquals(1f - p / 3f, CainBalance.threshold(p), 1e-6, "phase " + p + " ends at its third");
            assertTrue(CainBalance.threshold(p) > CainBalance.threshold(p + 1), "the thresholds fall");
            assertTrue(CainBalance.attackGap(p + 1) < CainBalance.attackGap(p), "he quickens");
        }
    }

    @Test
    void everyWindupEndsOnItsClipsBlow() {
        Map<String, Integer> windups = Map.of("lunge", CainBalance.LUNGE_WINDUP, "swarm", CainBalance.SWARM_WINDUP,
                "furrow", CainBalance.FURROW_WINDUP, "abels_blood", CainBalance.ABEL_WINDUP, "throw_tools", CainBalance.TOOLS_WINDUP,
                "execution", CainBalance.EXECUTION_WINDUP, "mark_flare", CainBalance.FLARE_WINDUP);
        for (var e : windups.entrySet()) {
            int cue = CainBalance.cue(e.getKey());
            assertTrue(cue > 0, e.getKey() + " has its blow in the art");
            assertEquals(CainAssets.HIT_TICKS.get(e.getKey()).intValue(), cue);
            assertTrue(e.getValue() >= cue, e.getKey() + ": the clip must start inside the windup (" + e.getValue() + " < " + cue + ")");
            assertTrue(e.getValue() >= 15, e.getKey() + ": a windup of " + e.getValue() + " ticks is too short to read");
        }
        assertEquals(0, CainBalance.cue("idle"), "a clip without a blow");
        // Each next lunge is drawn a spacing ahead, and its clip starts after the dash before it.
        assertTrue(CainBalance.LUNGE_SPACING > CainBalance.cue("lunge"), "the lunges' clips overlap");
        assertEquals(3, CainBalance.LUNGES);
        assertTrue(CainBalance.DEATH_TICKS > CainBalance.OFFER_TICKS, "the offer ends before he does");
    }

    @Test
    void abelsBloodSparesTheGroundRightByHim() {
        assertFalse(CainBalance.inAbelsRing(0), "at his feet");
        assertFalse(CainBalance.inAbelsRing(CainBalance.ABEL_INNER - 0.1), "inside the green");
        assertTrue(CainBalance.inAbelsRing(CainBalance.ABEL_INNER), "the ring's inner edge");
        assertTrue(CainBalance.inAbelsRing(5), "in the ring");
        assertTrue(CainBalance.inAbelsRing(CainBalance.ABEL_OUTER), "its outer edge");
        assertFalse(CainBalance.inAbelsRing(CainBalance.ABEL_OUTER + 0.1), "outside it");
        assertTrue(CainBalance.ABEL_INNER >= 2, "the safe middle is big enough to stand in");
    }

    @Test
    void theFlareSparesOnlyItsShelters() {
        double safe = CainBalance.FLARE_SAFE_RADIUS;
        assertTrue(CainBalance.flareHits(0, 10), "at his feet, far from a shelter");
        assertTrue(CainBalance.flareHits(CainBalance.FLARE_RADIUS, safe + 0.1), "the burst's edge");
        assertFalse(CainBalance.flareHits(CainBalance.FLARE_RADIUS + 0.1, 20), "out of reach");
        assertFalse(CainBalance.flareHits(10, safe), "the shelter's edge is safe");
        assertFalse(CainBalance.flareHits(10, 0), "the shelter's middle");
        assertTrue(CainBalance.flareHits(10, safe + 0.01), "a step outside the shelter");
        assertEquals(3, CainBalance.FLARE_SHELTERS);
        assertTrue(CainBalance.FLARE_SHELTER_REACH > 0.5 && CainBalance.FLARE_SHELTER_REACH < 1, "shelters inside the yard");
    }

    @Test
    void hisYardHoldsHisAttacks() {
        int yard = CainFarmLayout.ARENA_RADIUS;
        assertEquals(16, yard);
        assertTrue(CainBalance.FLARE_RADIUS <= yard, "the flare's burst fits the yard");
        // The shelters toward his hives (radius - 3.5 out) are in the yard, and the hives beyond its edge.
        assertTrue(yard - 3.5 + CainBalance.FLARE_SAFE_RADIUS < yard, "a shelter lies inside the yard");
        assertTrue(CainBalance.LUNGE_REACH < yard * 2 - 4, "a lunge stays inside the yard");
        assertTrue(CainBalance.TOOLS_MAX_REACH <= yard * 2, "the tools fly no farther than across it");
        assertTrue(CainBalance.ABEL_OUTER < yard, "the ring fits");
    }

    @Test
    void hisOwnBladeBitesHarderAndThePhasesHitHarder() {
        assertEquals(1.5f, CainBalance.FIRST_BLADE_VULNERABILITY, 1e-6);
        assertTrue(CainBalance.SWARM_DAMAGE < CainBalance.LUNGE_DAMAGE && CainBalance.LUNGE_DAMAGE < CainBalance.FURROW_DAMAGE,
                "the Beekeeper stings, the Mark tears");
        assertTrue(CainBalance.EXECUTION_DAMAGE > CainBalance.ABEL_DAMAGE && CainBalance.FLARE_DAMAGE > CainBalance.EXECUTION_DAMAGE,
                "the Father of Murder hits hardest, and the flare most");
    }
}
