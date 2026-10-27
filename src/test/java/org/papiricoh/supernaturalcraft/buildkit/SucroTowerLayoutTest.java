package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;
import org.papiricoh.supernaturalcraft.chain.seat.tower.SucroTower;
import org.papiricoh.supernaturalcraft.chain.seat.tower.SucroTowerCatalog;
import org.papiricoh.supernaturalcraft.entity.boss.dick.BoardroomGround;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Leviathans' seat, the SucroCorp tower ({@link SucroTower}), against the kit's construction standard ({@link LayoutQuality})
 * and its contract: the {@link DungeonAnchors} anchors (standable, walkable from the entrance, guards and vault spots in range),
 * the boardroom's own anchors, and a boardroom that works in place as {@link BoardroomGround}'s arena (a clear disc, the same
 * table, room overhead, closed in). Look at the structview renders too ({@code LayoutDumpTest}, {@link SucroTowerCatalog}).
 */
class SucroTowerLayoutTest {

    private static final long[] SEEDS = {0L, 1L, 6L, 42L, Long.MIN_VALUE};

    @TestFactory
    List<DynamicTest> everySeedMeetsTheStandard() {
        List<DynamicTest> out = new ArrayList<>();
        for (long seed : SEEDS) out.add(DynamicTest.dynamicTest("seed " + seed, () -> check(seed)));
        return out;
    }

