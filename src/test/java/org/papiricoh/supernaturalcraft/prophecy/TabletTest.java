package org.papiricoh.supernaturalcraft.prophecy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TabletTest {

    @Test
    void theTabletRestsLongerEachChapter() {
        assertEquals(1, Tablet.restDays(1));
        assertEquals(1, Tablet.restDays(2));
        assertEquals(2, Tablet.restDays(3));
        assertEquals(3, Tablet.restDays(4));
        assertEquals(6, Tablet.restDays(5));
        assertEquals(0, Tablet.restDays(Tablet.CHAPTERS));
    }

    @Test
    void sentencesGrowLongerChapterByChapter() {
        for (Tablet t : Tablet.values()) {
            assertTrue(WordCipher.phrase(t, Tablet.CHAPTERS).length() > WordCipher.phrase(t, 1).length(), t.id());
        }
        assertTrue(Tablet.SUPREME_WARD_CHAPTER <= Tablet.CHAPTERS && Tablet.TRIALS_CHAPTER == Tablet.CHAPTERS);
    }
}
