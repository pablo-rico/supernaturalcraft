package org.papiricoh.supernaturalcraft.prophecy;

import java.util.Locale;

/**
 * The tablets of the Word of God (v0.36), each read chapter by chapter ({@link #CHAPTERS}) through {@link WordCipher}.
 * Append only: the ordinal is saved in {@link Prophecy}.
 */
public enum Tablet {
    ANGEL, DEMON, LEVIATHAN;

    public static final int CHAPTERS = 6;
    /** The Angel Tablet's chapter that makes a painted ward supreme. */
    public static final int SUPREME_WARD_CHAPTER = 3;
    /** The Demon Tablet's chapter that teaches the engraved devil's trap, and the one that opens the Trials. */
    public static final int ENGRAVING_CHAPTER = 2, TRIALS_CHAPTER = 6;
    /** The Leviathan Tablet's chapter after which what comes out of a Purgatory jar fights for whoever opened it. */
    public static final int ALLY_CHAPTER = 4;

    /**
     * Days a tablet rests after its chapter {@code read} was read right, before the next one shows (v0.36): one after the
     * first two, then two, three and six; none after the last.
     */
    public static int restDays(int read) {
        return switch (read) {
            case 1, 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            case 5 -> 6;
            default -> 0;
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Tablet byOrdinal(int i) {
        Tablet[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
