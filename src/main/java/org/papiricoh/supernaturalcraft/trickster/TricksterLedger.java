package org.papiricoh.supernaturalcraft.trickster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What the Trickster has done to a hunter (v0.14; attachment {@code TRICKSTER}, survives death): how many of his pranks they
 * have noticed, which day of the world he last played one, and how many times they have beaten him (the first victory gives
 * everything, later ones a share: {@code GabrielSpoils}). Immutable.
 *
 * @param sightings pranks noticed (from {@link PrankRules#SIGHTINGS_NEEDED} the bait can be made)
 * @param lastPrankDay the world day ({@code dayTime / 24000}) of the last prank, -1 if none yet
 * @param victories times this hunter has beaten Gabriel
 */
public record TricksterLedger(int sightings, long lastPrankDay, int victories) {

    public static final TricksterLedger NONE = new TricksterLedger(0, -1, 0);

    public static final Codec<TricksterLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("sightings", 0).forGetter(TricksterLedger::sightings),
            Codec.LONG.optionalFieldOf("last_prank_day", -1L).forGetter(TricksterLedger::lastPrankDay),
            Codec.INT.optionalFieldOf("victories", 0).forGetter(TricksterLedger::victories)
    ).apply(i, TricksterLedger::new));

    public TricksterLedger sighted(long day) {
        return new TricksterLedger(sightings + 1, day, victories);
    }

    public TricksterLedger won() {
        return new TricksterLedger(sightings, lastPrankDay, victories + 1);
    }

    public boolean onHisTrail() {
        return sightings >= PrankRules.SIGHTINGS_NEEDED;
    }
}
