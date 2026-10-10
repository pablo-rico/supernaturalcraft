package org.papiricoh.supernaturalcraft.memory;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the shrines of a memory lane hold ({@link MemorySelection}). */
class MemorySelectionTest {

    /** 3 victories, 2 deals and 20 sightings, all at different times (sightings first). */
    private static List<Memory> story() {
        List<Memory> log = new ArrayList<>();
        for (int i = 0; i < 20; i++) log.add(MemoryRules.sighting("supernaturalcraft:mob_" + i, 0, 100 + i));
        log.add(MemoryRules.boss(BossProgression.Boss.AZAZEL, 0, 1000));
        log.add(MemoryRules.deal(0, "knowledge", "", false, 0, 1100));
        log.add(MemoryRules.boss(BossProgression.Boss.LILITH, 0, 1200));
        log.add(MemoryRules.deal(1, "upgrade", "0", true, 0, 1300));
        log.add(MemoryRules.boss(BossProgression.Boss.LUCIFER, 0, 1400));
        return log;
    }

    @Test
    void theWeightiestUngatheredComeFirstThenInTimeOrder() {
        List<Memory> chosen = MemorySelection.choose(story(), Set.of(), 12);
        assertEquals(12, chosen.size());
        Set<String> ids = new HashSet<>();
        chosen.forEach(m -> ids.add(m.id()));
        assertTrue(ids.containsAll(List.of("boss:azazel", "boss:lilith", "boss:lucifer", "deal:0", "deal:1")), "victories and deals first");
        // 7 sightings fill the rest: the oldest ones.
        for (int i = 0; i < 7; i++) assertTrue(ids.contains("seen:supernaturalcraft:mob_" + i), "sighting " + i);
        assertEquals(chosen, MemoryRules.chronological(chosen), "slot 0 is the oldest, by the gate");
        assertEquals("seen:supernaturalcraft:mob_0", chosen.getFirst().id());
        assertEquals("boss:lucifer", chosen.getLast().id());
    }

    @Test
    void gatheredMemoriesMakeWayButFillWhatIsLeft() {
        List<Memory> log = story();
        Set<String> gathered = Set.of("boss:azazel", "boss:lilith");
        List<String> ids = MemorySelection.ids(log, gathered, 12, 0);
        assertTrue(!ids.contains("boss:azazel") && !ids.contains("boss:lilith"), "gathered ones give way to ungathered");
        assertEquals(12, ids.size());
        // A short log: the gathered ones come back to fill the lane.
        List<Memory> small = List.of(MemoryRules.boss(BossProgression.Boss.AZAZEL, 0, 1), MemoryRules.boss(BossProgression.Boss.LILITH, 0, 2));
        assertEquals(List.of("boss:azazel", "boss:lilith"), MemorySelection.ids(small, Set.of("boss:azazel"), 12, 0));
    }

    @Test
    void whenAllIsGatheredTheLaneTurns() {
        List<Memory> log = story();
        Set<String> all = new HashSet<>();
        log.forEach(m -> all.add(m.id()));
        List<String> day0 = MemorySelection.ids(log, all, 12, 0), day1 = MemorySelection.ids(log, all, 12, 1);
        assertEquals(12, day0.size());
        assertEquals(12, day1.size());
        assertNotEquals(day0, day1, "the window moves with the days");
        assertEquals(day0, MemorySelection.ids(log, all, 12, log.size()), "and comes round again");
    }

    @Test
    void nothingToShow() {
        assertEquals(List.of(), MemorySelection.choose(List.of(), Set.of(), 12));
        assertEquals(List.of(), MemorySelection.choose(story(), Set.of(), 0));
        assertEquals(3, MemorySelection.choose(story(), Set.of(), 3).size());
    }
}
