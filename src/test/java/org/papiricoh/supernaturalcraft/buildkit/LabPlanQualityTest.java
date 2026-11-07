package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;
import org.papiricoh.supernaturalcraft.pact.layout.LabPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crowley's lab (v0.39) against the kit's quality bar ({@link LayoutQuality}) in every turn: the contract's points, the big
 * doors' air doorway, the rite's great circle and both fight spots clear, the water held in its basin, everything reachable
 * from the entry, no block entity (the whole site is the arena), lit inside.
 */
class LabPlanQualityTest {

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    @TestFactory
    List<DynamicTest> everyTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("lab turned " + q, () -> {
                Canvas c = LabPlan.canvas(d);
                LayoutPlan plan = LabPlan.plan(d);
                Map<String, LayoutPoint> pts = plan.points();
                for (String n : LabAnchors.required()) assertTrue(pts.containsKey(n), "missing point " + n);
                int[] a = LabPlan.rotate(LabAnchors.MIN_X, LabAnchors.MIN_Z, d), b = LabPlan.rotate(LabAnchors.MAX_X, LabAnchors.MAX_Z, d);
                Box limit = new Box(Math.min(a[0], b[0]), LabAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]), LabAnchors.TOP_Y,
                        Math.max(a[1], b[1]));
                LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
                LayoutQuality.assertBounds(c, limit);
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertWaterContained(c);
                LayoutQuality.assertNoBlockEntities(c);
                LayoutQuality.assertPalette(c, 30);
                LayoutPoint lo = pts.get(LabAnchors.gateMin(LabAnchors.GATE_DOORS)), hi = pts.get(LabAnchors.gateMax(LabAnchors.GATE_DOORS));
                for (int x = Math.min(lo.x(), hi.x()); x <= Math.max(lo.x(), hi.x()); x++)
                    for (int y = lo.y(); y <= hi.y(); y++)
                        for (int z = Math.min(lo.z(), hi.z()); z <= Math.max(lo.z(), hi.z()); z++)
                            assertTrue(c.isAir(x, y, z), "the doors not air at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                clear(c, pts.get(LabAnchors.HALL), 6, "hall");
                clear(c, pts.get(LabAnchors.DECK), 6, "deck");
                LayoutPoint rite = pts.get(LabAnchors.RITE);
                for (int dx = -4; dx <= 4; dx++)
                    for (int dz = -4; dz <= 4; dz++)
                        assertTrue(c.isAir(rite.x() + dx, rite.y(), rite.z() + dz) && Kinds.fullCube(c.world(rite.x() + dx, rite.y() - 1, rite.z() + dz)),
                                "the great circle's floor at " + dx + "," + dz);
                List<int[]> targets = new ArrayList<>();
                for (String n : LabAnchors.required()) {
                    if (n.startsWith("gate_") || n.startsWith("water_") || n.equals(LabAnchors.DOOR)) continue;
                    LayoutPoint t = pts.get(n);
                    assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                            + " over " + c.world(t.x(), t.y() - 1, t.z()));
                    targets.add(p(t));
                }
                LayoutPoint e = pts.get(LabAnchors.ENTRY);
                LayoutQuality.assertReachable(c, p(e), targets);
                LayoutQuality.assertHeadroom(c, p(e));
                assertEquals(LabPlan.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
            }));
        }
        return out;
    }

    /** Nothing to stand in the way (only air and carpets) for {@code r} round {@code at}, three high. */
    static void clear(Canvas c, LayoutPoint at, int r, String what) {
        for (int x = at.x() - r; x <= at.x() + r; x++)
            for (int z = at.z() - r; z <= at.z() + r; z++)
                for (int y = at.y(); y <= at.y() + 2; y++) {
                    String s = c.world(x, y, z);
                    assertTrue(Kinds.air(s) || St.path(s).endsWith("_carpet"), what + " not clear at " + x + "," + y + "," + z + ": " + s);
                }
    }

    @Test
    void looksThePart() {
        Canvas c = LabPlan.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-13, 0, -11, 13, 11, 11));
    }
}
