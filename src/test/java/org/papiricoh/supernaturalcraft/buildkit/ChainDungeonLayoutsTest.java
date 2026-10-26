package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Chain's strongholds (the Pit Castle, the Monastery) against the kit's construction standard ({@link LayoutQuality}) for
 * both factions and several seeds, and against their contract ({@link DungeonAnchors}): every anchor standable and walkable from
 * the entrance, 24–40 guards with at least two per room, 2–4 vault spots, the same plan for the same seed and different plans
 * for different seeds. Look at the structview renders too ({@code LayoutDumpTest}, {@link DungeonCatalog}).
 */
class ChainDungeonLayoutsTest {

    private static final long[] SEEDS = {0L, 1L, 7L, 42L, -99L, 1234567L, Long.MIN_VALUE};

    @TestFactory
    List<DynamicTest> everyStrongholdMeetsTheStandard() {
        List<DynamicTest> out = new ArrayList<>();
        for (ChainFaction f : ChainFaction.values()) {
            for (long seed : SEEDS) out.add(DynamicTest.dynamicTest(f + " " + seed, () -> check(f, seed)));
        }
        return out;
    }

    private static void check(ChainFaction faction, long seed) {
        Canvas c = DungeonCatalog.canvas(faction, seed);
        int r = DungeonAnchors.RADIUS;
        LayoutQuality.assertIdsKnown(c, BlockIds.vanilla());
        LayoutQuality.assertBounds(c, new Box(-r, -DungeonAnchors.DEPTH, -r, r, DungeonAnchors.HEIGHT, r));
        LayoutQuality.assertBudget(c, 160_000);
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertNoBlockEntities(c);
        LayoutQuality.assertWaterContained(c);
        assertLavaContained(c);
        LayoutQuality.assertPalette(c, 35);
        int[] entrance = c.anchor(DungeonAnchors.ENTRANCE);
        LayoutQuality.assertHeadroom(c, entrance);
        Set<Long> reach = LayoutQuality.walk(c, entrance);
        Map<String, Integer> perRoom = new HashMap<>();
        int guards = 0, vaults = 0;
        for (Map.Entry<String, int[]> e : c.anchors().entrySet()) {
            int[] p = e.getValue();
            String what = faction + " " + seed + " " + e.getKey() + " at " + p[0] + "," + p[1] + "," + p[2];
            assertTrue(LayoutQuality.standable(c, p[0], p[1], p[2]), what + " not standable: " + c.world(p[0], p[1], p[2]) + " over "
                    + c.world(p[0], p[1] - 1, p[2]));
            assertTrue(reach.contains(Canvas.key(p[0], p[1], p[2])), what + " out of reach from the entrance");
            String room = DungeonAnchors.roomOf(e.getKey());
            if (room != null) {
                guards++;
                perRoom.merge(room, 1, Integer::sum);
            }
            if (e.getKey().startsWith(DungeonAnchors.VAULT_PREFIX)) vaults++;
        }
        for (String a : List.of(DungeonAnchors.LORD, DungeonAnchors.ARRIVAL, DungeonAnchors.THRONE_ROOM, DungeonAnchors.COMMANDERS.get(0),
                DungeonAnchors.COMMANDERS.get(1))) c.anchor(a);
        assertTrue(guards >= 24 && guards <= 40, guards + " guards");
        for (Map.Entry<String, Integer> e : perRoom.entrySet()) assertTrue(e.getValue() >= 2, "room " + e.getKey() + " has " + e.getValue() + " guards");
        assertTrue(perRoom.size() >= 8, "rooms " + perRoom.keySet());
        assertTrue(vaults >= 2 && vaults <= 4, vaults + " vault spots");
        for (int i = 0; i < vaults; i++) c.anchor(DungeonAnchors.vault(i));
        int[] lord = c.anchor(DungeonAnchors.LORD), vault = c.anchor(DungeonAnchors.vault(0));
        assertTrue(lord[2] < -20 && vault[1] < -5, "the throne to the north, the vault below");
        LayoutQuality.assertNoDarkInteriors(c, 8);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-r, 0, -r, r, DungeonAnchors.HEIGHT, r));
        assertEquals(c.map(), Canvas.fromPlan(c.name(), DungeonCatalog.plan(faction, seed)).map());
    }

    /** Lava never flows: every lava cell has lava or something solid beside it and under it. */
    static void assertLavaContained(Canvas c) {
        List<String> leaks = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            if (!St.path(e.getValue()).equals("lava")) continue;
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            if (Kinds.air(c.world(x, y - 1, z)) || Kinds.passable(c.world(x, y - 1, z))) leaks.add(x + "," + y + "," + z + " over air");
            for (Dir d : Dir.HORIZONTAL) {
                String n = c.world(x + d.dx, y, z + d.dz);
                if (St.path(n).equals("lava")) continue;
                if (Kinds.air(n) || Kinds.passable(n)) leaks.add(x + "," + y + "," + z + " spills " + d.id());
            }
        }
        assertTrue(leaks.isEmpty(), leaks.size() + " lava leaks, e.g. " + leaks.subList(0, Math.min(12, leaks.size())));
    }

    @Test
    void theSameSeedGivesTheSamePlanAndSeedsVary() {
        for (ChainFaction f : ChainFaction.values()) {
            assertEquals(DungeonCatalog.canvas(f, 42).map(), DungeonCatalog.canvas(f, 42).map());
            assertNotEquals(DungeonCatalog.canvas(f, 1).map(), DungeonCatalog.canvas(f, 2).map());
            Set<List<String>> combos = new HashSet<>();
            for (long seed = 0; seed < 12; seed++) {
                List<String> rooms = DungeonCatalog.rooms(f, seed);
                assertEquals(4, rooms.size());
                assertEquals(4, new HashSet<>(rooms).size(), "distinct wings " + rooms);
                combos.add(rooms);
            }
            assertTrue(combos.size() >= 6, f + " only " + combos.size() + " wing layouts in 12 seeds");
        }
    }

    @Test
    void catalogIsRegisteredForVisualReview() {
        assertTrue(LayoutDumpTest.CATALOGS.contains(DungeonCatalog.class.getName()));
        assertEquals(4, new DungeonCatalog().layouts().size());
    }
}
