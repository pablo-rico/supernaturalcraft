package org.papiricoh.supernaturalcraft.legacy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Field notes on file in the Archive (v0.19): counted per topic, never below none nor above the cap. */
class ArchiveNotesTest {

    private static final String ZOMBIE = "creature:minecraft:zombie";

    @Test
    void notesAddUpAndArePaidOut() {
        Archive a = Archive.EMPTY.withNotes(ZOMBIE, 3).withNotes(ZOMBIE, 4).withNotes("arcane", 1);
        assertEquals(7, a.notes(ZOMBIE));
        assertEquals(1, a.notes("arcane"));
        assertEquals(0, a.notes("place"));
        assertTrue(a.hasNotes(ZOMBIE, 7));
        assertFalse(a.hasNotes(ZOMBIE, 8));
        a = a.withNotes(ZOMBIE, -5);
        assertEquals(2, a.notes(ZOMBIE));
    }

    @Test
    void notesNeverGoBelowNoneAndEmptyTopicsLeave() {
        Archive a = Archive.EMPTY.withNotes(ZOMBIE, 2).withNotes(ZOMBIE, -10);
        assertEquals(0, a.notes(ZOMBIE));
        assertFalse(a.notes().containsKey(ZOMBIE), "a spent topic leaves the map");
        assertTrue(Archive.EMPTY.withNotes("relic", -3).notes().isEmpty());
    }

    @Test
    void notesStopAtTheCap() {
        Archive a = Archive.EMPTY.withNotes("place", Archive.MAX_NOTES).withNotes("place", Integer.MAX_VALUE);
        assertEquals(Archive.MAX_NOTES, a.notes("place"));
    }

    @Test
    void otherChangesKeepTheNotes() {
        Archive a = Archive.EMPTY.withNotes(ZOMBIE, 5).finish(ZOMBIE).withCounter("case", 3);
        assertEquals(5, a.notes(ZOMBIE));
        assertEquals(Map.of(ZOMBIE, 5), a.notes());
    }
}
