package org.papiricoh.supernaturalcraft.hands;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.buildkit.BlockIds;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.LayoutQuality;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryAnchors;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryPlans;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five reliquaries of the Hands of God (v0.42) against the kit's quality bar ({@link LayoutQuality}) in every turn: the
 * contract's points, the altar's cell open over a floor, the altar and every chest reachable from the way in, no block entity
 * (the builder puts the altar and the chests), lit inside, no bland surfaces, water held in.
 */
class ReliquaryPlanQualityTest {

    static BlockIds ids() {
        List<String> mod = new ArrayList<>();
        for (List<String> l : S11Assets.RELIQUARY_BLOCKS.values()) for (String id : l) mod.add("supernaturalcraft:" + id);
        return BlockIds.vanilla().allow(mod.toArray(String[]::new));
    }

    @TestFactory
    List<DynamicTest> everyKindAndTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (S11Assets.Hand kind : S11Assets.Hand.values()) {
            for (int q = 0; q < 4; q++) {
                final int d = q;
                out.add(DynamicTest.dynamicTest(kind.reliquary + " turned " + q, () -> check(kind, d)));
            }
            out.add(DynamicTest.dynamicTest(kind.reliquary + " looks the part", () -> {
                Canvas c = ReliquaryPlans.canvas(kind, 0);
                LayoutQuality.assertNoDarkInteriors(c, 1);
                LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
                LayoutQuality.assertPalette(c, 12);
            }));
        }
        return out;
    }

    /** Every passable cell reached from {@code from} through passable cells (the canvas's bounds, grown by two). */
    static java.util.Set<Long> swim(Canvas c, LayoutPoint from) {
        Box b = c.bounds().grow(2);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        q.add(new int[]{from.x(), from.y(), from.z()});
        seen.add(Canvas.key(from.x(), from.y(), from.z()));
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int[] d : dirs) {
                int x = p[0] + d[0], y = p[1] + d[1], z = p[2] + d[2];
                if (!b.contains(x, y, z) || !Kinds.passable(c.world(x, y, z))) continue;
                if (seen.add(Canvas.key(x, y, z))) q.add(new int[]{x, y, z});
            }
        }
        return seen;
    }

    static void check(S11Assets.Hand kind, int q) {
        Canvas c = ReliquaryPlans.canvas(kind, q);
        LayoutPlan plan = ReliquaryPlans.plan(kind, q);
        Map<String, LayoutPoint> pts = plan.points();
        for (String n : ReliquaryAnchors.required(kind)) assertTrue(pts.containsKey(n), "missing point " + n);
        ReliquaryAnchors.Bounds b = ReliquaryAnchors.bounds(kind);
        int[] a = ReliquaryPlans.rotate(b.minX(), b.minZ(), q), z = ReliquaryPlans.rotate(b.maxX(), b.maxZ(), q);
        Box limit = new Box(Math.min(a[0], z[0]), b.bottomY(), Math.min(a[1], z[1]), Math.max(a[0], z[0]), b.topY(), Math.max(a[1], z[1]));
        LayoutQuality.assertIdsKnown(c, ids());
        LayoutQuality.assertBounds(c, limit);
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertNoBlockEntities(c);
        LayoutQuality.assertWaterContained(c);
        LayoutPoint altar = pts.get(ReliquaryAnchors.ALTAR);
        assertTrue(Kinds.passable(c.world(altar.x(), altar.y(), altar.z())), "the altar's cell is open: " + c.world(altar.x(), altar.y(), altar.z()));
        assertTrue(!Kinds.passable(c.world(altar.x(), altar.y() - 1, altar.z())), "the altar stands on something");
        if (kind == S11Assets.Hand.DELPHINES_HAND) {
            // The wreck is swum, not walked: every point is reached through open water from the way in.
            LayoutPoint e = pts.get(ReliquaryAnchors.ENTRY);
            java.util.Set<Long> wet = swim(c, e);
            for (String n : ReliquaryAnchors.required(kind)) {
                LayoutPoint t = pts.get(n);
                assertTrue(wet.contains(Canvas.key(t.x(), t.y(), t.z())), n + " cannot be swum to at " + t);
            }
            assertEquals(ReliquaryPlans.canvas(kind, 0).map().size(), c.map().size(), "turn " + q + " lost or gained blocks");
            return;
        }
        List<int[]> targets = new ArrayList<>();
        for (String n : ReliquaryAnchors.required(kind)) {
            if (n.equals(ReliquaryAnchors.ALTAR) || n.startsWith(ReliquaryAnchors.CHEST_PREFIX)) continue;
            LayoutPoint t = pts.get(n);
            assertTrue(LayoutQuality.standable(c, t.x(), t.y(), t.z()), n + " not standable at " + t + ": " + c.world(t.x(), t.y(), t.z())
                    + " over " + c.world(t.x(), t.y() - 1, t.z()));
            targets.add(new int[]{t.x(), t.y(), t.z()});
        }
        for (int n = 0; n < ReliquaryAnchors.chests(kind); n++) {
            LayoutPoint t = pts.get(ReliquaryAnchors.chest(n));
            assertTrue(Kinds.passable(c.world(t.x(), t.y(), t.z())), "chest " + n + " cell open");
            assertTrue(!Kinds.passable(c.world(t.x(), t.y() - 1, t.z())), "chest " + n + " stands on something");
        }
        LayoutPoint e = pts.get(ReliquaryAnchors.ENTRY);
        LayoutQuality.assertReachable(c, new int[]{e.x(), e.y(), e.z()}, targets);
        LayoutQuality.assertHeadroom(c, new int[]{e.x(), e.y(), e.z()});
        assertEquals(ReliquaryPlans.canvas(kind, 0).map().size(), c.map().size(), "turn " + q + " lost or gained blocks");
    }
}
