package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.coven.layout.ChurchLayout;
import org.papiricoh.supernaturalcraft.coven.layout.CovenLayout;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The witches' places (v0.21 P3) against the kit's quality bar ({@link LayoutQuality}), for several seeds and every turn: the
 * abandoned church (a floored 3x3 ring round its altar spot for the small circle, its crypt and chests reachable), the witch
 * coven (its real bowl, every spawn and chest reachable) and Rowena's house (lit rooms, her bowl outside her fight's arena and
 * no block entity inside it). Look at the structview renders too ({@code LayoutDumpTest}, {@code CovenLayoutCatalog}).
 */
class CovenLayoutsTest {

    static final BlockIds IDS = BlockIds.vanilla().allow("supernaturalcraft:spell_bowl");
    private static final long[] SEEDS = {7L, 1234567L, -99L};

    static int[] p(LayoutPoint l) {
        return new int[]{l.x(), l.y(), l.z()};
    }

    static void standable(Canvas c, LayoutPoint p, String what) {
        assertTrue(LayoutQuality.standable(c, p.x(), p.y(), p.z()), what + " " + p + ": " + c.world(p.x(), p.y(), p.z()) + " over "
                + c.world(p.x(), p.y() - 1, p.z()));
    }

    static void sound(Canvas c, Box bounds) {
        LayoutQuality.assertIdsKnown(c, IDS);
        LayoutQuality.assertBounds(c, bounds);
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertWaterContained(c);
        LayoutQuality.assertPalette(c, 25);
    }

