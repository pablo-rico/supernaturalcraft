package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.british.compound.CompoundAnchors;
import org.papiricoh.supernaturalcraft.british.compound.CompoundPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The British Men of Letters' compound (v0.41) against the kit's quality bar ({@link LayoutQuality}) in every turn: the
 * contract's points, the front doorway's air, a clear floor round Hess's place and the rite's 9×9, everything reachable from the
 * entry, no block entity (the whole manor is the arena), lit inside.
 */
class BritishCompoundPlanQualityTest {

    /** Every cell of the (2r+1)² square round {@code p} can be stood in, with {@code up} cells of air over it. */
    static void clear(Canvas c, LayoutPoint p, int r, int up, String what) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int x = p.x() + dx, z = p.z() + dz;
                assertTrue(LayoutQuality.standable(c, x, p.y(), z), what + " not standable at " + dx + "," + dz + ": " + c.world(x, p.y(), z)
                        + " over " + c.world(x, p.y() - 1, z));
                for (int y = 0; y < up; y++) {
                    assertTrue(c.isAir(x, p.y() + y, z), what + " not clear at " + dx + "," + y + "," + dz + ": " + c.world(x, p.y() + y, z));
                }
            }
    }

    @TestFactory
    List<DynamicTest> everyTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("compound turned " + q, () -> {
                Canvas c = CompoundPlan.canvas(d);
                LayoutPlan plan = CompoundPlan.plan(d);
                Map<String, LayoutPoint> pts = plan.points();
                for (String n : CompoundAnchors.required()) assertTrue(pts.containsKey(n), "missing point " + n);
                int[] a = CompoundPlan.rotate(CompoundAnchors.MIN_X, CompoundAnchors.MIN_Z, d), b = CompoundPlan.rotate(CompoundAnchors.MAX_X, CompoundAnchors.MAX_Z, d);
                Box limit = new Box(Math.min(a[0], b[0]), CompoundAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]),
                        CompoundAnchors.TOP_Y, Math.max(a[1], b[1]));
                LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
                LayoutQuality.assertBounds(c, limit);
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertNoBlockEntities(c);
                LayoutQuality.assertPalette(c, 30);
                LayoutPoint lo = pts.get(CompoundAnchors.gateMin(CompoundAnchors.GATE_FRONT)), hi = pts.get(CompoundAnchors.gateMax(CompoundAnchors.GATE_FRONT));
                for (int x = Math.min(lo.x(), hi.x()); x <= Math.max(lo.x(), hi.x()); x++)
                    for (int y = lo.y(); y <= hi.y(); y++)
                        for (int z = Math.min(lo.z(), hi.z()); z <= Math.max(lo.z(), hi.z()); z++)
                            assertTrue(c.isAir(x, y, z), "the front door not air at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                clear(c, pts.get(CompoundAnchors.HESS), 2, 3, "Hess's place");
                clear(c, pts.get(CompoundAnchors.RITE), 4, 3, "the rite's circle");
                List<int[]> targets = new ArrayList<>();
                for (String n : CompoundAnchors.required()) {
                    if (n.startsWith("gate_")) continue;
                    LayoutPoint t = pts.get(n);
                    assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                            + " over " + c.world(t.x(), t.y() - 1, t.z()));
                    targets.add(new int[]{t.x(), t.y(), t.z()});
                }
                LayoutPoint e = pts.get(CompoundAnchors.ENTRY);
                LayoutQuality.assertReachable(c, new int[]{e.x(), e.y(), e.z()}, targets);
                LayoutQuality.assertHeadroom(c, new int[]{e.x(), e.y(), e.z()});
                assertEquals(CompoundPlan.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
            }));
        }
        return out;
    }

    @Test
    void looksThePart() {
        Canvas c = CompoundPlan.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-19, 0, -11, 19, 8, 13));
    }
}
