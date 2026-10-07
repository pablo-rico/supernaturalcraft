package org.papiricoh.supernaturalcraft.author;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "The End": the hunter's own story, ending as it should. */
class ChronicleTest {

    private static final Chronicle.Facts FULL = new Chronicle.Facts("Jo", List.of("Azazel", "Lilith", "Lucifer"), 412, "Zombie", 120,
            23, 9, true, List.of("The Colt", "Lucifer's Grace"));

    @Test
    void theStoryIsTheHunters() {
        List<String> lines = Chronicle.write(FULL);
        String all = String.join("\n", lines);
        assertTrue(all.contains("Jo"));
        for (String boss : FULL.bosses()) assertTrue(all.contains(boss), "names " + boss);
        assertTrue(all.contains("412") && all.contains("Zombie") && all.contains("The Colt") && all.contains("crossroads"));
        assertEquals(Chronicle.THE_END, lines.getLast());
    }

    @Test
    void anEmptyLogStillMakesABook() {
        List<String> lines = Chronicle.write(new Chronicle.Facts("", List.of(), 0, null, 0, 0, 0, false, List.of()));
        assertEquals(Chronicle.THE_END, lines.getLast());
        assertFalse(String.join("", lines).contains("null"));
        assertTrue(String.join("", lines).contains("the hunter"));
    }

    @Test
    void theSameFactsMakeTheSameBook() {
        assertEquals(Chronicle.write(FULL), Chronicle.write(FULL));
    }
}