    private static void check(long seed) {
        Canvas c = SucroTower.canvas(seed);
        int r = SucroTower.RADIUS;
        LayoutQuality.assertIdsKnown(c, BlockIds.vanilla().allow("supernaturalcraft:leviathan_goo"));
        LayoutQuality.assertBounds(c, SucroTower.bounds());
        LayoutQuality.assertBudget(c, 400_000);
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertNoBlockEntities(c);
        LayoutQuality.assertWaterContained(c);
        LayoutQuality.assertPalette(c, 40);
        int[] entrance = c.anchor(DungeonAnchors.ENTRANCE);
        LayoutQuality.assertHeadroom(c, entrance);
        Set<Long> reach = LayoutQuality.walk(c, entrance);
        Map<String, Integer> perRoom = new HashMap<>();
        int guards = 0, vaults = 0;
        for (Map.Entry<String, int[]> e : c.anchors().entrySet()) {
            int[] p = e.getValue();
            String what = seed + " " + e.getKey() + " at " + p[0] + "," + p[1] + "," + p[2];
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
                DungeonAnchors.COMMANDERS.get(1), SucroTower.BOARDROOM, SucroTower.BOARDROOM_DOOR, SucroTower.DICK_SEAT)) c.anchor(a);
        assertTrue(guards >= 24 && guards <= 40, guards + " guards");
        for (Map.Entry<String, Integer> e : perRoom.entrySet()) assertTrue(e.getValue() >= 2, "room " + e.getKey() + " has " + e.getValue() + " guards");
        long playable = SucroTower.FLOORS.stream().filter(SucroTower.Floor::playable).count();
        assertTrue(playable >= 8 && playable <= 10, playable + " playable floors");
        assertEquals(playable - 1, perRoom.size(), "a garrison on every playable floor but the boardroom: " + perRoom.keySet());
        assertEquals(SucroTower.vaults(seed), vaults);
        assertTrue(vaults >= 4 && vaults <= 6, vaults + " vault spots");
        for (int i = 0; i < vaults; i++) c.anchor(DungeonAnchors.vault(i));
        assertArrayEquals(c.anchor(DungeonAnchors.LORD), c.anchor(SucroTower.DICK_SEAT));
        assertArrayEquals(c.anchor(DungeonAnchors.THRONE_ROOM), c.anchor(SucroTower.BOARDROOM));
        assertArrayEquals(SucroTower.boardroom(), c.anchor(SucroTower.BOARDROOM));
        assertArrayEquals(SucroTower.dickSeat(), c.anchor(SucroTower.DICK_SEAT));
        assertTrue(St.path(c.world(c.anchor(SucroTower.BOARDROOM_DOOR)[0], c.anchor(SucroTower.BOARDROOM_DOOR)[1],
                c.anchor(SucroTower.BOARDROOM_DOOR)[2])).endsWith("_door"), "the boardroom door is a door");
        LayoutQuality.assertNoDarkInteriors(c, 8);
        LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
        LayoutQuality.assertDepth(c, new Box(-r, 0, -r, r, SucroTower.HEIGHT, r));
        assertEquals(c.map(), Canvas.fromPlan(c.name(), SucroTower.plan(seed)).map(), "the plan is the canvas");
    }

    @Test
    void theBoardroomIsDicksArenaInPlace() {
        Canvas c = SucroTower.canvas(1L);
        int[] o = c.anchor(SucroTower.BOARDROOM);
        int ox = o[0], fy = o[1] - 1, oz = o[2];
        // BoardroomGround's cells are relative to its floor (dy = 0); the table and its chairs are what stands in the disc.
        Map<Long, String> table = new HashMap<>();
        for (ArenaCell a : BoardroomGround.plan(SucroTower.BOARDROOM_RADIUS)) {
            boolean furniture = a.dy() >= 1 && Math.abs(a.dx()) <= BoardroomGround.TABLE_HALF && a.dz() >= BoardroomGround.TABLE_Z - 2
                    && a.dz() <= BoardroomGround.TABLE_Z + 1 && !Kinds.air(a.block());
            if (furniture) table.put(Canvas.key(ox + a.dx(), fy + a.dy(), oz + a.dz()), a.block());
        }
        int[] seat = c.anchor(SucroTower.DICK_SEAT);
        assertTrue(table.containsKey(Canvas.key(seat[0], seat[1], seat[2])), "Dick's place is BoardroomGround's head chair");
        assertTrue(LayoutQuality.standable(c, seat[0], seat[1], seat[2]), "Dick stands at the head of the table");
        int tableCells = 0;
        for (Map.Entry<Long, String> e : table.entrySet()) {
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            if (x == seat[0] && y == seat[1] && z == seat[2]) continue;
            assertEquals(St.id(e.getValue()), St.id(c.world(x, y, z)), "table cell " + x + "," + y + "," + z);
            if (!Kinds.stairs(e.getValue())) tableCells++;
        }
        assertEquals((2 * BoardroomGround.TABLE_HALF + 1) * 2 + 3 * 2, tableCells, "slabs over both rows, fences at the ends and middle");
        int clear = BoardroomGround.WALL_HEIGHT + 4, rr = SucroTower.BOARDROOM_RADIUS;
        assertTrue(SucroTower.BOARDROOM_HEADROOM >= clear, "headroom " + SucroTower.BOARDROOM_HEADROOM);
        List<String> bad = new ArrayList<>();
        for (int dx = -rr; dx <= rr; dx++) {
            for (int dz = -rr; dz <= rr; dz++) {
                if (dx * dx + dz * dz > rr * rr) continue;
                int x = ox + dx, z = oz + dz;
                String floor = c.world(x, fy, z);
                if (!Kinds.fullCube(floor) || !Kinds.floor(floor)) bad.add(x + "," + fy + "," + z + " floor " + floor);
                for (int y = fy + 1; y < fy + 1 + SucroTower.BOARDROOM_HEADROOM; y++) {
                    if (table.containsKey(Canvas.key(x, y, z))) continue;
                    if (Math.abs(dx) <= 1 && dz == seat[2] - oz && y == seat[1]) continue; // the arms of Dick's chair
                    if (dx == 0 && dz == seat[2] - oz - 1 && y <= seat[1] + 2) continue; // its back
                    String s = c.world(x, y, z);
                    boolean light = St.path(s).equals("end_rod") || St.path(s).equals("light");
                    if (!Kinds.air(s) && !light) bad.add(x + "," + y + "," + z + " " + s);
                }
                String roof = c.world(x, fy + 1 + SucroTower.BOARDROOM_HEADROOM, z);
                if (!Kinds.opaqueCube(roof)) bad.add(x + "," + z + " roof " + roof);
            }
        }
        assertTrue(bad.isEmpty(), bad.size() + " cells in the way, e.g. " + bad.subList(0, Math.min(12, bad.size())));
        // Closed in: from the centre, across the room, nothing leads out but the door.
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{ox, o[1], oz});
        seen.add(Canvas.key(ox, o[1], oz));
        Box tower = new Box(-20, fy, SucroTower.TOWER_Z - 20, 20, fy + 1 + SucroTower.BOARDROOM_HEADROOM, SucroTower.TOWER_Z + 20);
        while (!q.isEmpty()) {
            int[] p = q.poll();
            assertTrue(tower.contains(p[0], p[1], p[2]), "the boardroom leaks at " + p[0] + "," + p[1] + "," + p[2]);
            for (Dir d : Dir.values()) {
                int x = p[0] + d.dx, y = p[1] + d.dy, z = p[2] + d.dz;
                String s = c.world(x, y, z);
                if (!Kinds.passable(s) || St.path(s).endsWith("_door")) continue;
                if (seen.add(Canvas.key(x, y, z))) q.add(new int[]{x, y, z});
            }
        }
    }

    @Test
    void theSameSeedGivesTheSamePlanAndSeedsVary() {
        assertEquals(SucroTower.canvas(42).map(), SucroTower.canvas(42).map());
        assertNotEquals(SucroTower.canvas(1).map(), SucroTower.canvas(2).map());
        Set<Integer> vaults = new HashSet<>();
        for (long seed = 0; seed < 12; seed++) vaults.add(SucroTower.vaults(seed));
        assertEquals(3, vaults.size(), "4, 5 and 6 vault spots all occur: " + vaults);
    }

    @Test
    void floorsAreStackedWithoutGaps() {
        List<SucroTower.Floor> f = SucroTower.FLOORS;
        assertEquals(0, f.get(0).y());
        for (int i = 1; i < f.size(); i++) assertEquals(f.get(i - 1).y() + f.get(i - 1).height(), f.get(i).y(), f.get(i).room());
        SucroTower.Floor top = f.get(f.size() - 1);
        assertEquals("boardroom", top.room());
        assertEquals(SucroTower.BOARDROOM_FLOOR, top.y());
        assertTrue(top.topY() + 1 < SucroTower.HEIGHT);
    }

    @Test
    void catalogIsRegisteredForVisualReview() {
        assertTrue(LayoutDumpTest.CATALOGS.contains(SucroTowerCatalog.class.getName()));
        assertEquals(SucroTowerCatalog.REVIEW_SEEDS.length, new SucroTowerCatalog().layouts().size());
    }
}
