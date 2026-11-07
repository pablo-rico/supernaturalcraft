package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressAnchors;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressPlan;
import org.papiricoh.supernaturalcraft.hell.rift.RiftShape;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Michael's fortress (v0.41) against the kit's quality bar ({@link LayoutQuality}) in every turn: the contract's points, the
 * gate's air opening, room for the rift in the alcove, his throne's floor, everything reachable from the road, no block entity
 * (the court is the arena), lit where it is roofed, and tall enough to be seen from afar.
 */
class FortressPlanQualityTest {

    static BlockIds ids() {
        return BlockIds.vanilla().allow(FortressPlan.CITADEL, FortressPlan.SCORCHED, FortressPlan.RUBBLE);
    }

    @TestFactory
    List<DynamicTest> everyTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("fortress turned " + q, () -> {
                Canvas c = FortressPlan.canvas(d);
                LayoutPlan plan = FortressPlan.plan(d);
                Map<String, LayoutPoint> pts = plan.points();
                for (String n : FortressAnchors.required()) assertTrue(pts.containsKey(n), "missing point " + n);
                int[] a = FortressPlan.rotate(FortressAnchors.MIN_X, FortressAnchors.MIN_Z, d), b = FortressPlan.rotate(FortressAnchors.MAX_X, FortressAnchors.MAX_Z, d);
                Box limit = new Box(Math.min(a[0], b[0]), FortressAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]),
                        FortressAnchors.TOP_Y, Math.max(a[1], b[1]));
                LayoutQuality.assertIdsKnown(c, ids());
                LayoutQuality.assertBounds(c, limit);
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertNoBlockEntities(c);
                LayoutQuality.assertPalette(c, 12);
                LayoutPoint lo = pts.get(FortressAnchors.gateMin(FortressAnchors.GATE)), hi = pts.get(FortressAnchors.gateMax(FortressAnchors.GATE));
                for (int x = Math.min(lo.x(), hi.x()); x <= Math.max(lo.x(), hi.x()); x++)
                    for (int y = lo.y(); y <= hi.y(); y++)
                        for (int z = Math.min(lo.z(), hi.z()); z <= Math.max(lo.z(), hi.z()); z++)
                            assertTrue(c.isAir(x, y, z), "the gate not air at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                // The rift (3 wide along x in plan space before the turn, 5 high) fits in its alcove whichever way it is turned.
                LayoutPoint rift = pts.get(FortressAnchors.RIFT);
                for (int y = 0; y < RiftShape.HEIGHT; y++)
                    for (int s = -1; s <= 1; s++) {
                        int[] off = Canvas.turn(s, 0, d);
                        assertTrue(c.isAir(rift.x() + off[0], rift.y() + y, rift.z() + off[1]), "no room for the rift at " + s + "," + y);
                    }
                List<int[]> targets = new ArrayList<>();
                for (String n : FortressAnchors.required()) {
                    if (n.startsWith("gate")) continue;
                    LayoutPoint t = pts.get(n);
                    assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                            + " over " + c.world(t.x(), t.y() - 1, t.z()));
                    targets.add(new int[]{t.x(), t.y(), t.z()});
                }
                LayoutPoint e = pts.get(FortressAnchors.ENTRY);
                LayoutQuality.assertReachable(c, new int[]{e.x(), e.y(), e.z()}, targets);
                assertEquals(FortressPlan.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
            }));
        }
        return out;
    }

    @Test
    void looksThePart() {
        Canvas c = FortressPlan.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        assertTrue(c.bounds().y1() >= 40, "the spire stands high over the land: " + c.bounds().y1());
    }
}
