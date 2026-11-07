package org.papiricoh.supernaturalcraft.pact;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Where a hunter stands in season 6 (v0.39), an attachment kept through death: the {@link PactRules.Step} (by ordinal), the
 * Alphas brought down (a mask of {@link PactRules.Alpha} bits), the day Castiel last came (or the pact last moved) and the
 * talks already had ({@link #scenes}, bits of {@link PactDialogue}), and whether they took in a Shard of Castiel's Grace.
 */
public record Pact(int step, int alphas, long lastVisit, int scenes, boolean grace) {

    public static final Pact EMPTY = new Pact(0, 0, -100L, 0, false);

    public static final Codec<Pact> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("step", 0).forGetter(Pact::step),
            Codec.INT.optionalFieldOf("alphas", 0).forGetter(Pact::alphas),
            Codec.LONG.optionalFieldOf("last_visit", -100L).forGetter(Pact::lastVisit),
            Codec.INT.optionalFieldOf("scenes", 0).forGetter(Pact::scenes),
            Codec.BOOL.optionalFieldOf("grace", false).forGetter(Pact::grace)
    ).apply(i, Pact::new));

    public PactRules.Step stage() {
        return PactRules.Step.of(step);
    }

    public Pact withStep(PactRules.Step s) {
        return new Pact(s.ordinal(), alphas, lastVisit, scenes, grace);
    }

    public Pact withAlphas(int mask) {
        return new Pact(step, mask, lastVisit, scenes, grace);
    }

    public Pact withVisit(long day) {
        return new Pact(step, alphas, day, scenes, grace);
    }

    /** Whether the hunter has taken in a Shard of Castiel's Grace. */
    public Pact withGrace(boolean taken) {
        return new Pact(step, alphas, lastVisit, scenes, taken);
    }

    public boolean seen(int scene) {
        return (scenes & scene) != 0;
    }

    public Pact withSeen(int scene) {
        return new Pact(step, alphas, lastVisit, scenes | scene, grace);
    }
}
