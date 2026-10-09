package org.papiricoh.supernaturalcraft.legacy;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchMath;
import org.papiricoh.supernaturalcraft.legacy.research.TopicKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyRulesTest {

    @Test
    void researchNeverMakesAMember() {
        assertEquals(0, LegacyRules.earned(0, 1000, 7));
    }

    @Test
    void ranksClimbOnCountAndSpread() {
        assertEquals(1, LegacyRules.earned(1, 4, 3));
        assertEquals(2, LegacyRules.earned(1, 5, 1));
        assertEquals(2, LegacyRules.earned(1, 20, 2), "fifteen of one or two kinds is not a Scholar");
        assertEquals(3, LegacyRules.earned(1, 15, 3));
        assertEquals(5, LegacyRules.earned(1, 70, 5));
        assertEquals(4, LegacyRules.earned(4, 0, 0), "ranks never fall");
    }

    @Test
    void moreRankMoreDesks() {
        assertEquals(0, LegacyRules.slots(0, true));
        for (int r = 2; r <= LegacyRules.MAX_RANK; r++) assertTrue(LegacyRules.slots(r, false) >= LegacyRules.slots(r - 1, false));
        assertEquals(LegacyRules.slots(5, false) + 1, LegacyRules.slots(5, true));
    }

    @Test
    void researchSlowsAndClosesOnItsCap() {
        for (int n = 1; n < 40; n++) {
            assertTrue(ResearchMath.notes(2, n) >= ResearchMath.notes(2, n - 1));
            assertTrue(ResearchMath.ticks(n, 1) >= ResearchMath.ticks(n - 1, 1));
            assertTrue(ResearchMath.creatureDamage(n) > ResearchMath.creatureDamage(n - 1));
        }
        assertEquals(ResearchMath.MAX_NOTES, ResearchMath.notes(2, 1000));
        assertEquals(ResearchMath.MAX_TICKS, ResearchMath.ticks(1000, 1));
        assertTrue(ResearchMath.creatureDamage(1000) <= ResearchMath.CREATURE_DAMAGE_CAP + 1e-9);
        assertEquals(0, ResearchMath.creatureDamage(0), 1e-9);
    }

    @Test
    void topicIdsSplitOnTheFirstColon() {
        assertEquals(TopicKind.CREATURE, TopicKind.of("creature:minecraft:zombie"));
        assertEquals("minecraft:zombie", TopicKind.subject("creature:minecraft:zombie"));
        assertNull(TopicKind.of("nonsense:x"));
    }

    @Test
    void finishingCountsKindsAndRaisesFiles() {
        Archive a = Archive.EMPTY.finish("creature:minecraft:zombie").finish("creature:minecraft:zombie").finish("lore:bunker");
        assertEquals(2, a.files().get("minecraft:zombie"));
        assertEquals(3, a.totalFinished());
        assertEquals(2, a.kindsFinished());
        assertTrue(a.knows("lore:bunker"));
    }
}
