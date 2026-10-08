package org.papiricoh.supernaturalcraft.balance;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionScaleTest {

    @Test
    void everyBossHasNumbersAndTheMainRoadNeverGetsEasier() {
        float last = 0;
        int lastTier = 0;
        for (Boss b : Boss.values()) {
            ProgressionScale.BossStats s = ProgressionScale.of(b);
            assertNotNull(s, b.name());
            if (b.optional) continue;
            assertTrue(s.trueHealth() >= last, b + " has less health than the boss before it");
            assertTrue(s.tier() >= lastTier, b + " is on an earlier tier than the boss before it");
            last = s.trueHealth();
            lastTier = s.tier();
        }
        assertEquals(100_000, ProgressionScale.of(Boss.CHUCK).trueHealth());
        // Lucifer Uncaged stands level with Michael: the two roads to the Author.
        assertEquals(ProgressionScale.of(Boss.MICHAEL), ProgressionScale.of(Boss.LUCIFER_UNCAGED));
    }

    @Test
    void theSoftCapIsContinuousAndNeverPassesTheHardCap() {
        float max = 100_000;
        float prev = 0;
        for (float a = 0; a <= 5000; a += 7.5f) {
            float c = ProgressionScale.softCap(a, max);
            assertTrue(c >= prev - 1e-3, "not monotone at " + a);
            assertTrue(c - prev <= 7.5f + 1e-3, "jumps at " + a);
            assertTrue(c <= max * ProgressionScale.DEFAULT_HARD_CAP + 1e-3);
            prev = c;
        }
        assertEquals(500, ProgressionScale.softCap(500, max), 1e-3);
        assertEquals(1000 + 300, ProgressionScale.softCap(2000, max), 1e-3);
        assertEquals(1500, ProgressionScale.softCap(1e9f, max), 1e-3);
    }

    @Test
    void theAuthorTakesSixtyToOneHundredAndTenBlows() {
        float max = ProgressionScale.of(Boss.CHUCK).trueHealth();
        // An infinite weapon (another mod's) still needs at least 60 blows.
        int infinite = (int) Math.ceil(max / ProgressionScale.softCap(Float.MAX_VALUE / 4, max));
        assertTrue(infinite >= 60 && infinite <= 100, "infinite weapon: " + infinite);
        // A fully ascended Penumbra (11 base) needs no more than 110.
        float own = 11 * ProgressionScale.ascensionMultiplier(5);
        int ownHits = (int) Math.ceil(max / ProgressionScale.softCap(own, max));
        assertTrue(ownHits >= 60 && ownHits <= 110, "ascended weapon: " + ownHits);
    }

    @Test
    void shardsAscendOneTierAtATime() {
        assertTrue(ProgressionScale.shardFits(0, 1));
        assertFalse(ProgressionScale.shardFits(0, 2));
        assertFalse(ProgressionScale.shardFits(1, 1));
        assertTrue(ProgressionScale.shardFits(4, 5));
        assertFalse(ProgressionScale.shardFits(5, 5));
        float prev = 0;
        for (int i = 0; i <= ProgressionScale.MAX_TIER; i++) {
            assertTrue(ProgressionScale.ascensionMultiplier(i) > prev);
            prev = ProgressionScale.ascensionMultiplier(i);
        }
    }

    @Test
    void theHuntersTierFollowsTheShardsTheirEnemiesLeave() {
        Set<String> done = new HashSet<>();
        assertEquals(1, ProgressionScale.playerTier(done::contains));
        done.add(Boss.AZAZEL.advancement);
        assertEquals(2, ProgressionScale.playerTier(done::contains));
        done.add(Boss.LUCIFER.advancement);
        assertEquals(3, ProgressionScale.playerTier(done::contains));
        done.add(Boss.METATRON.advancement);
        assertEquals(4, ProgressionScale.playerTier(done::contains));
        done.add(Boss.MICHAEL.advancement);
        assertEquals(5, ProgressionScale.playerTier(done::contains));
    }

    @Test
    void aegisAndDivineWrathAddUp() {
        ProgressionScale.Split s = ProgressionScale.split(100, ProgressionScale.DEFAULT_DIVINE_FRACTION);
        assertEquals(85, s.ordinary(), 1e-3);
        assertEquals(15, s.divine(), 1e-3);
        assertEquals(ProgressionScale.MAX_AEGIS, ProgressionScale.totalAegis(0.3f, 0.2f, 0.2f), 1e-6);
        assertEquals(50, ProgressionScale.applyAegis(100, 0.5f), 1e-3);
        assertEquals(40, ProgressionScale.applyAegis(100, 0.9f), 1e-3);
        assertEquals(0, ProgressionScale.vitalityHearts(Boss.CHUCK, 2));
        assertEquals(1, ProgressionScale.vitalityHearts(Boss.GABRIEL, 2));
        assertEquals(1, ProgressionScale.vitalityHearts(Boss.RAPHAEL, 2));
        assertEquals(2, ProgressionScale.vitalityHearts(Boss.AZAZEL, 2));
    }
}
