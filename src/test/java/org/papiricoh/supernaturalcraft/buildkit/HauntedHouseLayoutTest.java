package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout;
import org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout.Hiding;
import org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout.Spot;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The haunted house against the kit's quality bar ({@link LayoutQuality}) for seeds that cover both turret sides, every ruin
 * level and every hiding place, each turned a different way: chests and the study's chest spot reachable, the study's spot clear
 * floor, 2–4 bones each in an air cell on a solid floor inside the plot, dug out from a cell one can walk (or climb the well's
 * ladder) to within reach, and the same seed always the same house. Look at the structview renders too
 * ({@code LayoutDumpTest}, {@code HauntedLayoutCatalog}).
 */
class HauntedHouseLayoutTest {

    private static final long[] SEEDS = {3L, 11L, 5L, 7L, 1234567L, -99L, 42L, 2024L};

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    @TestFactory
    List<DynamicTest> everyHouse() {
        List<DynamicTest> out = new ArrayList<>();
        for (int i = 0; i < SEEDS.length; i++) {
            final long seed = SEEDS[i];
            final int dir = i % 4;
            out.add(DynamicTest.dynamicTest("house " + seed + " turned " + dir, () -> check(seed, dir)));
        }
        return out;
    }

    private static void check(long seed, int dir) {
        Canvas c = HauntedHouseLayout.canvas(seed, dir);
        int r = HauntedHouseLayout.RADIUS;
        Box bounds = new Box(-r, -HauntedHouseLayout.DEPTH, -r, r, HauntedHouseLayout.HEIGHT, r);
        LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
        LayoutQuality.assertBounds(c, bounds);
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertWaterContained(c);
        LayoutQuality.assertPalette(c, 40);

        LayoutPoint entry = HauntedHouseLayout.at(HauntedHouseLayout.ENTRY, seed, dir);
        assertTrue(LayoutQuality.standable(c, entry.x(), entry.y(), entry.z()), "the entry");
        List<int[]> targets = new ArrayList<>();
        for (int i = 0; i < HauntedHouseLayout.CHESTS.size(); i++) {
            LayoutPoint q = HauntedHouseLayout.at(HauntedHouseLayout.CHESTS.get(i), seed, dir);
            assertEquals("minecraft:chest", St.id(c.get(q.x(), q.y(), q.z())), "chest " + i + " at " + q);
            assertTrue(HauntedHouseLayout.loot(i).startsWith("supernaturalcraft:chests/haunted_house/"));
            targets.add(p(q));
        }
        // The study's chest spot: air over a full floor block, headroom, and the cell before it free.
        LayoutPoint study = HauntedHouseLayout.at(HauntedHouseLayout.STUDY_CHEST, seed, dir);
        assertTrue(c.isAir(study.x(), study.y(), study.z()) && c.isAir(study.x(), study.y() + 1, study.z())
                && Kinds.fullCube(c.world(study.x(), study.y() - 1, study.z())), "the study's chest spot " + study + ": "
                + c.world(study.x(), study.y(), study.z()) + " over " + c.world(study.x(), study.y() - 1, study.z()));
        targets.add(p(study));
        LayoutQuality.assertReachable(c, p(entry), targets);
        LayoutQuality.assertHeadroom(c, p(entry));

        // The family's bones.
        List<Spot> spots = HauntedHouseLayout.spots(seed, dir);
        assertTrue(spots.size() >= 2 && spots.size() <= 4, "2–4 ghosts, got " + spots.size());
        assertEquals(spots.stream().map(Spot::bones).toList(), HauntedHouseLayout.bones(seed, dir));
        Set<Long> reach = reachClimbing(c, p(entry));
        Box plot = new Box(-r, -HauntedHouseLayout.DEPTH, -r, r, HauntedHouseLayout.HEIGHT, r);
        for (Spot s : spots) {
            LayoutPoint b = s.bones(), a = s.access();
            String what = s.where() + " " + b;
            assertTrue(plot.contains(b.x(), b.y(), b.z()) && c.has(b.x(), b.y(), b.z()), what + " lies in the plan");
            assertTrue(c.isAir(b.x(), b.y(), b.z()), what + ": the bones' cell is left air, not " + c.world(b.x(), b.y(), b.z()));
            assertTrue(Kinds.sturdy(c.world(b.x(), b.y() - 1, b.z()), Dir.UP), what + " on a solid floor, not " + c.world(b.x(), b.y() - 1, b.z()));
            assertTrue(reach.contains(Canvas.key(a.x(), a.y(), a.z())), what + ": its access " + a + " can be reached");
            // Within reach of the cell over the bones, and nothing in between that one could not break (all vanilla, no bedrock).
            LayoutPoint over = new LayoutPoint(b.x(), b.y() + 1, b.z());
            double d = Math.sqrt(Math.pow(a.x() - over.x(), 2) + Math.pow(a.y() + 1 - over.y(), 2) + Math.pow(a.z() - over.z(), 2));
            assertTrue(d <= 4.5, what + ": the cover is " + d + " from where one stands");
            for (int x = Math.min(a.x(), over.x()); x <= Math.max(a.x(), over.x()); x++) {
                for (int y = Math.min(a.y(), over.y()); y <= Math.max(a.y(), over.y()); y++) {
                    for (int z = Math.min(a.z(), over.z()); z <= Math.max(a.z(), over.z()); z++) {
                        String id = St.id(c.world(x, y, z));
                        assertFalse(id.equals("minecraft:bedrock") || id.equals("minecraft:barrier"), what + ": unbreakable " + id);
                    }
                }
            }
            // Something covers them (they are found, not stumbled on): the cell over the bones or the way to them is not open air.
            boolean hidden = !c.isAir(over.x(), over.y(), over.z()) || s.where() == Hiding.CELLAR || s.where() == Hiding.ATTIC;
            assertTrue(hidden, what + " is hidden");
        }
    }

