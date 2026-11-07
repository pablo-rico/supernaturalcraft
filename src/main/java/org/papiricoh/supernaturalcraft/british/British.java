package org.papiricoh.supernaturalcraft.british;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Where a hunter stands with the British Men of Letters (v0.41), an attachment kept through death: their answer to Hess
 * ({@link BritishRules.Choice}, by ordinal), the day of it, the last night Ketch came for them, and one-time flags.
 */
public record British(int choice, long day, long lastHunt, int flags) {

    /** Ketch is beaten (he hunts them no more); Hess is beaten; the kit was handed over; the dossier was read; a member's bonus. */
    public static final int KETCH_BEATEN = 1, HESS_BEATEN = 2, KIT_GIVEN = 4, DOSSIER_READ = 8, LETTERS_BONUS = 16, DAGON_BEATEN = 32;

    public static final British EMPTY = new British(0, 0L, -100L, 0);

    public static final Codec<British> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("choice", 0).forGetter(British::choice),
            Codec.LONG.optionalFieldOf("day", 0L).forGetter(British::day),
            Codec.LONG.optionalFieldOf("last_hunt", -100L).forGetter(British::lastHunt),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(British::flags)
    ).apply(i, British::new));

    public BritishRules.Choice stance() {
        return BritishRules.Choice.of(choice);
    }

    public British withChoice(BritishRules.Choice c, long onDay) {
        return new British(c.ordinal(), onDay, lastHunt, flags);
    }

    public British withHunt(long onDay) {
        return new British(choice, day, onDay, flags);
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public British with(int flag) {
        return new British(choice, day, lastHunt, flags | flag);
    }
}
