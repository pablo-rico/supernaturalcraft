package org.papiricoh.supernaturalcraft.raphael;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Raphael's house (v0.16): inside its arena, the rite left alone, the rings and posts clear, the roof its own group. */
class HouseLayoutTest {

    /** Blocks the arena could never rewrite (they hold a block entity). */
    private static final List<String> FORBIDDEN = List.of("chest", "barrel", "sign", "banner", "_bed", "lectern", "furnace", "skull",
            "head", "spawner", "beacon", "hopper", "dispenser", "dropper", "jukebox", "campfire", "shulker", "bell", "decorated_pot",
            "smoker", "brewing", "enchanting", "chiseled_bookshelf");

    private static Map<String, String> byPos(List<ArenaCell> cells) {
        Map<String, String> out = new HashMap<>();
        for (ArenaCell c : cells) out.put(c.dx() + "," + c.dy() + "," + c.dz(), c.block());
        return out;
    }

    @Test
    void theHouseFitsItsArenaWithVanillaBlocksAndNoCellTwice() {
        List<ArenaCell> plan = HouseLayout.plan();
        assertFalse(plan.isEmpty());
        assertTrue(plan.size() < 12_000, "the house is small: " + plan.size());
        int r = HouseLayout.MIN_RADIUS - 1;
        Set<String> seen = new HashSet<>();
        for (ArenaCell c : plan) {
            assertTrue(c.distanceSq() <= r * r, "out of the smallest arena: " + c);
            assertTrue(seen.add(c.dx() + "," + c.dy() + "," + c.dz()), "a cell written twice: " + c);
            assertTrue(c.dy() >= HouseLayout.BOTTOM && c.dy() <= HouseLayout.TOP, "too high or too deep: " + c);
            assertTrue(c.block().startsWith("minecraft:"), "not vanilla: " + c);
            for (String f : FORBIDDEN) assertFalse(c.block().contains(f), "a block entity the arena can't rewrite: " + c);
        }
    }

    @Test
    void theRiteIsLeftAloneButHasAFloor() {
        for (ArenaCell c : HouseLayout.body()) {
            if (HouseLayout.inRitual(c.dx(), c.dz())) assertTrue(c.dy() <= 0, "something is built round the rite: " + c);
        }
        for (ArenaCell c : HouseLayout.roof()) {
            if (HouseLayout.inRitual(c.dx(), c.dz())) assertTrue(c.dy() >= HouseLayout.CEILING, "only the ceiling over the rite: " + c);
        }
        Map<String, String> at = byPos(HouseLayout.plan());
        for (int x = -HouseLayout.RITUAL; x <= HouseLayout.RITUAL; x++) {
            for (int z = -HouseLayout.RITUAL; z <= HouseLayout.RITUAL; z++) {
                assertTrue(at.get(x + ",0," + z).contains("planks"), "the parlour's floor under the rite at " + x + "," + z);
            }
        }
    }

    @Test
    void twoRoomsWithWallsWindowsAndADoor() {
        Map<String, String> at = byPos(HouseLayout.body());
        // The outer walls stand, but for the windows and the door.
        int walls = 0, windows = 0;
        for (int x = -HouseLayout.HALF_X; x <= HouseLayout.HALF_X; x++) {
            String b = at.get(x + ",3," + (-HouseLayout.HALF_Z));
            if (b.equals(HouseLayout.AIR)) windows++;
            else walls++;
        }
        assertTrue(walls > windows && windows >= 2, "the north wall: " + walls + " solid, " + windows + " windows");
        for (HouseLayout.Window w : HouseLayout.WINDOWS) {
            assertEquals(HouseLayout.AIR, at.get(w.dx() + ",2," + w.dz()), "a broken window at " + w);
            assertFalse(at.get(w.dx() + ",1," + w.dz()).equals(HouseLayout.AIR), "a window has a sill: " + w);
            // Its lane runs into the house.
            HouseLayout.Spot first = HouseLayout.lane(w).getFirst();
            assertTrue(HouseLayout.inFootprint(first.dx(), first.dz()), "a window looks into the house: " + w);
        }
        assertEquals(HouseLayout.AIR, at.get(HouseLayout.DOOR.dx() + ",1," + HouseLayout.DOOR.dz()), "the front door is open");
        assertEquals(HouseLayout.AIR, at.get(HouseLayout.DIVIDER_X + ",1,0"), "a doorway between the rooms");
        assertFalse(at.get(HouseLayout.DIVIDER_X + ",1,5").equals(HouseLayout.AIR), "a wall between the rooms");
        // Something knocked over in each room.
        assertTrue(at.get("0,1,-6").contains("trapdoor"), "the overturned table");
        assertTrue(at.values().stream().anyMatch(b -> b.contains("cauldron")), "the kitchen's sink");
    }

