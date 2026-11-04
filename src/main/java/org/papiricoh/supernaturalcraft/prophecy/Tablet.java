package org.papiricoh.supernaturalcraft.prophecy;

import java.util.Locale;

/**
 * The tablets of the Word of God (v0.36), each read chapter by chapter ({@link #CHAPTERS}) through {@link WordCipher}.
 * Append only: the ordinal is saved in {@link Prophecy}.
 */
public enum Tablet {
    ANGEL, DEMON, LEVIATHAN;

    public static final int CHAPTERS = 3;
    /** The Angel Tablet's chapter that makes a painted ward supreme. */
    public static final int SUPREME_WARD_CHAPTER = 2;
    /** The Demon Tablet's chapter that teaches the engraved devil's trap, and the one that opens the Trials. */
    public static final int ENGRAVING_CHAPTER = 2, TRIALS_CHAPTER = 3;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Tablet byOrdinal(int i) {
        Tablet[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
