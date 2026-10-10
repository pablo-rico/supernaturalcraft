package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsLayout;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Naomi's room, Zachariah's office and the wild crossroads keep their contracts and the kit's quality bar. */
class BossRoomsTest {

    static final BlockIds IDS = BlockIds.vanilla().allow("supernaturalcraft:reprogramming_console", "supernaturalcraft:filing_cabinet",
            "supernaturalcraft:crossroads_soil");

    private static void noBlockEntities(List<ArenaCell> cells, String what) {
        List<String> bad = new ArrayList<>();
        for (ArenaCell c : cells) {
            assertTrue(St.wellFormed(c.block()), what + ": malformed " + c.block());
            if (Kinds.blockEntity(c.block())) bad.add(c.toString());
        }
        assertTrue(bad.isEmpty(), what + " has block entities: " + bad);
    }

    private static void standable(Canvas c, LayoutPoint p, String what) {
        assertTrue(LayoutQuality.standable(c, p.x(), p.y(), p.z()), what + " " + p + ": " + c.world(p.x(), p.y(), p.z()) + " over "
                + c.world(p.x(), p.y() - 1, p.z()));
    }

    // --- Naomi ----------------------------------------------------------------------------------------------------------------

    @Test
    void naomisRoom() {
        Canvas c = ReprogrammingRoomLayout.canvas();
        LayoutQuality.assertIdsKnown(c, IDS);
        int r = ReprogrammingRoomLayout.RADIUS;
        LayoutQuality.assertBounds(c, new Box(-r, -4, -r, r, ReprogrammingRoomLayout.HEIGHT - 1, r));
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertPalette(c, 25);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        noBlockEntities(ReprogrammingRoomLayout.plan().cells(), "the room");
        standable(c, ReprogrammingRoomLayout.ENTRY, "entry");
        standable(c, ReprogrammingRoomLayout.NAOMI_SPOT, "Naomi");
        for (LayoutPoint p : ReprogrammingRoomLayout.CHAIRS) standable(c, p, "chair");
        for (LayoutPoint p : ReprogrammingRoomLayout.GUARD_SPAWNS) standable(c, p, "guard");
        for (LayoutPoint p : ReprogrammingRoomLayout.COPY_SPOTS) standable(c, p, "copy");
        standable(c, ReprogrammingRoomLayout.CONSOLE.offset(0, 0, 1), "before the console");
        LayoutPoint con = ReprogrammingRoomLayout.CONSOLE;
        assertEquals("supernaturalcraft:reprogramming_console", c.get(con.x(), con.y(), con.z()));
        LayoutPoint lift = ReprogrammingRoomLayout.ELEVATOR;
        assertTrue(c.isAir(lift.x(), 0, lift.z()) && c.isAir(lift.x(), 1, lift.z()), "the lift's seal cells are left as air");
        List<int[]> targets = new ArrayList<>();
        for (LayoutPoint p : ReprogrammingRoomLayout.CHAIRS) targets.add(new int[]{p.x(), p.y(), p.z()});
        for (LayoutPoint p : ReprogrammingRoomLayout.GUARD_SPAWNS) targets.add(new int[]{p.x(), p.y(), p.z()});
        targets.add(new int[]{lift.x(), 0, lift.z() + 1});
        LayoutPoint e = ReprogrammingRoomLayout.ENTRY;
        LayoutQuality.assertReachable(c, new int[]{e.x(), e.y(), e.z()}, targets);
        LayoutQuality.assertNoDarkInteriors(c, 8);
    }

    @Test
    void naomisSecondPhaseOnlyRetracts() {
        List<ArenaCell> two = ReprogrammingRoomLayout.phaseCells(2);
        assertFalse(two.isEmpty());
        assertTrue(ReprogrammingRoomLayout.phaseCells(1).isEmpty());
        noBlockEntities(two, "phase 2");
        Canvas c = ReprogrammingRoomLayout.canvas();
        for (ArenaCell a : two) {
            assertTrue(Math.abs(a.dx()) <= ReprogrammingRoomLayout.RADIUS && Math.abs(a.dz()) <= ReprogrammingRoomLayout.RADIUS, a.toString());
            assertTrue(!c.isAir(a.dx(), a.dy(), a.dz()), "phase 2 only changes what stands: " + a);
        }
        for (LayoutPoint p : ReprogrammingRoomLayout.COPY_SPOTS) {
            for (ArenaCell a : two) assertFalse(a.dx() == p.x() && a.dz() == p.z() && a.dy() <= 1 && !St.isAir(a.block()), "copy spot blocked");
        }
    }

    // --- Zachariah ------------------------------------------------------------------------------------------------------------

    @Test
    void theOfficeRepeatsExactlyInsideTheWindow() {
        Canvas c = ZachariahOfficeLayout.canvas();
        int t = ZachariahOfficeLayout.TILE, w = ZachariahOfficeLayout.WRAP_WINDOW;
        List<String> seams = new ArrayList<>();
        for (int x = -w; x + t <= w; x++) {
            for (int z = -w; z <= w; z++) {
                for (int y = -2; y <= ZachariahOfficeLayout.CEILING + 1; y++) {
                    if (!c.world(x, y, z).equals(c.world(x + t, y, z))) seams.add("x " + x + "," + y + "," + z);
                    if (z + t <= w && !c.world(z, y, x).equals(c.world(z, y, x + t))) seams.add("z " + z + "," + y + "," + x);
                }
            }
        }
        assertTrue(seams.isEmpty(), seams.size() + " seams: " + seams.subList(0, Math.min(8, seams.size())));
        // The wrap's shift lands on the same office too.
        assertEquals(0, ZachariahOfficeLayout.WRAP_SHIFT % t);
    }

