package org.papiricoh.supernaturalcraft.magic.mastery;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.magic.mastery.MasteryRules.Rank;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure rules of magical mastery (v0.21): XP per tool, the ranks, the learning gate and the mana bonus. */
class MasteryRulesTest {

    @Test
    void theRanksAndWhatTheyGive() {
        assertEquals(List.of(Rank.NONE, Rank.APPRENTICE, Rank.ADEPT, Rank.MASTER), List.of(Rank.values()), "only append ranks");
        assertEquals(150, Rank.APPRENTICE.xp);
        assertEquals(600, Rank.ADEPT.xp);
        assertEquals(1800, Rank.MASTER.xp);
        assertEquals(0f, Rank.NONE.discount);
        assertEquals(0.05f, Rank.APPRENTICE.discount, 1e-6);
        assertEquals(0.10f, Rank.ADEPT.discount, 1e-6);
        assertEquals(0.15f, Rank.MASTER.discount, 1e-6);
        assertEquals(0, Rank.NONE.manaBonus);
        assertEquals(5, Rank.APPRENTICE.manaBonus);
        assertEquals(10, Rank.ADEPT.manaBonus);
        assertEquals(20, Rank.MASTER.manaBonus);
        assertEquals("apprentice", Rank.APPRENTICE.id());
        assertEquals(Rank.ADEPT, Rank.APPRENTICE.next());
        assertNull(Rank.MASTER.next());
    }

    @Test
    void xpReachesItsRankAtTheThreshold() {
        assertEquals(Rank.NONE, MasteryRules.rank(0));
        assertEquals(Rank.NONE, MasteryRules.rank(149.99f));
        assertEquals(Rank.APPRENTICE, MasteryRules.rank(150));
        assertEquals(Rank.APPRENTICE, MasteryRules.rank(599));
        assertEquals(Rank.ADEPT, MasteryRules.rank(600));
        assertEquals(Rank.MASTER, MasteryRules.rank(1800));
        assertEquals(Rank.MASTER, MasteryRules.rank(MasteryRules.MAX_XP));
    }

    @Test
    void eachToolTrainsAtItsOwnRate() {
        // A sigil: mana / 5; the bowl twice that; a rite mana / 3.
        assertEquals(4f, MasteryRules.sigilXp(20), 1e-5);
        assertEquals(8f, MasteryRules.bowlXp(20), 1e-5);
        assertEquals(10f, MasteryRules.ritualXp(30), 1e-5);
        assertEquals(0f, MasteryRules.sigilXp(-5));
        assertEquals(0f, MasteryRules.ritualXp(-5));
    }

    @Test
    void addingNeverGoesBelowZeroOrPastTheCap() {
        assertEquals(15f, MasteryRules.add(10, 5), 1e-5);
        assertEquals(10f, MasteryRules.add(10, -5), 1e-5, "a negative gain takes nothing away");
        assertEquals(MasteryRules.MAX_XP, MasteryRules.add(MasteryRules.MAX_XP - 1, 100));
    }

    @Test
    void progressRunsFromOneRankToTheNext() {
        assertEquals(0f, MasteryRules.progress(0), 1e-5);
        assertEquals(0.5f, MasteryRules.progress(75), 1e-5);
        assertEquals(0f, MasteryRules.progress(150), 1e-5);
        assertEquals(0.5f, MasteryRules.progress(375), 1e-5);
        assertEquals(1f, MasteryRules.progress(1800), 1e-5);
        assertEquals(1f, MasteryRules.progress(5000), 1e-5);
    }

    @Test
    void harderPagesAskForHigherRanks() {
        assertEquals(Rank.NONE, MasteryRules.requiredRank(1));
        assertEquals(Rank.APPRENTICE, MasteryRules.requiredRank(2));
        assertEquals(Rank.ADEPT, MasteryRules.requiredRank(3));
        assertEquals(Rank.MASTER, MasteryRules.requiredRank(4));

        assertTrue(MasteryRules.mayLearn(1, Rank.NONE, false), "anyone reads a first-tier page");
        assertFalse(MasteryRules.mayLearn(2, Rank.NONE, false), "a novice cannot read tier 2");
        assertTrue(MasteryRules.mayLearn(2, Rank.APPRENTICE, false));
        assertFalse(MasteryRules.mayLearn(3, Rank.APPRENTICE, false));
        assertTrue(MasteryRules.mayLearn(3, Rank.ADEPT, false));
        assertFalse(MasteryRules.mayLearn(4, Rank.ADEPT, false));
        assertTrue(MasteryRules.mayLearn(4, Rank.MASTER, false));
    }

    @Test
    void morningstarsSparkCountsAsAdeptButNoHigher() {
        assertEquals(Rank.ADEPT, MasteryRules.forLearning(Rank.NONE, true));
        assertEquals(Rank.ADEPT, MasteryRules.forLearning(Rank.APPRENTICE, true));
        assertEquals(Rank.MASTER, MasteryRules.forLearning(Rank.MASTER, true), "the Spark never lowers a rank");
        assertEquals(Rank.NONE, MasteryRules.forLearning(Rank.NONE, false));
        assertTrue(MasteryRules.mayLearn(3, Rank.NONE, true), "the Spark reads tier 3 (Echo)");
        assertFalse(MasteryRules.mayLearn(4, Rank.NONE, true), "but not a Master's page");
    }

    @Test
    void onlyTheHighestRankAddsMana() {
        assertEquals(0, MasteryRules.manaBonus(List.of()));
        assertEquals(0, MasteryRules.manaBonus(List.of(Rank.NONE, Rank.NONE, Rank.NONE, Rank.NONE)));
        assertEquals(10, MasteryRules.manaBonus(List.of(Rank.APPRENTICE, Rank.ADEPT, Rank.APPRENTICE, Rank.NONE)),
                "an Adept and two Apprentices get the Adept's bonus, not the sum");
        assertEquals(20, MasteryRules.manaBonus(List.of(Rank.MASTER, Rank.MASTER, Rank.MASTER, Rank.MASTER)));
        assertEquals(Rank.ADEPT, MasteryRules.highest(List.of(Rank.APPRENTICE, Rank.ADEPT, Rank.NONE)));
    }
}
