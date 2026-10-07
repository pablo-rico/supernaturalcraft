package org.papiricoh.supernaturalcraft.bowl;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Speaking a bowl spell's incantation, letter by letter, against the clock. Pure (no Minecraft
 * types): the recitation screen drives one and the server re-checks its claims with the same
 * numbers ({@link #timeFor}, {@link #PENALTY_TICKS}).
 *
 * <p>Typing is tolerant: case and accents never matter ({@code é} is {@code e}), ligatures spell
 * out ({@code æ} is {@code ae}), classical spellings are interchangeable ({@code j} is {@code i},
 * {@code v} is {@code u}), and spaces and punctuation are optional. A wrong letter is a typo: it
 * does not advance, and it costs {@link #PENALTY_TICKS} off the remaining time. The recitation is
 * {@link #done()} once every letter has been typed, and {@link #expired()} if time runs out first.
 */
public final class Recitation {

    /** Ticks a typo costs. */
    public static final int PENALTY_TICKS = 20;
    /** Base time to begin, and time per letter, before the spell's difficulty multiplies them. */
    public static final int BASE_TICKS = 40, TICKS_PER_LETTER = 8;

    public enum Result {
        /** The letter was right; more to go. */
        LETTER,
        /** The last letter was right: the incantation is spoken. */
        DONE,
        /** Wrong letter: time lost, the cursor stays. */
        TYPO,
        /** Nothing to type (a space, punctuation) or the recitation is already over. */
        IGNORED
    }

    private final String original;
    private final String target;
    /** For each char of {@link #original}: where its letters start in {@link #target}, and how many there are. */
    private final int[] start, count;
    private final int allowedTicks, penaltyTicks;
    private int cursor;
    private int typos;
    private int elapsed;

    public Recitation(String incantation, int allowedTicks, int penaltyTicks) {
        this.original = incantation;
        this.allowedTicks = allowedTicks;
        this.penaltyTicks = penaltyTicks;
        this.start = new int[incantation.length()];
        this.count = new int[incantation.length()];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < incantation.length(); i++) {
            String n = normalize(String.valueOf(incantation.charAt(i)));
            start[i] = sb.length();
            count[i] = n.length();
            sb.append(n);
        }
        this.target = sb.toString();
    }

    // --- pure helpers -----------------------------------------------------------------------

    /**
     * The letters of {@code text} as they must be typed: accents stripped, lower case, ligatures
     * spelled out, j→i and v→u, and anything that is not a letter a–z dropped.
     */
    public static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = Character.toLowerCase(decomposed.charAt(i));
            switch (c) {
                case 'æ' -> out.append("ae");
                case 'œ' -> out.append("oe");
                case 'ß' -> out.append("ss");
                case 'j' -> out.append('i');
                case 'v' -> out.append('u');
                default -> {
                    if (c >= 'a' && c <= 'z') out.append(c);
                }
            }
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    /** How many letters must be typed to speak {@code phrase}. */
    public static int letters(String phrase) {
        return normalize(phrase).length();
    }

    /** Ticks allowed to recite {@code phrase}: {@code (40 + 8 per letter) × difficulty}, rounded. */
    public static int timeFor(String phrase, float difficulty) {
        return Math.round((BASE_TICKS + letters(phrase) * TICKS_PER_LETTER) * difficulty);
    }

    // --- typing -------------------------------------------------------------------------------

    /** Types one character (as the keyboard gave it). */
    public Result type(char c) {
        if (over()) return Result.IGNORED;
        String letters = normalize(String.valueOf(c));
        if (letters.isEmpty()) return Result.IGNORED;
        for (int i = 0; i < letters.length() && cursor < target.length(); i++) {
            if (letters.charAt(i) != target.charAt(cursor)) {
                typos++;
                return Result.TYPO;
            }
            cursor++;
        }
        return done() ? Result.DONE : Result.LETTER;
    }

    /** Lets {@code ticks} pass. */
    public void tick(int ticks) {
        if (!over()) elapsed += ticks;
    }

    public boolean done() {
        return cursor >= target.length();
    }

    /** Time ran out before the last letter. */
    public boolean expired() {
        return !done() && remaining() <= 0;
    }

    public boolean over() {
        return done() || expired();
    }

    /** Ticks left: the time allowed, less the time spent and the typos' penalties. */
    public int remaining() {
        return allowedTicks - elapsed - typos * penaltyTicks;
    }

    /** Remaining time as a fraction of the time allowed, 0..1. */
    public float timeFraction() {
        return allowedTicks <= 0 ? 0 : Math.max(0, Math.min(1, remaining() / (float) allowedTicks));
    }

    public int typos() {
        return typos;
    }

    public int elapsed() {
        return elapsed;
    }

    /** Letters typed so far. */
    public int cursor() {
        return cursor;
    }

    public int length() {
        return target.length();
    }

    public String original() {
        return original;
    }

    /** The letters to type, normalized. */
    public String target() {
        return target;
    }

    public int allowedTicks() {
        return allowedTicks;
    }

    public int penaltyTicks() {
        return penaltyTicks;
    }

    // --- display -------------------------------------------------------------------------------

    /**
     * Where the {@code i}-th character of the original text sits among the letters to type (its
     * first letter), so the screen can light up the text with its accents as typed. A space or
     * punctuation mark gets the position of the next letter.
     */
    public int displayIndex(int i) {
        return start[i];
    }

    /** How many letters the {@code i}-th original character stands for (0 for spaces, 2 for æ). */
    public int lettersAt(int i) {
        return count[i];
    }

    /** Whether every letter of the {@code i}-th original character has been typed. */
    public boolean typedAt(int i) {
        return count[i] > 0 && start[i] + count[i] <= cursor;
    }

    /** Whether the cursor is on the {@code i}-th original character. */
    public boolean currentAt(int i) {
        return count[i] > 0 && cursor >= start[i] && cursor < start[i] + count[i];
    }

    // --- server checks ---------------------------------------------------------------------

    /**
     * Whether a claimed success could be honest: the recitation took {@code elapsed} ticks with
     * {@code typos} mistakes, so it must have fitted the time allowed (plus {@code grace} for the
     * round trip), and nobody types more than {@link #MAX_LETTERS_PER_TICK} letters a tick.
     */
    public static boolean plausible(int letters, int allowedTicks, int penaltyTicks, int grace, long elapsed, int typos) {
        if (typos < 0) return false;
        if (elapsed + (long) typos * penaltyTicks > allowedTicks + grace) return false;
        return elapsed * MAX_LETTERS_PER_TICK >= letters;
    }

    /** No one types faster than this (about 80 letters a second): anything quicker is a forged result. */
    public static final int MAX_LETTERS_PER_TICK = 4;
}
