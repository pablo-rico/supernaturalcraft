package org.papiricoh.supernaturalcraft.apocalypse.blood;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanicRoomRulesTest {

    /** An iron box with inside {@code [0, w) × [0, h) × [0, d)}, a trap on the ceiling at {@code trap} unless null. */
    static PanicRoomRules.World box(int w, int h, int d, int[] trap, Set<Long> holes) {
        return new PanicRoomRules.World() {
            @Override
            public boolean wall(int x, int y, int z) {
                if (holes.contains(PanicRoomRules.pack(x, y, z))) return false;
                boolean inside = x >= 0 && x < w && y >= 0 && y < h && z >= 0 && z < d;
                boolean shell = x >= -1 && x <= w && y >= -1 && y <= h && z >= -1 && z <= d;
                return shell && !inside;
            }

            @Override
            public boolean ceilingTrap(int x, int y, int z) {
                return trap != null && x == trap[0] && y == trap[1] && z == trap[2];
            }
        };
    }

    @Test
    void aSealedIronRoomWithATrapOnItsCeilingIsOne() {
        PanicRoomRules.Room r = PanicRoomRules.check(box(5, 3, 5, new int[]{2, 2, 2}, Set.of()), 2, 0, 2);
        assertEquals(PanicRoomRules.Verdict.OK, r.verdict());
        assertEquals(5 * 3 * 5, r.cells().size());
        assertTrue(r.contains(0, 0, 0) && r.contains(4, 2, 4));
    }

    @Test
    void withoutTheTrapItIsJustAnIronBox() {
        assertEquals(PanicRoomRules.Verdict.NO_TRAP, PanicRoomRules.check(box(5, 3, 5, null, Set.of()), 2, 0, 2).verdict());
    }

    @Test
    void anOpeningLetsItLeak() {
        Set<Long> hole = new HashSet<>();
        hole.add(PanicRoomRules.pack(5, 1, 2)); // a gap in the east wall
        assertEquals(PanicRoomRules.Verdict.LEAKS, PanicRoomRules.check(box(5, 3, 5, new int[]{2, 2, 2}, hole), 2, 0, 2).verdict());
    }

    @Test
    void aHallTooBigIsNoPanicRoom() {
        assertEquals(PanicRoomRules.Verdict.LEAKS, PanicRoomRules.check(box(9, 3, 5, new int[]{2, 2, 2}, Set.of()), 2, 0, 2).verdict());
        assertEquals(PanicRoomRules.Verdict.OK, PanicRoomRules.check(box(7, 5, 7, new int[]{3, 4, 3}, Set.of()), 3, 0, 3).verdict(),
                "seven by five by seven is the most");
    }
}