    @Test
    void theRingsLieOnClearFloorInsideTheRooms() {
        Map<String, String> at = byPos(HouseLayout.body());
        assertEquals(RaphaelBalance.OIL_RINGS, HouseLayout.RINGS.size());
        Set<String> used = new HashSet<>();
        for (int i = 0; i < HouseLayout.RINGS.size(); i++) {
            List<HouseLayout.Spot> cells = HouseLayout.ringCells(i);
            assertEquals(16, cells.size(), "a ring is the border of a 5x5 square");
            HouseLayout.Spot c = HouseLayout.RINGS.get(i);
            for (int x = -HouseLayout.RING_HALF; x <= HouseLayout.RING_HALF; x++) {
                for (int z = -HouseLayout.RING_HALF; z <= HouseLayout.RING_HALF; z++) {
                    int dx = c.dx() + x, dz = c.dz() + z;
                    assertTrue(HouseLayout.indoors(dx, dz), "ring " + i + " leaves the rooms at " + dx + "," + dz);
                    assertFalse(HouseLayout.inRitual(dx, dz), "ring " + i + " lies on the rite");
                    assertTrue(used.add(dx + "," + dz), "two rings overlap at " + dx + "," + dz);
                    assertEquals(HouseLayout.AIR, at.get(dx + ",1," + dz), "ring " + i + " is not clear at " + dx + "," + dz);
                    assertEquals(HouseLayout.AIR, at.get(dx + ",2," + dz), "ring " + i + " has no headroom at " + dx + "," + dz);
                    assertTrue(at.get(dx + ",0," + dz).startsWith("minecraft:") && !at.get(dx + ",0," + dz).equals(HouseLayout.AIR),
                            "ring " + i + " lies on a floor");
                    assertEquals(i, HouseLayout.ringAt(dx, dz));
                }
            }
            assertTrue(HouseLayout.isOil(c.dx() + 2, c.dz()) && !HouseLayout.isOil(c.dx(), c.dz()), "oil round, floor inside");
        }
        // Two rings a room.
        long parlour = HouseLayout.RINGS.stream().filter(s -> s.dx() < HouseLayout.DIVIDER_X).count();
        assertEquals(2, parlour);
    }

    @Test
    void theGarrisonsPostsAreClearAndOffTheRings() {
        Map<String, String> at = byPos(HouseLayout.body());
        assertTrue(HouseLayout.POSTS.size() >= RaphaelBalance.GARRISON);
        for (HouseLayout.Spot p : HouseLayout.POSTS) {
            assertTrue(HouseLayout.indoors(p.dx(), p.dz()), "a post outside: " + p);
            assertEquals(-1, HouseLayout.ringAt(p.dx(), p.dz()), "a post on a ring: " + p);
            assertFalse(HouseLayout.inRitual(p.dx(), p.dz()), "a post on the rite: " + p);
            assertEquals(HouseLayout.AIR, at.get(p.dx() + ",1," + p.dz()), "a post in furniture: " + p);
            assertEquals(HouseLayout.AIR, at.get(p.dx() + ",2," + p.dz()), "a post without headroom: " + p);
        }
    }

    @Test
    void theRoofIsItsOwnGroupAndCoversTheHouse() {
        List<ArenaCell> roof = HouseLayout.roof(), body = HouseLayout.body();
        Set<String> bodyCells = byPos(body).keySet();
        for (ArenaCell c : roof) {
            assertFalse(bodyCells.contains(c.dx() + "," + c.dy() + "," + c.dz()), "the roof shares a cell with the house: " + c);
            assertTrue(c.dy() >= HouseLayout.WALL_TOP, "the roof is above the walls: " + c);
        }
        Map<String, String> at = byPos(roof);
        for (int x = -HouseLayout.HALF_X; x <= HouseLayout.HALF_X; x++) {
            for (int z = -HouseLayout.HALF_Z; z <= HouseLayout.HALF_Z; z++) {
                boolean covered = false;
                for (int y = HouseLayout.CEILING + 1; y <= HouseLayout.TOP; y++) {
                    String b = at.get(x + "," + y + "," + z);
                    if (b != null && b.contains("slab")) covered = true;
                }
                assertTrue(covered, "a hole in the roof at " + x + "," + z);
            }
        }
        // Torn off, nothing of it is left above the walls.
        assertTrue(roof.size() > 400, "a roof worth tearing off: " + roof.size());
    }
}
