package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.SeatLayoutCatalog;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;

import static org.junit.jupiter.api.Assertions.*;

class ChainSeatLayoutsTest {
    @Test
    void bothSeatsMeetTheConstructionStandard() {
        for (ChainFaction faction : ChainFaction.values()) {
            Canvas c = SeatLayoutCatalog.canvas(faction);
            LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
            LayoutQuality.assertBounds(c, new Box(-12, -4, -12, 12, 12, 12));
            LayoutQuality.assertBudget(c, 12000);
            LayoutQuality.assertShapes(c);
            LayoutQuality.assertSupported(c);
            LayoutQuality.assertNoBlockEntities(c);
            LayoutQuality.assertWaterContained(c);
            LayoutQuality.assertPalette(c, 10);
            LayoutQuality.assertHeadroom(c, c.anchor(SeatLayoutCatalog.ENTRANCE));
            var reachable = LayoutQuality.walk(c, c.anchor(SeatLayoutCatalog.ENTRANCE));
            assertEquals(5, c.anchors().size());
            for (var entry : c.anchors().entrySet()) {
                int[] p = entry.getValue();
                assertTrue(LayoutQuality.standable(c, p[0], p[1], p[2]), faction + " " + entry.getKey());
                assertTrue(reachable.contains(Canvas.key(p[0], p[1], p[2])), faction + " " + entry.getKey());
            }
            LayoutQuality.assertNoDarkInteriors(c, 8);
            LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
            LayoutQuality.assertDepth(c, new Box(-12, 0, -12, 12, 10, 12));
            assertEquals(c.map(), Canvas.fromPlan(c.name(), SeatLayoutCatalog.plan(faction)).map());
        }
    }

    @Test
    void sitesAreStableDistantAndOpposite() {
        for (long seed : new long[]{0, 1, -1, 42, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var demon = SeatSite.of(seed, ChainFaction.DEMON);
            var angel = SeatSite.of(seed, ChainFaction.ANGEL);
            assertEquals(demon, SeatSite.of(seed, ChainFaction.DEMON));
            for (var site : new SeatSite.Site[]{demon, angel}) {
                double radius = Math.hypot(site.x(), site.z());
                assertTrue(radius >= SeatSite.MIN_DISTANCE && radius <= SeatSite.MAX_DISTANCE);
            }
            assertTrue(Math.hypot(demon.x() - angel.x(), demon.z() - angel.z()) >= 6000);
        }
        assertNotEquals(SeatSite.of(1, ChainFaction.DEMON), SeatSite.of(2, ChainFaction.DEMON));
    }

    @Test
    void catalogIsRegisteredForVisualReview() {
        assertTrue(LayoutDumpTest.CATALOGS.contains(SeatLayoutCatalog.class.getName()));
        assertEquals(2, new SeatLayoutCatalog().layouts().size());
    }
}
