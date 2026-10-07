package org.papiricoh.supernaturalcraft.entity.chuck.arena;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaKind;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaPlan;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaShapes;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BlankPageErosion;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.HellLayout;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.StormLayout;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The five arenas the Author writes: inside the arena, within budget, a floor to stand on, his spot clear. */
class ArenaLayoutsTest {

    /** The test arena, the config's range and its default. */
    private static final int[] RADII = {16, 28, 34, 48};
    private static final long SEED = 0x5EEDL;

    @Test
    void everyPlanStaysInsideTheArenaWithoutRepeatsAndWithinBudget() {
        for (int r : RADII) {
            for (Chapter ch : Chapter.values()) {
                ArenaPlan plan = ArenaLayouts.plan(ch, r, SEED);
                Set<Long> seen = new HashSet<>();
                for (ArenaPlan.Cell c : plan.cells()) {
                    assertTrue(c.dx() * c.dx() + c.dz() * c.dz() <= r * r, ch + " r" + r + " outside the arena: " + c);
                    assertTrue(c.dy() >= ArenaPlan.MIN_DY && c.dy() <= 30, ch + " r" + r + " too high or deep: " + c);
                    assertTrue(seen.add(ArenaPlan.key(c.dx(), c.dy(), c.dz())), ch + " r" + r + " twice: " + c);
                }
                assertTrue(plan.cells().size() <= ArenaLayouts.budget(r), ch + " r" + r + " over budget: " + plan.cells().size());
            }
        }
    }

    @Test
    void everyColumnHasAFloorAndTheCentreIsWalkable() {
        for (int r : RADII) {
            for (Chapter ch : Chapter.values()) {
                ArenaPlan plan = ArenaLayouts.plan(ch, r, SEED);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dz * dz > r * r) continue;
                        assertNotNull(plan.at(dx, 0, dz), ch + " r" + r + " leaves the floor at " + dx + "," + dz);
                        if (dx * dx + dz * dz <= 25) {
                            assertTrue(plan.at(dx, 0, dz).solid(), ch + " r" + r + " no floor near the centre at " + dx + "," + dz);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theAuthorsSpotIsClearAndStandsOnSomething() {
        for (int r : RADII) {
            for (Chapter ch : Chapter.values()) {
                ArenaPlan plan = ArenaLayouts.plan(ch, r, SEED);
                ArenaPlan.Cell s = plan.spawn();
                ArenaKind under = plan.at(s.dx(), s.dy() - 1, s.dz());
                assertNotNull(under, ch + " spawn floats");
                assertTrue(under.solid(), ch + " spawn on " + under);
                for (int y = 0; y < 3; y++) {
                    ArenaKind k = plan.at(s.dx(), s.dy() + y, s.dz());
                    assertTrue(k == null || k == ArenaKind.AIR, ch + " spawn blocked by " + k + " at +" + y);
                }
            }
        }
    }

    @Test
    void edenHasPoolsLightAndATree() {
        ArenaPlan eden = ArenaLayouts.plan(Chapter.EDEN, 34, SEED);
        assertTrue(eden.water().size() >= 6 * 9, "pools: " + eden.water().size());
        assertTrue(eden.lights().size() >= 30, "light: " + eden.lights().size());
        assertTrue(eden.cells().stream().anyMatch(c -> c.kind() == ArenaKind.GOLDEN_APPLE), "no golden apples");
        assertEquals(ArenaKind.OAK_LOG, eden.at(0, 5, 0), "the tree of knowledge stands at the centre");
        // Water never spills: every pool cell has a bottom and no open side.
        for (ArenaPlan.Cell w : eden.water()) {
            assertNotNull(eden.at(w.dx(), w.dy() - 1, w.dz()), "pool without a bottom at " + w);
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                ArenaKind side = eden.at(w.dx() + d[0], w.dy(), w.dz() + d[1]);
                assertTrue(side != null && (side.solid() || side.water()), "pool open at " + w);
            }
        }
    }

    @Test
    void hellHasACageWithDoorsAndHellfire() {
        int r = 34;
        ArenaPlan hell = ArenaLayouts.plan(Chapter.HELL, r, SEED);
        int cage = HellLayout.cageRadius(r);
        int bars = 0, open = 0;
        for (ArenaShapes.RingCell c : ArenaShapes.ring(cage)) {
            ArenaKind k = hell.at(c.x(), 1, c.z());
            if (k == ArenaKind.BARS) bars++;
            if (k == null) open++;
        }
        assertTrue(bars > 50, "bars: " + bars);
        assertTrue(open >= HellLayout.DOORWAYS * 2, "doorways: " + open);
        assertTrue(hell.lights().stream().filter(c -> c.kind() == ArenaKind.HELLFIRE).count() >= 8 * 5, "braziers");
        assertEquals(3, hell.spawn().dy(), "he stands on the dais");
    }

    @Test
    void theStormHasGapsDeepEnoughToBeRescuedAndIslandsWithinAJump() {
        for (int r : RADII) {
            ArenaPlan storm = ArenaLayouts.plan(Chapter.STORM, r, SEED);
            int gaps = 0, solid = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    if (storm.at(dx, 0, dz) == ArenaKind.AIR) {
                        gaps++;
                        for (int y = 0; y > -StormLayout.PIT_DEPTH; y--) {
                            assertEquals(ArenaKind.AIR, storm.at(dx, y, dz), "a shallow gap at " + dx + "," + y + "," + dz);
                        }
                    } else {
                        solid++;
                    }
                }
            }
            assertTrue(gaps > solid / 5, "r" + r + " hardly any gaps: " + gaps);
            assertTrue(solid > gaps / 2, "r" + r + " hardly any cloud: " + solid);
            // From the centre, hops of up to three blocks reach almost every island.
            Set<Long> reached = reach(storm, r, 3.2);
            assertTrue(reached.size() >= solid * 0.95, "r" + r + " islands out of reach: " + reached.size() + "/" + solid);
        }
    }

