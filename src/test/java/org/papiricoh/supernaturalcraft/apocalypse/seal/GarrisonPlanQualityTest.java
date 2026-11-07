package org.papiricoh.supernaturalcraft.apocalypse.seal;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.seal.garrison.GarrisonPlan;
import org.papiricoh.supernaturalcraft.buildkit.BlockIds;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.LayoutQuality;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uriel's garrison (v0.40) against the kit's quality bar ({@link LayoutQuality}) in every turn: the contract's points, the gate's
 * air doorway, the yard and Uriel's spot clear for the fight, everything reachable from the entry, no block entity (the whole
 * site is the arena), the chapel lit inside.
 */
class GarrisonPlanQualityTest {

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    @TestFactory
    List<DynamicTest> everyTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("garrison turned " + q, () -> {
                Canvas c = GarrisonPlan.canvas(d);
                LayoutPlan plan = GarrisonPlan.plan(d);
                Map<String, LayoutPoint> pts = plan.points();
                for (String n : GarrisonAnchors.required()) assertTrue(pts.containsKey(n), "missing point " + n);
                int[] a = GarrisonPlan.rotate(GarrisonAnchors.MIN_X, GarrisonAnchors.MIN_Z, d), b = GarrisonPlan.rotate(GarrisonAnchors.MAX_X, GarrisonAnchors.MAX_Z, d);
                Box limit = new Box(Math.min(a[0], b[0]), GarrisonAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]), GarrisonAnchors.TOP_Y,
                        Math.max(a[1], b[1]));
                LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
                LayoutQuality.assertBounds(c, limit);
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertWaterContained(c);
                LayoutQuality.assertNoBlockEntities(c);
                LayoutQuality.assertPalette(c, 25);
                LayoutPoint lo = pts.get(GarrisonAnchors.gateMin(GarrisonAnchors.GATE)), hi = pts.get(GarrisonAnchors.gateMax(GarrisonAnchors.GATE));
                for (int x = Math.min(lo.x(), hi.x()); x <= Math.max(lo.x(), hi.x()); x++)
                    for (int y = lo.y(); y <= hi.y(); y++)
                        for (int z = Math.min(lo.z(), hi.z()); z <= Math.max(lo.z(), hi.z()); z++)
                            assertTrue(c.isAir(x, y, z), "the gate not air at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                clear(c, pts.get(GarrisonAnchors.YARD), 6, "yard");
                clear(c, pts.get(GarrisonAnchors.URIEL), 5, "uriel");
                List<int[]> targets = new ArrayList<>();
                for (String n : GarrisonAnchors.required()) {
                    if (n.startsWith("gate_")) continue;
                    LayoutPoint t = pts.get(n);
                    assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                            + " over " + c.world(t.x(), t.y() - 1, t.z()));
                    targets.add(p(t));
                }
                LayoutPoint e = pts.get(GarrisonAnchors.ENTRY);
                LayoutQuality.assertReachable(c, p(e), targets);
                LayoutQuality.assertHeadroom(c, p(e));
                assertEquals(GarrisonPlan.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
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
        Canvas c = GarrisonPlan.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-8, 0, 6, 8, 9, 15));
    }
}
