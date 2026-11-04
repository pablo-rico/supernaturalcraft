package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crowley's asylum (v0.35) against the kit's quality bar ({@link LayoutQuality}) in every turn: the contract's points
 * ({@link AsylumAnchors#required()}), the gates' air doorways, its rooms clear for the fight (9×9 round the hall and the cells,
 * 13×13 round the throne room's point), the rite's ring of floor, everything reachable from the entry through the open gates,
 * no block entity (the whole building is the arena), lit interiors. Look at the structview renders too ({@code LayoutDumpTest}).
 */
class AsylumPlanQualityTest {

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    @TestFactory
    List<DynamicTest> everyTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("asylum turned " + q, () -> {
                Canvas c = AsylumPlan.canvas(d);
                LayoutPlan plan = AsylumPlan.plan(d);
                Map<String, LayoutPoint> pts = plan.points();
                for (String n : AsylumAnchors.required()) assertTrue(pts.containsKey(n), "missing point " + n);
                int[] a = AsylumPlan.rotate(AsylumAnchors.MIN_X, AsylumAnchors.MIN_Z, d), b = AsylumPlan.rotate(AsylumAnchors.MAX_X, AsylumAnchors.MAX_Z, d);
                Box limit = new Box(Math.min(a[0], b[0]), AsylumAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]), AsylumAnchors.TOP_Y,
                        Math.max(a[1], b[1]));
                LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
                LayoutQuality.assertBounds(c, limit);
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertWaterContained(c);
                LayoutQuality.assertNoBlockEntities(c);
                LayoutQuality.assertPalette(c, 40);
                for (String g : AsylumAnchors.GATES) {
                    LayoutPoint lo = pts.get(AsylumAnchors.gateMin(g)), hi = pts.get(AsylumAnchors.gateMax(g));
                    for (int x = Math.min(lo.x(), hi.x()); x <= Math.max(lo.x(), hi.x()); x++)
                        for (int y = lo.y(); y <= hi.y(); y++)
                            for (int z = Math.min(lo.z(), hi.z()); z <= Math.max(lo.z(), hi.z()); z++)
                                assertTrue(c.isAir(x, y, z), g + " not air at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                }
                clear(c, pts.get(AsylumAnchors.HALL), 4, "hall");
                clear(c, pts.get(AsylumAnchors.CELLS), 4, "cells");
                clear(c, pts.get(AsylumAnchors.THRONE), 6, "throne");
                LayoutPoint rite = pts.get(AsylumAnchors.RITE);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        assertTrue(c.isAir(rite.x() + dx, rite.y(), rite.z() + dz) && Kinds.fullCube(c.world(rite.x() + dx, rite.y() - 1, rite.z() + dz)),
                                "rite ring at " + dx + "," + dz);
                List<int[]> targets = new ArrayList<>();
                for (String n : AsylumAnchors.required()) {
                    if (n.startsWith("gate_") || n.equals(AsylumAnchors.THRONE_SEAT)) continue;
                    LayoutPoint t = pts.get(n);
                    assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                            + " over " + c.world(t.x(), t.y() - 1, t.z()));
                    targets.add(p(t));
                }
                targets.add(p(pts.get(AsylumAnchors.THRONE_SEAT)));
                LayoutPoint e = pts.get(AsylumAnchors.ENTRY);
                LayoutQuality.assertReachable(c, p(e), targets);
                LayoutQuality.assertHeadroom(c, p(e));
                assertEquals(AsylumPlan.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
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
    void looksTheParts() {
        Canvas c = AsylumPlan.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-26, 0, -21, 26, 20, 20));
    }
}
