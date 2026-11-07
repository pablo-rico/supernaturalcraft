package org.papiricoh.supernaturalcraft.apocalypse.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A hunter's own share of the seals (v0.40, the Seals slice), an attachment kept through death: the events held, the
 * seals let break, the angel-family events held (two open the way to Uriel) and the game time of the last event they
 * were called to (the spawner's throttle). Fields added later read with defaults (old worlds lack them):
 * {@code kindsHeld} (a bit per {@link SealKind} held, for the journal), {@code day}/{@code today} (the events they were called to
 * on that day: {@link SealRules#MAX_PER_DAY}), {@code compass} (the first call brought them a Seal Compass), {@code envoy}
 * (Castiel has come to warn them of the traitor: 0 not yet, 1 came, 2 heard him out).
 */
public record SealRecord(int defended, int letBreak, int angelsDefended, long lastEvent, int kindsHeld, long day, int today,
                         boolean compass, int envoy) {

    public static final SealRecord EMPTY = new SealRecord(0, 0, 0, -1L, 0, -1L, 0, false, 0);

    public static final Codec<SealRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("defended", 0).forGetter(SealRecord::defended),
            Codec.INT.optionalFieldOf("let_break", 0).forGetter(SealRecord::letBreak),
            Codec.INT.optionalFieldOf("angels_defended", 0).forGetter(SealRecord::angelsDefended),
            Codec.LONG.optionalFieldOf("last_event", -1L).forGetter(SealRecord::lastEvent),
            Codec.INT.optionalFieldOf("kinds_held", 0).forGetter(SealRecord::kindsHeld),
            Codec.LONG.optionalFieldOf("day", -1L).forGetter(SealRecord::day),
            Codec.INT.optionalFieldOf("today", 0).forGetter(SealRecord::today),
            Codec.BOOL.optionalFieldOf("compass", false).forGetter(SealRecord::compass),
            Codec.INT.optionalFieldOf("envoy", 0).forGetter(SealRecord::envoy)
    ).apply(i, SealRecord::new));

    /** Called to an event at game time {@code now} on {@code day}. */
    public SealRecord called(long now, long dayIndex) {
        int t = dayIndex == day ? today + 1 : 1;
        return new SealRecord(defended, letBreak, angelsDefended, now, kindsHeld, dayIndex, t, compass, envoy);
    }

    /** The events called on {@code dayIndex}. */
    public int calledOn(long dayIndex) {
        return dayIndex == day ? today : 0;
    }

    public SealRecord held(SealKind kind) {
        return new SealRecord(defended + 1, letBreak, angelsDefended + (kind.family == SealFamily.ANGELS ? 1 : 0), lastEvent,
                kindsHeld | 1 << kind.ordinal(), day, today, compass, envoy);
    }

    public SealRecord letBroken() {
        return new SealRecord(defended, letBreak + 1, angelsDefended, lastEvent, kindsHeld, day, today, compass, envoy);
    }

    public SealRecord withCompass() {
        return new SealRecord(defended, letBreak, angelsDefended, lastEvent, kindsHeld, day, today, true, envoy);
    }

    public SealRecord withEnvoy(int e) {
        return new SealRecord(defended, letBreak, angelsDefended, lastEvent, kindsHeld, day, today, compass, Math.max(envoy, e));
    }

    public SealRecord withAngels(int n) {
        return new SealRecord(defended, letBreak, n, lastEvent, kindsHeld, day, today, compass, envoy);
    }

    public boolean heldKind(SealKind kind) {
        return (kindsHeld & 1 << kind.ordinal()) != 0;
    }
}
