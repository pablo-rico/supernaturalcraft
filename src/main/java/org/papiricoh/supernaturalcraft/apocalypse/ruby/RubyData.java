package org.papiricoh.supernaturalcraft.apocalypse.ruby;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Where a hunter stands with Ruby (v0.40), an attachment kept through death: the step of her road (an ordinal of
 * {@code RubyRules.Step}, append-only), the day she last came, the talks already had (bits) and {@link #flags}: one-off
 * bits such as what the v0.40 migration already did for this hunter ({@link #MIGRATED}, {@link #SHACKLE_GIVEN}).
 */
public record RubyData(int step, long lastVisit, int scenes, int flags) {

    /** The migration has looked at this hunter. */
    public static final int MIGRATED = 1;
    /** A hunter who beat the old (pre-v0.40) Lucifer was given the Cage Shackle. */
    public static final int SHACKLE_GIVEN = 2;

    public static final RubyData EMPTY = new RubyData(0, -100L, 0, 0);

    public static final Codec<RubyData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("step", 0).forGetter(RubyData::step),
            Codec.LONG.optionalFieldOf("last_visit", -100L).forGetter(RubyData::lastVisit),
            Codec.INT.optionalFieldOf("scenes", 0).forGetter(RubyData::scenes),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(RubyData::flags)
    ).apply(i, RubyData::new));

    public RubyData withStep(int s) {
        return new RubyData(s, lastVisit, scenes, flags);
    }

    public RubyData withVisit(long day) {
        return new RubyData(step, day, scenes, flags);
    }

    public RubyData withScenes(int bits) {
        return new RubyData(step, lastVisit, bits, flags);
    }

    public RubyData withFlag(int flag) {
        return new RubyData(step, lastVisit, scenes, flags | flag);
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }
}
