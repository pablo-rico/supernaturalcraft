package org.papiricoh.supernaturalcraft.entity.horsemen;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.LimboPalette;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorsemenLayoutsTest {

    private static final Map<String, BiFunction<Integer, Long, List<ArenaCell>>> PLANS = Map.of(
            "battlefield", HorsemenLayouts::battlefield, "dead farm", HorsemenLayouts::deadFarm,
            "toxic swamp", HorsemenLayouts::toxicSwamp, "living world", HorsemenLayouts::livingWorld);

    @Test
    void everyPlanStaysInsideTheArenaAndUnderBudget() {
        for (var e : PLANS.entrySet()) {
            for (int radius : new int[]{16, 22, 24, 40}) {
                for (long seed = 0; seed < 5; seed++) {
                    List<ArenaCell> cells = e.getValue().apply(radius, seed * 7919);
                    assertFalse(cells.isEmpty(), e.getKey());
                    assertTrue(cells.size() <= HorsemenLayouts.BUDGET, e.getKey() + " r" + radius + ": " + cells.size());
                    int r = radius - HorsemenLayouts.MARGIN;
                    Set<String> seen = new HashSet<>();
                    for (ArenaCell c : cells) {
                        assertTrue(c.distanceSq() <= r * r, e.getKey() + " leaves the arena at " + c);
                        assertTrue(seen.add(c.dx() + "," + c.dy() + "," + c.dz()), e.getKey() + " writes a cell twice: " + c);
                        assertTrue(c.dy() >= -2 && c.dy() <= 8, e.getKey() + " digs or builds too far: " + c);
                    }
                }
            }
        }
    }

    @Test
    void nothingTallAtTheCentre() {
        for (var e : PLANS.entrySet()) {
            for (ArenaCell c : e.getValue().apply(22, 42L)) {
                if (c.distanceSq() < 4 && c.dy() >= 2) throw new AssertionError(e.getKey() + " builds on the centre: " + c);
            }
        }
    }

    @Test
    void plansAreTheSameForTheSameSeed() {
        assertEquals(HorsemenLayouts.battlefield(22, 9), HorsemenLayouts.battlefield(22, 9));
        assertEquals(HorsemenLayouts.livingWorld(24, 3), HorsemenLayouts.livingWorld(24, 3));
    }

    @Test
    void fourStandardsAndSomeVialSpots() {
        assertEquals(4, HorsemenLayouts.standardSpots(22, 1).size());
        assertFalse(HorsemenLayouts.vialSpots(22, 1).isEmpty());
        for (int[] s : HorsemenLayouts.vialSpots(22, 1)) assertTrue(s[0] * s[0] + s[1] * s[1] <= 20 * 20);
    }

    @Test
    void limboPaletteIsReversible() {
        Set<String> twins = new HashSet<>();
        for (var p : LimboPalette.pairs().entrySet()) {
            assertTrue(twins.add(p.getValue()), "two blocks share a grey twin: " + p.getValue());
            assertFalse(LimboPalette.pairs().containsKey(p.getValue()), "a twin is itself alive: " + p.getValue());
            assertEquals(p.getKey(), LimboPalette.toLiving(LimboPalette.toLimbo(p.getKey())));
        }
        // Every block of the living world flips and comes back.
        for (ArenaCell c : HorsemenLayouts.livingWorld(24, 5)) {
            assertEquals(c.block(), LimboPalette.toLiving(LimboPalette.toLimbo(c.block())));
            assertTrue(LimboPalette.pairs().containsKey(c.block()), "no grey twin for " + c.block());
        }
    }
}
