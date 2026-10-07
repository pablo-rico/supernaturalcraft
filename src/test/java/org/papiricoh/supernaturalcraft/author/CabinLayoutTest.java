package org.papiricoh.supernaturalcraft.author;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The cabin's plan: no block entities (the arena must erase and restore it), inside its box, furnished. */
class CabinLayoutTest {

    /** Vanilla blocks that carry a block entity (substrings of their ids). */
    private static final List<String> BLOCK_ENTITIES = List.of("chest", "barrel", "lectern", "sign", "_bed", "banner",
            "brewing_stand", "decorated_pot", "bell", "campfire", "furnace", "smoker", "chiseled_bookshelf", "skull", "_head",
            "jukebox", "enchanting_table", "beehive", "bee_nest", "spawner", "hopper", "dispenser", "dropper", "shulker_box",
            "conduit", "beacon", "comparator", "daylight_detector", "command_block", "structure_block", "jigsaw", "sculk_sensor",
            "sculk_catalyst", "sculk_shrieker", "crafter", "vault", "end_gateway", "end_portal", "moving_piston", "suspicious");

    @Test
    void nothingInTheCabinHasABlockEntity() {
        for (String state : CabinLayout.palette()) {
            String id = CabinLayout.blockId(state);
            for (String bad : BLOCK_ENTITIES) assertTrue(!id.contains(bad), state + " has a block entity (" + bad + ")");
            assertTrue(id.startsWith("minecraft:") || id.equals("supernaturalcraft:typewriter"), "unexpected block " + id);
        }
    }

    @Test
    void everyBlockFitsTheBox() {
        Set<String> seen = new HashSet<>();
        for (CabinLayout.Cell c : CabinLayout.cells()) {
            assertTrue(c.x() >= CabinLayout.MIN_X && c.x() <= CabinLayout.MAX_X, c + " outside in X");
            assertTrue(c.z() >= CabinLayout.MIN_Z && c.z() <= CabinLayout.MAX_Z, c + " outside in Z");
            assertTrue(c.y() >= 0 && c.y() <= CabinLayout.MAX_Y && CabinLayout.MAX_Y < CabinLayout.CLEAR_TO, c + " outside in Y");
            assertTrue(seen.add(c.x() + "," + c.y() + "," + c.z()), "two blocks at " + c);
        }
    }

    @Test
    void theCabinIsFurnished() {
        assertEquals("supernaturalcraft:typewriter", CabinLayout.blockId(at(CabinLayout.TYPEWRITER)));
        assertTrue(at(CabinLayout.CHAIR).contains("stairs"), "a chair before the desk");
        assertTrue(at(CabinLayout.DOOR).contains("door"), "a door");
        assertEquals("minecraft:air", at(new int[]{CabinLayout.CHAIR[0], CabinLayout.CHAIR[1] + 1, CabinLayout.CHAIR[2]}), "room above the chair");
        long drafts = CabinLayout.cells().stream().filter(c -> c.state().equals("minecraft:white_carpet")).count();
        assertTrue(drafts >= 4, "drafts on the floor");
        assertTrue(CabinLayout.palette().stream().anyMatch(s -> s.startsWith("minecraft:bookshelf")), "bookshelves");
        assertTrue(CabinLayout.palette().stream().anyMatch(s -> s.startsWith("minecraft:brown_candle")), "empty bottles");
    }

    @Test
    void rotationTurnsTheDoor() {
        assertArrayEquals(new int[]{0, 5}, CabinLayout.rotate(0, 5, 0));
        assertArrayEquals(new int[]{-5, 0}, CabinLayout.rotate(0, 5, 1), "clockwise: the door looks west");
        assertArrayEquals(new int[]{0, -5}, CabinLayout.rotate(0, 5, 2));
        assertArrayEquals(new int[]{5, 0}, CabinLayout.rotate(0, 5, 3));
        for (int q = 0; q < 4; q++) {
            int[] box = CabinLayout.box(100, 64, -200, q);
            for (CabinLayout.Cell c : CabinLayout.cells()) {
                int[] w = CabinLayout.toWorld(new int[]{c.x(), c.y(), c.z()}, 100, 64, -200, q);
                assertTrue(w[0] >= box[0] && w[0] <= box[3] && w[1] >= box[1] && w[1] <= box[4] && w[2] >= box[2] && w[2] <= box[5],
                        "rotation " + q + ": " + c + " outside the box");
            }
        }
    }

    private static String at(int[] p) {
        return CabinLayout.cells().stream().filter(c -> c.x() == p[0] && c.y() == p[1] && c.z() == p[2])
                .map(CabinLayout.Cell::state).findFirst().orElse("minecraft:air");
    }
}
