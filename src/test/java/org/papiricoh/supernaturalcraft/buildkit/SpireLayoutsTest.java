package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.chorus.Melody;
import org.papiricoh.supernaturalcraft.structure.SpireLayout;
import org.papiricoh.supernaturalcraft.structure.spire.AscentLayout;
import org.papiricoh.supernaturalcraft.structure.spire.BelfryLayout;
import org.papiricoh.supernaturalcraft.structure.spire.CryptLayout;
import org.papiricoh.supernaturalcraft.structure.spire.LookoutLayout;
import org.papiricoh.supernaturalcraft.structure.spire.MonasteryLayout;
import org.papiricoh.supernaturalcraft.structure.spire.SpireKit;
import org.papiricoh.supernaturalcraft.structure.spire.SpireLayoutCatalog;
import org.papiricoh.supernaturalcraft.structure.spire.SummitLayout;
import org.papiricoh.supernaturalcraft.structure.spire.TempleLayout;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The remade Hymnal Spire's plans against the kit's quality bar ({@link LayoutQuality}): every part for a few seeds and every
 * turn, the stair on a synthetic mountain; and the contracts (the temple's window and lectern, the bells, the pillars that
 * shade the Chorus fight's last floor, the chests). Look at the structview renders too ({@code SpireLayoutCatalog}).
 */
class SpireLayoutsTest {

    static final BlockIds IDS;

    static {
        List<String> mod = new ArrayList<>(List.of(SpireKit.ALTAR));
        for (int i = 0; i < Melody.NOTES; i++) mod.add(SpireKit.bell(i));
        IDS = BlockIds.vanilla().allow(mod.toArray(String[]::new));
    }

    private static final long[] SEEDS = {7L, 1234567L, -99L};
    private static final byte[] HYMN = {2, 5, 1};

    static void sound(Canvas c, int palette) {
        LayoutQuality.assertIdsKnown(c, IDS);
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertWaterContained(c);
        LayoutQuality.assertPalette(c, palette);
    }

    static int[] at(Canvas c, int x, int y, int z, int dir) {
        int[] t = Canvas.turn(x, z, dir);
        return new int[]{t[0], y, t[1]};
    }

    static void chest(Canvas c, int[] p, String loot) {
        assertEquals("minecraft:chest", St.id(c.get(p[0], p[1], p[2])), "a chest at " + p[0] + "," + p[1] + "," + p[2]);
        assertTrue(c.decor().stream().anyMatch(d -> d.kind() == Decor.Kind.LOOT && d.x() == p[0] && d.y() == p[1] && d.z() == p[2] && d.data().equals(loot)),
                "chest " + p[0] + "," + p[1] + "," + p[2] + " holds " + loot);
    }

    @TestFactory
    List<DynamicTest> theTemple() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) {
            for (int dir = 0; dir < 4; dir++) {
                long sd = seed;
                int d = dir;
                out.add(DynamicTest.dynamicTest("temple " + seed + " turned " + dir, () -> {
                    Canvas c = TempleLayout.canvas(sd, HYMN, d);
                    sound(c, 25);
                    LayoutQuality.assertBounds(c, new Box(-16, -4, -16, 16, 24, 16));
                    int[] lectern = at(c, 0, 0, TempleLayout.LECTERN, d);
                    assertEquals("minecraft:lectern", St.id(c.get(lectern[0], lectern[1], lectern[2])));
                    for (int i = 0; i < 3; i++) {
                        for (int y = 2; y <= 5; y++) {
                            int[] p = at(c, 2 - 2 * i, y, TempleLayout.BACK, d);
                            assertEquals(SpireKit.noteGlass(HYMN[i]), c.get(p[0], p[1], p[2]), "pane " + i + " row " + y);
                        }
                    }
                    int[] chest = at(c, TempleLayout.CHEST[0], TempleLayout.CHEST[1], TempleLayout.CHEST[2], d);
                    chest(c, chest, SpireKit.LOOT_TEMPLE);
                    int[] entry = c.anchor("entry");
                    LayoutQuality.assertReachable(c, entry, List.of(lectern, chest));
                    LayoutQuality.assertHeadroom(c, entry);
                    LayoutQuality.assertNoDarkInteriors(c, 1);
                    LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
                    if (d == 0) LayoutQuality.assertDepth(c, new Box(-8, 0, -1, 8, 8, 14));
                }));
            }
        }
        return out;
    }

    @TestFactory
    List<DynamicTest> theMonasteryAndItsCrypt() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) {
            for (int dir = 0; dir < 4; dir++) {
                long sd = seed;
                int d = dir;
                out.add(DynamicTest.dynamicTest("monastery " + seed + " turned " + dir, () -> {
                    Canvas c = MonasteryLayout.canvas(sd, d);
                    sound(c, 40);
                    List<int[]> targets = new ArrayList<>();
                    for (int[] p : MonasteryLayout.CHESTS) {
                        int[] q = at(c, p[0], p[1], p[2], d);
                        chest(c, q, SpireKit.LOOT_MONASTERY);
                        targets.add(q);
                    }
                    int[] crypt = at(c, CryptLayout.CHEST[0], CryptLayout.CHEST[1], CryptLayout.CHEST[2], d);
                    chest(c, crypt, SpireKit.LOOT_CRYPT);
                    targets.add(crypt);
                    targets.add(c.anchor("crypt"));
                    targets.add(c.anchor("garth"));
                    int[] entry = c.anchor("entry");
                    LayoutQuality.assertReachable(c, entry, targets);
                    LayoutQuality.assertHeadroom(c, entry);
                    LayoutQuality.assertNoDarkInteriors(c, 1);
                    LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
                    if (d == 0) LayoutQuality.assertDepth(c, new Box(-12, -10, -1, 12, 8, 22));
                }));
            }
        }
        return out;
    }

    @TestFactory
    List<DynamicTest> theBelfryAndTheLookouts() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) {
            out.add(DynamicTest.dynamicTest("belfry " + seed, () -> {
                Canvas c = BelfryLayout.canvas(seed, Math.floorMod((int) seed, 4));
                sound(c, 25);
                LayoutQuality.assertReachable(c, c.anchor("entry"), List.of(c.anchor("bell")));
                LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
            }));
            for (int v = 0; v < LookoutLayout.VARIANTS; v++) {
                int variant = v;
                out.add(DynamicTest.dynamicTest("lookout " + seed + " variant " + v, () -> {
                    Canvas c = LookoutLayout.canvas(seed, variant, variant);
                    sound(c, 8);
                    LayoutQuality.assertReachable(c, c.anchor("entry"), List.of(c.anchor("view")));
                    LayoutQuality.assertHeadroom(c, c.anchor("entry"));
                }));
            }
        }
        return out;
    }

    @Test
    void theSummitKeepsTheFightsContract() {
        for (long seed : SEEDS) {
            Canvas c = SummitLayout.canvas(seed);
            sound(c, 20);
            LayoutQuality.assertBounds(c, new Box(-SummitLayout.RADIUS - 1, -20, -SummitLayout.RADIUS - 1, SummitLayout.RADIUS + 1, 16,
                    SummitLayout.RADIUS + 1));
            assertEquals(SpireKit.ALTAR, c.get(0, 0, 0));
            for (int i = 0; i < Melody.NOTES; i++) {
                int[] b = SummitLayout.bell(seed, i);
                assertEquals(SpireKit.bell(i), c.get(b[0], 1, b[1]), "bell " + i);
                assertEquals(SpireKit.noteGlass(i), c.get(b[0], 0, b[1]), "bell " + i + " stands on its glass");
            }
            int pillars = 0;
            for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) if (String.valueOf(c.get(x, 1, z)).startsWith("minecraft:quartz_pillar")) pillars++;
            assertTrue(pillars >= 24, "the inner pillars shade the last floor: " + pillars);
            // Nothing stands over the altar: the Chorus comes down onto it.
            for (int y = 1; y <= 12; y++) for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) assertTrue(c.isAir(x, y, z), "clear over the altar at " + x + "," + y + "," + z);
            // No block entity on the platform (the arena restores no block entities): no chests up here.
            for (Cell cell : c.cells()) assertTrue(!St.id(cell.state()).equals("minecraft:chest"), "a chest on the summit at " + cell);
        }
    }

    @Test
    void theStairOnAMountain() {
        for (long seed : SEEDS) {
            SpireLayout.Plan plan = SpireLayoutCatalog.plan(SpireLayoutCatalog.MOUNTAIN, seed);
            Canvas c = AscentLayout.canvas(SpireLayout.ascentInput(plan));
            sound(c, 15);
            LayoutQuality.assertReachable(c, c.anchor("top"), List.of(c.anchor("bottom")));
            LayoutQuality.assertHeadroom(c, c.anchor("top"));
            int shrine = AscentLayout.shrineNode(SpireLayout.relative(plan.path(), plan.altar()));
            if (shrine >= 0) {
                int[] s = c.anchor("shrine");
                assertTrue(Math.hypot(s[0], s[2]) >= AscentLayout.SHRINE_MIN_DISTANCE - 1, "the summit chest is out of the arena");
                LayoutQuality.assertReachable(c, c.anchor("top"), List.of(s));
            }
        }
    }

    @Test
    void theWholeSpireStaysInReach() {
        Canvas c = SpireLayoutCatalog.whole(SpireLayoutCatalog.MOUNTAIN, SpireLayoutCatalog.SEED, false);
        LayoutQuality.assertIdsKnown(c, IDS);
        LayoutQuality.assertBounds(c, SpireLayoutCatalog.reach());
    }
}
