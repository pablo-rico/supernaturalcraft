package org.papiricoh.supernaturalcraft.legacy.research;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchBoardTest {

    private static ResearchBoard.State state(int rank, Set<String> researched, Map<String, Integer> files, Set<String> running) {
        return new ResearchBoard.State(rank, researched, files, 0, 0, running,
                List.of(new ResearchBoard.Creature("minecraft:zombie", false), new ResearchBoard.Creature("supernaturalcraft:vampire", true)),
                List.of(new ResearchBoard.Boss("supernaturalcraft:azazel", 1), new ResearchBoard.Boss("supernaturalcraft:lucifer", 2)),
                List.of(new ResearchBoard.Artifact(99L, "ring", 0), new ResearchBoard.Artifact(7L, "doll", 3)),
                List.of(new ResearchBoard.SolvedCase(0, "supernaturalcraft:vampire", 2)));
    }

    @Test
    void notAMemberGetsNothing() {
        assertTrue(ResearchBoard.board(state(0, Set.of(), Map.of(), Set.of()), 1).isEmpty());
    }

    @Test
    void tiersStayWithinTheRank() {
        for (int rank = 1; rank <= 5; rank++) {
            for (ResearchBoard.Topic t : ResearchBoard.board(state(rank, Set.of(), Map.of(), Set.of()), 1)) {
                assertTrue(t.tier() >= 1 && t.tier() <= rank, t.topic() + " at rank " + rank);
            }
        }
        List<ResearchBoard.Topic> r1 = ResearchBoard.board(state(1, Set.of(), Map.of(), Set.of()), 1);
        assertNotNull(find(r1, "formula:0"));
        assertNotNull(find(r1, "rite:0"));
        assertNotNull(find(r1, "creature:minecraft:zombie"));
        assertNotNull(find(r1, "boss:supernaturalcraft:azazel"));
        assertNull(find(r1, "boss:supernaturalcraft:lucifer"), "a tier II boss waits for rank 2");
        assertNotNull(find(r1, "artifact:99"));
        assertNull(find(r1, "artifact:7"), "a legendary artifact is tier IV");
        assertNull(find(r1, "case:0"));
        assertNotNull(find(r1, "lore:bunker"));
        assertNull(find(r1, "lore:the_thule"));
        assertNotNull(find(ResearchBoard.board(state(5, Set.of(), Map.of(), Set.of()), 1), "lore:the_thule"));
    }

    @Test
    void doneAndRunningTopicsLeaveTheBoardButFilesGoOn() {
        Set<String> done = Set.of("lore:bunker", "boss:supernaturalcraft:azazel", "creature:minecraft:zombie");
        List<ResearchBoard.Topic> b = ResearchBoard.board(state(5, done, Map.of("minecraft:zombie", 3), Set.of("lore:henry")), 1);
        assertNull(find(b, "lore:bunker"));
        assertNull(find(b, "lore:henry"));
        assertNull(find(b, "boss:supernaturalcraft:azazel"));
        ResearchBoard.Topic zombie = find(b, "creature:minecraft:zombie");
        assertNotNull(zombie, "a file always has a next level");
        assertEquals(List.of("#entity.minecraft.zombie", "4"), zombie.args());
        assertEquals(ResearchBoard.creatureTier(3), zombie.tier());
        assertEquals(ResearchMath.notes(2, 3), zombie.cost().noteCount());
        assertEquals("creature:minecraft:zombie", zombie.cost().notes());
        assertEquals(ResearchMath.ticks(3, 1), zombie.ticks());
    }

    @Test
    void costsFollowTheTopic() {
        List<ResearchBoard.Topic> b = ResearchBoard.board(state(5, Set.of(), Map.of(), Set.of()), 2);
        assertEquals("arcane", find(b, "formula:0").cost().notes());
        assertEquals("arcane", find(b, "rite:0").cost().notes());
        assertEquals("relic", find(b, "artifact:7").cost().notes());
        assertEquals("creature:supernaturalcraft:vampire", find(b, "case:0").cost().notes());
        assertEquals("creature:supernaturalcraft:lucifer", find(b, "boss:supernaturalcraft:lucifer").cost().notes());
        assertEquals("place", find(b, "lore:order_history").cost().notes());
        ResearchBoard.Topic legendary = find(b, "artifact:7");
        assertEquals(4, legendary.tier());
        assertEquals("supernaturalcraft:holy_water", legendary.cost().reagent());
        assertEquals("", find(b, "lore:bunker").cost().reagent());
        assertEquals("supernaturalcraft:demon_blood", find(b, "lore:the_thule").cost().reagent());
        assertEquals(ResearchMath.ticks(0, 2), find(b, "formula:0").ticks(), "speed shortens research");
        for (ResearchBoard.Topic t : b) {
            assertTrue(t.cost().noteCount() >= 1 && t.cost().noteCount() <= ResearchMath.MAX_NOTES);
            assertEquals(1 + t.tier(), t.cost().paper());
            assertEquals("research.supernaturalcraft.topic." + t.kind().id(), t.titleKey());
        }
    }

    @Test
    void findValidatesAStart() {
        ResearchBoard.State s = state(1, Set.of(), Map.of(), Set.of());
        assertNotNull(ResearchBoard.find(s, "lore:bunker", 1));
        assertNull(ResearchBoard.find(s, "lore:the_thule", 1));
        assertNull(ResearchBoard.find(s, "creature:minecraft:creeper", 1), "never seen");
        assertNull(ResearchBoard.find(s, "formula:5", 1), "only the next formula");
    }

    @Test
    void loreIdsAreUniqueAndSpread() {
        assertEquals(ArchiveLore.ALL.size(), Set.copyOf(ArchiveLore.ids()).size());
        for (int tier = 1; tier <= 5; tier++) {
            int t = tier;
            assertTrue(ArchiveLore.ALL.stream().anyMatch(l -> l.tier() == t), "a lore page at tier " + t);
        }
    }

    private static ResearchBoard.Topic find(List<ResearchBoard.Topic> board, String topic) {
        return board.stream().filter(t -> t.topic().equals(topic)).findFirst().orElse(null);
    }
}
