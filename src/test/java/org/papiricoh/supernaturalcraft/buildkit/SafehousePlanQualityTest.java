package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehouseAnchors;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehousePlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kelly Kline's safe house (v0.41) against the kit's quality bar ({@link LayoutQuality}) in every turn: the contract's points,
 * the cabin door's air, clear ground round Dagon's post and the rite's 9×9, room over the rift, the cot and Lucifer's place
 * inside, everything reachable from the entry, no block entity (the whole place is the arena), lit inside.
 */
class SafehousePlanQualityTest {

    @TestFactory
    List<DynamicTest> everyTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("safe house turned " + q, () -> {
                Canvas c = SafehousePlan.canvas(d);
                LayoutPlan plan = SafehousePlan.plan(d);
                Map<String, LayoutPoint> pts = plan.points();
                for (String n : SafehouseAnchors.required()) assertTrue(pts.containsKey(n), "missing point " + n);
                int[] a = SafehousePlan.rotate(SafehouseAnchors.MIN_X, SafehouseAnchors.MIN_Z, d), b = SafehousePlan.rotate(SafehouseAnchors.MAX_X, SafehouseAnchors.MAX_Z, d);
                Box limit = new Box(Math.min(a[0], b[0]), SafehouseAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]),
                        SafehouseAnchors.TOP_Y, Math.max(a[1], b[1]));
                LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
                LayoutQuality.assertBounds(c, limit);
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertNoBlockEntities(c);
                LayoutQuality.assertPalette(c, 30);
                LayoutPoint lo = pts.get(SafehouseAnchors.gateMin(SafehouseAnchors.GATE_DOOR)), hi = pts.get(SafehouseAnchors.gateMax(SafehouseAnchors.GATE_DOOR));
                for (int x = Math.min(lo.x(), hi.x()); x <= Math.max(lo.x(), hi.x()); x++)
                    for (int y = lo.y(); y <= hi.y(); y++)
                        for (int z = Math.min(lo.z(), hi.z()); z <= Math.max(lo.z(), hi.z()); z++)
                            assertTrue(c.isAir(x, y, z), "the cabin's door not air at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                BritishCompoundPlanQualityTest.clear(c, pts.get(SafehouseAnchors.GUARD), 2, 3, "Dagon's post");
                BritishCompoundPlanQualityTest.clear(c, pts.get(SafehouseAnchors.RITE), 4, 3, "the rite's circle");
                BritishCompoundPlanQualityTest.clear(c, pts.get(SafehouseAnchors.RIFT), 0, 3, "the rift");
                for (String n : List.of(SafehouseAnchors.COT, SafehouseAnchors.ROURKE)) {
                    LayoutPoint t = pts.get(n);
                    assertTrue(Light.covered(c, t.x(), t.y(), t.z(), 12), n + " is not under the cabin's roof");
                }
                List<int[]> targets = new ArrayList<>();
                for (String n : SafehouseAnchors.required()) {
                    if (n.startsWith("gate_")) continue;
                    LayoutPoint t = pts.get(n);
                    assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                            + " over " + c.world(t.x(), t.y() - 1, t.z()));
                    targets.add(new int[]{t.x(), t.y(), t.z()});
                }
                LayoutPoint e = pts.get(SafehouseAnchors.ENTRY);
                LayoutQuality.assertReachable(c, new int[]{e.x(), e.y(), e.z()}, targets);
                LayoutQuality.assertHeadroom(c, new int[]{e.x(), e.y(), e.z()});
                assertEquals(SafehousePlan.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
            }));
        }
        return out;
    }

    @Test
    void looksThePart() {
        Canvas c = SafehousePlan.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-9, 0, 1, 9, 12, 17));
    }
}
