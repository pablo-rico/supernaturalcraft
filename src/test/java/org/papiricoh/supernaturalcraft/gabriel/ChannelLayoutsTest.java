package org.papiricoh.supernaturalcraft.gabriel;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.arena.ChannelLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TV Land's four sets (v0.14): inside the arena, under budget, the centre clear, and the spots the fight relies on. */
class ChannelLayoutsTest {

    private static final int[] RADII = {18, 20, 26, 32};

    /** Blocks the arena could never rewrite (they hold a block entity) or that are not vanilla. */
    private static final List<String> FORBIDDEN = List.of("chest", "barrel", "sign", "banner", "_bed", "lectern", "furnace", "skull",
            "head", "spawner", "beacon", "hopper", "dispenser", "dropper", "jukebox", "campfire", "shulker", "bell", "decorated_pot");

    private static Map<String, String> byPos(List<ArenaCell> cells) {
        Map<String, String> out = new HashMap<>();
        for (ArenaCell c : cells) out.put(c.dx() + "," + c.dy() + "," + c.dz(), c.block());
        return out;
    }

    @Test
    void everySetStaysInsideTheArenaAndUnderBudget() {
        for (Channel ch : Channel.values()) {
            for (int radius : RADII) {
                List<ArenaCell> cells = ChannelLayouts.plan(ch, radius);
                assertFalse(cells.isEmpty());
                assertTrue(cells.size() <= ChannelLayouts.BUDGET, ch + " r" + radius + ": " + cells.size());
                int r = radius - ChannelLayouts.MARGIN;
                Set<String> seen = new HashSet<>();
                for (ArenaCell c : cells) {
                    assertTrue(c.distanceSq() <= r * r, ch + " leaves the margin at " + c);
                    assertTrue(seen.add(c.dx() + "," + c.dy() + "," + c.dz()), ch + " writes a cell twice: " + c);
                    assertTrue(c.dy() >= ChannelLayouts.BOTTOM && c.dy() <= ChannelLayouts.TOP, ch + " goes too far: " + c);
                    assertFalse(c.block().isEmpty(), ch + " leaves a cell blank: " + c);
                    assertTrue(c.block().startsWith("minecraft:"), ch + " uses a block that is not vanilla: " + c);
                    for (String f : FORBIDDEN) assertFalse(c.block().contains(f), ch + " uses a block entity the arena can't rewrite: " + c);
                }
            }
        }
    }