    @Test
    void theOfficeKeepsItsContract() {
        Canvas c = ZachariahOfficeLayout.canvas();
        LayoutQuality.assertIdsKnown(c, IDS);
        int r = ZachariahOfficeLayout.RADIUS;
        LayoutQuality.assertBounds(c, new Box(-r, -3, -r, r, ZachariahOfficeLayout.CEILING + 2, r));
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertPalette(c, 25);
        noBlockEntities(ZachariahOfficeLayout.plan().cells(), "the office");
        standable(c, ZachariahOfficeLayout.DAIS, "dais");
        standable(c, ZachariahOfficeLayout.ENTRY, "entry");
        standable(c, ZachariahOfficeLayout.ENTRY.offset(0, 0, 2), "the lift's way down");
        for (LayoutPoint p : ZachariahOfficeLayout.DESK_SAFE) standable(c, p, "approved desk");
        for (LayoutPoint p : ZachariahOfficeLayout.CLERK_SPAWNS) standable(c, p, "clerk");
        for (LayoutPoint p : ZachariahOfficeLayout.FRAMES) assertTrue(c.isAir(p.x(), p.y(), p.z()), "frame spot " + p);
        for (int n = 0; n < ZachariahOfficeLayout.CABINETS.size(); n++) {
            LayoutPoint p = ZachariahOfficeLayout.CABINETS.get(n);
            String s = c.get(p.x(), p.y(), p.z());
            assertEquals("supernaturalcraft:filing_cabinet", St.id(s));
            assertEquals(String.valueOf(n + 1), St.get(s, "number"), "cabinet " + (n + 1) + " at " + p);
        }
        LayoutPoint e = ZachariahOfficeLayout.ENTRY;
        List<int[]> targets = new ArrayList<>();
        for (LayoutPoint p : ZachariahOfficeLayout.DESK_SAFE) targets.add(new int[]{p.x(), p.y(), p.z()});
        for (LayoutPoint p : ZachariahOfficeLayout.CABINETS) targets.add(new int[]{p.x(), p.y() + 1, p.z()});
        targets.add(new int[]{0, 1, 0});
        LayoutQuality.assertReachable(c, new int[]{e.x(), e.y(), e.z()}, targets);
        LayoutQuality.assertNoDarkInteriors(c, 7);
    }

    @Test
    void shufflesAndTheDissolveSpareThePoints() {
        List<LayoutPoint> keep = new ArrayList<>(ZachariahOfficeLayout.DESK_SAFE);
        keep.addAll(ZachariahOfficeLayout.CLERK_SPAWNS);
        keep.addAll(ZachariahOfficeLayout.CABINETS);
        keep.add(ZachariahOfficeLayout.ENTRY);
        keep.add(ZachariahOfficeLayout.ENTRY.offset(0, 0, 2));
        keep.add(new LayoutPoint(0, 0, 0));
        for (int step = 0; step < 4; step++) {
            List<ArenaCell> cells = ZachariahOfficeLayout.shuffle(step);
            assertFalse(cells.isEmpty());
            noBlockEntities(cells, "shuffle " + step);
            for (ArenaCell a : cells) {
                for (LayoutPoint p : keep) assertFalse(a.dx() == p.x() && a.dz() == p.z(), "shuffle " + step + " walls in " + p);
                assertTrue(Math.abs(a.dx()) <= ZachariahOfficeLayout.WRAP_WINDOW + 8 && Math.abs(a.dz()) <= ZachariahOfficeLayout.WRAP_WINDOW + 8);
            }
        }
        List<ArenaCell> gone = ZachariahOfficeLayout.dissolve();
        noBlockEntities(gone, "dissolve");
        assertTrue(gone.stream().allMatch(a -> St.isAir(a.block())), "the dissolve only clears");
        assertTrue(gone.stream().noneMatch(a -> a.dy() < 0), "the floor stays");
        assertTrue(gone.stream().anyMatch(a -> a.dy() == ZachariahOfficeLayout.CEILING), "the ceiling goes");
    }

    // --- the crossroads -------------------------------------------------------------------------------------------------------

    @Test
    void crossroadsEveryWay() {
        for (int dir = 0; dir < 4; dir++) {
            Canvas c = CrossroadsLayout.canvas(dir * 977L + 11, dir);
            LayoutQuality.assertIdsKnown(c, IDS);
            int r = CrossroadsLayout.RADIUS;
            LayoutQuality.assertBounds(c, new Box(-r, -6, -r, r, CrossroadsLayout.HEIGHT, r));
            LayoutQuality.assertShapes(c);
            LayoutQuality.assertSupported(c);
            LayoutQuality.assertPalette(c, 20);
            LayoutPoint ce = CrossroadsLayout.CENTRE;
            assertEquals("supernaturalcraft:crossroads_soil", c.get(ce.x(), ce.y(), ce.z()));
            assertTrue(c.isAir(0, 0, 0) && c.isAir(0, 1, 0), "room to stand on the crossing");
            assertTrue(c.size() < 30_000, "budget: " + c.size());
            LayoutPlan plan = CrossroadsLayout.plan(dir * 977L + 11, dir);
            assertEquals(c.size(), plan.size());
            assertTrue(plan.decor().stream().allMatch(d -> d.kind().equals("sign")), "only signs as decor");
        }
    }
}