    @Test
    void theHouseLooksTheParts() {
        Canvas c = HauntedHouseLayout.canvas(3L, 0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-12, 0, -9, 15, 22, 10));
    }

    @Test
    void theSeedDecidesEverythingAndNothingElse() {
        for (long seed : new long[]{3L, 11L, -99L}) {
            assertEquals(HauntedHouseLayout.plan(seed, 1).cells(), HauntedHouseLayout.plan(seed, 1).cells(), "same seed, same house");
            assertEquals(HauntedHouseLayout.bones(seed, 2), HauntedHouseLayout.bones(seed, 2));
        }
        // Over many seeds: 2–4 ghosts, every hiding used, both turret sides, every ruin level.
        Set<Hiding> used = EnumSet.noneOf(Hiding.class);
        Set<Integer> counts = new HashSet<>(), ruins = new HashSet<>();
        Set<Boolean> sides = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            List<Hiding> h = HauntedHouseLayout.hidings(seed);
            assertTrue(h.size() >= 2 && h.size() <= 4, "seed " + seed + ": " + h);
            assertEquals(h.size(), new HashSet<>(h).size(), "no hiding twice");
            used.addAll(h);
            counts.add(h.size());
            ruins.add(HauntedHouseLayout.ruin(seed));
            sides.add(HauntedHouseLayout.mirrored(seed));
        }
        assertEquals(EnumSet.allOf(Hiding.class), used);
        assertEquals(Set.of(2, 3, 4), counts);
        assertEquals(Set.of(0, 1, 2), ruins);
        assertEquals(Set.of(true, false), sides);
        // The tested seeds cover it all too.
        Set<Hiding> tested = EnumSet.noneOf(Hiding.class);
        Set<Boolean> testedSides = new HashSet<>();
        Set<Integer> testedRuins = new HashSet<>();
        for (long seed : SEEDS) {
            tested.addAll(HauntedHouseLayout.hidings(seed));
            testedSides.add(HauntedHouseLayout.mirrored(seed));
            testedRuins.add(HauntedHouseLayout.ruin(seed));
        }
        assertEquals(EnumSet.allOf(Hiding.class), tested, "the seeds tested cover every hiding");
        assertEquals(Set.of(true, false), testedSides, "the seeds tested cover both turret sides");
        assertEquals(Set.of(0, 1, 2), testedRuins, "the seeds tested cover every ruin level");
    }

    @Test
    void theSiteIsChosenOnFlatDryGround() {
        int[] flat = {70, 71, 70, 72, 70};
        assertTrue(HauntedHouseLayout.suitable(flat, flat, 63));
        int[] water = {60, 60, 60, 60, 60}, seabed = {50, 50, 50, 50, 50};
        assertFalse(HauntedHouseLayout.suitable(water, seabed, 63));
        assertFalse(HauntedHouseLayout.suitable(new int[]{70, 78, 70, 70, 70}, new int[]{70, 78, 70, 70, 70}, 63));
    }

    /**
     * Every feet cell a walker reaches from {@code from} stepping up or down one block ({@link LayoutQuality#walk}), and also
     * climbing: up and down ladders, and off a ladder onto a floor beside it.
     */
    static Set<Long> reachClimbing(Canvas c, int[] from) {
        Box b = c.bounds().grow(2);
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(from);
        seen.add(Canvas.key(from[0], from[1], from[2]));
        while (!queue.isEmpty()) {
            int[] q = queue.poll();
            List<int[]> next = new ArrayList<>();
            for (Dir d : Dir.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) next.add(new int[]{q[0] + d.dx, q[1] + dy, q[2] + d.dz});
            if (ladder(c, q[0], q[1], q[2]) || ladder(c, q[0], q[1] - 1, q[2])) {
                next.add(new int[]{q[0], q[1] + 1, q[2]});
                next.add(new int[]{q[0], q[1] - 1, q[2]});
            }
            for (int[] n : next) {
                if (!b.contains(n[0], n[1], n[2]) || !node(c, n[0], n[1], n[2])) continue;
                if (n[1] > q[1] && !Kinds.passable(c.world(q[0], q[1] + 2, q[2]))) continue;
                if (seen.add(Canvas.key(n[0], n[1], n[2]))) queue.add(n);
            }
        }
        return seen;
    }

    private static boolean ladder(Canvas c, int x, int y, int z) {
        return St.path(c.world(x, y, z)).equals("ladder");
    }

    private static boolean node(Canvas c, int x, int y, int z) {
        if (!Kinds.passable(c.world(x, y, z)) || !Kinds.passable(c.world(x, y + 1, z))) return false;
        return LayoutQuality.standable(c, x, y, z) || ladder(c, x, y, z);
    }
}