    @Test
    void theCentreStaysClearAndEveryFloorIsWhole() {
        for (Channel ch : Channel.values()) {
            List<ArenaCell> cells = ChannelLayouts.plan(ch, 20);
            int floor = 0;
            for (ArenaCell c : cells) {
                if (c.dy() >= 1) {
                    assertTrue(c.distanceSq() > ChannelLayouts.CLEAR_CENTRE * ChannelLayouts.CLEAR_CENTRE, ch + " builds on the centre: " + c);
                }
                if (c.dy() == 0) floor++;
            }
            int r = 20 - ChannelLayouts.MARGIN, expected = 0;
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) if (x * x + z * z <= r * r) expected++;
            assertEquals(expected, floor, ch + " must lay its whole floor");
        }
    }

    @Test
    void theUnionIsPinnedOnceAndEverySetFillsAllOfIt() {
        for (int radius : RADII) {
            List<ArenaCell> union = ChannelLayouts.union(radius);
            assertTrue(union.size() <= ChannelLayouts.UNION_BUDGET, "r" + radius + ": " + union.size());
            Set<String> seen = new HashSet<>();
            int lastDy = Integer.MAX_VALUE;
            for (ArenaCell c : union) {
                assertTrue(seen.add(c.dx() + "," + c.dy() + "," + c.dz()), "pinned twice: " + c);
                assertTrue(c.dy() <= lastDy, "the union must go from the top down");
                lastDy = c.dy();
            }
            for (Channel ch : Channel.values()) {
                List<String> blocks = ChannelLayouts.blocksOf(ch, union, radius);
                assertEquals(union.size(), blocks.size());
                Map<String, String> mine = byPos(ChannelLayouts.plan(ch, radius));
                for (int i = 0; i < union.size(); i++) {
                    ArenaCell c = union.get(i);
                    String key = c.dx() + "," + c.dy() + "," + c.dz();
                    String expected = mine.getOrDefault(key, ChannelLayouts.filler(ch, c.dx(), c.dy(), c.dz()));
                    assertEquals(expected, blocks.get(i), ch + " at " + key);
                    if (!mine.containsKey(key) && c.dy() > 0) assertEquals(ChannelLayouts.AIR, blocks.get(i));
                }
            }
        }
    }

    @Test
    void theGameShowHasItsThreeColouredPlatformsOverFoamPits() {
        Map<String, String> show = byPos(ChannelLayouts.plan(Channel.GAME_SHOW, 20));
        assertEquals(3, ChannelLayouts.PLATFORMS.size());
        // Left to right as seen from the front (the south): west to east.
        assertTrue(ChannelLayouts.PLATFORMS.get(0).dx() < ChannelLayouts.PLATFORMS.get(1).dx());
        assertTrue(ChannelLayouts.PLATFORMS.get(1).dx() < ChannelLayouts.PLATFORMS.get(2).dx());
        for (int i = 0; i < 3; i++) {
            for (ChannelLayouts.Spot s : ChannelLayouts.platformCells(i)) {
                assertEquals(ChannelLayouts.PLATFORM_BLOCKS.get(i), show.get(s.dx() + ",0," + s.dz()), "platform " + i + " at " + s);
                assertEquals(i, ChannelLayouts.platformAt(s.dx(), s.dz()));
                for (int y = -1; y > -ChannelLayouts.PIT_DEPTH; y--) assertEquals(ChannelLayouts.AIR, show.get(s.dx() + "," + y + "," + s.dz()));
                assertEquals(ChannelLayouts.FOAM, show.get(s.dx() + "," + -ChannelLayouts.PIT_DEPTH + "," + s.dz()));
            }
        }
        assertEquals(-1, ChannelLayouts.platformAt(0, 0), "no platform at the centre");
        assertTrue(ChannelLayouts.PLATFORM_BLOCKS.get(0).contains("red") && ChannelLayouts.PLATFORM_BLOCKS.get(1).contains("blue")
                && ChannelLayouts.PLATFORM_BLOCKS.get(2).contains("yellow"));
    }

    @Test
    void theSpotsTheFightUsesAreThere() {
        Map<String, String> ad = byPos(ChannelLayouts.plan(Channel.COMMERCIAL, 20));
        assertEquals(5, ChannelLayouts.PODIUMS.size(), "five spokesmen, five podiums");
        for (ChannelLayouts.Spot s : ChannelLayouts.PODIUMS) {
            assertTrue(ad.containsKey(s.dx() + ",1," + s.dz()), "a podium to stand on at " + s);
            assertFalse(ad.containsKey(s.dx() + ",2," + s.dz()), "and room to stand at " + s);
            assertFalse(ad.containsKey(s.dx() + ",3," + s.dz()), "and room to stand at " + s);
        }
        Map<String, String> sitcom = byPos(ChannelLayouts.plan(Channel.SITCOM, 20));
        for (ChannelLayouts.Spot d : ChannelLayouts.DOORS) {
            assertTrue(sitcom.get(d.dx() + ",1," + d.dz()).contains("door"), "a door in the back flat at " + d);
            assertTrue(sitcom.get(d.dx() + ",2," + d.dz()).contains("open=true"), "an open door at " + d);
        }
        Map<String, String> ward = byPos(ChannelLayouts.plan(Channel.HOSPITAL, 20));
        for (ChannelLayouts.Spot b : ChannelLayouts.BEDS) {
            assertFalse(ward.containsKey(b.dx() + ",1," + b.dz()), "room for a nurse to get up at " + b);
            assertTrue(ward.containsKey((b.dx() + Integer.signum(b.dx())) + ",1," + b.dz()), "a bed beside " + b);
        }
    }
}
