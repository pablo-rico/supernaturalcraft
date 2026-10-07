package org.papiricoh.supernaturalcraft.bowl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecitationTest {

    private static Recitation typeAll(Recitation r, String text) {
        for (char c : text.toCharArray()) r.type(c);
        return r;
    }

    @Test
    void normalizeStripsAccentsCaseAndNonLetters() {
        assertEquals("aperioculos", Recitation.normalize("Áperi, ÓCULOS!"));
        assertEquals("caelum", Recitation.normalize("Cælum"));
        assertEquals("poena", Recitation.normalize("Pœna"));
        assertEquals("iuuenis", Recitation.normalize("Juvenis"), "j is i and v is u");
        assertEquals("", Recitation.normalize(" ,.;-'1 2 3"));
    }

    @Test
    void timeGrowsWithLettersAndDifficulty() {
        assertEquals(40 + 5 * 8, Recitation.timeFor("Aperi", 1f));
        assertEquals(Math.round((40 + 5 * 8) * 1.5f), Recitation.timeFor("Aperi", 1.5f));
        assertEquals(Recitation.timeFor("aperi oculos", 1f), Recitation.timeFor("APERI, ÓCULOS", 1f), "case, accents and punctuation are free");
    }

    @Test
    void spacesAndAccentsAreOptional() {
        Recitation r = new Recitation("Ábscondite me", 400, 20);
        typeAll(r, "abscondite me");
        assertTrue(r.done());
        assertEquals(0, r.typos());
        Recitation compact = typeAll(new Recitation("Ábscondite me", 400, 20), "ABSCONDITEME");
        assertTrue(compact.done(), "no spaces needed, any case");
    }

    @Test
    void ligaturesAndClassicalLettersMatchEitherWay() {
        assertTrue(typeAll(new Recitation("Cælum", 400, 20), "caelum").done(), "ae for æ");
        assertTrue(typeAll(new Recitation("Caelum", 400, 20), "cæl").cursor() == 4, "æ for ae");
        assertTrue(typeAll(new Recitation("Juvenis", 400, 20), "iuuenis").done(), "i/u for j/v");
        assertTrue(typeAll(new Recitation("Iuuenis", 400, 20), "juvenis").done(), "j/v for i/u");
    }

    @Test
    void aTypoCostsTimeAndDoesNotAdvance() {
        Recitation r = new Recitation("ab", 100, 20);
        assertEquals(Recitation.Result.LETTER, r.type('a'));
        assertEquals(Recitation.Result.TYPO, r.type('x'));
        assertEquals(1, r.cursor(), "the cursor stays on the missed letter");
        assertEquals(1, r.typos());
        assertEquals(80, r.remaining());
        assertEquals(Recitation.Result.DONE, r.type('b'));
        assertEquals(Recitation.Result.IGNORED, r.type('c'), "nothing more to type once spoken");
        assertEquals(Recitation.Result.IGNORED, new Recitation("ab", 100, 20).type(' '), "spaces never count");
    }

    @Test
    void runningOutOfTimeExpires() {
        Recitation r = new Recitation("abc", 50, 20);
        r.type('a');
        r.tick(30);
        assertFalse(r.expired());
        r.type('q');
        assertTrue(r.expired(), "30 ticks spent + one 20-tick typo use up 50");
        assertEquals(Recitation.Result.IGNORED, r.type('b'), "too late");
        assertFalse(r.done());
    }

    @Test
    void finishingStopsTheClock() {
        Recitation r = typeAll(new Recitation("ab", 50, 20), "ab");
        r.tick(500);
        assertTrue(r.done());
        assertFalse(r.expired());
    }

    @Test
    void displayIndexMapsTheOriginalText() {
        Recitation r = new Recitation("Æ b, ç", 400, 20);
        // Æ -> "ae" (0..1), ' ' -> 2, b -> 2, ',' -> 3, ' ' -> 3, ç -> 3
        assertEquals(0, r.displayIndex(0));
        assertEquals(2, r.lettersAt(0));
        assertEquals(0, r.lettersAt(1));
        assertEquals(2, r.displayIndex(2));
        assertEquals(3, r.displayIndex(5));
        assertEquals("aebc", r.target());
        r.type('a');
        assertTrue(r.currentAt(0), "half of æ typed: still current");
        assertFalse(r.typedAt(0));
        r.type('e');
        assertTrue(r.typedAt(0));
        assertTrue(r.currentAt(2));
        assertFalse(r.currentAt(1), "spaces are never current");
    }

    @Test
    void serverPlausibility() {
        // 10 letters, 100 ticks allowed, 20 per typo, 40 grace.
        assertTrue(Recitation.plausible(10, 100, 20, 40, 60, 2), "60 + 40 <= 140");
        assertFalse(Recitation.plausible(10, 100, 20, 40, 120, 2), "120 + 40 > 140");
        assertFalse(Recitation.plausible(10, 100, 20, 40, 1, 0), "10 letters in one tick is not human");
        assertTrue(Recitation.plausible(10, 100, 20, 40, 3, 0));
        assertFalse(Recitation.plausible(10, 100, 20, 40, 50, -1), "negative typos are a forgery");
    }
}
