package org.papiricoh.supernaturalcraft.apocalypse.blood;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A drinker's demon blood (v0.40), an attachment kept through death: the blood still in the body (0-1, it fades), the
 * dependency (0-1, it fades by the day), the lifetime doses (what unlocks the powers' tiers) and the last day the
 * dependency was let down ({@link BloodRules#fadeDays}); while shut in a Panic Room, its door ({@link #panicDoor}, a packed
 * {@code BlockPos}, 0 = none) and the day-time the cure ends ({@link #panicUntil}).
 */
public record BloodData(float blood, float dependency, int doses, long lastDay, long panicDoor, long panicUntil) {

    public static final BloodData EMPTY = new BloodData(0f, 0f, 0, -1L, 0L, -1L);

    public BloodData(float blood, float dependency, int doses, long lastDay) {
        this(blood, dependency, doses, lastDay, 0L, -1L);
    }

    public static final Codec<BloodData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("blood", 0f).forGetter(BloodData::blood),
            Codec.FLOAT.optionalFieldOf("dependency", 0f).forGetter(BloodData::dependency),
            Codec.INT.optionalFieldOf("doses", 0).forGetter(BloodData::doses),
            Codec.LONG.optionalFieldOf("last_day", -1L).forGetter(BloodData::lastDay),
            Codec.LONG.optionalFieldOf("panic_door", 0L).forGetter(BloodData::panicDoor),
            Codec.LONG.optionalFieldOf("panic_until", -1L).forGetter(BloodData::panicUntil)
    ).apply(i, BloodData::new));

    public BloodData withBlood(float b) {
        return new BloodData(b, dependency, doses, lastDay, panicDoor, panicUntil);
    }

    public BloodData withDependency(float d) {
        return new BloodData(blood, d, doses, lastDay, panicDoor, panicUntil);
    }

    public BloodData withDoses(int n) {
        return new BloodData(blood, dependency, n, lastDay, panicDoor, panicUntil);
    }

    public BloodData withLastDay(long day) {
        return new BloodData(blood, dependency, doses, day, panicDoor, panicUntil);
    }

    public BloodData withPanic(long door, long until) {
        return new BloodData(blood, dependency, doses, lastDay, door, until);
    }

    public boolean inPanicRoom() {
        return panicDoor != 0L;
    }
}
