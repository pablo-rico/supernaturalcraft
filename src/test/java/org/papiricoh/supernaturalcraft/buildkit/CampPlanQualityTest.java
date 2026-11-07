package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampAnchors;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Michael's outposts (v0.41), the prisoner camp and the checkpoint, against the kit's quality bar ({@link LayoutQuality}) in every
 * turn: the contract's points, every guard post and cage reachable from the entry, each cage's lock in its front with its prisoner
 * shut in behind it, no block entity, lit inside.
 */
class CampPlanQualityTest {

    static BlockIds ids() {
        return BlockIds.vanilla().allow(CampPlan.SOIL, CampPlan.ASH, CampPlan.RUBBLE, CampPlan.PLANKS, CampPlan.LOCK, CampPlan.STONE,
                CampPlan.BRICKS, "supernaturalcraft:burned_log", "supernaturalcraft:wrecked_metal");
    }

    @TestFactory
    List<DynamicTest> everyKindAndTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (CampPlan.Kind kind : CampPlan.Kind.values()) {
            for (int q = 0; q < 4; q++) {
                final int d = q;
                out.add(DynamicTest.dynamicTest(kind + " turned " + q, () -> {
                    Canvas c = CampPlan.canvas(kind, d);
                    LayoutPlan plan = CampPlan.plan(kind, d);
                    Map<String, LayoutPoint> pts = plan.points();
                    for (String n : CampAnchors.required(kind)) assertTrue(pts.containsKey(n), "missing point " + n);
                    int[] a = CampPlan.rotate(CampAnchors.MIN_X, CampAnchors.MIN_Z, d), b = CampPlan.rotate(CampAnchors.MAX_X, CampAnchors.MAX_Z, d);
                    Box limit = new Box(Math.min(a[0], b[0]), CampAnchors.BOTTOM_Y, Math.min(a[1], b[1]), Math.max(a[0], b[0]),
                            CampAnchors.TOP_Y, Math.max(a[1], b[1]));
                    LayoutQuality.assertIdsKnown(c, ids());
                    LayoutQuality.assertBounds(c, limit);
                    LayoutQuality.assertShapes(c);
                    LayoutQuality.assertSupported(c);
                    LayoutQuality.assertNoBlockEntities(c);
                    LayoutQuality.assertPalette(c, 10);
                    List<int[]> targets = new ArrayList<>();
                    for (Map.Entry<String, LayoutPoint> e : pts.entrySet()) {
                        LayoutPoint t = e.getValue();
                        if (e.getKey().startsWith(CampAnchors.LOCK_PREFIX)) {
                            assertEquals(CampPlan.LOCK, c.world(t.x(), t.y(), t.z()), e.getKey() + " is a lock");
                            targets.add(new int[]{t.x(), t.y(), t.z()});
                            continue;
                        }
                        assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), e.getKey() + " not standable at " + t);
                        if (!e.getKey().startsWith(CampAnchors.PRISONER_PREFIX)) targets.add(new int[]{t.x(), t.y(), t.z()});
                    }
                    LayoutPoint entry = pts.get(CampAnchors.ENTRY);
                    Set<Long> reach = LayoutQuality.assertReachable(c, new int[]{entry.x(), entry.y(), entry.z()}, targets);
                    // A prisoner is shut in: nobody walks into a cage until its lock is broken.
                    for (int n = 0; kind == CampPlan.Kind.CAMP && n < CampAnchors.CAGES; n++) {
                        LayoutPoint p = pts.get(CampAnchors.prisoner(n));
                        assertTrue(!reach.contains(Canvas.key(p.x(), p.y(), p.z())), "prisoner " + n + " can walk out");
                    }
                    assertEquals(CampPlan.canvas(kind, 0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
                }));
            }
        }
        return out;
    }

    @Test
    void litInsideAndBothKindsTurnUp() {
        for (CampPlan.Kind kind : CampPlan.Kind.values()) LayoutQuality.assertNoDarkInteriors(CampPlan.canvas(kind, 0), 1);
        Set<CampPlan.Kind> seen = EnumSet.noneOf(CampPlan.Kind.class);
        for (long seed = 0; seed < 64; seed++) seen.add(CampPlan.kindOf(seed * 0x9E3779B97F4A7C15L));
        assertEquals(EnumSet.allOf(CampPlan.Kind.class), seen);
    }
}
