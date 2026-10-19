package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.cain.layout.CainFarmLayout;
import org.papiricoh.supernaturalcraft.cain.layout.CainLayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cain's farm (v0.21 P4) against the kit's quality bar ({@link LayoutQuality}) in every turn: known ids, shapes, supports, water
 * kept in; the farmhouse rooms, barn, yard and grave reachable from the lane; his yard (the fight's arena) flat and clear of any
 * block entity within {@code ARENA_RADIUS + 1} (the arena restores what it touches); the six hives beyond it on one side, the
 * fields farmland, the grave a free cell over grass. Look at the structview render too ({@code LayoutDumpTest},
 * {@link CainLayoutCatalog}).
 */
class CainLayoutsTest {

    static final BlockIds IDS = BlockIds.vanilla();

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    @TestFactory
    List<DynamicTest> theFarmInEveryTurn() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("farm turned " + q, () -> {
                Canvas c = CainFarmLayout.canvas(d);
                UnaryOperator<LayoutPoint> at = w -> CainFarmLayout.at(w, d);
                int r = CainFarmLayout.RADIUS;
                LayoutQuality.assertIdsKnown(c, IDS);
                LayoutQuality.assertBounds(c, new Box(-r, -CainFarmLayout.DEPTH, -r, r, CainFarmLayout.HEIGHT, r));
                LayoutQuality.assertShapes(c);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertWaterContained(c);
                LayoutQuality.assertPalette(c, 25);
                // The yard: nothing that holds a block entity within the arena and a block more.
                LayoutPoint ac = at.apply(CainFarmLayout.ARENA_CENTRE);
                int ar = CainFarmLayout.ARENA_RADIUS;
                List<String> bad = new ArrayList<>();
                for (Map.Entry<Long, String> e : c.map().entrySet()) {
                    int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
                    if (Kinds.blockEntity(e.getValue()) && Math.hypot(x - ac.x(), z - ac.z()) <= ar + 1) {
                        bad.add(x + "," + Canvas.ky(e.getKey()) + "," + z + " " + e.getValue());
                    }
                }
                assertTrue(bad.isEmpty(), "block entities in his yard (the arena erases and restores it): " + bad);
                // ... and its ground is whole and level at y = -1 (no pits, no towers) for the fight.
                List<String> holes = new ArrayList<>();
                for (int x = -ar; x <= ar; x++) {
                    for (int z = -ar; z <= ar; z++) {
                        if (Math.hypot(x, z) > ar) continue;
                        int wx = ac.x() + x, wz = ac.z() + z;
                        String floor = c.world(wx, -1, wz);
                        if (floor == null || floor.equals("minecraft:air") || c.isAir(wx, -1, wz)) holes.add(wx + "," + wz + " no floor");
                        for (int y = 3; y <= CainFarmLayout.HEIGHT; y++) {
                            if (!c.isAir(wx, y, wz)) holes.add(wx + "," + y + "," + wz + " " + c.world(wx, y, wz));
                        }
                    }
                }
                assertTrue(holes.isEmpty(), "his yard must be open, level ground: " + holes.subList(0, Math.min(12, holes.size())));
                // The hives: beehives, beyond the yard, all on one side (q = 0: east).
                List<int[]> targets = new ArrayList<>();
                for (LayoutPoint h : CainFarmLayout.HIVES) {
                    LayoutPoint w = at.apply(h);
                    assertEquals("minecraft:beehive", St.id(c.get(w.x(), w.y(), w.z())), "a hive at " + w);
                    double dist = Math.hypot(w.x() - ac.x(), w.z() - ac.z());
                    assertTrue(dist >= ar + 1.5 && dist <= ar + 7, "hive " + w + " at " + dist + " from the centre");
                    int[] east = CainFarmLayout.rotate(1, 0, d);
                    assertTrue(w.x() * east[0] + w.z() * east[1] > ar, "hive " + w + " on the apiary's side");
                }
                assertEquals(6, CainFarmLayout.HIVES.size());
                for (LayoutPoint f : CainFarmLayout.FIELDS) {
                    LayoutPoint w = at.apply(f);
                    assertEquals("minecraft:farmland", St.id(c.get(w.x(), w.y(), w.z())), "a field at " + w);
                    assertTrue(Math.hypot(w.x(), w.z()) <= ar, "the field " + w + " lies in the yard");
                }
                LayoutPoint g = at.apply(CainFarmLayout.GRAVE);
                assertTrue(c.isAir(g.x(), g.y(), g.z()) && c.isAir(g.x(), g.y() + 1, g.z()), "the grave's cell is free: " + c.world(g.x(), g.y(), g.z()));
                assertEquals("minecraft:grass_block", St.id(c.world(g.x(), g.y() - 1, g.z())), "the grave is dug in grass");
                assertTrue(Math.hypot(g.x(), g.z()) > ar, "the grave lies beyond the yard (the arena would wipe it)");
                // Reachable from the lane: his porch, the fight's spot, every room, the barn and the grave.
                for (LayoutPoint w : List.of(CainFarmLayout.NPC_HOME, CainFarmLayout.BOSS_SPAWN, CainFarmLayout.ARENA_CENTRE,
                        CainFarmLayout.SITTING_ROOM, CainFarmLayout.KITCHEN, CainFarmLayout.BEDROOM, CainFarmLayout.STUDY,
                        CainFarmLayout.BARN, CainFarmLayout.GRAVE)) {
                    LayoutPoint a = at.apply(w);
                    assertTrue(LayoutQuality.standable(c, a.x(), a.y(), a.z()), "point " + a + ": " + c.world(a.x(), a.y(), a.z()) + " over "
                            + c.world(a.x(), a.y() - 1, a.z()));
                    targets.add(p(a));
                }
                LayoutPoint e = at.apply(CainFarmLayout.ENTRY);
                assertTrue(LayoutQuality.standable(c, e.x(), e.y(), e.z()), "the lane at " + e);
                LayoutQuality.assertReachable(c, p(e), targets);
                LayoutQuality.assertHeadroom(c, p(e));
                // Every turn is the same farm.
                assertEquals(CainFarmLayout.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
            }));
        }
        return out;
    }

    @Test
    void theFarmLooksTheParts() {
        Canvas c = CainFarmLayout.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 8);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-9, 0, -29, 7, 12, -19));
    }

    @Test
    void theCatalogDumpsIt() {
        var layouts = new CainLayoutCatalog().layouts();
        assertTrue(layouts.containsKey("cain_farm"));
        assertFalse(layouts.get("cain_farm").get().toString().isEmpty());
        assertTrue(LayoutDumpTest.CATALOGS.contains(CainLayoutCatalog.class.getName()), "LayoutDumpTest must list the catalog");
    }
}
