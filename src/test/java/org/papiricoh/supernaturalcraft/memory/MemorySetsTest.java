package org.papiricoh.supernaturalcraft.memory;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The memory sets and their gifts ({@link MemorySets}). */
class MemorySetsTest {

    private static MemoryLog log(List<Memory> memories, boolean gatherAll) {
        MemoryLog log = MemoryLog.EMPTY;
        for (Memory m : memories) log = log.with(m);
        if (gatherAll) for (Memory m : memories) log = log.withCollected(m.id());
        return log;
    }

    @Test
    void everyKindCountsTowardsASet() {
        Set<String> sets = new HashSet<>();
        for (MemorySets.Set s : MemorySets.Set.values()) sets.add(s.id);
        for (MemoryKind k : MemoryKind.values()) assertTrue(sets.contains(k.set), k + " counts towards " + k.set);
        for (MemorySets.Set s : MemorySets.Set.values()) assertTrue(s.goal() > 0, s + " has a goal");
    }

    @Test
    void onlyGatheredMemoriesCount() {
        List<Memory> five = new ArrayList<>();
        for (BossProgression.Boss b : List.of(BossProgression.Boss.AZAZEL, BossProgression.Boss.LILITH, BossProgression.Boss.LUCIFER,
                BossProgression.Boss.WAR, BossProgression.Boss.GABRIEL)) {
            five.add(MemoryRules.boss(b, 0, 0));
        }
        assertFalse(MemorySets.complete(MemorySets.Set.VICTORIES, five, Set.of()), "remembered is not gathered");
        MemoryLog gathered = log(five, true);
        assertEquals(5, MemorySets.progress(MemorySets.Set.VICTORIES, gathered.entries(), gathered.collected()));
        assertTrue(MemorySets.completed(gathered).contains(MemorySets.Set.VICTORIES));
        assertFalse(MemorySets.completed(gathered).contains(MemorySets.Set.ALL_VICTORIES));
        MemoryLog four = log(five.subList(0, 4), true);
        assertFalse(MemorySets.completed(four).contains(MemorySets.Set.VICTORIES));
        assertEquals(EnumSet.of(MemorySets.Set.VICTORIES), MemorySets.newlyCompleted(four, gathered));
    }

    @Test
    void allVictoriesNeedsTheWholeMainRoad() {
        List<Memory> road = new ArrayList<>();
        for (BossProgression.Boss b : BossProgression.Boss.values()) if (!b.optional) road.add(MemoryRules.boss(b, 0, 0));
        assertEquals(road.size(), MemorySets.Set.ALL_VICTORIES.goal());
        assertTrue(MemorySets.completed(log(road, true)).contains(MemorySets.Set.ALL_VICTORIES));
        List<Memory> sideRoads = new ArrayList<>(road.subList(1, road.size()));
        for (BossProgression.Boss b : BossProgression.Boss.values()) if (b.optional) sideRoads.add(MemoryRules.boss(b, 0, 0));
        assertFalse(MemorySets.completed(log(sideRoads, true)).contains(MemorySets.Set.ALL_VICTORIES), "side roads do not stand in for the main one");
    }

    @Test
    void theSmallerSets() {
        List<Memory> sights = new ArrayList<>();
        for (int i = 0; i < 9; i++) sights.add(MemoryRules.sighting("supernaturalcraft:mob_" + i, 0, 0));
        assertFalse(MemorySets.completed(log(sights, true)).contains(MemorySets.Set.SIGHTINGS));
        sights.add(MemoryRules.prey("minecraft:zombie", 40, 0, 0));
        assertTrue(MemorySets.completed(log(sights, true)).contains(MemorySets.Set.SIGHTINGS), "favourite prey counts as a sighting");

        MemoryLog kin = log(List.of(MemoryRules.rank("hunter", 1, 0, 0), MemoryRules.legacyRank(1, 0, 0),
                new Memory("call:messenger", MemoryKind.HEEDED_CALL, "messenger", "", 0, 0, 0)), true);
        assertTrue(MemorySets.completed(kin).contains(MemorySets.Set.KIN));

        MemoryLog cases = log(List.of(MemoryRules.caseClosed(0, "a", "b", true, 0, 0), MemoryRules.caseClosed(1, "a", "b", false, 0, 0),
                MemoryRules.caseClosed(2, "a", "b", true, 0, 0)), true);
        assertTrue(MemorySets.completed(cases).contains(MemorySets.Set.CASES), "a lost case is still a case");

        MemoryLog pet = log(List.of(MemoryRules.petLost("x", "minecraft:wolf", "", 0, 0)), true);
        assertTrue(MemorySets.completed(pet).contains(MemorySets.Set.COMPANIONS));

        MemoryLog deals = log(List.of(MemoryRules.deal(0, "a", "", false, 0, 0), MemoryRules.deal(1, "a", "", true, 0, 0)), true);
        assertTrue(MemorySets.completed(deals).contains(MemorySets.Set.CROSSROADS));
    }

    @Test
    void theGiftsStaySmall() {
        assertTrue(MemorySets.VICTORY_AEGIS > 0 && MemorySets.VICTORY_AEGIS <= 0.05f);
        assertEquals(2.0, MemorySets.ALL_VICTORIES_HEALTH);
        assertTrue(MemorySets.CASES_RESEARCH_TIME < 1 && MemorySets.CASES_RESEARCH_TIME >= 0.9);
        assertTrue(MemorySets.SIGHTING_DAMAGE <= 0.05f);
    }
}
