package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseAnchors;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehousePlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Alastair's warehouse (v0.40) against the kit's quality bar ({@link LayoutQuality}) in every turn: the contract's points, the
 * loading doors' air doorway, the trap's floor clear and solid, everything reachable from the entry, no block entity (the whole
 * shed is the arena), lit inside.
 */
class WarehousePlanQualityTest {

    @TestFactory
    List<DynamicTest> everyTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("warehouse turned " + q, () -> {
                Canvas c = WarehousePlan.canvas(d);
                LayoutPlan plan = WarehousePlan.plan(d);
                Map<String, LayoutPoint> pts = plan.points();
                for (String n : WarehouseAnchors.required()) assertTrue(pts.containsKey(n), "missing point " + n);
                int[] a = WarehousePlan.rotate(WarehouseAnchors.MIN_X, WarehouseAnchors.MIN_Z, d), b = WarehousePlan.rotate(WarehouseAnchors.MAX_X, WarehouseAnchors.MAX_Z, d);
                Box limit = new Box(Math.min(a[0], b[0]), WarehouseAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]),
                        WarehouseAnchors.TOP_Y, Math.max(a[1], b[1]));
                LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
                LayoutQuality.assertBounds(c, limit);
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertNoBlockEntities(c);
                LayoutQuality.assertPalette(c, 30);
                LayoutPoint lo = pts.get(WarehouseAnchors.gateMin(WarehouseAnchors.GATE_DOORS)), hi = pts.get(WarehouseAnchors.gateMax(WarehouseAnchors.GATE_DOORS));
                for (int x = Math.min(lo.x(), hi.x()); x <= Math.max(lo.x(), hi.x()); x++)
                    for (int y = lo.y(); y <= hi.y(); y++)
                        for (int z = Math.min(lo.z(), hi.z()); z <= Math.max(lo.z(), hi.z()); z++)
                            assertTrue(c.isAir(x, y, z), "the doors not air at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                LayoutPoint trap = pts.get(WarehouseAnchors.TRAP);
                for (int dx = -4; dx <= 4; dx++)
                    for (int dz = -4; dz <= 4; dz++) {
                        assertTrue(c.isAir(trap.x() + dx, trap.y(), trap.z() + dz) && Kinds.fullCube(c.world(trap.x() + dx, trap.y() - 1, trap.z() + dz)),
                                "the trap's floor at " + dx + "," + dz);
                        for (int y = 1; y <= 3; y++) assertTrue(c.isAir(trap.x() + dx, trap.y() + y, trap.z() + dz), "the trap's room at " + dx + "," + y + "," + dz);
                    }
                List<int[]> targets = new ArrayList<>();
                for (String n : WarehouseAnchors.required()) {
                    if (n.startsWith("gate_")) continue;
                    LayoutPoint t = pts.get(n);
                    assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                            + " over " + c.world(t.x(), t.y() - 1, t.z()));
                    targets.add(new int[]{t.x(), t.y(), t.z()});
                }
                LayoutPoint e = pts.get(WarehouseAnchors.ENTRY);
                LayoutQuality.assertReachable(c, new int[]{e.x(), e.y(), e.z()}, targets);
                LayoutQuality.assertHeadroom(c, new int[]{e.x(), e.y(), e.z()});
                assertEquals(WarehousePlan.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
            }));
        }
        return out;
    }

    @Test
    void looksThePart() {
        Canvas c = WarehousePlan.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-17, 0, -13, 17, 10, 13));
    }
}
