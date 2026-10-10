package org.papiricoh.supernaturalcraft.heaven.plot;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.buildkit.BlockIds;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.LayoutQuality;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.RoadhouseLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A hunter's Heaven in each style, and the Roadhouse: the contract points and the kit's quality bar. */
class HeavenLayoutsTest {

    static final BlockIds IDS = BlockIds.vanilla().allow("supernaturalcraft:memory_veil", "supernaturalcraft:hearth",
            "supernaturalcraft:celestial_seal", "supernaturalcraft:cloud_stone", "supernaturalcraft:cloud_bricks", "supernaturalcraft:heaven_gate");

    private static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    private static void air(Canvas c, int x, int y, int z, String what) {
        assertTrue(c.isAir(x, y, z), what + " at " + x + "," + y + "," + z + " is " + c.world(x, y, z));
    }

    @TestFactory
    List<DynamicTest> plots() {
        List<DynamicTest> out = new ArrayList<>();
        for (HeavenPlotLayout.Style style : HeavenPlotLayout.Style.values()) {
            Canvas c = HeavenPlotLayout.canvas(7L, style);
            String n = style.name();
            out.add(DynamicTest.dynamicTest(n + " ids, bounds, budget", () -> {
                LayoutQuality.assertIdsKnown(c, IDS);
                int r = HeavenPlotLayout.RADIUS;
                LayoutQuality.assertBounds(c, new Box(-r, -60, -r, r, 40, r));
                LayoutQuality.assertBudget(c, 450_000);
                LayoutQuality.assertPalette(c, 50);
            }));
            out.add(DynamicTest.dynamicTest(n + " shapes and support", () -> {
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertWaterContained(c);
            }));
            out.add(DynamicTest.dynamicTest(n + " gates and seals are left open", () -> {
                for (int x = -1; x <= 1; x++) for (int y = 0; y <= 3; y++) air(c, x, y, HeavenPlotLayout.EXIT_GATE.z(), "exit gate");
                for (LayoutPoint s : HeavenPlotLayout.SHRINES) for (int y = 0; y <= 1; y++) air(c, s.x(), y, s.z(), "shrine veil");
                for (LayoutPoint d : List.of(HeavenPlotLayout.HOME_DOOR, HeavenPlotLayout.ROAD_DOOR, HeavenPlotLayout.WING_DOOR)) {
                    for (int y = 0; y <= 1; y++) air(c, d.x(), y, d.z(), "door");
                }
                assertEquals("supernaturalcraft:hearth", St.id(c.get(HeavenPlotLayout.HEARTH.x(), 0, HeavenPlotLayout.HEARTH.z())));
                for (LayoutPoint s : HeavenPlotLayout.STORAGE) assertEquals("minecraft:chest", St.id(c.get(s.x(), s.y(), s.z())));
                for (LayoutPoint t : HeavenPlotLayout.TROPHIES) {
                    assertTrue(Kinds.holdsAbove(c.world(t.x(), t.y(), t.z())), "a pedestal at " + t);
                    air(c, t.x(), t.y() + 1, t.z(), "room for a bust");
                }
            }));
            out.add(DynamicTest.dynamicTest(n + " walkable from the landing", () -> {
                List<int[]> targets = new ArrayList<>();
                for (LayoutPoint s : HeavenPlotLayout.SHRINES) targets.add(p(s));
                targets.add(p(HeavenPlotLayout.HOME_DOOR));
                targets.add(p(HeavenPlotLayout.HEARTH));
                targets.add(p(HeavenPlotLayout.ROAD_DOOR));
                targets.add(p(HeavenPlotLayout.WING_DOOR));
                targets.add(p(HeavenPlotLayout.STAGE_ENTRY));
                targets.add(p(HeavenPlotLayout.STAGE_CENTER));
                targets.add(p(HeavenPlotLayout.EXIT_GATE.offset(0, 0, -1)));
                for (LayoutPoint t : HeavenPlotLayout.TROPHIES) targets.add(p(t.offset(0, 0, 1)));
                LayoutQuality.assertReachable(c, p(HeavenPlotLayout.LANDING), targets);
                LayoutQuality.assertHeadroom(c, p(HeavenPlotLayout.LANDING));
            }));
            out.add(DynamicTest.dynamicTest(n + " the house is lit and varied", () -> {
                Canvas house = c.copy("house");
                LayoutQuality.assertNoDarkInteriors(house, 7);
                LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
                LayoutQuality.assertDepth(c, new Box(-15, 0, -56, 15, 12, -26));
            }));
            out.add(DynamicTest.dynamicTest(n + " the stage is clear and the room's ground is free", () -> {
                LayoutPoint sc = HeavenPlotLayout.STAGE_CENTER;
                for (Map.Entry<Long, String> e : c.map().entrySet()) {
                    int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
                    if (Math.hypot(x - sc.x(), z - sc.z()) <= HeavenPlotLayout.STAGE_R && y >= 0 && y < HeavenPlotLayout.STAGE_H) {
                        assertTrue(Kinds.air(e.getValue()), "the stage keeps clear: " + x + "," + y + "," + z + " " + e.getValue());
                    }
                    LayoutPoint w = HeavenPlotLayout.WING_ORIGIN;
                    if (Math.abs(x - w.x()) <= 19 && Math.abs(z - w.z()) <= 19 && y >= -2) {
                        assertTrue(!St.id(e.getValue()).startsWith("minecraft:smooth_quartz") || y < -2, "the plot leaves Naomi's room alone at " + x + "," + y + "," + z);
                    }
                }
            }));
            out.add(DynamicTest.dynamicTest(n + " zones in the contract's order", () -> {
                LayoutPlan plan = c.toPlan();
                List<String> names = plan.zones().stream().map(LayoutPlan.Zone::name).toList();
                assertEquals(List.of("gate_plaza", "memory_lane", "home", "road", "wing", "stage", "island"), names);
                assertEquals(c.size(), plan.size());
                assertTrue(plan.zones().get(0).cells().stream().anyMatch(a -> a.dx() == 0 && a.dy() == -1 && a.dz() == HeavenPlotLayout.LANDING.z()),
                        "the landing's floor is in the first zone");
            }));
        }
        return out;
    }

    @Test
    void theRoadhouse() {
        Canvas c = RoadhouseLayout.canvas(3L);
        LayoutQuality.assertIdsKnown(c, IDS);
        LayoutQuality.assertBounds(c, new Box(-RoadhouseLayout.RADIUS, -60, -RoadhouseLayout.RADIUS, RoadhouseLayout.RADIUS, 40, RoadhouseLayout.RADIUS));
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertPalette(c, 45);
        LayoutPoint d = RoadhouseLayout.PLOT_DOOR;
        air(c, d.x(), 0, d.z(), "plot door");
        air(c, d.x(), 1, d.z(), "plot door");
        assertTrue(LayoutQuality.standable(c, RoadhouseLayout.ASH_SPOT.x(), 0, RoadhouseLayout.ASH_SPOT.z()), "Ash's spot");
        LayoutQuality.assertReachable(c, p(RoadhouseLayout.HUB_LANDING), List.of(p(RoadhouseLayout.ASH_SPOT), p(d)));
        LayoutQuality.assertNoDarkInteriors(c, 7);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
    }
}
