package org.papiricoh.supernaturalcraft.prophecy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Reading the Word of God (v0.36), pure: each chapter of a tablet is a Latin sentence ({@link #phrase}) written in the 26
 * glyphs of the Word, one glyph per letter. Which glyph stands for which letter ({@link #permutation}) is the tablet's own
 * for each reader in each world, so no two hunters share a key. A reader sees the letters they know (a 26-bit mask: murals,
 * confessions, visions, earlier chapters) and must work out the rest; a try is judged by {@link #wrong}. Reading a chapter
 * teaches every letter in it ({@link #lettersOf}), so each chapter makes the next easier.
 */
public final class WordCipher {

    public static final int LETTERS = 26;
    /** Letters a reader is shown free when opening a chapter: a prophet sees more. */
    public static final int FREE_LETTERS = 2, PROPHET_FREE_LETTERS = 5;
    /** A failed try shows a prophet this many more letters. */
    public static final int PROPHET_REVEAL = 1;

    private static final String[][] PHRASES = {
            // The Angel Tablet: the Fall, the ward that keeps a house, the casting out of the children of light.
            {"CADENT DE CAELO ALAE EORUM", "SIGNUM SANCTUM CUSTODIT DOMUM", "EXPELLE OMNES FILIOS LUCIS"},
            // The Demon Tablet: the great exorcism, the sign cut in stone, the three trials that shut the gates of Hell.
            {"EXI OMNIS SPIRITUS IMMUNDUS", "SIGNUM IN LAPIDE NON DELEBITUR", "TRES PROBATIONES CLAUDENT PORTAS INFERI"},
            // The Leviathan Tablet: the first beasts bound, sealed in a jar, Purgatory remembered.
            {"PRIMAS BESTIAS LIGA IN PURGATORIO", "ANIMAM MONSTRI IN VASE SIGILLA", "MEMENTO SILVAE OBSCURAE"}};

    private WordCipher() {
    }

    /** The sentence of {@code chapter} (1..{@link Tablet#CHAPTERS}) of {@code t}. */
    public static String phrase(Tablet t, int chapter) {
        return PHRASES[t.ordinal()][Math.max(1, Math.min(Tablet.CHAPTERS, chapter)) - 1];
    }

    /** The key of one reader of one tablet in one world. */
    public static long seed(long worldSeed, UUID reader, Tablet t) {
        return worldSeed * 31 + reader.getMostSignificantBits() * 17 + reader.getLeastSignificantBits() + t.ordinal() * 0x9E3779B97F4A7C15L;
    }

    /** letter (0 = A) → glyph, a shuffle of 0..25. */
    public static int[] permutation(long seed) {
        int[] perm = new int[LETTERS];
        for (int i = 0; i < LETTERS; i++) perm[i] = i;
        Random r = new Random(seed);
        for (int i = LETTERS - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = perm[i];
            perm[i] = perm[j];
            perm[j] = t;
        }
        return perm;
    }

    /** glyph → letter, the inverse of {@link #permutation}. */
    public static int[] inverse(int[] perm) {
        int[] inv = new int[LETTERS];
        for (int l = 0; l < LETTERS; l++) inv[perm[l]] = l;
        return inv;
    }

    /** The sentence as glyphs, {@code -1} for a space. */
    public static int[] glyphs(String phrase, int[] perm) {
        int[] out = new int[phrase.length()];
        for (int i = 0; i < phrase.length(); i++) {
            int l = letter(phrase.charAt(i));
            out[i] = l < 0 ? -1 : perm[l];
        }
        return out;
    }

    private static int letter(char c) {
        return c >= 'A' && c <= 'Z' ? c - 'A' : -1;
    }

    /** The letters a sentence uses, as a mask. */
    public static int lettersOf(String phrase) {
        int mask = 0;
        for (int i = 0; i < phrase.length(); i++) {
            int l = letter(phrase.charAt(i));
            if (l >= 0) mask |= 1 << l;
        }
        return mask;
    }

    /**
     * How many of the sentence's distinct glyphs a reader got wrong. {@code guess} is glyph → letter (−1 = left blank, which
     * counts as wrong).
     */
    public static int wrong(String phrase, int[] perm, int[] guess) {
        int mask = lettersOf(phrase), wrong = 0;
        for (int l = 0; l < LETTERS; l++) {
            if ((mask & 1 << l) == 0) continue;
            int g = perm[l];
            if (g >= guess.length || guess[g] != l) wrong++;
        }
        return wrong;
    }

    /**
     * Up to {@code count} letters of the sentence the reader does not know yet, the most frequent first (ties by a stable
     * draw from {@code seed}), as a mask.
     */
    public static int hints(String phrase, int known, int count, long seed) {
        int[] freq = new int[LETTERS];
        for (int i = 0; i < phrase.length(); i++) {
            int l = letter(phrase.charAt(i));
            if (l >= 0 && (known & 1 << l) == 0) freq[l]++;
        }
        Random r = new Random(seed);
        double[] score = new double[LETTERS];
        for (int l = 0; l < LETTERS; l++) score[l] = freq[l] + r.nextDouble() * 0.5;
        int out = 0;
        for (int k = 0; k < count; k++) {
            int best = -1;
            for (int l = 0; l < LETTERS; l++) {
                if (freq[l] == 0 || (out & 1 << l) != 0) continue;
                if (best < 0 || score[l] > score[best]) best = l;
            }
            if (best < 0) break;
            out |= 1 << best;
        }
        return out;
    }

    /** {@code count} distinct letters drawn from {@code seed} (a mural's lesson), as a mask. */
    public static int lettersFrom(long seed, int count) {
        Random r = new Random(seed);
        int out = 0;
        while (Integer.bitCount(out) < Math.min(count, LETTERS)) out |= 1 << r.nextInt(LETTERS);
        return out;
    }

    /** "A, E, S" for a mask. */
    public static String letters(int mask) {
        List<String> out = new ArrayList<>();
        for (int l = 0; l < LETTERS; l++) if ((mask & 1 << l) != 0) out.add(String.valueOf((char) ('A' + l)));
        return String.join(", ", out);
    }
}
