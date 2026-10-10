package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.buildkit.demo.KitDemoCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The kit's demo builds pass every {@link LayoutQuality} check: the template of a layout test.
 */
class KitDemoTest {

    @TestFactory
    List<DynamicTest> demosAreSound() {
        List<DynamicTest> out = new ArrayList<>();
        BlockIds ids = BlockIds.vanilla();
        for (Map.Entry<String, Supplier<Canvas>> e : KitDemoCatalog.canvases().entrySet()) {
            Canvas c = e.getValue().get();
            String n = e.getKey();
            out.add(DynamicTest.dynamicTest(n + " ids", () -> LayoutQuality.assertIdsKnown(c, ids)));
            out.add(DynamicTest.dynamicTest(n + " bounds and budget", () -> {
                LayoutQuality.assertBounds(c, new Box(-40, -40, -40, 40, 40, 40));
                LayoutQuality.assertBudget(c, 120_000);
            }));
            out.add(DynamicTest.dynamicTest(n + " shapes", () -> LayoutQuality.assertShapes(c)));
            out.add(DynamicTest.dynamicTest(n + " supported", () -> LayoutQuality.assertSupported(c)));
            out.add(DynamicTest.dynamicTest(n + " headroom", () -> LayoutQuality.assertHeadroom(c, c.anchor("entry"))));
            out.add(DynamicTest.dynamicTest(n + " reachable", () -> {
                LayoutQuality.assertReachable(c, c.anchor("entry"), c.anchors().values());
            }));
            out.add(DynamicTest.dynamicTest(n + " lit inside", () -> LayoutQuality.assertNoDarkInteriors(c, 7)));
            out.add(DynamicTest.dynamicTest(n + " palette", () -> LayoutQuality.assertPalette(c, 45)));
            out.add(DynamicTest.dynamicTest(n + " surfaces", () -> LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL)));
            out.add(DynamicTest.dynamicTest(n + " depth", () -> {
                for (LayoutZone z : c.zones()) LayoutQuality.assertDepth(c, z.bounds());
            }));
            out.add(DynamicTest.dynamicTest(n + " water", () -> LayoutQuality.assertWaterContained(c)));
            out.add(DynamicTest.dynamicTest(n + " as a plan", () -> {
                LayoutPlan plan = c.toPlan();
                assertEquals(c.size(), plan.size(), "every cell in exactly one zone");
                Canvas back = Canvas.fromPlan(n, plan);
                assertEquals(c.map(), back.map(), "a plan rebuilds the same canvas");
                assertEquals(c.anchors().keySet(), plan.points().keySet());
                assertTrue(plan.zones().get(0).name().equals(Canvas.BASE_ZONE), "terrain is written first");
            }));
        }
        return out;
    }
}
