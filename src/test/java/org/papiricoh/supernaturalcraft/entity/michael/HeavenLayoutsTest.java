package org.papiricoh.supernaturalcraft.entity.michael;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.michael.arena.HeavenLayouts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeavenLayoutsTest {

    private static final int[] RADII = {20, 26, 32, 40};

    @Test
    void everyHeavenStaysInsideTheArenaAndUnderBudget() {
        for (int w = 0; w < HeavenLayouts.COUNT; w++) {
            for (int radius : RADII) {
                for (long seed = 0; seed < 4; seed++) {
                    List<ArenaCell> cells = HeavenLayouts.plan(w, radius, seed * 7919);
                    assertFalse(cells.isEmpty());
                    assertTrue(cells.size() <= HeavenLayouts.BUDGET, "heaven " + w + " r" + radius + ": " + cells.size());
                    int r = radius - HeavenLayouts.MARGIN;
                    Set<String> seen = new HashSet<>();
                    for (ArenaCell c : cells) {
                        assertTrue(c.distanceSq() <= r * r, "heaven " + w + " leaves the arena at " + c);
                        assertTrue(seen.add(c.dx() + "," + c.dy() + "," + c.dz()), "heaven " + w + " writes a cell twice: " + c);
                        assertTrue(c.dy() >= HeavenLayouts.BOTTOM && c.dy() <= HeavenLayouts.TOP, "heaven " + w + " goes too far: " + c);
                        assertFalse(c.block().isEmpty(), "heaven " + w + " leaves a cell blank: " + c);
                    }
                }
            }
        }
    }

    @Test
    void theUnionIsPinnedOnceAndEveryHeavenFillsAllOfIt() {
        for (int radius : RADII) {
            List<ArenaCell> union = HeavenLayouts.union(radius, 42);
            assertTrue(union.size() <= HeavenLayouts.UNION_BUDGET, "r" + radius + ": " + union.size());
            Set<String> seen = new HashSet<>();
            int lastDy = Integer.MAX_VALUE;
            for (ArenaCell c : union) {
                assertTrue(seen.add(c.dx() + "," + c.dy() + "," + c.dz()), "pinned twice: " + c);
                assertTrue(c.dy() <= lastDy, "the union must go from the top down");
                lastDy = c.dy();
            }
            for (int w = 0; w < HeavenLayouts.COUNT; w++) {
                List<String> blocks = HeavenLayouts.blocksOf(w, union, radius, 42);
                assertEquals(union.size(), blocks.size());
                // Every cell of this Heaven keeps its own block, and what it does not use is cleared.
                Set<String> mine = new HashSet<>();
                for (ArenaCell c : HeavenLayouts.plan(w, radius, 42)) mine.add(c.dx() + "," + c.dy() + "," + c.dz() + "=" + c.block());
                for (int i = 0; i < union.size(); i++) {
                    ArenaCell c = union.get(i);
                    String b = blocks.get(i);
                    if (!mine.contains(c.dx() + "," + c.dy() + "," + c.dz() + "=" + b)) {
                        assertEquals(HeavenLayouts.filler(w, c.dx(), c.dy(), c.dz()), b);
                        if (c.dy() > 0) assertEquals(HeavenLayouts.AIR, b);
                    }
                }
            }
        }
    }

    @Test
    void nothingTallAtTheCentre() {
        for (int w = 0; w < HeavenLayouts.COUNT; w++) {
            for (ArenaCell c : HeavenLayouts.plan(w, 26, 7L)) {
                if (c.distanceSq() <= HeavenLayouts.CLEAR_CENTRE * HeavenLayouts.CLEAR_CENTRE && c.dy() >= 1) {
                    throw new AssertionError("heaven " + w + " builds on the centre: " + c);
                }
            }
        }
    }

    @Test
    void eachHeavenHasItsLandmarks() {
        List<ArenaCell> garden = HeavenLayouts.garden(26, 3), war = HeavenLayouts.warInHeaven(26, 3), throne = HeavenLayouts.throneRoom(26, 3);
        assertTrue(garden.stream().anyMatch(c -> c.block().startsWith("minecraft:birch_stairs")), "the bench");
        assertTrue(garden.stream().anyMatch(c -> c.block().startsWith("minecraft:cherry_leaves")), "the cherry trees");
        assertTrue(garden.stream().anyMatch(c -> c.block().equals("minecraft:ochre_froglight")), "the golden light");
        assertTrue(war.stream().anyMatch(c -> c.block().equals("minecraft:soul_fire")), "holy fire");
        assertTrue(war.stream().anyMatch(c -> c.dy() < 0), "craters");
        assertTrue(war.stream().anyMatch(c -> c.block().equals("minecraft:quartz_pillar")), "broken columns");
        assertTrue(throne.stream().anyMatch(c -> c.block().startsWith("minecraft:quartz_stairs[facing=south]") && c.dy() == 2), "the throne");
        assertTrue(throne.stream().filter(c -> c.block().equals("minecraft:gold_block") && c.dy() == 7).count() >= 6, "the columns");
    }

    @Test
    void plansAreTheSameForTheSameSeed() {
        for (int w = 0; w < HeavenLayouts.COUNT; w++) assertEquals(HeavenLayouts.plan(w, 26, 9), HeavenLayouts.plan(w, 26, 9));
        assertEquals(HeavenLayouts.union(30, 9), HeavenLayouts.union(30, 9));
    }
}
