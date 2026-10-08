package org.papiricoh.supernaturalcraft.reward.michael;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What Heaven owes a hunter (a player attachment, kept through death): which pieces of the General's armour Michael has
 * already left them ({@code armour}, one bit per piece, helmet first), whether they took in his Grace, and whether they
 * have flown on it yet. Pure: the next piece is {@link #nextPiece}, and once all four have been given the round starts again.
 */
public record HeavenLedger(int armour, boolean grace, boolean flown) {

    public static final HeavenLedger NONE = new HeavenLedger(0, false, false);
    /** Pieces of the set, in the order they are given: helmet, chestplate, leggings, boots. */
    public static final int PIECES = 4;
    public static final int FULL = (1 << PIECES) - 1;

    public static final Codec<HeavenLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("armour", 0).forGetter(HeavenLedger::armour),
            Codec.BOOL.optionalFieldOf("grace", false).forGetter(HeavenLedger::grace),
            Codec.BOOL.optionalFieldOf("flown", false).forGetter(HeavenLedger::flown)
    ).apply(i, HeavenLedger::new));

    /** The piece Michael leaves this hunter next (0 helmet … 3 boots): the first not yet given. */
    public int nextPiece() {
        int given = (armour & FULL) == FULL ? 0 : armour;
        for (int p = 0; p < PIECES; p++) if ((given & (1 << p)) == 0) return p;
        return 0;
    }

    /** The ledger once {@code piece} has been given (a full set starts the round again). */
    public HeavenLedger give(int piece) {
        int given = (armour & FULL) == FULL ? 0 : armour;
        return new HeavenLedger(given | (1 << piece), grace, flown);
    }

    public boolean has(int piece) {
        return (armour & (1 << piece)) != 0;
    }

    public int given() {
        return Integer.bitCount(armour & FULL);
    }

    public HeavenLedger withGrace(boolean g) {
        return new HeavenLedger(armour, g, flown);
    }

    public HeavenLedger withFlown(boolean f) {
        return new HeavenLedger(armour, grace, f);
    }
}
