package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladePlan;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladeSite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Eve's glade (v0.39) against the kit's quality bar ({@link LayoutQuality}) for several seeds: known states, within its bounds,
 * shaped and supported, the throne, the rift's spot, every birth spot and the centre reachable on foot from the way in, the rift's
 * spot clear for a rift three wide and five tall, the fight's clearing open, and the site a walk from the landing. Look at the
 * structview renders too ({@code LayoutDumpTest} does not dump it; render it from {@link EveGladePlan#canvas}).
 */
class EveGladeLayoutTest {

    private static final long[] SEEDS = {1L, 42L, -7L, 20251107L};

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    @TestFactory
    List<DynamicTest> everyGlade() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) out.add(DynamicTest.dynamicTest("glade " + seed, () -> check(seed)));
        return out;
    }

    private static void check(long seed) {
        Canvas c = EveGladePlan.canvas(seed);
        int r = EveGladePlan.EXTENT;
        LayoutQuality.assertIdsKnown(c, BlockIds.vanilla().allow(EveGladePlan.MOD_BLOCKS.toArray(String[]::new)));
        LayoutQuality.assertBounds(c, new Box(-r, -EveGladePlan.DEPTH, -r, r, EveGladePlan.HEIGHT, r));
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertPalette(c, 14);
        LayoutQuality.assertBudget(c, 80_000);

        int[] entry = p(EveGladePlan.at(EveGladePlan.ENTRY));
        assertTrue(LayoutQuality.standable(c, entry[0], entry[1], entry[2]), "the way in");
        List<int[]> targets = new ArrayList<>();
        for (String a : List.of(EveGladePlan.CENTRE, EveGladePlan.THRONE, EveGladePlan.RIFT)) targets.add(p(EveGladePlan.at(a)));
        for (int i = 0; i < EveGladePlan.BIRTHS; i++) targets.add(p(EveGladePlan.birth(i)));
        LayoutQuality.assertReachable(c, entry, targets);
        LayoutQuality.assertHeadroom(c, entry);

        // The rift home stands at its point, along x: three wide, five tall, all air (and firm ground under its middle).
        LayoutPoint rift = EveGladePlan.at(EveGladePlan.RIFT);
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy < 5; dy++) {
            assertTrue(c.isAir(rift.x() + dx, rift.y() + dy, rift.z()) || Kinds.passable(c.world(rift.x() + dx, rift.y() + dy, rift.z())),
                    "the rift's room at " + dx + "," + dy);
        }
        assertTrue(Kinds.sturdy(c.world(rift.x(), rift.y() - 1, rift.z()), Dir.UP), "the rift stands on ground");
        // Eve rises on the dais before her throne.
        LayoutPoint throne = EveGladePlan.at(EveGladePlan.THRONE);
        assertTrue(LayoutQuality.standable(c, throne.x(), throne.y(), throne.z()), "her spot before the throne");
        // The fight's ground: most of the clearing's floor is open to walk on (no forest of stones in it).
        int open = 0, all = 0;
        int in = EveGladePlan.RADIUS - 3;
        for (int x = -in; x <= in; x++) for (int z = -in; z <= in; z++) {
            if (x * x + z * z > in * in) continue;
            all++;
            if (Kinds.passable(c.world(x, 0, z)) && Kinds.passable(c.world(x, 1, z))) open++;
        }
        assertTrue(open > all * 0.85, "the clearing is open: " + open + "/" + all);
    }

    @Test
    void theSameSeedRaisesTheSameGlade() {
        assertEquals(EveGladePlan.plan(42L).cells(), EveGladePlan.plan(42L).cells());
    }

    @Test
    void theGladeIsAWalkFromTheLanding() {
        for (long seed : SEEDS) {
            List<int[]> cands = EveGladeSite.candidates(seed);
            assertEquals(EveGladeSite.CANDIDATES, cands.size());
            Set<String> distinct = new HashSet<>();
            for (int[] c : cands) {
                double d = Math.sqrt(c[0] * (double) c[0] + c[1] * (double) c[1]);
                assertTrue(d >= EveGladeSite.RING_MIN - 1 && d <= EveGladeSite.RING_MIN + 6 * EveGladeSite.RING_STEP, "a walk: " + d);
                distinct.add(c[0] + "," + c[1]);
            }
            assertEquals(cands.size(), distinct.size(), "no two candidates alike");
            assertEquals(EveGladeSite.candidates(seed).get(3)[0], cands.get(3)[0], "the seed decides");
        }
        // The first wooded and level candidate wins; failing that, the first level one; failing that, the first.
        assertEquals(4, EveGladeSite.choose(i -> i == 4 || i == 7, i -> i >= 2));
        assertEquals(2, EveGladeSite.choose(i -> false, i -> i >= 2));
        assertEquals(0, EveGladeSite.choose(i -> true, i -> false));
        assertTrue(EveGladeSite.level(new int[]{70, 72, 69, 71}, 4));
        assertTrue(!EveGladeSite.level(new int[]{70, 80, 69, 71}, 4));
    }
}
