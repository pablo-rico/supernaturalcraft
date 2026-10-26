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
            var stats = ProgressionScale.of(b);
            assertNotNull(stats, b.name());
            if (b.optional) continue;
            // Only demon-bane can explain a raw health drop; compare the actual reference workload.
            float workload = ProgressionScale.trueHealth(b) / ProgressionScale.bane(b);
            assertTrue(workload >= last, b + " has less reference health than the boss before it");
            assertTrue(stats.tier() >= lastTier, b.name());
            last = workload;
            lastTier = stats.tier();
        }
        assertEquals(70_300, ProgressionScale.trueHealth(Boss.CHUCK));
        assertEquals(ProgressionScale.of(Boss.MICHAEL), ProgressionScale.of(Boss.LUCIFER_UNCAGED));
    }

    @Test
    void healthIsTheRoundedBudgetOfReferenceBlows() {
        float[] reference = {37, 91, 181, 331, 541};
        for (int t = 1; t <= 5; t++) assertEquals(reference[t - 1], ProgressionScale.referenceHit(t));
        for (Boss b : Boss.values()) {
            var stats = ProgressionScale.of(b);
            float hit = ProgressionScale.referenceHit(stats.tier()) * ProgressionScale.bane(b);
            assertEquals(Math.round(stats.targetHits() * hit / 100f) * 100f, ProgressionScale.trueHealth(b), b.name());
            assertEquals(stats.targetHits(), ProgressionScale.trueHealth(b) / hit, 50f / hit, b.name());
        }
    }

    @Test
    void eachBossKeepsItsApprovedTargetAndBane() {
        int[] targets = {35, 45, 55, 65, 65, 65, 80, 85, 88, 88, 100, 100, 130};
        Boss[] road = {Boss.AZAZEL, Boss.LILITH, Boss.LUCIFER, Boss.WAR, Boss.FAMINE, Boss.PESTILENCE,
                Boss.BROKEN_CHORUS, Boss.METATRON, Boss.AMARA, Boss.DEATH, Boss.LUCIFER_UNCAGED, Boss.MICHAEL, Boss.CHUCK};
        for (int i = 0; i < road.length; i++) assertEquals(targets[i], ProgressionScale.of(road[i]).targetHits(), road[i].name());
        assertEquals(55, ProgressionScale.of(Boss.GABRIEL).targetHits());
        assertEquals(55, ProgressionScale.of(Boss.CAIN).targetHits());
        assertEquals(67, ProgressionScale.of(Boss.ABADDON).targetHits());
        assertEquals(68, ProgressionScale.of(Boss.RAPHAEL).targetHits());
        assertEquals(70, ProgressionScale.of(Boss.ROWENA).targetHits());
        assertEquals(70, ProgressionScale.of(Boss.GRAND_WITCH).targetHits());
        assertEquals(82, ProgressionScale.of(Boss.NAOMI).targetHits());
        assertEquals(90, ProgressionScale.of(Boss.ZACHARIAH).targetHits());
        Set<Boss> demons = Set.of(Boss.AZAZEL, Boss.LILITH, Boss.CAIN, Boss.ABADDON);
        for (Boss b : Boss.values()) assertEquals(demons.contains(b) ? ProgressionScale.EXPECTED_BANE : 1f, ProgressionScale.bane(b), b.name());
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
