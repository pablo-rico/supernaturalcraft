package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import static org.junit.jupiter.api.Assertions.*;

class ChainRulesTest {
    @Test void statsRemainOrdinaryMobScale() {
        assertEquals(1.25, ChainRules.healthMultiplier(ChainRank.SOLDIER, 0));
        assertEquals(3, ChainRules.healthMultiplier(ChainRank.LORD, 0));
        for (ChainRank rank : ChainRank.values()) {
            double base = ChainRules.healthMultiplier(rank, 0);
            assertEquals(base * 1.05, ChainRules.healthMultiplier(rank, 1), 1e-9);
            assertEquals(base, ChainRules.healthMultiplier(rank, -100));
            assertTrue(ChainRules.healthMultiplier(rank, Integer.MAX_VALUE) <= 4);
            assertTrue(ChainRules.damageMultiplier(rank, Integer.MAX_VALUE) <= 2.5);
            for (int seed = 0; seed < 50; seed++) {
                int size = ChainRules.escortSize(rank, new GenSeed.Rng(seed));
                assertTrue(size >= rank.ordinal() && size <= (rank == ChainRank.SOLDIER ? 0 : rank.ordinal() + 1));
            }
        }
        assertEquals(Integer.MAX_VALUE, ChainRules.levelOnPlayerKill(Integer.MAX_VALUE));
    }

    @Test void returnProbabilityDelaysAndHeat() {
        GenSeed.Rng rng = ChainRules.rng(1, 2, 3);
        int returns = 0;
        for (int i = 0; i < 10000; i++) {
            assertFalse(ChainRules.returns(true, rng));
            if (ChainRules.returns(false, rng)) returns++;
            int delay = ChainRules.returnDelayDays(rng);
            assertTrue(delay >= 2 && delay <= 5);
        }
        assertTrue(returns > 4700 && returns < 5300);
        for (ChainRank rank : ChainRank.values()) assertTrue(ChainRules.vengeanceDelayDays(rank) >= 1 && ChainRules.vengeanceDelayDays(rank) <= 3);
        assertEquals(5, ChainRules.destabilizeDays());
        for (int heat = -1; heat < 100; heat++) assertTrue(ChainRules.ambushIntervalDays(heat) >= ChainRules.ambushIntervalDays(heat + 1));
        assertEquals(1, ChainRules.ambushIntervalDays(Integer.MAX_VALUE));
    }

    @Test void dailyEventsRespectGatesAndPreferRecruitment() {
        GenSeed.Rng rng = new GenSeed.Rng(11);
        int recruits = 0, falls = 0;
        for (int i = 0; i < 10000; i++) {
            assertEquals(ChainRules.DailyEvent.NONE, ChainRules.pickDailyEvent(rng, new ChainRules.State(ChainFaction.ANGEL, true, true, true)));
            assertNotEquals(ChainRules.DailyEvent.FALL, ChainRules.pickDailyEvent(rng, new ChainRules.State(ChainFaction.DEMON, true, true, false)));
            var noVacancy = ChainRules.pickDailyEvent(rng, new ChainRules.State(ChainFaction.ANGEL, false, false, false));
            assertNotEquals(ChainRules.DailyEvent.FALL, noVacancy);
            assertNotEquals(ChainRules.DailyEvent.RECRUIT, noVacancy);
            var pick = ChainRules.pickDailyEvent(rng, new ChainRules.State(ChainFaction.ANGEL, true, true, false));
            if (pick == ChainRules.DailyEvent.RECRUIT) recruits++;
            if (pick == ChainRules.DailyEvent.FALL) falls++;
        }
        assertTrue(recruits > 4000);
        assertTrue(falls > 0);
    }

    @Test void combatOddsRewardLevelsAndRanksAndSeedsUseAllInputs() {
        Officer weak = new Officer(0, ChainFaction.DEMON), strong = new Officer(1, ChainFaction.ANGEL);
        strong.rank = ChainRank.LORD;
        strong.level = 10;
        int wins = 0;
        GenSeed.Rng rng = new GenSeed.Rng(99);
        for (int i = 0; i < 10000; i++) if (ChainRules.challengeWinner(strong, weak, rng) == strong) wins++;
        assertTrue(wins > 8500 && wins < 9500);
        assertFalse(ChainRules.shouldPromoteOnKill(strong, rng));
        assertEquals(ChainRules.rng(1, 2, 3).nextLong(), ChainRules.rng(1, 2, 3).nextLong());
        assertNotEquals(ChainRules.rng(1, 2, 3).nextLong(), ChainRules.rng(2, 2, 3).nextLong());
        assertNotEquals(ChainRules.rng(1, 2, 3).nextLong(), ChainRules.rng(1, 3, 3).nextLong());
        assertNotEquals(ChainRules.rng(1, 2, 3).nextLong(), ChainRules.rng(1, 2, 4).nextLong());
    }
}
