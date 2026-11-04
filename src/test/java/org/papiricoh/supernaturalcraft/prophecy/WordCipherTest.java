package org.papiricoh.supernaturalcraft.prophecy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WordCipherTest {

    @Test
    void theKeyIsAShuffleAndTheSameForTheSameReader() {
        UUID u = UUID.fromString("00000000-0000-0000-0000-000000000042");
        int[] a = WordCipher.permutation(WordCipher.seed(7, u, Tablet.DEMON));
        assertArrayEquals(a, WordCipher.permutation(WordCipher.seed(7, u, Tablet.DEMON)));
        int[] sorted = a.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < WordCipher.LETTERS; i++) assertEquals(i, sorted[i]);
        assertFalse(Arrays.equals(a, WordCipher.permutation(WordCipher.seed(7, u, Tablet.ANGEL))), "two tablets share a key");
    }

    @Test
    void onlyTheRightReadingIsRight() {
        for (Tablet t : Tablet.values()) {
            for (int c = 1; c <= Tablet.CHAPTERS; c++) {
                String phrase = WordCipher.phrase(t, c);
                assertTrue(phrase.matches("[A-Z ]+"), phrase);
                int[] perm = WordCipher.permutation(t.ordinal() * 100L + c);
                assertEquals(0, WordCipher.wrong(phrase, perm, WordCipher.inverse(perm)));
                int[] blank = new int[WordCipher.LETTERS];
                Arrays.fill(blank, -1);
                assertEquals(Integer.bitCount(WordCipher.lettersOf(phrase)), WordCipher.wrong(phrase, perm, blank));
            }
        }
    }

    @Test
    void hintsAreUnknownLettersOfTheSentence() {
        String phrase = WordCipher.phrase(Tablet.DEMON, 3);
        int all = WordCipher.lettersOf(phrase);
        int known = 1 << ('S' - 'A');
        int h = WordCipher.hints(phrase, known, WordCipher.PROPHET_FREE_LETTERS, 9);
        assertEquals(WordCipher.PROPHET_FREE_LETTERS, Integer.bitCount(h));
        assertEquals(h, h & all & ~known);
        assertEquals(h, WordCipher.hints(phrase, known, WordCipher.PROPHET_FREE_LETTERS, 9));
        assertEquals(3, Integer.bitCount(WordCipher.lettersFrom(5, 3)));
    }
}
