package org.papiricoh.supernaturalcraft.structure.enochian;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.buildkit.BlockIds;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.LayoutQuality;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The buried Enochian crypt (v0.36) against the kit's quality bar ({@link LayoutQuality}) for several seeds, and against its
 * contract ({@link EnochianCryptAnchors}): plates, doors, murals, pedestal, chests and ceiling traps on the right blocks, the
 * doors really sealing the way on (and everything reachable once they are open), the same plan for the same seed. Look at
 * the structview renders too ({@link EnochianCryptCatalog}).
 */
class EnochianCryptPlanTest {

    private static final long[] SEEDS = {0L, 1L, 7L, 42L, -99L, 1234567L, Long.MIN_VALUE};
    private static final BlockIds IDS = BlockIds.vanilla().allow(EnochianCryptPlan.MOD_IDS.toArray(String[]::new));
    /** Every standable interior cell is at least this bright (a dim crypt, but nothing spawns in it). */
    private static final int MIN_LIGHT = 5;

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    @TestFactory
    List<DynamicTest> everySeedMeetsTheStandard() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) out.add(DynamicTest.dynamicTest("seed " + seed, () -> check(seed)));
        return out;
    }

    private static void check(long seed) {
        EnochianCryptPlan plan = EnochianCryptPlan.of(seed);
        Canvas c = plan.canvas();
        EnochianCryptAnchors a = plan.anchors();
        LayoutQuality.assertIdsKnown(c, IDS);
        LayoutQuality.assertBounds(c, new Box(0, EnochianCryptAnchors.BOTTOM_Y, 0, EnochianCryptAnchors.SIZE_X - 1, EnochianCryptAnchors.TOP_Y,
                EnochianCryptAnchors.SIZE_Z - 1));
        LayoutQuality.assertBudget(c, 60_000);
        LayoutQuality.assertShapes(c);
        // The sealed door's id ends in "_door", which the support check reads as a two-high door: it is a solid block.
        Canvas solid = c.copy("solid");
        for (java.util.Map.Entry<Long, String> e : c.map().entrySet())
            if (e.getValue().equals(EnochianCryptPlan.DOOR)) solid.set(Canvas.kx(e.getKey()), Canvas.ky(e.getKey()), Canvas.kz(e.getKey()), EnochianCryptPlan.STONE);
        LayoutQuality.assertSupported(solid);
        LayoutQuality.assertWaterContained(c);
        LayoutQuality.assertPalette(c, 28);
        LayoutQuality.assertNoDarkInteriors(c, MIN_LIGHT);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(0, EnochianCryptAnchors.GROUND_Y, 0, 12, EnochianCryptAnchors.TOP_Y, 16));

        // The contract's blocks.
        for (int i = 0; i < EnochianCryptAnchors.DOORS; i++) {
            EnochianCryptAnchors.Plate pl = a.plate(i);
            String s = c.get(pl.pos().x(), pl.pos().y(), pl.pos().z());
            assertEquals(EnochianCryptPlan.PLATE_ID, St.id(s), "plate " + i);
            assertEquals(a.doorSigil(i), St.get(s, "sigil"));
            assertEquals(pl.sigil(), St.get(s, "sigil"));
            assertEquals(pl.facing(), St.get(s, "facing"));
            assertEquals("false", St.get(s, "solved"));
            LayoutPoint lo = a.doorMin(i), hi = a.doorMax(i);
            int n = 0;
            for (int x = lo.x(); x <= hi.x(); x++)
                for (int y = lo.y(); y <= hi.y(); y++)
                    for (int z = lo.z(); z <= hi.z(); z++) {
                        assertEquals(EnochianCryptPlan.DOOR, c.get(x, y, z), "door " + i + " at " + x + "," + y + "," + z);
                        n++;
                    }
            assertEquals(9, n, "door " + i + " is 3×3");
            // The plate is in the door's wall, beside the door, its face toward open air.
            assertEquals(lo.z(), pl.pos().z());
            int fx = pl.pos().x(), fz = pl.pos().z() + (pl.facing().equals("south") ? 1 : -1);
            assertTrue(c.isAir(fx, pl.pos().y(), fz) || !Kinds.opaqueCube(c.get(fx, pl.pos().y(), fz)), "plate " + i + " faces a wall");
        }
        assertEquals(List.of("banishing", "ward", "alarm"), List.of(a.doorSigil(0), a.doorSigil(1), a.doorSigil(2)));
        assertEquals(EnochianCryptPlan.PEDESTAL, c.get(a.pedestal().x(), a.pedestal().y(), a.pedestal().z()));
        assertEquals(2, a.chests().size());
        for (LayoutPoint ch : a.chests()) assertEquals("chest", St.path(c.get(ch.x(), ch.y(), ch.z())));
        int murals = a.murals().size();
        assertTrue(murals >= 8 && murals <= 12, murals + " murals");
        Set<Integer> variants = new HashSet<>();
        for (EnochianCryptAnchors.Mural m : a.murals()) {
            String s = c.get(m.pos().x(), m.pos().y(), m.pos().z());
            assertEquals(EnochianCryptPlan.MURAL_ID, St.id(s));
            assertEquals(m.facing(), St.get(s, "facing"));
            assertEquals(String.valueOf(m.variant()), St.get(s, "variant"));
            variants.add(m.variant());
        }
        assertEquals(Set.of(0, 1, 2, 3), variants);
        assertEquals(3, a.ceilingTrapCentres().size());
        for (LayoutPoint t : a.ceilingTrapCentres()) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    String s = c.get(t.x() + dx, t.y(), t.z() + dz);
                    assertEquals(EnochianCryptPlan.TRAP_ID, St.id(s));
                    assertEquals("ceiling", St.get(s, "face"));
                    assertEquals(String.valueOf((dz + 1) * 3 + dx + 1), St.get(s, "part"));
                    assertTrue(Kinds.opaqueCube(c.world(t.x() + dx, t.y() + 1, t.z() + dz)), "trap under the ceiling");
                    // Over the walkway: there is a floor under it within reach.
                    assertTrue(LayoutQuality.standable(c, t.x() + dx, 0, t.z() + dz), "walkway under the trap");
                }
        }
        int guards = a.guardianSpots().size();
        assertTrue(guards >= 4 && guards <= 6, guards + " guardians");

        // Sealed: from the surface one cannot get past the first door.
        int[] entry = p(a.entrance());
        Set<Long> closed = LayoutQuality.walk(c, entry);
        assertFalse(closed.contains(Canvas.key(a.pedestal().x() - 1, 0, a.pedestal().z())), "the vault is open with the doors shut");
        for (LayoutPoint g : a.guardianSpots()) assertTrue(closed.contains(Canvas.key(g.x(), g.y(), g.z())), "guardian " + g + " not before the doors");

        // Opened: everything within reach.
        Canvas open = c.copy("open");
        for (int i = 0; i < EnochianCryptAnchors.DOORS; i++)
            for (int x = a.doorMin(i).x(); x <= a.doorMax(i).x(); x++)
                for (int y = a.doorMin(i).y(); y <= a.doorMax(i).y(); y++) open.carve(x, y, a.doorMin(i).z());
        LayoutQuality.assertHeadroom(open, entry);
        List<int[]> targets = new ArrayList<>();
        targets.add(p(a.pedestal()));
        for (LayoutPoint ch : a.chests()) targets.add(p(ch));
        for (LayoutPoint g : a.guardianSpots()) {
            assertTrue(LayoutQuality.standable(c, g.x(), g.y(), g.z()), "guardian " + g);
            targets.add(p(g));
        }
        for (int i = 0; i < EnochianCryptAnchors.DOORS; i++) targets.add(p(a.plate(i).pos().offset(0, -1, 0)));
        // A mural is seen from the floor in front of it.
        for (EnochianCryptAnchors.Mural m : a.murals()) {
            org.papiricoh.supernaturalcraft.buildkit.Dir f = org.papiricoh.supernaturalcraft.buildkit.Dir.of(m.facing());
            targets.add(new int[]{m.pos().x() + f.dx, 0, m.pos().z() + f.dz});
        }
        LayoutQuality.assertReachable(open, entry, targets);
        assertTrue(LayoutQuality.standable(c, entry[0], entry[1], entry[2]));
        assertEquals(EnochianCryptAnchors.GROUND_Y, entry[1]);
    }

    @Test
    void theSameSeedGivesTheSamePlanAndSeedsVary() {
        assertEquals(EnochianCryptPlan.of(42).canvas().map(), EnochianCryptPlan.of(42).canvas().map());
        assertEquals(EnochianCryptPlan.of(42).anchors().murals(), EnochianCryptPlan.of(42).anchors().murals());
        assertNotEquals(EnochianCryptPlan.of(1).canvas().map(), EnochianCryptPlan.of(2).canvas().map());
        // The plan as the contract carries the same cells.
        EnochianCryptPlan p = EnochianCryptPlan.of(5);
        assertEquals(p.canvas().map(), Canvas.fromPlan("x", p.plan()).map());
    }

    @Test
    void catalogListsTheCrypt() {
        assertEquals(2, new EnochianCryptCatalog().layouts().size());
    }
}