    @Test
    void theLibraryTowersAndLightsItself() {
        ArenaPlan lib = ArenaLayouts.plan(Chapter.LIBRARY, 34, SEED);
        long shelves = lib.cells().stream().filter(c -> c.kind() == ArenaKind.BOOKSHELF).count();
        int top = lib.cells().stream().filter(c -> c.kind() == ArenaKind.BOOKSHELF).mapToInt(ArenaPlan.Cell::dy).max().orElse(0);
        assertTrue(shelves > 1500, "shelves: " + shelves);
        assertTrue(top >= 12, "the outer wall should tower: " + top);
        assertTrue(lib.lights().size() >= 20, "lights: " + lib.lights().size());
        long ink = lib.cells().stream().filter(c -> c.dy() == 0 && c.kind() == ArenaKind.INK).count();
        assertTrue(ink > 200, "the book should be written in: " + ink);
    }

    @Test
    void theBlankPageIsPaperAndInkOnly() {
        ArenaPlan blank = ArenaLayouts.plan(Chapter.BLANK, 34, SEED);
        for (ArenaPlan.Cell c : blank.cells()) {
            assertEquals(0, c.dy(), "something stands on the blank page: " + c);
            assertTrue(c.kind() == ArenaKind.PAGE || c.kind() == ArenaKind.INK, "not paper: " + c);
        }
        assertTrue(blank.lights().isEmpty());
        assertNull(blank.at(0, 1, 0));
    }

    @Test
    void theWholeFightFitsTheArenasMemory() {
        // Every distinct position the five chapters, the erosion and a ceiling can touch, plus a forest's worth.
        for (int r : new int[]{34, 48}) {
            Set<Long> touched = new HashSet<>();
            for (Chapter ch : Chapter.values()) {
                for (ArenaPlan.Cell c : ArenaLayouts.plan(ch, r, SEED).cells()) touched.add(ArenaPlan.key(c.dx(), c.dy(), c.dz()));
            }
            for (BlankPageErosion.Column c : BlankPageErosion.order(r, SEED)) {
                for (int y = 0; y > -BlankPageErosion.DEPTH; y--) touched.add(ArenaPlan.key(c.dx(), y, c.dz()));
            }
            int ceiling = (int) Math.round(Math.PI * r * r);
            // A dense forest and the cabin: about two blocks standing per column.
            int forest = (int) Math.round(2 * Math.PI * r * r);
            int total = touched.size() + ceiling + forest;
            assertTrue(total <= 90_000, "r" + r + " needs " + total + " remembered positions");
        }
    }

    /** Columns of solid floor reachable from the centre in hops of at most {@code hop} blocks. */
    private static Set<Long> reach(ArenaPlan plan, int r, double hop) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{0, 0});
        seen.add(ArenaPlan.key(0, 0, 0));
        int h = (int) Math.ceil(hop);
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int dx = -h; dx <= h; dx++) {
                for (int dz = -h; dz <= h; dz++) {
                    if (dx * dx + dz * dz > hop * hop) continue;
                    int x = p[0] + dx, z = p[1] + dz;
                    ArenaKind k = plan.at(x, 0, z);
                    if (k == null || !k.solid() || !seen.add(ArenaPlan.key(x, 0, z))) continue;
                    queue.add(new int[]{x, z});
                }
            }
        }
        return seen;
    }

    @Test
    void ringsHaveNoDiagonalSteps() {
        for (double radius : new double[]{5, 12.5, 17, 31}) {
            List<ArenaShapes.RingCell> ring = ArenaShapes.ring(radius);
            Set<Long> cells = new HashSet<>();
            ring.forEach(c -> cells.add(ArenaPlan.key(c.x(), 0, c.z())));
            for (ArenaShapes.RingCell c : ring) {
                int sides = 0;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    if (cells.contains(ArenaPlan.key(c.x() + d[0], 0, c.z() + d[1]))) sides++;
                }
                assertTrue(sides >= 2, "ring " + radius + " breaks at " + c);
            }
        }
    }
}