    @TestFactory
    List<DynamicTest> theChurch() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) {
            for (int dir = 0; dir < 4; dir++) {
                final long sd = seed;
                final int d = dir;
                out.add(DynamicTest.dynamicTest("church " + seed + " turned " + dir, () -> {
                    Canvas c = ChurchLayout.canvas(sd, d);
                    int r = ChurchLayout.RADIUS;
                    sound(c, new Box(-r, -ChurchLayout.DEPTH, -r, r, ChurchLayout.HEIGHT, r));
                    UnaryOperator<LayoutPoint> at = q -> ChurchLayout.at(q, d);
                    LayoutPoint altar = at.apply(ChurchLayout.ALTAR), crypt = at.apply(ChurchLayout.CRYPT), entry = at.apply(ChurchLayout.ENTRY);
                    List<int[]> targets = new ArrayList<>(List.of(p(altar), p(crypt)));
                    for (LayoutPoint s : ChurchLayout.SPAWNS) {
                        standable(c, at.apply(s), "spawn");
                        targets.add(p(at.apply(s)));
                    }
                    for (LayoutPoint s : ChurchLayout.CHESTS) {
                        LayoutPoint q = at.apply(s);
                        assertEquals("minecraft:chest", St.id(c.get(q.x(), q.y(), q.z())), "a chest at " + q);
                        targets.add(p(q));
                    }
                    standable(c, entry, "entry");
                    standable(c, altar, "altar");
                    standable(c, crypt, "crypt");
                    LayoutQuality.assertReachable(c, p(entry), targets);
                    LayoutQuality.assertHeadroom(c, p(entry));
                    // The small circle's ring round the ritual altar: air over a full floor.
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            assertTrue(c.isAir(altar.x() + dx, altar.y(), altar.z() + dz) && Kinds.fullCube(c.world(altar.x() + dx, altar.y() - 1, altar.z() + dz)),
                                    "ring at " + dx + "," + dz + ": " + c.world(altar.x() + dx, altar.y(), altar.z() + dz) + " over "
                                            + c.world(altar.x() + dx, altar.y() - 1, altar.z() + dz));
                        }
                    }
                }));
            }
        }
        return out;
    }

    @Test
    void theChurchLooksTheParts() {
        Canvas c = ChurchLayout.canvas(7L, 0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-7, 0, -16, 7, 18, 12));
    }

    @TestFactory
    List<DynamicTest> theCovenstead() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) {
            for (int dir = 0; dir < 4; dir++) {
                final long sd = seed;
                final int d = dir;
                out.add(DynamicTest.dynamicTest("coven " + seed + " turned " + dir, () -> {
                    Canvas c = CovenLayout.canvas(sd, d);
                    int r = CovenLayout.RADIUS;
                    sound(c, new Box(-r, -6, -r, r, CovenLayout.HEIGHT, r));
                    UnaryOperator<LayoutPoint> at = q -> CovenLayout.at(q, d);
                    LayoutPoint b = at.apply(CovenLayout.BOWL);
                    assertEquals("supernaturalcraft:spell_bowl", c.get(b.x(), b.y(), b.z()), "the coven's real bowl");
                    List<LayoutPoint> pts = new ArrayList<>(List.of(CovenLayout.ALTAR, CovenLayout.COVEN_WITCH_SPAWN));
                    pts.addAll(CovenLayout.WITCH_SPAWNS);
                    pts.addAll(CovenLayout.FAMILIAR_SPAWNS);
                    pts.addAll(CovenLayout.VICTIM_SPAWNS);
                    List<int[]> targets = new ArrayList<>();
                    for (LayoutPoint q : pts) {
                        standable(c, at.apply(q), "spawn");
                        targets.add(p(at.apply(q)));
                    }
                    for (LayoutPoint q : CovenLayout.CHESTS) {
                        LayoutPoint w = at.apply(q);
                        assertEquals("minecraft:chest", St.id(c.get(w.x(), w.y(), w.z())), "a chest at " + w);
                        targets.add(p(w));
                    }
                    LayoutPoint e = at.apply(CovenLayout.ENTRY);
                    standable(c, e, "entry");
                    LayoutQuality.assertReachable(c, p(e), targets);
                    LayoutQuality.assertHeadroom(c, p(e));
                }));
            }
        }
        return out;
    }

    @Test
    void theCovensteadLooksTheParts() {
        Canvas c = CovenLayout.canvas(7L, 0);
        LayoutQuality.assertNoDarkInteriors(c, 1);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
    }

    @TestFactory
    List<DynamicTest> rowenasHouse() {
        List<DynamicTest> out = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            final int d = q;
            out.add(DynamicTest.dynamicTest("house turned " + q, () -> {
                Canvas c = RowenaHouseLayout.canvas(d);
                UnaryOperator<LayoutPoint> at = w -> RowenaHouseLayout.at(w, d);
                int r = RowenaHouseLayout.RADIUS;
                sound(c, new Box(-r, -RowenaHouseLayout.DEPTH, -r, r, RowenaHouseLayout.HEIGHT, r));
                LayoutPoint b = at.apply(RowenaHouseLayout.BOWL);
                assertEquals("supernaturalcraft:spell_bowl", c.get(b.x(), b.y(), b.z()), "her bowl");
                LayoutPoint ac = at.apply(RowenaHouseLayout.ARENA_CENTRE);
                int ar = RowenaHouseLayout.ARENA_RADIUS;
                List<String> bad = new ArrayList<>();
                for (Map.Entry<Long, String> e : c.map().entrySet()) {
                    int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
                    if (Kinds.blockEntity(e.getValue()) && Math.hypot(x - ac.x(), z - ac.z()) <= ar) bad.add(x + "," + z + " " + e.getValue());
                }
                assertTrue(bad.isEmpty(), "block entities in her arena (it is erased and restored): " + bad);
                assertTrue(Math.hypot(b.x() - ac.x(), b.z() - ac.z()) > ar, "the bowl lies outside the arena");
                List<int[]> targets = new ArrayList<>();
                for (LayoutPoint w : List.of(RowenaHouseLayout.NPC_HOME, RowenaHouseLayout.BOWL_STAND, RowenaHouseLayout.GARDEN,
                        RowenaHouseLayout.ARENA_CENTRE)) {
                    standable(c, at.apply(w), "point");
                    targets.add(p(at.apply(w)));
                }
                LayoutPoint e = at.apply(RowenaHouseLayout.ENTRY);
                standable(c, e, "entry");
                LayoutQuality.assertReachable(c, p(e), targets);
                LayoutQuality.assertHeadroom(c, p(e));
                // Every turn is the same house: the same number of blocks.
                assertEquals(RowenaHouseLayout.canvas(0).map().size(), c.map().size(), "turn " + d + " lost or gained blocks");
            }));
        }
        return out;
    }

    @Test
    void rowenasHouseLooksTheParts() {
        Canvas c = RowenaHouseLayout.canvas(0);
        LayoutQuality.assertNoDarkInteriors(c, 8);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-8, 0, -7, 11, 20, 10));
    }

    @Test
    void theSitesAreChosenOnFlatDryGround() {
        int[] flat = {70, 71, 70, 72, 70};
        assertTrue(ChurchLayout.suitable(flat, flat, 63) && CovenLayout.suitable(flat, flat, 63), "flat dry ground will do");
        int[] water = {60, 60, 60, 60, 60}, seabed = {50, 50, 50, 50, 50};
        assertFalse(ChurchLayout.suitable(water, seabed, 63), "no church on the water");
        assertFalse(CovenLayout.suitable(water, seabed, 63), "no coven on the water");
    }
}
