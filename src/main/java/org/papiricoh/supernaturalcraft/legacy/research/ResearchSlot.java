package org.papiricoh.supernaturalcraft.legacy.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One research under way (v0.17): its topic and the game ticks it started and ends ({@code level.getGameTime()}), so it goes on
 * while its hunter is away.
 */
public record ResearchSlot(String topic, long start, long end) {

    public static final Codec<ResearchSlot> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("topic").forGetter(ResearchSlot::topic),
            Codec.LONG.fieldOf("start").forGetter(ResearchSlot::start),
            Codec.LONG.fieldOf("end").forGetter(ResearchSlot::end)
    ).apply(i, ResearchSlot::new));

    public boolean done(long now) {
        return now >= end;
    }

    /** 0..1 of the way through at {@code now}. */
    public float progress(long now) {
        if (end <= start) return 1;
        return (float) Math.max(0, Math.min(1, (now - start) / (double) (end - start)));
    }
}
